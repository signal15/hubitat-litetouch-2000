/*******************************************************************************************
 *  LiteTouch2000 App
 *
 *  Author: Jay Austad
 *  Namespace: com.snowautomation
 *
 *  This parent app handles:
 *    - Continuous polling of LiteTouch loads via " 18" command (round-robin style)
 *    - Parsing responses, updating child devices
 *    - Setting load levels (command 10) and fades (command 11) with optional "priority" query
 *    - Splitting incoming data on carriage return (\r) only (no \n)
 *******************************************************************************************/

import hubitat.device.HubAction
import hubitat.device.Protocol
import hubitat.device.HubAction.Type

definition(
    name: "LiteTouch2000 App",
    namespace: "com.snowautomation",
    author: "Jay Austad",
    description: "Parent app for LiteTouch2000 system via ethernet-to-serial adapter with reconnection logic",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: ""
)

/*******************************************************************************************
 *  Preferences
 *******************************************************************************************/
preferences {
    section("Adapter Settings") {
        input "adapterHost", "text", title: "Ethernet Adapter IP", required: true
        input "adapterPort", "number", title: "Ethernet Adapter Port", required: true
    }
    section("Polling Settings") {
        input "pollIntervalSec", "number", title: "Polling Interval (seconds)", required: true, defaultValue: 2
    }
    section("LiteTouch Device Configuration") {
        paragraph "Enter one device per line in CSV format: <b>deviceID,type,name</b>\n" +
                  "e.g.\n" +
                  "02-1, relay, Front Porch\n" +
                  "03-1, dimmer, Living Room"
        input "deviceConfig", "textarea", title: "Device list", required: true, defaultValue: ""
    }
}

/*******************************************************************************************
 *  Lifecycle / Initialization
 *******************************************************************************************/
def installed() {
    log.debug "LiteTouch2000 App installed"
    initialize()
}

def updated() {
    log.debug "LiteTouch2000 App updated"
    unschedule()
    initialize()
}

def initialize() {
    log.debug "initialize() called"
    state.cmdBuffer       = ""
    state.queryList       = []  // array of child deviceNetworkIds
    state.priorityQuery   = null
    state.lastResponseTime= now()
    state.reconnectAttempts = 0

    parseDeviceConfig()

    // Start continuous poll loop
    runIn(pollIntervalSec, pollDevices)

    // Check connection every 10 seconds
    runIn(10, checkConnection)
}

/*******************************************************************************************
 *  parseDeviceConfig()
 *    - Creates child devices from the user-provided CSV, updates state.queryList
 *******************************************************************************************/
def parseDeviceConfig() {
    def lines = deviceConfig.split("\n")
    def configDevices = []
    lines.each { line ->
        def parts = line.split(",")
        if (parts.size() >= 3) {
            def devId   = parts[0].trim()     // e.g. "02-1"
            def typeStr = parts[1].trim().toLowerCase()  // "relay" or "dimmer"
            def nameStr = parts[2].trim()

            def prefix = ""
            if (typeStr.startsWith("r")) {  // "relay"
                prefix = "r"
            } else if (typeStr.startsWith("d")) { // "dimmer"
                prefix = "d"
            } else {
                log.warn "Unknown device type in line: ${line}"
                return
            }
            def dni = prefix + devId  // e.g. "r02-1" or "d03-1"
            configDevices << [dni: dni, name: nameStr]
        }
    }

    // Remove child devices not in config
    getChildDevices().each { child ->
        if (!configDevices.find { it.dni == child.deviceNetworkId }) {
            log.debug "Deleting child device ${child.deviceNetworkId} (no longer in config)"
            deleteChildDevice(child.deviceNetworkId)
        }
    }

    // Create/update child devices
    configDevices.each { dev ->
        def child = getChildDevice(dev.dni)
        if (!child) {
            log.debug "Creating child device ${dev.dni} (${dev.name})"
            try {
                addChildDevice(
                    "com.snowautomation",
                    "LiteTouch2000 Child",
                    dev.dni,
                    null,
                    [label: dev.name, data: [configuredId: dev.dni]]
                )
            } catch (e) {
                log.error "Error creating child device ${dev.dni}: ${e}"
            }
        } else {
            if (child.label != dev.name) {
                child.setLabel(dev.name)
            }
        }
        // Add to poll queue if not already present
        if (!state.queryList.contains(dev.dni)) {
            state.queryList << dev.dni
        }
    }
}

