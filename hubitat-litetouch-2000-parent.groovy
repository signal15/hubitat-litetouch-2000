metadata {
    definition(name: "LiteTouch2000 Ethernet Driver (No Hub Param)",
               namespace: "com.snowautomation",
               author: "Jay Austad") {
        capability "Initialize"
        capability "Configuration"
    }

    preferences {
        input name: "adapterHost",    type: "text",   title: "Ethernet-Serial Adapter IP", required: true
        input name: "adapterPort",    type: "text",   title: "Adapter TCP Port", required: true, defaultValue: "2001"
        input name: "pollIntervalSec",type: "number", title: "Polling Interval (seconds)", required: true, defaultValue: 2

        // One-time CSV input so we don't store big multiline text in normal preferences
        input name: "csvOneTime", type: "textarea", title: "LiteTouch Device CSV (One-Time Paste)",
              description: "Paste your CSV lines here, then Save. The driver will store it in state.liteTouchCSV, then clear this.",
              required: false
    }
}

// Called once when installing
def installed() {
    log.debug "[LiteTouch2000] installed()"
    initialize()
}

// Called on every Save
def updated() {
    log.debug "[LiteTouch2000] updated()"

    // If user pasted new CSV, store it in state, then clear preference
    if (csvOneTime) {
        log.debug "[LiteTouch2000] updated(): new CSV length=${csvOneTime.size()}"
        try {
            state.liteTouchCSV = csvOneTime
            log.debug "[LiteTouch2000] stored CSV in state (len=${state.liteTouchCSV.size()})"
        } catch(e) {
            log.error "[LiteTouch2000] Error storing csvOneTime: ${e}"
        }
        // Clear it so we don't keep re-saving that big text
        try {
            device.updateSetting("csvOneTime", [value:"", type:"text"])
            log.debug "[LiteTouch2000] cleared csvOneTime preference"
        } catch(e2) {
            log.error "[LiteTouch2000] Error clearing csvOneTime: ${e2}"
        }
    } else {
        log.debug "[LiteTouch2000] No new CSV pasted."
    }

    // Create or update child devices from state
    try {
        createOrUpdateChildDevicesFromState()
    } catch(e3) {
        log.error "[LiteTouch2000] Exception in createOrUpdateChildDevicesFromState: ${e3}"
    }

    // Initialize connection + poll
    initialize()
}

// Called when user hits "Configure"
def configure() {
    log.debug "[LiteTouch2000] configure()"
    if (csvOneTime) {
        log.debug "[LiteTouch2000] configure(): new CSV length=${csvOneTime.size()}"
        try {
            state.liteTouchCSV = csvOneTime
        } catch(e1) {
            log.error "[LiteTouch2000] configure(): Error storing CSV: ${e1}"
        }
        try {
            device.updateSetting("csvOneTime", [value:"", type:"text"])
        } catch(e2) {
            log.error "[LiteTouch2000] configure(): Error clearing csvOneTime: ${e2}"
        }
    }
    try {
        createOrUpdateChildDevicesFromState()
    } catch(e3) {
        log.error "[LiteTouch2000] configure(): createOrUpdateChildDevicesFromState: ${e3}"
    }
    initialize()
}

/************************************************************************************************
 *  INITIALIZE
 ***********************************************************************************************/
def initialize() {
    log.debug "[LiteTouch2000] initialize()"
    closeConnection()

    state.cmdBuffer         = ""
    state.queryList         = state.queryList ?: []
    state.priorityQuery     = null
    state.lastResponseTime  = now()
    state.reconnectAttempts = 0

    // Convert port from text -> integer
    def rawPort = adapterPort?.replaceAll(",", "")?.trim()
    if (!rawPort?.isInteger()) {
        log.warn "[LiteTouch2000] Invalid port '${adapterPort}', using 2001"
        rawPort = "2001"
    }
    def portNum = rawPort.toInteger()

    log.info "[LiteTouch2000] Opening raw socket to ${adapterHost}:${portNum}"
    try {
        // 3-argument connect: (host, port, [options])
        interfaces.rawSocket.connect(adapterHost, portNum, [byteInterface:true])
    } catch(e) {
        log.error "[LiteTouch2000] initialize(): Error opening socket: ${e}"
    }

    // Start poll loop
    unschedule(pollDevices)
    runIn(pollIntervalSec, pollDevices)

    // Connection check
    unschedule(checkConnection)
    runIn(10, checkConnection)
}

/************************************************************************************************
 *  CREATE OR UPDATE CHILD DEVICES (no hub param)
 ***********************************************************************************************/
