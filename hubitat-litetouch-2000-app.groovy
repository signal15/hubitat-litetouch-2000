/***********************************************************************************************************************
*
*  A Hubitat App for managing LiteTouch 2000 Integration
*
*  License:
*  This program is free software: you can redistribute it and/or modify it under the terms of the GNU
*  General Public License as published by the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
*  implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
*  for more details.
*
*  Name: LiteTouch 2000 Application
*
*  Special Thanks to Doug Beard and Mike Magrann for the framework of this application!
***See Release Notes at the bottom***
***********************************************************************************************************************/

public static String version()      {  return "v0.1.0"  }

definition(
    name: "LiteTouch 2000 Application",
    namespace: "signal15",
    singleInstance: true,
    author: "Jay Austad",
    description: "Integrate your LiteTouch 2000 CCU",
    category: "My Apps",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
	page(name: "mainPage", nextPage: "zoneMapsPage")
    page(name: "zoneMapsPage", nextPage: "mainPage")
	page(name: "notificationPage", nextPage: "mainPage")
	page(name: "lockPage", nextPage: "mainPage")
	page(name: "defineZoneMap", nextPage: "zoneMapsPage")
	page(name: "defineZoneMapImport", nextPage: "importZones")
	page(name: "importZones", nextPage: "zoneMapsPage")
    page(name: "editZoneMapPage", nextPage: "zoneMapsPage")
//    page(name: "aboutPage", nextPage: "mainPage")
}

//App Pages/Views
def mainPage() {
    ifDebug("Showing mainPage")
	state.isDebug = isDebug

	return dynamicPage(name: "mainPage", title: "", install: false, uninstall: true) {
        if(!state.elkM1IntegrationInstalled && getChildDevices().size() == 0) {
            section("Define your Elk M1 device") {
                clearStateVariables()
            	input "elkM1Name", "text", title: "Elk M1 Name", required: true, multiple: false, defaultValue: "Elk M1", submitOnChange: false
                input "elkM1IP", "text", title: "Elk M1 IP Address", required: true, multiple: false, defaultValue: "", submitOnChange: false
                input "elkM1Port", "text", title: "Elk M1 Port", required: true, multiple: false, defaultValue: "2101", submitOnChange: false
				input "elkM1Password", "text", title: "Elk M1 Password", required: true, multiple: false, defaultValue: "", submitOnChange: false
                input "elkM1Code", "text", title: "Elk M1 Disarm Code", required: true, multiple: false, defaultValue: "", submitOnChange: false
            }
        }
        else {
			section("<h1>Device Mapping</h1>") {
                href (name: "zoneMapsPage", title: "Devices",
                description: "Create Virtual Devices and Map them to Existing Zones, Outputs, Tasks and/or Thermostats in your Elk M1 setup",
                page: "zoneMapsPage")
//            }

//			section("<h1>Outputs</h1>") {
//                href (name: "zoneMapsPage", title: "Outputs",
//                description: "Create Virtual Switches and Map them to Existing Outputs in your Elk M1 setup",
//                page: "outputMapsPage")
//                href (name: "zoneMapsPage", title: "Tasks",
//                description: "Create Virtual Switches and Map them to Existing Tasks in your Elk M1 setup",
//                page: "outputMapsPage")
            }

//			section("<h1>Notifications</h1>") {
//                href (name: "notificationPage", title: "Notifications",
//                description: "Enable Push and TTS Messages",
//                page: "notificationPage")
//            }

			section("<h1>Locks</h1>") {
                href (name: "lockPage", title: "Locks",
                description: "Integrate Locks",
                page: "lockPage")
            }


//             state.enableHSM = enableHSM
//                section("<h1>Safety Monitor</h1>") {
//                    paragraph "Enabling Hubitat Safety Monitor Integration will tie your Envisalink state to the state of HSM.  Your Envisalink will receive the Arm Away, Arm Home and Disarm commands based on the HSM state. "
//                        input "enableHSM", "bool", title: "Enable HSM Integration", required: false, multiple: false, defaultValue: false, submitOnChange: true
//               }


        }
//        section("<br/><br/>") {
//            href (name: "aboutPage", title: "About",
//                  description: "Find out more about Elk M1 Application",
//                  page: "aboutPage")
//        }
        section("") {
            input "isDebug", "bool", title: "Enable Debug Logging", required: false, multiple: false, defaultValue: false, submitOnChange: true
        }

    }
}