/*******************************************************************************************
 *  pollDevices()
 *    - Round-robin poll approach:
 *      - Poll the first device in the queue (but do not remove it yet).
 *      - Remove/re-insert in receiveCommand() after we parse the response
 *******************************************************************************************/
def pollDevices() {
    try {
        if (!state.queryList || state.queryList.isEmpty()) {
            log.debug "No devices in queryList; skipping poll"
        } else {
            def loadDni = state.queryList[0]
            pollLoad(loadDni)
        }
    } catch(e) {
        log.error "pollDevices() error: ${e}", e
    } finally {
        runIn(pollIntervalSec, pollDevices)
    }
}

def pollLoad(String dni) {
    if (!dni) return
    def idPart = dni.substring(1)
    def cmd = " 18 ${idPart}"
    log.debug "Polling load ${dni} with command: '${cmd}'"
    sendSerialCommand(cmd)
}

/*******************************************************************************************
 *  sendSerialCommand(command)
 *    - Adds carriage return, sends via HubAction with no newline
 *******************************************************************************************/
def sendSerialCommand(String command) {
    if (!command.startsWith(" ")) {
        command = " " + command
    }
    def fullCmd = command + "\r"
    log.debug "Sending command: '${fullCmd}' to ${adapterHost}:${adapterPort}"

    try {
        def hubCmd = new HubAction(
            fullCmd,
            Protocol.LAN,
            [
                type              : Type.LAN_TYPE_RAW,
                destinationAddress: "${adapterHost}:${adapterPort}"
            ]
        )
        sendHubCommand(hubCmd)
    } catch(e) {
        log.error "Error sending TCP command: ${e}"
        reconnectAdapter()
    }
}

/*******************************************************************************************
 *  parse(description)
 *    - Called whenever the CCU sends data
 *    - The CCU uses \r as the line terminator (no \n)
 *******************************************************************************************/
def parse(String description) {
    log.debug "parse() got data: ${description}"
    // We got some data => connection is alive
    state.reconnectAttempts = 0
    state.lastResponseTime  = now()

    // Accumulate in a buffer
    state.cmdBuffer = (state.cmdBuffer ?: "") + description

    // Extract lines up to \r
    while (state.cmdBuffer.contains("\r")) {
        int idx = state.cmdBuffer.indexOf("\r")
        def cmdLine = state.cmdBuffer.substring(0, idx)
        state.cmdBuffer = state.cmdBuffer.substring(idx + 1)
        receiveCommand(cmdLine)
    }
}

/*******************************************************************************************
 *  receiveCommand(cmd)
 *    - Similar to your Lua code's ltReceiveCmd()
 *******************************************************************************************/
def receiveCommand(String cmd) {
    log.debug "receiveCommand() line='${cmd}'"

    if (cmd.length() < 4) {
        log.warn "Received short command line: '${cmd}' - ignoring"
        return
    }

    // If we have a priority query, use that device; else rotate the poll queue
    def deviceDni = state.priorityQuery ?: state.queryList.remove(0)
    def devType   = deviceDni.substring(0,1) // 'r' or 'd'
    def levelStr  = cmd.substring(3, 6)      // e.g. "000","125","250"

    if (devType == "r") {
        // Relay
        def isOn = (levelStr != "000")
        log.debug "Updating relay ${deviceDni} => ${(isOn ? 'on' : 'off')}"
        def child = getChildDevice(deviceDni)
        if (child) child.updateStatus(isOn ? "on" : "off")
    } else if (devType == "d") {
        // Dimmer
        def rawVal = (levelStr.isInteger()) ? levelStr.toInteger() : 0
        def percent= Math.ceil(rawVal / 2.5) as int
        log.debug "Updating dimmer ${deviceDni} => ${percent}%"
        def child = getChildDevice(deviceDni)
        if (child) child.updateStatus(percent)
    }

    // If not priority, re-insert to end of queue
    if (!state.priorityQuery) {
        state.queryList << deviceDni
    } else {
        state.priorityQuery = null
    }
}