private createOrUpdateChildDevicesFromState() {
    log.debug "[LiteTouch2000] createOrUpdateChildDevicesFromState()"
    if (!state.liteTouchCSV) {
        log.warn "[LiteTouch2000] No CSV in state.liteTouchCSV"
        return
    }

    def lines = state.liteTouchCSV.split("\\r?\\n")
    def configDevices = []
    log.debug "[LiteTouch2000] Found ${lines.size()} lines in CSV"

    lines.eachWithIndex { origLine, idx ->
        def line = origLine.trim()
        if (!line) {
            log.debug "[LiteTouch2000] Skipping blank line #${idx}"
            return
        }
        def parts = line.split(",")
        if (parts.size() < 3) {
            log.warn "[LiteTouch2000] CSV line #${idx} malformed: '${origLine}'"
            return
        }
        try {
            def devId   = parts[0].trim()
            def typeStr = parts[1].trim().toLowerCase()
            def nameStr = parts[2].trim()

            def prefix
            if (typeStr.startsWith("r")) {
                prefix = "r"
            } else if (typeStr.startsWith("d")) {
                prefix = "d"
            } else {
                log.warn "[LiteTouch2000] line #${idx}: unknown device type '${typeStr}'"
                return
            }
            def dni = prefix + devId
            configDevices << [dni: dni, name: nameStr]
            log.debug "[LiteTouch2000] line #${idx} => dni=${dni}, name='${nameStr}'"
        } catch(eLine) {
            log.warn "[LiteTouch2000] parse error line #${idx}: ${eLine}"
        }
    }

    // Remove child devices not in CSV
    def existingChildren = getChildDevices() ?: []
    existingChildren.each { child ->
        if (!configDevices.find { it.dni == child.deviceNetworkId }) {
            log.info "[LiteTouch2000] Removing child not in CSV: ${child.deviceNetworkId}"
            deleteChildDevice(child.deviceNetworkId)
        }
    }

    // Rebuild poll queue
    state.queryList = []

    // Create/update children with 3-argument addChildDevice()
    configDevices.each { dev ->
        def dni  = dev.dni
        def name = dev.name
        def child = getChildDevice(dni)
        if (!child) {
            log.info "[LiteTouch2000] Creating child => dni=${dni}, name='${name}'"
            try {
                // Using the older 3-param call: addChildDevice(namespace, driver, dni, Map)
                // No separate "hubId" param to cause MissingMethodException
                addChildDevice(
                    "com.snowautomation",  // namespace
                    "LiteTouch2000 Child", // driver name
                    dni,
                    [                      // properties map
                        name: name,
                        label: name,
                        isComponent: false
                    ]
                )
            } catch(e2) {
                log.error "[LiteTouch2000] Error creating child dni=${dni}: ${e2}"
            }
        } else {
            if (child.getLabel() != name) {
                child.setLabel(name)
            }
        }
        // Add to poll queue
        state.queryList << dni
    }

    log.info "[LiteTouch2000] Done creating/updating children. queryList=${state.queryList}"
}

/************************************************************************************************
 *  RAW SOCKET HANDLERS
 ***********************************************************************************************/
def parse(String msg) {
    log.debug "[LiteTouch2000] parse() got data: ${msg}"
    state.lastResponseTime = now()

    state.cmdBuffer = (state.cmdBuffer ?: "") + msg
    while (state.cmdBuffer.contains("\r")) {
        int idx = state.cmdBuffer.indexOf("\r")
        def line = state.cmdBuffer.substring(0, idx)
        state.cmdBuffer = state.cmdBuffer.substring(idx + 1)
        receiveCommand(line)
    }
}

def socketStatus(String status) {
    log.warn "[LiteTouch2000] socketStatus => ${status}"
    if (status.contains("disconnect") || status.contains("error")) {
        reconnectAdapter()
    }
}

/************************************************************************************************
 *  COMMAND/PARSE LOGIC
 ***********************************************************************************************/
def receiveCommand(String cmd) {
    log.debug "[LiteTouch2000] receiveCommand('${cmd}')"
    if (cmd.size() < 4) {
        log.warn "[LiteTouch2000] short command => '${cmd}'"
        return
    }

    def deviceDni = state.priorityQuery ?: (state.queryList ? state.queryList.remove(0) : null)
    if (!deviceDni) {
        log.warn "[LiteTouch2000] no deviceDni for cmd='${cmd}'"
        return
    }

    def devType = deviceDni.substring(0,1) 
    def levelStr= cmd.substring(3,6)

    if (devType == "r") {
        // Relay
        def isOn = (levelStr != "000")
        updateChildRelay(deviceDni, isOn)
    } else if (devType == "d") {
        // Dimmer
        def rawVal = levelStr.isInteger() ? levelStr.toInteger() : 0
        def percent= Math.ceil(rawVal / 2.5) as int
        updateChildDimmer(deviceDni, percent)
    }

    if (!state.priorityQuery) {
        state.queryList << deviceDni
    } else {
        state.priorityQuery = null
    }
}

private updateChildRelay(String dni, boolean isOn) {
    def child = getChildDevice(dni)
    if (child) {
        child.updateStatus(isOn ? "on" : "off")
    }
}

private updateChildDimmer(String dni, int level) {
    def child = getChildDevice(dni)
    if (child) {
        child.updateStatus(level)
    }
}

/************************************************************************************************
 *  POLLING
 ***********************************************************************************************/