def aboutPage() {
    ifDebug("Showing aboutPage")

	dynamicPage(name: "aboutPage", title: none){
        section("<h1>Introducing Elk M1 Integration</h1>"){
            paragraph "Elk M1 module allows you to upgrade your existing security system with IP control ... " +
                "EElk M1 Integration connects to your M1XEP module via Telnet, using Hubitat."
            paragraph "Elk M1 Integration automates installation and configuration of the Elk M1 Driver" +
                " as well as Virtual Contacts representing the dry contact zones and Virtual Motion Detection configured in your Elk M1 Alarm system."
            paragraph "You must have the Hubitat Elk M1 driver already installed before making use of Elk M1 application "
			paragraph "Currently, Elk M1 application and driver only work with Elk M1XEP"
            paragraph "Special Thanks to Doug Beard."
        }
	}
}

def lockPage() {
    ifDebug("Showing lockPage")

	dynamicPage(name: "lockPage", title: none){
        section("<h1>Locks</h1>"){
				paragraph "Enable Lock Integration, selected locks will lock when armed and/or unlock when disarmed"
					input "armLocks", "capability.lock", title: "Which locks to lock when armed?", required:false, multiple:true, submitOnChange:true
					input "disarmLocks", "capability.lock", title: "Which locks to unlock when disarmed?", required:false, multiple:true, submitOnChange:true
			}
	}
}

def notificationPage(){
	dynamicPage(name: "notificationPage", title: none){
		section("<h1>Notifications</h1>"){
				paragraph "Enable TTS and Notification integration will announcing arming and disarming over your supported audio and/or push enabled device"

				paragraph "<h3><b>Notification Text</b></h2>"

				input "armingHomeBool", "bool", title: "Enable Arming Home Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (armingHomeBool){
					input "armingHomeText", "text", title: "Notification for Arming Home", required: false, multiple: false, defaultValue: "Arming Home", submitOnChange: false, visible: armingHomeBool
				}

				input "armingAwayBool", "bool", title: "Enable Arming Away Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (armingAwayBool){
					input "armingAwayText", "text", title: "Notification for Arming Away", required: false, multiple: false, defaultValue: "Arming Away", submitOnChange: false
				}
				input "armingNightBool", "bool", title: "Enable Arming Night Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (armingNightBool){
					input "armingNightText", "text", title: "Notification for Arming Night", required: false, multiple: false, defaultValue: "Arming Night", submitOnChange: false
				}
				input "armedBool", "bool", title: "Enable Armed Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (armedBool){
					input "armedText", "text", title: "Notification for Armed", required: false, multiple: false, defaultValue: "Armed", submitOnChange: false
				}
				input "disarmingBool", "bool", title: "Enable Disarming Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (disarmingBool){
					input "disarmingText", "text", title: "Notification for Disarming", required: false, multiple: false, defaultValue: "Disarming", submitOnChange: false
				}
				input "disarmedBool", "bool", title: "Enable Disarmed Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (disarmedBool){
					input "disarmedText", "text", title: "Notification for Disarmed", required: false, multiple: false, defaultValue: "Disarmed", submitOnChange: false
				}
				input "entryDelayAlarmBool", "bool", title: "Enable Entry Delay Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (entryDelayAlarmBool){
					input "entryDelayAlarmText", "text", title: "Notification for Entry Delay", required: false, multiple: false, defaultValue: "Entry Delay in Progress, Alarm eminent", submitOnChange: false
				}
				input "exitDelayAlarmBool", "bool", title: "Enable Exit Delay Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (exitDelayAlarmBool){
					input "exitDelayAlarmText", "text", title: "Notification for Exit Delay", required: false, multiple: false, defaultValue: "", submitOnChange: false
				}
				input "alarmBool", "bool", title: "Enable Alarm Notification", required: false, multiple: false, defaultValue: false, submitOnChange: true
				if (alarmBool){
					input "alarmText", "text", title: "Notification for Alarm", required: false, multiple: false, defaultValue: "Alarm, Alarm, Alarm, Alarm, Alarm", submitOnChange: false
				}
				paragraph "<h3><b>Notification Devices</b></h2>"
				input "speechDevices", "capability.speechSynthesis", title: "Which speech devices?", required:false, multiple:true, submitOnChange:true
				input "notificationDevices", "capability.notification", title: "Which notification devices?", required:false, multiple:true, submitOnChange:true

			}
	}
}