/*******************************************************************************************
 *  checkConnection()
 *    - Runs every 10s to confirm we got data recently
 *******************************************************************************************/
def checkConnection() {
    def nowTime = now()
    // Timeout after 5 poll intervals
    def timeout = pollIntervalSec * 5 * 1000
    if ((nowTime - state.lastResponseTime) > timeout) {
        log.warn "No response from CCU for ${nowTime - state.lastResponseTime}ms, reconnecting"
        reconnectAdapter()
    }
    runIn(10, checkConnection)
}

/*******************************************************************************************
 *  reconnectAdapter()
 *******************************************************************************************/
def reconnectAdapter() {
    state.reconnectAttempts = (state.reconnectAttempts ?: 0) + 1
    if (state.reconnectAttempts > 5) {
        log.error "Reconnect failed 5 times; giving up"
        return
    }
    log.warn "Attempting reconnection (#${state.reconnectAttempts}) in 10s..."
    runIn(10, doReconnectionStep)
}

def doReconnectionStep() {
    if (state.queryList && state.queryList.size() > 0) {
        def firstDevice = state.queryList[0]
        log.debug "Reconnection step: pollLoad(${firstDevice})"
        pollLoad(firstDevice)
    } else {
        log.warn "No devices available to test reconnection"
    }
}

/*******************************************************************************************
 *  sendDeviceCommand() Overloads
 *
 *  Overload #1: (deviceDni, commandType, value)
 *     - "switch" => " 10 <module>-<load> 000|001" or "000|250"
 *     - "setLevel" => " 10 <module>-<load> <000..250>"
 *******************************************************************************************/
def sendDeviceCommand(String deviceDni, String commandType, value) {
    if (!deviceDni) return
    def idPart = deviceDni.substring(1)

    if (commandType == "switch") {
        // For relays => 000=off, 001=on
        // For dimmers => 000=off, 250=on
        def cmd = " 10 ${idPart} "
        if (deviceDni.startsWith("r")) {
            cmd += (value.toString() == "0") ? "000" : "001"
        } else if (deviceDni.startsWith("d")) {
            cmd += (value.toString() == "0") ? "000" : "250"
        }
        log.debug "Switch command => ${cmd}"
        state.priorityQuery = deviceDni
        sendSerialCommand(cmd)

    } else if (commandType == "setLevel") {
        // 0..100 => 000..250
        def computed = Math.round((value as BigDecimal) * 2.5) as int
        def raw = String.format("%03d", computed)
        def cmd = " 10 ${idPart} ${raw}"
        log.debug "SetLevel command => ${cmd}"
        state.priorityQuery = deviceDni
        sendSerialCommand(cmd)

    } else {
        log.warn "Unknown commandType: ${commandType}"
    }
}

/*******************************************************************************************
 *  Overload #2: (deviceDni, commandType, Map params)
 *    - e.g. "setLevelWithFade"
 *******************************************************************************************/
def sendDeviceCommand(String deviceDni, String commandType, Map params) {
    if (commandType == "setLevelWithFade") {
        if (!deviceDni.startsWith("d")) {
            log.warn "Fade requested on a relay device => ignoring"
            return
        }
        // " 11 <id> <level> <seconds>"
        def idPart = deviceDni.substring(1)
        def lvl = params.level as BigDecimal
        def computedLevel = Math.round(lvl * 2.5) as int
        def rawLvl  = String.format("%03d", computedLevel)

        def fadeSec = params.duration ? params.duration.toInteger() : 0
        if (fadeSec < 0) fadeSec = 0
        if (fadeSec > 3600) fadeSec = 3600
        def rawFade = String.format("%04d", fadeSec)

        def cmd = " 11 ${idPart} ${rawLvl} ${rawFade}"
        log.debug "SetLevelWithFade => ${cmd}"
        state.priorityQuery = deviceDni
        sendSerialCommand(cmd)
    }
    else {
        log.warn "Unknown map-based commandType: ${commandType}"
    }
}