def pollDevices() {
    try {
        if (!state.queryList || state.queryList.isEmpty()) {
            log.debug "[LiteTouch2000] nothing to poll"
        } else {
            def loadDni = state.queryList[0]
            pollLoad(loadDni)
        }
    } catch(e) {
        log.error "[LiteTouch2000] pollDevices() error: ${e}"
    } finally {
        runIn(pollIntervalSec, pollDevices)
    }
}

def pollLoad(String dni) {
    if (!dni) return
    def idPart = dni.substring(1)
    def cmd = " 18 ${idPart}"
    log.debug "[LiteTouch2000] pollLoad(${dni}) => '${cmd}'"
    sendSerialCommand(cmd)
}

/************************************************************************************************
 *  RECONNECT / CHECK
 ***********************************************************************************************/
def checkConnection() {
    def nowTime = now()
    def timeout = pollIntervalSec * 5 * 1000
    if ((nowTime - state.lastResponseTime) > timeout) {
        log.warn "[LiteTouch2000] no response for ${(nowTime - state.lastResponseTime)}ms => reconnect"
        reconnectAdapter()
    }
    runIn(10, checkConnection)
}

def reconnectAdapter() {
    state.reconnectAttempts = (state.reconnectAttempts ?: 0) + 1
    if (state.reconnectAttempts > 5) {
        log.error "[LiteTouch2000] reconnect failed 5 times, giving up"
        return
    }
    log.warn "[LiteTouch2000] Attempting reconnect (#${state.reconnectAttempts}) in 10s..."
    runIn(10, doReconnectionStep)
}

def doReconnectionStep() {
    log.info "[LiteTouch2000] doReconnectionStep => re-open socket"
    closeConnection()
    try {
        def rawPort = adapterPort?.replaceAll(",", "")?.trim()
        if (!rawPort?.isInteger()) rawPort = "2001"
        def portNum = rawPort.toInteger()
        log.info "[LiteTouch2000] Reconnecting to ${adapterHost}:${portNum}"
        interfaces.rawSocket.connect(adapterHost, portNum, [byteInterface:true])
    } catch(e) {
        log.error "[LiteTouch2000] Reconnect error: ${e}"
    }
}

def closeConnection() {
    try {
        interfaces.rawSocket.close()
        log.info "[LiteTouch2000] Socket closed"
    } catch(e) {
        log.warn "[LiteTouch2000] closeConnection error: ${e}"
    }
}

/************************************************************************************************
 *  sendSerialCommand(cmd)
 ***********************************************************************************************/
def sendSerialCommand(String command) {
    if (!command.startsWith(" ")) {
        command = " " + command
    }
    def fullCmd = command + "\r"
    log.debug "[LiteTouch2000] sendSerialCommand('${fullCmd}')"
    try {
        interfaces.rawSocket.sendMessage(fullCmd)
    } catch(e) {
        log.error "[LiteTouch2000] Error sending message: ${e}"
        reconnectAdapter()
    }
}

/************************************************************************************************
 *  CHILD COMMANDS
 ***********************************************************************************************/
def sendDeviceCommand(String deviceDni, String commandType, value) {
    def idPart = deviceDni.substring(1)

    if (commandType == "switch") {
        def cmd = " 10 ${idPart} "
        if (deviceDni.startsWith("r")) {
            cmd += (value.toString() == "0") ? "000" : "001"
        } else {
            cmd += (value.toString() == "0") ? "000" : "250"
        }
        state.priorityQuery = deviceDni
        log.debug "[LiteTouch2000] Switch command => ${cmd}"
        sendSerialCommand(cmd)
    }
    else if (commandType == "setLevel") {
        def computed = Math.round((value as BigDecimal) * 2.5) as int
        def rawLevel = String.format("%03d", computed)
        def cmd = " 10 ${idPart} ${rawLevel}"
        state.priorityQuery = deviceDni
        log.debug "[LiteTouch2000] setLevel => ${cmd}"
        sendSerialCommand(cmd)
    }
    else {
        log.warn "[LiteTouch2000] Unknown commandType: ${commandType}"
    }
}

def sendDeviceCommand(String deviceDni, String commandType, Map params) {
    if (commandType == "setLevelWithFade") {
        if (!deviceDni.startsWith("d")) {
            log.warn "[LiteTouch2000] Fade requested on relay => ignoring"
            return
        }
        def idPart = deviceDni.substring(1)
        def lvl = params.level as BigDecimal
        def computed = Math.round(lvl * 2.5) as int
        def rawLevel = String.format("%03d", computed)
        def fadeSec = params.duration?.toInteger() ?: 0
        if (fadeSec<0) fadeSec=0
        if (fadeSec>3600) fadeSec=3600
        def rawFade = String.format("%04d", fadeSec)

        def cmd = " 11 ${idPart} ${rawLevel} ${rawFade}"
        state.priorityQuery = deviceDni
        log.debug "[LiteTouch2000] setLevelWithFade => ${cmd}"
        sendSerialCommand(cmd)
    }
    else {
        log.warn "[LiteTouch2000] Unknown map-based commandType: ${commandType}"
    }
}