def zoneMapsPage() {
    ifDebug("Showing zoneMapsPage")
    if (getChildDevices().size() == 0 && !state.elkM1IntegrationInstalled)
    {
        createElkM1ParentDevice()
    }

    if (state.creatingZone)
    {
        createZone()
    }

	dynamicPage(name: "zoneMapsPage", title: "", install: true, uninstall: false){

        section("<h1>Device Maps</h1>"){
            paragraph "The partition of your Elk M1 Installation may consist of Zones, Outputs, Tasks and Thermostats.  You can choose to map the devices manually or use the import method. "
            paragraph "You'll want to determine the device number as it is defined in your Elk M1 setup. " +
                " Define a new device in Elk M1 application and the application will then create either a Virtual sensor component device or an Elk Child device , which will report the state of the Elk M1 device to which it is mapped. " +
                " The devices can be used in Rule Machine or any other application that is capable of leveraging the devices capability.  Elk M1 is capable of 208 zones, your zone map should correspond to the numeric representation of that zone."
        }
       section("<h2>Create New Devices</h2>"){
            href (name: "createZoneImportPage", title: "Import Elk Devices",
            description: "Click to import Elk devices",
				  page: "defineZoneMapImport")
	   }
		section("") {
            href (name: "createZoneMapPage", title: "Create a Device Map",
            description: "Create a Virtual Device Manually",
            page: "defineZoneMap")
        }

       section("<h2>Existing Devices</h2>"){
       		def deviceList = ""
            getChildDevice(state.ElkM1DNI).getChildDevices().each{
                href (name: "editZoneMapPage", title: "${it.label}",
                description: "Device Details",
                params: [deviceNetworkId: it.deviceNetworkId],
                page: "editZoneMapPage")
            }
		}
	}
}

def defineZoneMap() {
    ifDebug("Showing defineZoneMap")
    state.creatingZone = true;
	dynamicPage(name: "defineZoneMap", title: ""){
        section("<h1>Create a Device Map</h1>"){
            paragraph "Create a Map for a device in Elk M1"
           	input "zoneName", "text", title: "Device Name", required: true, multiple: false, defaultValue: "Zone x", submitOnChange: false
            input "zoneNumber", "number", title: "Which Device 1-208", required: true, multiple: false, defaultValue: 001, range: "1..208", submitOnChange: false
            input "zoneType", "enum", title: "Motion, Contact, Output, Task or Thermostat Device?", required: true, multiple: false,
                options: [['00':"Contact"],['01':"Motion"],['11':"Thermostat"],['04':"Output"],['05':"Task"]]
        }
	}
}

def defineZoneMapImport() {
    ifDebug("Showing defineZoneMapImport")
	getChildDevice(state.ElkM1DNI).initialize()
	state.creatingZone = true;
	dynamicPage(name: "defineZoneMapImport", title: ""){
        section("<h1>Import Elk Zones</h1>"){
		paragraph "Create a Map for a zone in Elk M1"
		input "deviceType", "enum", title: "Select Device Type", required: true, multiple: false,
			options: [['00':"Zones"],['11':"Thermostat"],['04':"Output"],['05':"Task"]]
//			state.deviceType = deviceType;
	}
	}

//	importZones(deviceType)
}

