/***********************************************************************************************************************
*
*  A Hubitat Driver using Telnet to connect to the LiteTouch 2000 via an IP->serial gateway.
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
*  Name: LiteTouch 2000 Driver
*
*  A Special Thanks to Doug Beard and Mike Magrann for the framework of this driver!
*
*
***
***********************************************************************************************************************/

public static String version()      {  return "v0.1.0"  }
public static boolean isDebug() { return true }
import groovy.transform.Field
import java.util.regex.Matcher
import java.util.regex.Pattern;


metadata {
	definition (name: "LiteTouch 2000 Driver", namespace: "signal15", author: "Jay Austad") {
		capability "Switch"
		capability "SwitchLevel"
		capability "Telnet"
	}
	preferences {
		input("ip", "text", title: "IP Address", description: "ip", required: true)
		input("port", "text", title: "Port", description: "port", required: true)
		input("switches", "text", title: "Switches", description "e.g. 01-1,01-2,02-A", required: true)
		input("dimmers", "text", title: "Dimmers", description "e.g. 03-1,03-2,04-A", required: true)
	}
}

//general handlers
def installed() {
	log.warn "installed..."
    initialize()
   }
def updated() {
	ifDebug("updated...")
    ifDebug("Configuring IP: ${ip}, Port${port}, Switches: ${switches}, Dimmers: ${dimmers}")
	initialize()
}
def initialize() {
    telnetClose()
	try {
		//open telnet connection
		telnetConnect([termChars:[13,10]], ip, port, null, null)
		//give it a chance to start
		pauseExecution(1000)
		ifDebug("Telnet connection to LiteTouch 2000 CCU established")
        //poll()
	} catch(e) {
		log.warn "initialize error: ${e.message}"
	}
}
def uninstalled() {
    telnetClose()
	removeChildDevices(getChildDevices())
}


//LiteTouch 2000 Command Line Request - Start of
def Disarm(){
 	ifDebug("Disarm()")
    def cmd = elkCommands["Disarm"]
    prepMsg(cmd)
}
def ArmAway(){
	ifDebug("ArmAway()")
    def cmd = elkCommands["ArmAway"]
    prepMsg(cmd)
}
def ArmHome(){
 	ifDebug("armHome()")
    def cmd = elkCommands["ArmHome"]
    prepMsg(cmd)
}
def ArmStayInstant(){
 	ifDebug("armStayInstant()")
    def cmd = elkCommands["ArmStayInstant"]
    prepMsg(cmd)
}
def ArmNight(){
 	ifDebug("armNight()")
    def cmd = elkCommands["ArmNight"]
    prepMsg(cmd)
}
def ArmNightInstant(){
 	ifDebug("armNightInstant()")
    def cmd = elkCommands["ArmNightInstant"]
    prepMsg(cmd)
}
def ArmVacation(){
 	ifDebug("armVacation()")
    def cmd = elkCommands["ArmVacation"]
    prepMsg(cmd)
}
def RequestArmStatus(){
 	ifDebug("requestArmStatus()")
    def cmd = elkCommands["RequestArmStatus"]
    prepMsg2(cmd)
}
def RequestTemperatureData(){
 	ifDebug("requestTemperatureData()")
    def cmd = elkCommands["RequestTemperatureData"]
	prepMsg2(cmd)
}

//This for loop now works properly
def RequestTextDescriptions(deviceType){
 	ifDebug("request Text Descriptions()")
	def cmd = elkCommands["RequestTextDescriptions"]
	def type = deviceType;
	def future = "00";
	  for (i = 1; i <= 208; i++) {
	number = (i.toString());
		  		number = (number.padLeft(3,'0'));
	def msg = cmd + type + number + future;
	def len  = (msg.length()+2);
	String msgStr = Integer.toHexString(len);
	msgStr = (msgStr.toUpperCase());
    msgStr = (msgStr.padLeft(2,'0'));
	msg = msgStr + msg
	def cc = generateChksum(msg);
		msg = msg + cc
sendHubCommand(new hubitat.device.HubAction(msg, hubitat.device.Protocol.TELNET))
	pauseExecution(50)
	  }
}

