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
def updateStatus(status) {
    log.debug "Child ${device.deviceNetworkId} updateStatus: ${status}"
    if (device.deviceNetworkId.startsWith("r")) {
        sendEvent(name: "switch", value: status) // on/off
    } else if (device.deviceNetworkId.startsWith("d")) {
        def lvl = status.toInteger()
        sendEvent(name: "level", value: lvl)
        sendEvent(name: "switch", value: (lvl>0 ? "on" : "off"))
    }
}
def on() {
    parent.sendDeviceCommand(device.deviceNetworkId, "switch", 1)
}
def off() {
    parent.sendDeviceCommand(device.deviceNetworkId, "switch", 0)
}
def setLevel(Number lvl) {
    parent.sendDeviceCommand(device.deviceNetworkId, "setLevel", lvl)
    sendEvent(name: "level", value: lvl)
    sendEvent(name: "switch", value: (lvl>0 ? "on" : "off"))
}
def setLevel(Number lvl, Number dur) {
    if (device.deviceNetworkId.startsWith("d")) {
        parent.sendDeviceCommand(device.deviceNetworkId, "setLevelWithFade", [level:lvl, duration:dur])
    } else {
        if (lvl>0) on() else off()
    }
}