def editZoneMapPage(message) {
    ifDebug("Showing editZoneMapPage")
    ifDebug("editing ${message.deviceNetworkId}")
    state.allZones = getChildDevice(state.ElkM1DNI).getChildDevices()
    def zoneDevice = getChildDevice(state.ElkM1DNI).getChildDevice(message.deviceNetworkId)
    def paragraphText = ""
    state.editedZoneDNI = message.deviceNetworkId;
    if (zoneDevice.capabilities.find { item -> item.name.startsWith('Motion')}){
        paragraphText = paragraphText + "Motion Sensor\n"
    }
    if (zoneDevice.capabilities.find { item -> item.name.startsWith('Contact')}){
        paragraphText = paragraphText + "Contact Sensor\n"
    }
	if (zoneDevice.capabilities.find { item -> item.name.startsWith('Thermostat')}){
        paragraphText = paragraphText + "Thermostat\n"
    }
	if (zoneDevice.capabilities.find { item -> item.name.startsWith('Switch')}){
        paragraphText = paragraphText + "Virtual Switch\n"
    }
//	if (zoneDevice.capabilities.find { item -> item.name.startsWith('Switch')}){
//        paragraphText = paragraphText + "Virtual Switch\n"
//    }
    dynamicPage(name: "editZoneMapPage", title: ""){
        section("<h1>${zoneDevice.label}</h1>"){
            paragraph paragraphText
        }
    }
}

//End New Code Temp
def clearStateVariables(){
	ifDebug("Clearing State Variables just in case.")
    state.ElkM1DeviceName = null
    state.ElkM1IP = null
    state.ElkM1Port = null
    state.ElkM1Password = null
    state.ElkM1Code = null
}

def createElkM1ParentDevice(){
 	ifDebug("Creating Parent ElkM1 Device")
    if (getChildDevice(state.ElkM1DNI) == null){
        state.ElkM1DNI = UUID.randomUUID().toString()
    	ifDebug("Setting state.ElkM1DNI ${state.ElkM1DNI}")
	    addChildDevice("belk", "Elk M1 Driver", state.ElkM1DNI, null, [name: elkM1Name, isComponent: true, label: elkM1Name])
        getChildDevice(state.ElkM1DNI).updateSetting("ip",[type:"text", value:elkM1IP])
     	getChildDevice(state.ElkM1DNI).updateSetting("port",[type:"text", value:elkM1Port])
	   	getChildDevice(state.ElkM1DNI).updateSetting("passwd",[type:"text", value:elkM1Password])
    	getChildDevice(state.ElkM1DNI).updateSetting("code",[type:"text", value:elkM1Code])
		castElkM1DeviceStates()
    }
}

def castElkM1DeviceStates(){
  	ifDebug("Casting to State Variables")
    state.ElkM1DeviceName = elkM1Name
    ifDebug("Setting state.ElkM1DeviceName ${state.ElkM1DeviceName}")
    state.ElkM1IP = elkM1IP
    ifDebug("Setting state.ElkM1IP ${state.ElkM1IP}")
	state.ElkM1Port = elkM1Port
    ifDebug("Setting state.ElkM1Port ${state.ElkM1Port}")
    state.ElkM1Password = elkM1Password
    ifDebug("Setting state.ElkM1Password ${state.ElkM1Password}")
    state.ElkM1Code = elkM1Code
    ifDebug("Setting state.ElkM1Code ${state.ElkM1Code}")
    if (getChildDevice(state.ElkM1DNI)){
        ifDebug("Found a Child Elk M1 ${getChildDevice(state.ElkM1DNI).label}")
    }
    else{
     	ifDebug("Did not find a Parent Elk M1")
    }
}