def ControlOutputOn(output, time){
 	ifDebug("controlOutputOn()")
    def cmd = elkCommands["ControlOutputOn"]
	output = output.toString();
	time = time.toString();
    output = (output.padLeft(3,'0'));
	time = (time.padLeft(5,'0'));
	cmd = cmd + output + time;
    prepMsg2(cmd)
//	On()
}
def ControlOutputOff(output){
 	ifDebug("controlOutputOff()" + output)
    def cmd = elkCommands["ControlOutputOff"]
	output = output.toString();
    output = (output.padLeft(3,'0'));
	cmd = cmd + output;
    prepMsg2(cmd)
//	Off()
}
def ControlOutputToggle(output){
 	ifDebug("controlOutputToggle()")
    def cmd = elkCommands["ControlOutputToggle"]
	output = output.toString();
    output = (output.padLeft(3,'0'));
	cmd = cmd + output;
    prepMsg2(cmd)
}
def TaskActivation(task){
 	ifDebug("taskActivation()")
    def cmd = elkCommands["TaskActivation"]
	task = task.toString();
    task = (task.padLeft(3,'0'));
	cmd = cmd + task;
    prepMsg2(cmd)
}
def RequestThermostatData(){
 	ifDebug("requestThermostatData()")
    def cmd = elkCommands["RequestThermostatData"]
	def thermostat = "01";
	cmd = cmd + thermostat;
    prepMsg2(cmd)
}
def setThermostatMode(tstat, thermostatmode){
    def cmd = elkCommands["setThermostatMode"]
    thermostat = tstat;
    def value = elkThermostatModeSet[thermostatmode]
    	value = (value.padLeft(2,'0'));
	element = "0";
	cmd = cmd + thermostat + value + element;
 	ifDebug("setThermostatMode()" + cmd + thermostatmode + thermostat)
    prepMsg2(cmd)
}
def auto(tstat){
    def cmd = elkCommands["auto"]
    thermostat = tstat;
    value = "03";
	element = "0";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def heat(tstat){
    def cmd = elkCommands["heat"]
    thermostat = tstat;
    value = "01";
	element = "0";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def cool(tstat){
   def cmd = elkCommands["cool"]
    thermostat = tstat;
    value = "02";
	element = "0";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def off(tstat){
    def cmd = elkCommands["off"]
    thermostat = tstat;
    value = "00";
	element = "0";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def setThermostatFanMode(tstat, fanmode){
    def cmd = elkCommands["setThermostatFanMode"]
    thermostat = tstat;
    def value = elkThermostatFanModeSet[fanmode]
    	value = (value.padLeft(2,'0'));
	element = "2";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def fanOn(tstat){
    def cmd = elkCommands["fanOn"]
    thermostat = tstat;
    value = "01";
	element = "2";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def fanAuto(tstat){
    def cmd = elkCommands["fanAuto"]
    thermostat = tstat;
    value = "00";
	element = "2";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def setHeatingSetpoint(tstat, degrees){
    def cmd = elkCommands["setHeatingSetpoint"]
    thermostat = tstat;
    value = degrees;
    element = "5";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
def setCoolingSetpoint(tstat, degrees){
    def cmd = elkCommands["setCoolingSetpoint"]
    thermostat = tstat;
    value = degrees;
    element = "4";
	cmd = cmd + thermostat + value + element;
    prepMsg2(cmd)
}
//def setThermostatSetpoint(tstat, degrees){
//    def cmd = elkCommands["setHeatingSetpoint"]
//    thermostat = tstat;
//    value = degrees;
//    element = "4";
//	cmd = cmd + thermostat + value + element;
//    prepMsg2(cmd)
//}
def RequestZoneDefinitions(){
 	ifDebug("request Zone Definitions()")
    def cmd = elkCommands["RequestZoneDefinitions"]
	prepMsg2(cmd)
}
def ZoneStatusRequest(){
 	ifDebug("zoneStatusRequest()")
    def cmd = elkCommands["ZoneStatusRequest"]
    prepMsg2(cmd)
}
//Elk M1 Command Line Request - End of


//Elk M1 Message Send Lines - Start of
def prepMsg(cmd) {
	def area = "1";
    def code = (code.padLeft(6,'0'));
	cmd = cmd + area + code;
	prepMsg2(cmd)
}

def prepMsg2(cmd) {
	def future = "00";
	def msg = cmd + future;
	def len  = (msg.length()+2);
	String msgStr = Integer.toHexString(len);
	msgStr = (msgStr.toUpperCase());
    	msgStr = (msgStr.padLeft(2,'0'));
	msg = msgStr + msg
	def cc = generateChksum(msg);
	msg = msg + cc + '\r\n'
	sendMsg(msg)
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

def sendMsg(msg) {
    s = msg
    ifDebug("sendMsg $s")
	return new hubitat.device.HubAction(s, hubitat.device.Protocol.TELNET)
}

//Elk M1 Message Send Lines - End of


//Elk M1 Event Receipt Lines
private parse(String message) {
    ifDebug("Parsing Incoming message: " + message)
		def commandCode = message.substring(2, 4);


		if (commandCode.matches("ZC")){
			def zoneNumber = message.substring(4, 7)
			def zoneStatus = message.substring(7, 8)

  if (zoneStatus == '9') {
	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + ViolatedOpen);
	  				zoneOpen(message)
  }  else {
  if (zoneStatus == 'B') {
	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + ViolatedShort);
//This should actually be zoneClosed but in order to show active status we are using zoneOpen.
	  				zoneOpen(message)
  } else {
  if (zoneStatus == '1') {
	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - " + NormalOpen);
//This should actually be zoneOpen but in order to show inactive status we are using zoneClosed.
	  				zoneClosed(message)
  }  else {
  if (zoneStatus == '3') {
	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + NormalShort);
	  				zoneClosed(message)
  }

//	  else {
//  if (zoneStatus == '0') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - " + NormalUnconfigured);
//  else {
//  if (zoneStatus == '2') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + NormalEOL);
//              zoneClosed(message)
//  }
//      else {
//  if (zoneStatus == '5') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + TroubleOpen);
//  }  else {
//  if (zoneStatus == '6') {
//    zoneStatus = TroubleEOL;
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + TroubleEOL);
//  }  else {
//  if (zoneStatus == '7') {
//	  		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + TroubleShort);
//  }  else {
//  if (zoneStatus == '8') {
//			ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - "  + NotUsed);
//  }
	 else {
    zoneStatus = 'Unknown zone status message';
  }
////		ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus);
//  }}}}}}
}}}

		}
  else {
		if (commandCode.matches("AM")){
            ifDebug("The event is unknown: ");
  }  else {
		if (commandCode.matches("AS")){
  def armStatusesString = message.substring(4, 12);
  def armStatus;
  def armUpStatesString = message.substring(12, 20);
  def armUpStates;
  def alarmStatesString = message.substring(20, 28);
  def alarmStates;
//		            ifDebug("ArmStatuses: " + armStatusesString);
//		            ifDebug("ArmingStatusState: " + armUpStatesString);
//		            ifDebug("AlarmStates: " + alarmStatesString);
// Removed in order to reduce the logging data (Added Back)
////  for (i = 1; i <= 1; i++) {
////	armStatus = elkArmStatuses[armStatusesString.substring(i - 1, i)];
////	armUpStates = elkArmUpStates[armUpStatesString.substring(i - 1, i)];
////	alarmStates = elkAlarmStates[alarmStatesString.substring(i - 1, i)];
	armStatus = elkArmStatuses[armStatusesString.substring(0, 1)];
	armUpStates = elkArmUpStates[armUpStatesString.substring(0, 1)];
	alarmStates = elkAlarmStates[alarmStatesString.substring(0, 1)];
////	ifDebug("ArmStatus: " + "Area " + i + " - " + armStatus);
////	ifDebug("ArmUpState: " + "Area " + i + " - " + armUpStates);
//// 	ifDebug("AlarmState: " + "Area " + i + " - " + alarmStates);
		  sendEvent(name:"ArmStatus", value: armStatus, displayed:false, isStateChange: true)
			sendEvent(name:"ArmState", value: armUpStates, displayed:false, isStateChange: true)
			sendEvent(name:"AlarmState", value: alarmStates, displayed:false, isStateChange: true)
//	          	armReady()
////  }
  }
		else {
	if (commandCode.matches("AR")){
            ifDebug("AlarmReporting: ");
			def accountNumber = message.substring(4, 10)
			def alarmCode = message.substring(10, 14)
			def area = message.substring(14, 16)
			def zone = message.substring(16, 19)
			def telIp = message.substring(19, 20)
	}
  else {
		if (commandCode.matches("CC")){
            ifDebug("Output Change Update: ");
			outputStatus(message)
  }  else {
		if (commandCode.matches("CS")){
            ifDebug("ControlOutputStatusReport: ");

			def outputString = message.substring(4, 212);
			def outputStatus;
			for (i = 1; i <= 208; i++) {
			outputStatus = elkOutputStates[outputString.substring(i - 1, i)];
  }
  }  else {
		if (commandCode.matches("DS")){
            ifDebug("LightStatus: ");
			def deviceNumber = message.substring(4, 7)
			def deviceState = message.substring(7, 8)
  }  else {
		if (commandCode.matches("LD")){
		    def eventCode = message.substring(4, 8)
            ifDebug("LogData: " + eventCode);
			if(eventCode == '1000') {
			ifDebug("ZoneChange: " + zoneNumber + " - " + zoneStatus + " - " + NormalUnconfigured);
			sendEvent(name:"Status", value: NoEvent, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1001') {
	        sendEvent(name:"Status", value: FIREALARM, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1002') {
	        sendEvent(name:"Status", value: FIRESUPERVISORYALARM, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1003') {
			sendEvent(name:"Status", value: BURGLARALARMANYAREA, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1008') {
	        sendEvent(name:"Status", value: CARBONMONOXIDEALARMANYAREA, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1009') {
			sendEvent(name:"Status", value: EMERGENCYALARMANYAREA, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1010') {
   		    sendEvent(name:"Status", value: FREEZEALARMANYAREA, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1011') {
  	        sendEvent(name:"Status", value: GASALARMANYAREA, displayed:false, isStateChange: true)
		    }
			if(eventCode == '1012') {
         	sendEvent(name:"Status", value: HEATALARMANYAREA, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1013') {
         	sendEvent(name:"Status", value: WATERALARMANYAREA, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1014') {
			sendEvent(name:"Status", value: ALARMANYAREA, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1173') {
			ifDebug("LogData: " + eventCode + "AREAARMED");
//			sendEvent(name:"Status", value: AREAARMED, displayed:false, isStateChange: true)
			sendEvent(name: "switch", value: "on")
        	systemArmed()}
			if(eventCode == '1174') {
			ifDebug("LogData: " + eventCode + "AREADISARMED");
//			sendEvent(name:"Status", value: AREADISARMED, displayed:false, isStateChange: true)
			sendEvent(name: "switch", value: "off")
			disarming()}
			if(eventCode == '1175') {
         	sendEvent(name:"Status", value: AREA1ARMSTATE, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1183') {
         	sendEvent(name:"Status", value: AREA1ISARMEDAWAY, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1191') {
         	sendEvent(name:"Status", value: AREA1ISARMEDSTAY, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1199') {
         	sendEvent(name:"Status", value: AREA1ISARMEDSTAYINSTANT, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1207') {
         	sendEvent(name:"Status", value: AREA1ISARMEDNIGHT, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1215') {
         	sendEvent(name:"Status", value: AREA1ISARMEDNIGHTINSTANT, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1223') {
         	sendEvent(name:"Status", value: AREA1ISARMEDVACATION, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1231') {
        	sendEvent(name:"Status", value: AREA1ISFORCEARMED, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1239') {
         	sendEvent(name:"Status", value: ZONEBYPASSED, displayed:false, isStateChange: true)
    		}
			if(eventCode == '1240') {
         	sendEvent(name:"Status", value: ZONEUNBYPASSED, displayed:false, isStateChange: true)
    		}
		}
  else {
		if (commandCode.matches("LW")){
			def KP1 = message.substring(4, 7).toInteger()-40;
			def KP2 = message.substring(7, 10).toInteger()-40;
			def TS1 = message.substring(7, 10).toInteger()-60;
			def TS2 = message.substring(7, 10).toInteger()-60;
            ifDebug("Temps: " + KP1 + ' - ' + TS1);
  }
  else {
		if (commandCode.matches("SD")){
			def type = message.substring(4, 6);
//			type = elkTextDescriptionsTypes[type];
			def name = message.substring(6, 9)
			def text = message.substring(9, 25)
			def zoneType;
		if (type.matches("04")){
			zoneType = "Output"
		}
		else
		if (type.matches("05")){
			zoneType = "Task"
		} 		else
		if (type.matches("11")){
			zoneType = "Thermostat"
		}
 		else
		if (type.matches("02")){
			zoneType = "Thermostat"
		}
 		else
		if (type.matches("00")){
//			zoneType = "Contact"
		if (text.matches("(.*)Door(.*)")){
			zoneType = "Contact"
		} else
		if (text.matches("(.*)door(.*)")){
			zoneType = "Contact"
		} else
		if (text.matches("(.*)indow(.*)")){
			zoneType = "Contact"
		} else
			if (text.matches("(.*)Motion(.*)")){
			zoneType = "Motion"
			}
		}

			else
			zoneType = "Alert";
		if(name > '000'){
			ifDebug("Zones: " + name + text + zoneType);
			createZones(name, text, zoneType)
		}
  }
  else {
		if (commandCode.matches("ST")){
            ifDebug("ReplyRequestedTemp: ");
			def group = message.substring(4, 5);
			group = elkTempTypes[group];
			def device = message.substring(6, 8)
			def temp = message.substring(8, 11)
  }  else {
		if (commandCode.matches("TC")){
	  				taskStatus(message)
  }  else {
		if (commandCode.matches("TR")){
	  				zoneTemp(message)
  }  else {
		if (commandCode.matches("XK")){
            ifDebug("Ethernet: " + message);
			def ethernet = "Ehternet Test"
  }

  else {
		if (commandCode.matches("ZD")){
			def zoneString = message.substring(4, 212);
			def zoneDefinitions;
  for (i = 1; i <= 208; i++) {
		zoneDefinitions = elkZoneDefinitions[zoneString.substring(i - 1, i)];
						ifDebug("ZoneDefinitions: " + "Zone " + i + " - " + zoneDefinitions);
  }

  }  else {
		if (commandCode.matches("ZS")){
			def zoneString = message.substring(4, 212);
			def zoneStatus;
  for (i = 1; i <= 208; i++) {
//    message['zone' + i] = elkZoneStatuses[message.substring(i - 1, i)];
		zoneStatus = elkZoneStatuses[zoneString.substring(i - 1, i)];
						ifDebug("ZoneStatus: " + "Zone " + i + " - " + zoneStatus);
  }
		}
			else {
            ifDebug("The event is unknown");
  }
}

  }}}}}}}}}}}}}}}

// Zone Status
def zoneOpen(message){
    def zoneDevice
    def substringCount = (message.length()-8);
     zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${message.substring(substringCount).take(3)}")
    if (zoneDevice == null){
        zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${message.substring(substringCount).take(3)}")
//		ifDebug("Motion Active")
		zoneDevice.active()
    }
		else
		{
//		ifDebug("Contact Open")
		zoneDevice.open()
    }
//    if (zoneDevice){
//        if (zoneDevice.capabilities.find { item -> item.name.startsWith('Contact')}){
//            ifDebug("Contact Open")
//            zoneDevice.open()
//		}
}
def zoneClosed(message){
    def zoneDevice
    def substringCount = (message.length()-8);
    zoneDevice = getChildDevice("${device.deviceNetworkId}_C_${message.substring(substringCount).take(3)}")
    if (zoneDevice == null){
        zoneDevice = getChildDevice("${device.deviceNetworkId}_M_${message.substring(substringCount).take(3)}")
//            ifDebug("Motion Inactive")
            zoneDevice.inactive()
    }
			else {
//            ifDebug("Contact Closed")
            zoneDevice.close()
        }
}
def outputStatus(message){
		def ElkM1DNI = device.deviceNetworkId
		def outputNumber = message.substring(4, 7)
			def outputState = message.substring(7, 8)
			outputState = elkOutputStates[outputState];
		deviceNetworkId = ElkM1DNI + "_O_" + outputNumber;
if (getChildDevice(deviceNetworkId)!=null){
     zoneDevice = getChildDevice(deviceNetworkId)
         if (zoneDevice.capabilities.find { item -> item.name.startsWith('Switch')}){
            zoneDevice.sendEvent(name: "switch", value: (outputState), isStateChange: true)
         }
}
}

def taskStatus(message){
}

//Zone Status (Currently working on this)
def zoneTemp(message){
   String deviceNetworkId
    def zoneDevice
	def ElkM1DNI = device.deviceNetworkId
			def tNumber = message.substring(4, 6);
			def mode = message.substring(6, 7)
			mode = elkThermostatMode[mode];
			def hold = message.substring(7, 8)
			hold = elkThermostatHold[hold];
			def fan = message.substring(8, 9)
			fan = elkThermostatFan[fan];
			def cTemp = message.substring(9, 11)
			def hSet = message.substring(11, 13)
			def cSet = message.substring(13, 15)
			def cHumid = message.substring(15, 17)
		ifDebug("Thermostat Data Check: " + tNumber + ", " + mode + " Mode, Hold temperature = " + hold + ", " + fan + ", Current Temperature = " + cTemp + ", Heat Setpoint = " + hSet + ", Cool Setpoint = " + cSet);
    	tNumber = (tNumber.padLeft(3,'0'));
		deviceNetworkId = ElkM1DNI + "_T_" + tNumber;
//		ifDebug("Thermostat Data Check: " + deviceNetworkId);

if (getChildDevice(deviceNetworkId)!=null){
     zoneDevice = getChildDevice(deviceNetworkId)
         if (zoneDevice.capabilities.find { item -> item.name.startsWith('Thermostat')}){
            zoneDevice.sendEvent(name: "temperature", value: (cTemp))
            zoneDevice.sendEvent(name: "thermostatMode", value: (mode))
            zoneDevice.sendEvent(name: "thermostatFanMode", value: (fan))
            zoneDevice.sendEvent(name: "coolingSetpoint", value: (cSet))
            zoneDevice.sendEvent(name: "heatingSetpoint", value: (hSet))
         }
}
}


//NEW CODE
//Manage Zones
def createZones(name, text, zoneType){
//    log.info "Creating ${zoneName} with deviceNetworkId = ${deviceNetworkId} of type: ${zoneType}"
   String deviceNetworkId
    def newDevice
	def ElkM1DNI = device.deviceNetworkId
            ifDebug("DNI: " + ElkM1DNI);
	def textLabel = "Zone " + name + " - " + text
	def taskLabel = "Task " + name + " - " + text
	def outputLabel = "Output " + name + " - " + text
	def tstatLabel = "Thermostat " + name + " - " + text
    if (zoneType == "Contact")
    {
	deviceNetworkId = ElkM1DNI + "_C_" + name
    	addChildDevice("hubitat", "Virtual Contact Sensor", deviceNetworkId, [name: textLabel, isComponent: false, label: textLabel])
	}
	else if (zoneType == "Motion")
{
	deviceNetworkId = ElkM1DNI + "_M_" + name
    	addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: textLabel, isComponent: false, label: textLabel])
        newDevice = getChildDevice(deviceNetworkId)
        newDevice.updateSetting("autoInactive",[type:"string", value:'disabled'])
}
	else if (zoneType == "Thermostat")
{
	deviceNetworkId = ElkM1DNI + "_T_" + name
    	addChildDevice("belk", "Elk M1 Driver Thermostat", deviceNetworkId, [name: tstatLabel, isComponent: false, label: tstatLabel])
        newDevice = getChildDevice(deviceNetworkId)
}	else if (zoneType == "Output")
{
	deviceNetworkId = ElkM1DNI + "_O_" + name
    	addChildDevice("belk", "Elk M1 Driver Outputs", deviceNetworkId, [name: outputLabel, isComponent: false, label: outputLabel])
        newDevice = getChildDevice(deviceNetworkId)
}
	else if (zoneType == "Task")
{
	deviceNetworkId = ElkM1DNI + "_K_" + name
    	addChildDevice("belk", "Elk M1 Driver Tasks", deviceNetworkId, [name: taskLabel, isComponent: false, label: taskLabel])
        newDevice = getChildDevice(deviceNetworkId)
}
	else {
        deviceNetworkId = ElkM1DNI + "_M_" + name
     	addChildDevice("hubitat", "Virtual Motion Sensor", deviceNetworkId, [name: textLabel, isComponent: false, label: textLabel])
        newDevice = getChildDevice(deviceNetworkId)
        newDevice.updateSetting("autoInactive",[type:"string", value:'disabled'])
    }
}

def createZone(zoneInfo){
    log.info "Creating ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId} of type: ${zoneInfo.zoneType}"
    def newDevice
    if (zoneInfo.zoneType == "00")
    {
    	addChildDevice("hubitat", "Virtual Contact Sensor", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: false, label: zoneInfo.zoneName])
	}
	else if (zoneInfo.zoneType == "11")
    {
    	addChildDevice("belk", "Elk M1 Driver Thermostat", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: false, label: zoneInfo.zoneName])
    }
	else if (zoneInfo.zoneType == "05")
    {
    	addChildDevice("belk", "Elk M1 Driver Tasks", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: false, label: zoneInfo.zoneName])
    }
	else if (zoneInfo.zoneType == "04")
    {
    	addChildDevice("belk", "Elk M1 Driver Outputs", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: false, label: zoneInfo.zoneName])
    } else {
     	addChildDevice("hubitat", "Virtual Motion Sensor", zoneInfo.deviceNetworkId, [name: zoneInfo.zoneName, isComponent: false, label: zoneInfo.zoneName])
        newDevice = getChildDevice(zoneInfo.deviceNetworkId)
        newDevice.updateSetting("autoInactive",[type:"enum", value:0])
    }
}
def removeZone(zoneInfo){
    log.info "Removing ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}"
    deleteChildDevice(zoneInfo.deviceNetworkId)
}

def disarming(){
	if (state.armState != "disarmed"){
		ifDebug("disarming")
		state.armState = "disarmed"
		parent.unlockIt()
		parent.speakDisarmed()
		if (location.hsmStatus != "disarmed")
		{
			sendLocationEvent(name: "hsmSetArm", value: "disarm")
		}
	}
}
def systemArmed(){
	if (state.armState != "armed"){
		ifDebug("armed")
		state.armState = "armed"
		parent.lockIt()
		parent.speakArmed()
		if (location.hsmStatus == "disarmed")
		{
			sendLocationEvent(name: "hsmSetArm", value: "armHome")
		}
	}
}

//def armReady(){
//	if (state.armUpStates != "Ready To Arm"){
//		ifDebug("ready to arm")
//		state.armUpStates = "Ready To Arm"
//		parent.lockIt()
//		parent.speakArmed()
//		if (location.hsmStatus == "disarmed")
//		{
//			sendLocationEvent(name: "hsmSetArm", value: "armHome")
//		}
//	}
//}

//Telnet
def getReTry(Boolean inc){
	def reTry = (state.reTryCount ?: 0).toInteger()
	if (inc) reTry++
	state.reTryCount = reTry
	return reTry
}

def telnetStatus(String status){
	log.warn "telnetStatus- error: ${status}"
	if (status != "receive error: Stream is closed"){
		getReTry(true)
		log.error "Telnet connection dropped..."
		initialize()
	} else {
		log.warn "Telnet is restarting..."
	}
}

private ifDebug(msg)
{
	parent.ifDebug('Connection Driver: ' + msg)
}


////REFERENCES AND MAPPINGS////

// Event Mapping Readable Text
@Field static final String NoEvent = "No Event"
@Field static final String FIREALARM = "Fire Alarm"
@Field static final String FIRESUPERVISORYALARM = "Fire Supervisory Alarm"
@Field static final String BURGLARALARMANYAREA = "Burglar Alarm, Any Area"
@Field static final String MEDICALALARMANYAREA = "Medical Alarm, Any Area"
@Field static final String POLICEALARMANYAREA = "Police Alarm, Any Area"
@Field static final String AUX124HRANYAREA = "Aux1 24 Hr, Any Area"
@Field static final String AUX224HRANYAREA = "Aux2 24 Hr, Any Area"
@Field static final String CARBONMONOXIDEALARMANYAREA = "Carbon Monoxide Alarm, Any Area"
@Field static final String EMERGENCYALARMANYAREA = "Emergency Alarm, Any Area"
@Field static final String FREEZEALARMANYAREA = "Freeze Alarm, Any Area"
@Field static final String GASALARMANYAREA = "Gas Alarm, Any Area"
@Field static final String HEATALARMANYAREA = "Heat Alarm, Any Area"
@Field static final String WATERALARMANYAREA = "Water Alarm, Any Area"
@Field static final String ALARMANYAREA = "Alarm, Any Area"
@Field static final String CODELOCKOUTANYKEYPAD = "Code Lockout, Any Keypad"
@Field static final String FIRETROUBLEANYZONE = "Fire Trouble, Any Zone"
@Field static final String BURGLARTROUBLEANYZONE = "Burglar Trouble, Any Zone"
@Field static final String FAILTOCOMMUNICATETROUBLE = "Fail To Communicate Trouble"
@Field static final String RFSENSORLOWBATTERYTROUBLE = "Rf Sensor Low Battery Trouble"
@Field static final String LOSTANCMODULETROUBLE = "Lost Anc Module Trouble"
@Field static final String LOSTKEYPADTROUBLE = "Lost Keypad Trouble"
@Field static final String LOSTINPUTEXPANDERTROUBLE = "Lost Input Expander Trouble"
@Field static final String LOSTOUTPUTEXPANDERTROUBLE = "Lost Output Expander Trouble"
@Field static final String EEPROMMEMORYERRORTROUBLE = "Eeprom Memory Error Trouble"
@Field static final String FLASHMEMORYERRORTROUBLE = "Flash Memory Error Trouble"
@Field static final String ACFAILURETROUBLE = "Ac Failure Trouble"
@Field static final String CONTROLLOWBATTERYTROUBLE = "Control Low Battery Trouble"
@Field static final String CONTROLOVERCURRENTTROUBLE = "Control Over Current Trouble"
@Field static final String EXPANSIONMODULETROUBLE = "Expansion Module Trouble"
@Field static final String OUTPUT2SUPERVISORYTROUBLE = "Output 2 Supervisory Trouble"
@Field static final String TELEPHONELINEFAULTTROUBLE1 = "Telephone Line Fault Trouble1"
@Field static final String RESTOREFIREZONE = "Estore Fire Zone"
@Field static final String RESTOREFIRESUPERVISORYZONE = "Restore Fire Supervisory Zone"
@Field static final String RESTOREBURGLARZONE = "Restore Burglar Zone"
@Field static final String RESTOREMEDICALZONE = "Restore Medical Zone"
@Field static final String RESTOREPOLICEZONE = "Restore Police Zone"
@Field static final String RESTOREAUX124HRZONE = "Restore Aux1 24 Hr Zone"
@Field static final String RESTOREAUX224HRZONE = "Restore Aux2 24 Hr Zone"
@Field static final String RESTORECOZONE = "Restore Co Zone"
@Field static final String RESTOREEMERGENCYZONE = "Restore Emergency Zone"
@Field static final String RESTOREFREEZEZONE = "Restore Freeze Zone"
@Field static final String RESTOREGASZONE = "Restore Gas Zone"
@Field static final String RESTOREHEATZONE = "Restore Heat Zone"
@Field static final String RESTOREWATERZONE = "Restore Water Zone"
@Field static final String COMMUNICATIONFAILRESTORE = "Communication Fail Restore"
@Field static final String ACFAILRESTORE = "Ac Fail Restore"
@Field static final String LOWBATTERYRESTORE = "Low Battery Restore"
@Field static final String CONTROLOVERCURRENTRESTORE = "Control Over Current Restore"
@Field static final String EXPANSIONMODULERESTORE = "Expansion Module Restore"
@Field static final String OUTPUT2RESTORE = "Output2 Restore"
@Field static final String TELEPHONELINERESTORE = "Telephone Line Restore"
@Field static final String ALARMMEMORYANYAREA = "Alarm Memory, Any Area"
@Field static final String AREAARMED = "Area Armed"
@Field static final String AREADISARMED = "Area Disarmed"
@Field static final String AREA1ARMSTATE = "Area 1 Armed State"
@Field static final String AREA1ISARMEDAWAY = "Area 1 Is Armed Away"
@Field static final String AREA1ISARMEDSTAY = "Area 1 Is Armed Stay"
@Field static final String AREA1ISARMEDSTAYINSTANT = "Area 1 Is Armed Stay Instant"
@Field static final String AREA1ISARMEDNIGHT = "Area 1 Is Armed Night"
@Field static final String AREA1ISARMEDNIGHTINSTANT = "Area 1 Is Armed Night Instant"
@Field static final String AREA1ISARMEDVACATION = "Area 1 Is Armed Vacation"
@Field static final String AREA1ISFORCEARMED = "Area 1 Is Force Armed"
@Field static final String ZONEBYPASSED = "Zone Bypassed"
@Field static final String ZONEUNBYPASSED = "Zone Unbypassed"
@Field static final String ANYBURGLARZONEISFAULTED = "Any Burglar Zone Is Faulted"
@Field static final String BURGLARSTATUSOFALLAREAS = "Burglar Status Of All Areas"
@Field static final String AREA1CHIMEMODE = "Area 1 Chime Mode"
@Field static final String AREA1CHIMEALERT = "Area 1 Chime Alert"
@Field static final String ENTRYDELAYANYAREA = "Entry Delay, Any Area"
@Field static final String EXITDELAYANYAREA = "Exit Delay, Any Area"
@Field static final String AREA1EXITDELAYENDS = "Area 1 Exit Delay Ends"

// Event Mapping
@Field final Map 	elkResponses = [
1000: NoEvent,
1001: FIREALARM,
1002: FIRESUPERVISORYALARM,
1003: BURGLARALARMANYAREA,
1004: MEDICALALARMANYAREA,
1005: POLICEALARMANYAREA,
1006: AUX124HRANYAREA,
1007: AUX224HRANYAREA,
1008: CARBONMONOXIDEALARMANYAREA,
1009: EMERGENCYALARMANYAREA,
1010: FREEZEALARMANYAREA,
1011: GASALARMANYAREA,
1012: HEATALARMANYAREA,
1013: WATERALARMANYAREA,
1014: ALARMANYAREA,
1111: CODELOCKOUTANYKEYPAD,
1128: FIRETROUBLEANYZONE,
1129: BURGLARTROUBLEANYZONE,
1130: FAILTOCOMMUNICATETROUBLE,
1131: RFSENSORLOWBATTERYTROUBLE,
1132: LOSTANCMODULETROUBLE,
1133: LOSTKEYPADTROUBLE,
1134: LOSTINPUTEXPANDERTROUBLE,
1135: LOSTOUTPUTEXPANDERTROUBLE,
1136: EEPROMMEMORYERRORTROUBLE,
1137: FLASHMEMORYERRORTROUBLE,
1138: ACFAILURETROUBLE,
1139: CONTROLLOWBATTERYTROUBLE,
1140: CONTROLOVERCURRENTTROUBLE,
1141: EXPANSIONMODULETROUBLE,
1142: OUTPUT2SUPERVISORYTROUBLE,
1143: TELEPHONELINEFAULTTROUBLE1,
1144: RESTOREFIREZONE,
1145: RESTOREFIRESUPERVISORYZONE,
1146: RESTOREBURGLARZONE,
1147: RESTOREMEDICALZONE,
1148: RESTOREPOLICEZONE,
1149: RESTOREAUX124HRZONE,
1150: RESTOREAUX224HRZONE,
1151: RESTORECOZONE,
1152: RESTOREEMERGENCYZONE,
1153: RESTOREFREEZEZONE,
1154: RESTOREGASZONE,
1155: RESTOREHEATZONE,
1156: RESTOREWATERZONE,
1157: COMMUNICATIONFAILRESTORE,
1158: ACFAILRESTORE,
1159: LOWBATTERYRESTORE,
1160: CONTROLOVERCURRENTRESTORE,
1161: EXPANSIONMODULERESTORE,
1162: OUTPUT2RESTORE,
1163: TELEPHONELINERESTORE,
1164: ALARMMEMORYANYAREA,
1173: AREAARMED,
1174: AREADISARMED,
1175: AREA1ARMSTATE,
1183: AREA1ISARMEDAWAY,
1191: AREA1ISARMEDSTAY,
1199: AREA1ISARMEDSTAYINSTANT,
1207: AREA1ISARMEDNIGHT,
1215: AREA1ISARMEDNIGHTINSTANT,
1223: AREA1ISARMEDVACATION,
1231: AREA1ISFORCEARMED,
1239: ZONEBYPASSED,
1240: ZONEUNBYPASSED,
1241: ANYBURGLARZONEISFAULTED,
1242: BURGLARSTATUSOFALLAREAS,
1251: AREA1CHIMEMODE,
1259: AREA1CHIMEALERT,
1267: ENTRYDELAYANYAREA,
1276: EXITDELAYANYAREA,
1285: AREA1EXITDELAYENDS
]


@Field static final String Disarmed = "Disarmed"
@Field static final String ArmedAway = "Armed Away"
@Field static final String ArmedStay = "Armed Stay"
@Field static final String ArmedStayInstant = "Armed Stay Instant"
@Field static final String ArmedtoNight = "Armed To Night"
@Field static final String ArmedtoNightInstant = "Armed To Night Instance"
@Field static final String ArmedtoVacation = "Armed To Vacation"

@Field final Map	elkArmStatuses = [
  '0': Disarmed,
  '1': ArmedAway,
  '2': ArmedStay,
  '3': ArmedStayInstant,
  '4': ArmedtoNight,
  '5': ArmedtoNightInstant,
  '6': ArmedtoVacation
]

@Field static final String NotReadytoArm = "Not Ready to Arm"
@Field static final String ReadytoArm = "Ready to Arm"
@Field static final String ReadytoArmBut = "Ready to Arm, but a zone is violated and can be force armed"
@Field static final String ArmedwithExit = "Armed with Exit Timer working"
@Field static final String ArmedFully = "Armed Fully"
@Field static final String ForceArmed = "Force Armed with a force arm zone violated"
@Field static final String ArmedwithaBypass = "Armed with a Bypass"

@Field final Map	elkArmUpStates = [
  '0': NotReadytoArm,
  '1': ReadytoArm,
  '2': ReadytoArmBut,
  '3': ArmedwithExit,
  '4': ArmedFully,
  '5': ForceArmed,
  '6': ArmedwithaBypass
	]

@Field static final String NoActiveAlarm = "No Active Alarm"
@Field static final String EntranceDelayisActive = "Entrance Delay is Active"
@Field static final String AlarmAbortDelayActive = "Alarm Abort Delay Active"
@Field static final String FireAlarm = "Fire Alarm"
@Field static final String MedicalAlarm = "Medical Alarm"
@Field static final String PoliceAlarm = "Police Alarm"
@Field static final String BurgularAlarm = "Burgular Alarm"

@Field final Map	elkAlarmStates = [
  '0': NoActiveAlarm,
  '1': EntranceDelayisActive,
  '2': AlarmAbortDelayActive,
  '3': FireAlarm,
  '4': MedicalAlarm,
  '5': PoliceAlarm,
  '6': BurgularAlarm
	]

// Zone Status Mapping Readable Text
@Field static final String NormalUnconfigured = "Normal: Unconfigured"
@Field static final String NormalOpen = "Normal: Open"
@Field static final String NormalEOL = "Normal: EOL"
@Field static final String NormalShort = "Normal: Short"
@Field static final String TroubleOpen = "Trouble: Open"
@Field static final String TroubleEOL = "Trouble: EOL"
@Field static final String TroubleShort = "Trouble: Short"
@Field static final String notused = "not used"
@Field static final String ViolatedOpen = "Violated: Open"
@Field static final String ViolatedEOL = "Violated: EOL"
@Field static final String ViolatedShort = "Violated: Short"
@Field static final String SoftBypassed = "Soft Bypassed"
@Field static final String BypassedOpen = "Bypassed: Open"
@Field static final String BypassedEOL = "Bypassed: EOL"
@Field static final String BypassedShort = "Bypassed: Short"

// Zone Status Mapping
@Field final Map	elkZoneStatuses  = [
  '0': NormalUnconfigured,
  '1': NormalOpen,
  '2': NormalEOL,
  '3': NormalShort,
  '5': TroubleOpen,
  '6': TroubleEOL,
  '7': TroubleShort,
  '8': notused,
  '9': ViolatedOpen,
  'A': ViolatedEOL,
  'B': ViolatedShort,
  'C': SoftBypassed,
  'D': BypassedOpen,
  'E': BypassedEOL,
  'F': BypassedShort

]

@Field static final String Off = "off"
@Field static final String On = "on"

@Field final Map	elkOutputStates = [
  '0': Off,
  '1': On
]

@Field static final String Fahrenheit = "Fahrenheit"
@Field static final String Celcius = "Celcius"

@Field final Map	elkTemperatureModes = [
  F: Fahrenheit,
  C: Celcius
]

@Field static final String User = "User"

@Field final Map	elkUserCodeTypes = [
  1: User,
]

@Field static final String ZoneName = "Zone Name"
@Field static final String AreaName = "Area Name"
@Field static final String UserName = "User Name"
@Field static final String Keypad = "Keypad"
@Field static final String OutputName = "Output Name"
@Field static final String TaskName = "Task Name"
@Field static final String TelephoneName = "Telephone Name"
@Field static final String LightName = "Light Name"
@Field static final String AlarmDurationName = "Alarm Duration Name"
@Field static final String CustomSettings = "Custom Settings"
@Field static final String CountersNames = "Counters Names"
@Field static final String ThermostatNames = "Thermostat Names"
@Field static final String FunctionKey1Name = "FunctionKey1 Name"
@Field static final String FunctionKey2Name = "FunctionKey2 Name"
@Field static final String FunctionKey3Name = "FunctionKey3 Name"
@Field static final String FunctionKey4Name = "FunctionKey4 Name"
@Field static final String FunctionKey5Name = "FunctionKey5 Name"
@Field static final String FunctionKey6Name = "FunctionKey6 Name"


@Field final Map	elkTextDescriptionsTypes = [
'0': ZoneName,
'1': AreaName,
'2': UserName,
'3': Keypad,
'4': OutputName,
'5': TaskName,
'6': TelephoneName,
'7': LightName,
'8': AlarmDurationName,
'9': CustomSettings,
'10': CountersNames,
'11': ThermostatNames,
'12': FunctionKey1Name,
'13': FunctionKey2Name,
'14': FunctionKey3Name,
'15': FunctionKey4Name,
'16': FunctionKey5Name,
'17': FunctionKey6Name
]


@Field static final String TemperatureProbe = "Temperature Probe"
@Field static final String Keypads = "Keypads"
@Field static final String Thermostats = "Thermostats"

@Field final Map	elkTempTypes = [
0: TemperatureProbe,
1: Keypads,
2: Thermostats
]

//NEW CODE
@Field static final String off = "off"
@Field static final String heat = "heat"
@Field static final String cool = "cool"
@Field static final String auto = "auto"
@Field static final String emergencyHeat = "emergency Heat"
@Field static final String False = "False"
@Field static final String True = "True"
@Field static final String FanAuto = "Fan Auto"
@Field static final String Fanturnedon = "Fan turned on"


@Field final Map 	elkThermostatMode = ['0': off, '1': heat, '2': cool, '3': auto, '4': emergencyHeat]
@Field final Map 	elkThermostatHold = ['0': False, '1': True]
@Field final Map 	elkThermostatFan = ['0': FanAuto, '1': Fanturnedon]

@Field final Map 	elkThermostatModeSet = [off: '0', heat: '1', cool: '2', auto: '3', emergencyHeat: '4']
@Field final Map 	elkThermostatFanModeSet = [auto: '0', on: '1']


@Field final Map	elkCommands = [

			Disarm: "a0",
			ArmAway: "a1",
			ArmHome: "a2",
			ArmStayInstant: "a3",
			ArmNight: "a4",
			ArmNightInstant: "a5",
			ArmVacation: "a6",
			ArmStepAway: "a7",
			ArmStepStay: "a8",
			RequestArmStatus: "as",
			AlarmByZoneRequest: "az",
			RequestTemperatureData: "lw",
			RequestRealTimeClockRead: "rr",
			RealTimeClockWrite: "rw",
			RequestTextDescriptions: "sd",
			Speakphrase: "sp",
			RequestSystemTroubleStatus: "ss",
			Requesttemperature: "st",
			Speakword: "sw",
			TaskActivation: "tn",
			RequestThermostatData: "tr",
			SetThermostatData: "ts",
			Requestusercodeareas: "ua",
			requestVersionNumberofM1: "vn",
			ReplyfromEthernettest: "xk",
			Zonebypassrequest: "zb",
			RequestZoneDefinitions: "zd",
			Zonepartitionrequest: "zp",
			ZoneStatusRequest: "zs",
			RequestZoneanalogvoltage: "zv",
			SetThermostatData: "ts",
			setHeatingSetpoint: "ts",
			setCoolingSetpoint: "ts",
			setThermostatSetpoint: "ts",
			setThermostatFanMode: "ts",
			setThermostatMode: "ts",
			auto: "ts",
			cool: "ts",
			emergencyHeat: "ts",
			fanAuto: "ts",
//			fanCirculate: "ts",
			fanOn: "ts",
			heat: "ts",
			off: "ts",
			ControlOutputOn: "cn",
			ControlOutputOff: "cf",
			ControlOutputToggle: "ct",
]

@Field static final String Disabled = "Disabled"
@Field static final String BurglarEntryExit1 = "Burglar Entry/Exit 1"
@Field static final String BurglarEntryExit2 = "Burglar Entry/Exit 2"
@Field static final String BurglarPerimeterInstant = "Burglar Perimeter Instant"
@Field static final String BurglarInterior = "Burglar Interior"
@Field static final String BurglarInteriorFollower = "Burglar Interior Follower"
@Field static final String BurglarInteriorNight = "Burglar Interior Night"
@Field static final String BurglarInteriorNightDelay = "Burglar Interior Night Delay"
@Field static final String Burglar24Hour = "Burglar 24 Hour"
@Field static final String BurglarBoxTamper = "Burglar Box Tamper"
@Field static final String aFireAlarm = "Fire Alarm"
@Field static final String FireVerified = "Fire Verified"
@Field static final String FireSupervisory = "Fire Supervisory"
@Field static final String AuxAlarm1 = "Aux Alarm 1"
@Field static final String AuxAlarm2 = "Aux Alarm 2"
//@Field static final String Keyfob = "Key fob"
//@Field static final String NonAlarm = "Non Alarm"
@Field static final String CarbonMonoxide = "Carbon Monoxide"
@Field static final String EmergencyAlarm = "Emergency Alarm"
@Field static final String FreezeAlarm = "Freeze Alarm"
@Field static final String GasAlarm = "Gas Alarm"
@Field static final String HeatAlarm = "Heat Alarm"
@Field static final String aMedicalAlarm = "Medical Alarm"
@Field static final String aPoliceAlarm = "Police Alarm"
@Field static final String PoliceNoIndication = "Police No Indication"
@Field static final String WaterAlarm = "Water Alarm"
@Field static final String KeyMomentaryArmDisarm = "Key Momentary Arm / Disarm"
@Field static final String KeyMomentaryArmAway = "Key Momentary Arm Away"
@Field static final String KeyMomentaryArmStay = "Key Momentary Arm Stay"
@Field static final String KeyMomentaryDisarm = "Key Momentary Disarm"
@Field static final String KeyOnOff = "Key On/Off"
@Field static final String MuteAudibles = "Mute Audibles"
@Field static final String PowerSupervisory = "Power Supervisory"
@Field static final String Temperature = "Temperature"
@Field static final String AnalogZone = "Analog Zone"
@Field static final String PhoneKey = "Phone Key"
@Field static final String IntercomKey = "Intercom Key"

@Field final Map	elkZoneDefinitions = [
'0': Disabled,
'1': BurglarEntryExit1,
'2': BurglarEntryExit2,
'3': BurglarPerimeterInstant,
'4': BurglarInterior,
'5': BurglarInteriorFollower,
'6': BurglarInteriorNight,
'7': BurglarInteriorNightDelay,
'8': Burglar24Hour,
'9': BurglarBoxTamper,
':': aFireAlarm,
';': FireVerified,
'<': FireSupervisory,
'=': AuxAlarm1,
'>': AuxAlarm2,
//'?' //not used: Keyfob,
//'@' //not used: NonAlarm,
'A': CarbonMonoxide,
'B': EmergencyAlarm,
'C': FreezeAlarm,
'D': GasAlarm,
'E': HeatAlarm,
'F': aMedicalAlarm,
'G': aPoliceAlarm,
'H': PoliceNoIndication,
'I': WaterAlarm,
'J': KeyMomentaryArmDisarm,
'K': KeyMomentaryArmAway,
'L': KeyMomentaryArmStay,
'M': KeyMomentaryDisarm,
'N': KeyOnOff,
'O': MuteAudibles,
'P': PowerSupervisory,
'Q': Temperature,
'R': AnalogZone,
'S': PhoneKey,
'T': IntercomKey
]

//Not currently using this
//@Field static final String Disabled = "Disabled"
@Field static final String ContactBurglarEntryExit1 = "Contact"
@Field static final String ContactBurglarEntryExit2 = "Contact"
@Field static final String ContactBurglarPerimeterInstant = "Contact"
@Field static final String MotionBurglarInterior = "Motion"
@Field static final String MotionBurglarInteriorFollower = "Motion"
@Field static final String MotionBurglarInteriorNight = "Motion"
@Field static final String MotionBurglarInteriorNightDelay = "Motion"
@Field static final String AlertBurglar24Hour = "Alert"
@Field static final String AlertBurglarBoxTamper = "Alert"
@Field static final String AlertFireAlarm = "Alert"
@Field static final String AlertFireVerified = "Alert"
@Field static final String AlertFireSupervisory = "Alert"
@Field static final String AlertAuxAlarm1 = "Alert"
@Field static final String AlertAuxAlarm2 = "Alert"
@Field static final String AlertCarbonMonoxide = "Alert"
@Field static final String AlertEmergencyAlarm = "Alert"
@Field static final String AlertFreezeAlarm = "Alert"
@Field static final String AlertGasAlarm = "Alert"
@Field static final String AlertHeatAlarm = "Alert"
@Field static final String AlertMedicalAlarm = "Alert"
@Field static final String AlertPoliceAlarm = "Alert"
@Field static final String AlertPoliceNoIndication = "Alert"
@Field static final String AlertWaterAlarm = "Alert"

//Not currently using this
@Field final Map	elkZoneTypes = [
//'0': Disabled,
'1': ContactBurglarEntryExit1,
'2': ContactBurglarEntryExit2,
'3': ContactBurglarPerimeterInstant,
'4': MotionBurglarInterior,
'5': MotionBurglarInteriorFollower,
'6': MotionBurglarInteriorNight,
'7': MotionBurglarInteriorNightDelay,
'8': AlertBurglar24Hour,
'9': AlertBurglarBoxTamper,
':': AlertFireAlarm,
';': AlertFireVerified,
'<': AlertFireSupervisory,
'=': AlertAuxAlarm1,
'>': AlertAuxAlarm2,
'A': AlertCarbonMonoxide,
'B': AlertEmergencyAlarm,
'C': AlertFreezeAlarm,
'D': AlertGasAlarm,
'E': AlertHeatAlarm,
'F': AlertMedicalAlarm,
'G': AlertPoliceAlarm,
'H': AlertPoliceNoIndication,
'I': AlertWaterAlarm
]

/***********************************************************************************************************************
*
* Release Notes (see Known Issues Below)
*
* 0.1.24
* Rerouted some code for efficiency
* Turned off some of the extra debugging
*
* 0.1.23
* Outputs are now functional with Rule Machine
* Change switch case to else if statements
*
* 0.1.22
* Fixed code to show operating state and thermostat setpoint on dashboard tile
* Reorder some code to see if it helps with some delay issues
* Consolidated code for zone open and zone closed to see if it helps with some delay issues (need to check if this has any other impact elsewhere)
*
* 0.1.21
* Updated mapping for output reporting code
* Changed Reply Arming Status Report Data to work as Area 1 only and to report current states
*
* 0.1.20
* Added back some code for 'Reply Arming Status Report Data (AS)' to clean up logging
*
* 0.1.19
* Removed some code for 'Reply Arming Status Report Data (AS)' to clean up logging
*
* 0.1.18
* Add support for Occupancy Sensors - this will be a work in progress since not sure how to code it
*
* 0.1.17
* Changed devices 'isComponent' to 'False' - this will allow the removal of devices and changing of drivers
*
* 0.1.16
* Changed the one import to be not case sensitive
*
* 0.1.15
* Added support for manual inclusion of Elk M1 outputs and tasks
* Added support for importing Elk M1 outputs, tasks and thermostats
* Added better support for child devices (all communication goes through Elk M1 device)
* Cleaned up some descriptions and instructions
*
* 0.1.14
* Added support for importing Elk M1 zones
* Fixed erroneous error codes
* Added actuator capability to allow custom commands to work in dashboard and rule machine
* Added command to request temperature data
* 0.1.13
* Elk M1 Code - No longer requires a 6 digit code (Add leading zeroes to 4 digit codes)
* Outputs and tasks can now be entered as a number
* Code clean up - removed some unused code
* 0.1.12
* Added support for outputs
* 0.1.11
* Built seperate thermostat child driver should allow for multiple thermostats
* 0.1.10
* Ability to control thermostat 1
* 0.1.9
* Minor changes
* 0.1.8
* Ability to read thermostat data (haven't confirmed multiple thermostat support)
* Added additional mappings for thermostat support
* Additional code clean up
* 0.1.7
* Rewrite of the various commands
* 0.1.6
* Changed text description mapping to string characters
* 0.1.5
* Added zone types
* Added zone definitions
* 0.1.4
* Added additional command requests
* 0.1.3
* Receive status messages and interpret data
* 0.1.2
* Minor changes to script nomenclature and formating
* 0.1.1
* Abiltiy to connect Elk M1 and see data
* Ability to send commands to Elk M1
* Changed code to input parameter
*
***********************************************************************************************************************/
/***********************************************************************************************************************
*
*Feature Request & Known Issues
* I - Port is hard coded to 2101
* I - Area is hard coded to 1
* F - Import Zone data from Elk
* F - Arm and Disarm from dashboard
* F - Controls for thermostat 1 (mulitple)
* F - Activate elk task by name (via dashboard button)
* F - Output support
* F - Lighting support (this is low priority for me since HE is handling my Zwave lights)
* F - Thermostat setup page (currenty uses the zone map page)
* I - Fan Circulate, emergency heat and set schedule not supported
* F - Request text descriptions for zone setup, tasks and outputs
* I - A device with the same device network ID exists (this is really not an issue)
*
***********************************************************************************************************************/
