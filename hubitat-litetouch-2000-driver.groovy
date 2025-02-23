/*******************************************************************************************
 *  LiteTouch2000 Child
 *
 *  Author: Jay Austad
 *  Namespace: com.snowautomation
 *
 *  - Minimal child driver that implements Switch + SwitchLevel
 *  - If the device ID starts with 'r', we treat it like a relay
 *  - If it starts with 'd', we treat it like a dimmer
 *******************************************************************************************/

metadata {
    definition(name: "LiteTouch2000 Child", namespace: "com.snowautomation", author: "Jay Austad") {
        capability "Switch"
        capability "SwitchLevel"
        command "updateStatus", ["string"]
    }
}

def parse(String description) {
    log.debug "Child parse called with: ${description}"
}

/**
 * updateStatus(status) is called by the parent to set new state
 *   For a relay, status might be "on"/"off"
 *   For a dimmer, status is an integer 0..100
 */
def updateStatus(status) {
    log.debug "Child ${device.deviceNetworkId} updateStatus received: ${status}"

    if(device.deviceNetworkId.startsWith("r")) {
        // Relay: either "on" or "off"
        sendEvent(name: "switch", value: status)
    }
    else if(device.deviceNetworkId.startsWith("d")) {
        // Dimmer: 0..100
        def levelInt = status.toInteger()
        sendEvent(name: "level", value: levelInt)
        sendEvent(name: "switch", value: (levelInt > 0 ? "on" : "off"))
    }
}

/**
 * Basic Switch commands
 */
def on() {
    log.debug "Child ${device.deviceNetworkId} on()"
    parent.sendDeviceCommand(device.deviceNetworkId, "switch", 1)
}

def off() {
    log.debug "Child ${device.deviceNetworkId} off()"
    parent.sendDeviceCommand(device.deviceNetworkId, "switch", 0)
}

/**
 * setLevel(level) with no duration
 */
def setLevel(Number level) {
    log.debug "Child ${device.deviceNetworkId} setLevel(${level})"
    sendEvent(name: "level", value: level)
    sendEvent(name: "switch", value: (level > 0 ? "on" : "off"))
    parent.sendDeviceCommand(device.deviceNetworkId, "setLevel", level)
}

/**
 * setLevel(level, duration) with fade
 *   Only valid for dimmers (deviceNetworkId starts with 'd').
 */
def setLevel(Number level, Number duration) {
    log.debug "Child ${device.deviceNetworkId} setLevel(${level}, ${duration})"
    sendEvent(name: "level", value: level)
    sendEvent(name: "switch", value: (level > 0 ? "on" : "off"))
    
    if (device.deviceNetworkId.startsWith("d")) {
        // It's a dimmer => fade
        parent.sendDeviceCommand(device.deviceNetworkId, "setLevelWithFade", [level: level, duration: duration])
    } else {
        // Relay => no fade
        log.warn "Fade requested on a relay device => ignoring"
        if (level > 0) on() else off()
    }
}