private generateChksum(String msg){
		def msgArray = msg.toCharArray()
        def msgSum = 0
        msgArray.each { (msgSum += (int)it) }
		msgSum = msgSum  % 256;
		msgSum = 256 - msgSum;
	 	String  chkSumStr = Integer.toHexString(msgSum);
		chkSumStr = (chkSumStr.toUpperCase());
	    chkSumStr = (chkSumStr.padLeft(2,'0'));
}

def importZones(){
	dynamicPage(name: "defineZoneMapImport", title: ""){
	section("<h1>Import Elk Zones</h1>"){
	paragraph "Finished importing zones for Elk M1 - Click Next to continue"
	getChildDevice(state.ElkM1DNI).RequestTextDescriptions(deviceType)
	state.creatingZone = false;
	   }
 }
}

def createZone(){
    ifDebug("Starting validation of ${zoneName} ZoneType: ${zoneType}")
    String formatted = String.format("%03d", zoneNumber)
    String formatted2 = String.format("%03d", zoneNumber)
   String deviceNetworkId
    if (zoneType == "05"){
	    deviceNetworkId = state.ElkM1DNI + "_K_" + formatted
    }else
    if (zoneType == "04"){
	    deviceNetworkId = state.ElkM1DNI + "_O_" + formatted
    }else
    if (zoneType == "11"){
	    deviceNetworkId = state.ElkM1DNI + "_T_" + formatted2
    }else
    if (zoneType == "00"){
	    deviceNetworkId = state.ElkM1DNI + "_C_" + formatted
    }else{
        deviceNetworkId = state.ElkM1DNI + "_M_" + formatted
    }
    ifDebug("Entered zoneNumber: ${zoneNumber} formatted as: ${formatted}")
    getChildDevice(state.ElkM1DNI).createZone([zoneName: zoneName, deviceNetworkId: deviceNetworkId, zoneType: zoneType])
    state.creatingZone = false;
}

def editZone(){
    def childZone = getChildDevice(state.ElkM1DNI).getChildDevice(state.editedZoneDNI);
	ifDebug("Starting validation of ${childZone.label}")
    ifDebug("Attempting rename of zone to ${newZoneName}")
    childZone.updateSetting("label",[type:"text", value:newZoneName])
   	newZoneName = null;
    state.editingZone = false
    state.editedZoneDNI = null;

}

private ifDebug(msg)
{
    if (msg && state.isDebug)  log.debug 'Elk M1 Module: ' + msg
}

//General App Events
def installed() {
    state.ElkM1Installed = true
	initialize()
}

def updated() {
	log.info "updated"
	unsubscribe()
	initialize()
}

def initialize() {
	log.info "initialize"
	unsubscribe()
    state.creatingZone = false;
    subscribe(location, "hsmStatus", statusHandler)
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

def statusHandler(evt) {
    log.info "HSM Alert: $evt.value"
	def lock
	if (!lock){
		lock = true
		if (getChildDevice(state.ElkM1DNI).currentValue("Status") != "Exit Delay in Progress"
			&& getChildDevice(state.ElkM1DNI).currentValue("Status") != "Entry Delay in Progress"
		   	&& evt.value != "disarmed")
		{
			if (evt.value && state.enableHSM)
			{
				ifDebug("HSM is enabled")
				switch(evt.value){
					case "armedAway":
					ifDebug("Sending Arm Away")
						if (getChildDevice(state.ElkM1DNI).currentValue("Status") != "Armed")
						{
							speakArmingAway()
							getChildDevice(state.ElkM1DNI).ArmAway()
						}
						break
					case "armedHome":
					ifDebug("Sending Arm Home")
						if (getChildDevice(state.ElkM1DNI).currentValue("Status") != "Armed")
						{
							speakArmingHome()
							getChildDevice(state.ElkM1DNI).ArmHome()
						}
						break
					case "armedNight":
					ifDebug("Sending Arm Home")
						if (getChildDevice(state.ElkM1DNI).currentValue("Status") != "Armed")
						{
							speakArmingNight()
							getChildDevice(state.EnvElkM1DNI).ArmHome()
						}
						break

				}
			}
		} else {
			if (evt.value == "disarmed")
			{
				if (state.enableHSM)
				{
					ifDebug("HSM is enabled")
					ifDebug("Sending Disarm")
					if (getChildDevice(state.ElkM1DNI).currentValue("Status") != "Ready")
					{
						speakDisarming()
						getChildDevice(state.ElkM1DNI).Disarm()
					}
				}
			}
		}
		lock = false;
	}
}

def speakArmed(){
	if (!armedBool) return
	if (armedText != ""){
		speakIt(armedText)
	}
}

def speakArmingAway(){
	if (!armingAwayBool) return
	if (armingAwayText){
		speakIt(armingAwayText)
	} else {
		speakIt("Arming Away")
	}
}

def speakArmingHome(){
	if (!armingHomeBool) return
	if (armingHomeText != ""){
		speakIt(armingHomeText)
	}
}

def speakArmingNight(){
	if (!armingNightBool) return
	if (armingNightText != ""){
		speakIt(armingNightText)
	}
}

def speakDisarming(){
	if (!disarmingBool) return
	if (disarmedText){
		speakIt(disarmingText)
	} else {
		speakIt("Disarming")
	}

}

def speakDisarmed(){
	if (!disarmedBool) return
	if (disarmedText != ""){
		speakIt(disarmedText)
	}
}

def speakEntryDelay(){
	if (!entryDelayAlarmBool) return
	if (entryDelayAlarmText != ""){
		speakIt(entryDelayAlarmText)
	}
}

def speakExitDelay(){
	if (!exitDelayAlarmBool) return
	if (exitDelayAlarmText != ""){
		speakIt(exitDelayAlarmText)
	}
}


def speakAlarm(){
	if (!alarmBool) return
	if (alarmText != ""){
		speakIt(alarmText)
	}
}

private speakIt(str)	{
	ifDebug("TTS: $str")
	if (state.speaking)		{
		ifDebug("Already Speaking")
		runOnce(new Date(now() + 10000), speakRetry, [overwrite: false, data: [str: str]])
		return
	}

	if (!speechDevices)		return;
	ifDebug("Found Speech Devices")

	state.speaking = true
	speechDevices.speak(str)

	if (notificationDevices){
		ifDebug("Found Notification Devices")
		notificationDevices.deviceNotification(str)
	}
	state.speaking = false
}

private lockIt(){
	ifDebug("Lock")
	if (!armLocks) return
	ifDebug("Found Lock")
	armLocks.lock()
}

private unlockIt(){
	ifDebug("Unlock")
	if (!disarmLocks) return
	ifDebug("Found Lock")
	disarmLocks.unlock()
}


def speakRetry(data)	{
	if (data.str)		speakIt(data.str);
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}


/***********************************************************************************************************************
/***********************************************************************************************************************
*
* Release Notes
*
* Version: 0.1.5
* Added support for manual inclusion of Elk M1 outputs and tasks
* Added support for importing Elk M1 outputs, tasks and thermostats
* Cleaned up app and related code
*
* Version: 0.1.4
* Added support for importing Elk M1 zones
*
* Version: 0.1.3
* Removed any unsupported features for now
*
* Version: 0.1.2
* Added thermostat support (data receipt only)
*
* Version: 0.1.1
* Ported code from Doug Beard Envisalink Integration
* HSM integration not currently supported
* All functionality not fully tested
*
***********************************************************************************************************************/
/***********************************************************************************************************************
*
*Feature Request & Known Issues
*
* I - Must initialize the Elk M1 Device prior to using the Elk M1 App Import Zone functions
* I - System configuration needs to be set up manually on the thermostat device page
*
*/
