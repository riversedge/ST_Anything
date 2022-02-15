/**
 *  Parent_ST_Anything_Ethernet.groovy
 *
 *  Copyright 202222
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 *    
 *
 *	
 */
 
metadata {
	definition (name: "Parent_ST_Anything_Ethernet", namespace: "riversedge", author: "Rivers Edge", mnmn: "SmartThingsCommunity", vid: "5eb3144b-8bfb-37e4-90b6-021bc1223638") {
        //capability "Configuration"
        capability "Button"
        capability "Holdable Button"
        capability "Signal Strength"
        capability "Presence Sensor"  //used to determine is the Arduino microcontroller is still reporting data or not
        capability "afterwatch06989.refresh"
        
        command "sendData", ["string"]
        //command "deleteAllChildDevices"
	}

    simulator {
    }

    // Preferences
	preferences {
    	input "ip", "text", title: "Arduino IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
		input "port", "text", title: "Arduino Port", description: "port in form of 8090", required: true, displayDuringSetup: true
		input "mac", "text", title: "Arduino MAC Addr", description: "MAC Address in form of 02A1B2C3D4E5", required: true, displayDuringSetup: true
		input "timeOut", "number", title: "Timeout in Seconds", description: "Arduino max time (try 900)", range: "120..86400", required: true, displayDuringSetup:true
	}
}

// parse events into attributes
def parse(String description) {
	//log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
	def headerString = msg.header

	if (!headerString) {
		//log.debug "headerstring was null for some reason :("
    }

	def bodyString = msg.body

	if (bodyString) {
        log.debug "Parsing: $bodyString"
    	def parts = bodyString.split(" ")
    	def name  = parts.length>0?parts[0].trim():null
    	def value = parts.length>1?parts[1].trim():null
        def value2 = parts.length>2?parts[2].trim():null
        
		def nameparts = name.split("\\d+", 2)
		def namebase = nameparts.length>0?nameparts[0].trim():null
        def namenum = name.substring(namebase.length()).trim()
		
        def results = []
 
        if (device.currentValue("presence") != "present") {
            sendEvent(name: "presence", value: "present", isStateChange: true, descriptionText: "New update received from Arduino device")
        }
        
		if (timeOut != null) {
            runIn(timeOut, timeOutArduino)
        } else {
           	log.info "Using 900 second default timeout.  Please set the timeout setting appropriately and then click save."
           	runIn(900, timeOutArduino)
        }
        
		if (name.startsWith("button")) {
			//log.debug "In parse:  name = ${name}, value = ${value}, btnName = ${name}, btnNum = ${namemun}"
             if (state.numButtons < namenum.toInteger()) {
                state.numButtons = namenum.toInteger()
                sendEvent(name: "numberOfButtons", value: state.numButtons)
            }
            
            if ((value == "pushed") || (value == "held")) {
                results = createEvent([name: namebase, value: value, data: [buttonNumber: namenum], descriptionText: "${namebase} ${namenum} was ${value} ", isStateChange: true, displayed: true])
                log.debug results
                return results
            }
            else
            {
                return
            }
        }

		if (name.startsWith("rssi")) {
			//log.debug "In parse: RSSI name = ${name}, value = ${value}"
           	results = createEvent(name: name, value: value, displayed: false)
            log.debug results
			return results
        }

        def isChild = containsDigit(name)
   		//log.debug "Name = ${name}, isChild = ${isChild}, namebase = ${namebase}, namenum = ${namenum}"      

		try {
            def childDevice = childDevices.find{it.deviceNetworkId == "${device.deviceNetworkId}-${name}"}
            //if (childDevice) log.debug "childDevice.deviceNetworkId = ${childDevice.deviceNetworkId}"
            
            //If a child should exist, but doesn't yet, automatically add it!            
        	if (isChild && childDevice == null) {
        		log.debug "isChild = true, but no child found - Auto Add it!"
            	//log.debug "    Need a ${namebase} with id = ${namenum}"            
            	childDevice = createChildDevice(namebase, namenum)
			}   
            
            if (childDevice != null) {
                //log.debug "parse() found child device ${childDevice.deviceNetworkId}"
                childDevice.parse("${namebase} ${value} ${value2}")
				log.debug "${childDevice.deviceNetworkId} - name: ${namebase}, value: ${value}"
            }
            else  //must not be a child, perform normal update
            {
                results = createEvent(name: name, value: value)
                log.debug results
                return results
            }
		}
        catch (e) {
        	log.error "Error in parse() routine, error = ${e}"
        }
	}
}

private getHostAddress() {
    def ip = settings.ip
    def port = settings.port
    
    log.debug "Using ip: ${ip} and port: ${port} for device: ${device.id}"
    return ip + ":" + port
}

def sendData(message) {
    sendEthernet(message) 
}

def sendEthernet(message) {
	log.debug "Executing 'sendEthernet( \"${message}\" )'"
	if (settings.ip != null && settings.port != null) {
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "POST",
            path: "/${message}?",
            headers: [ HOST: "${getHostAddress()}" ]
        ))
    }
    else {
        state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        runIn(2, "sendAlert")   
    }
}

// refresh capability command callback
def refresh() {
	log.debug "Executing 'refresh()'"
	sendEthernet("refresh")
}

def installed() {
	log.debug "Executing 'installed()'"
    if ( device.deviceNetworkId =~ /^[A-Z0-9]{12}$/)
    {
    }
    else
    {
        log.info "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        //state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        //runIn(2, "sendAlert")
    }

    state.numButtons = 0
    sendEvent(name: "numberOfButtons", value: state.numButtons)
}

def uninstalled() {
    deleteAllChildDevices()
}

def initialize() {
	log.debug "Executing 'initialize()'"
}

def updated() {
	if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 5000) {
		state.updatedLastRanAt = now()
		log.debug "Executing 'updated()'"
    	runIn(3, "updateDeviceNetworkID")

        log.debug "Hub IP Address = ${device.hub.getDataValue("localIP")}"
        log.debug "Hub Port = ${device.hub.getDataValue("localSrvPortTCP")}"

        //Schedule inactivity timeout
        log.info "Device inactivity timer started for ${timeOut} seconds"
        runIn(timeOut, timeOutArduino)

	}
	else {
		//log.trace "updated(): Ran within last 5 seconds so aborting."
	}
}

def updateDeviceNetworkID() {
	log.debug "Executing 'updateDeviceNetworkID'"
    def formattedMac = mac.toUpperCase()
    formattedMac = formattedMac.replaceAll(":", "")
    if(device.deviceNetworkId!=formattedMac) {
        log.debug "setting deviceNetworkID = ${formattedMac}"
        device.setDeviceNetworkId("${formattedMac}")
	}
    //Need deviceNetworkID updated BEFORE we can create Child Devices
	//Have the Arduino send an updated value for every device attached.  This will auto-created child devices!
	refresh()
}

private createChildDevice(String deviceName, String deviceNumber) {
    def deviceHandlerName = ""
    if ( device.deviceNetworkId =~ /^[A-Z0-9]{12}$/) {
    
		log.trace "createChildDevice:  Creating Child Device '${device.displayName} (${deviceName}${deviceNumber})'"
        
		try {
        	
        	switch (deviceName) {
         		case "contact": 
                		deviceHandlerName = "Child Contact Sensor" 
                	break
         		case "switch": 
                		deviceHandlerName = "Child Switch" 
                	break
         		case "dimmerSwitch": 
                		deviceHandlerName = "Child Dimmer Switch" 
                	break
         		case "rgbSwitch": 
                		deviceHandlerName = "Child RGB Switch" 
                	break
         		case "generic": 
                		deviceHandlerName = "Child Generic Sensor" 
                	break
         		case "rgbwSwitch": 
                		deviceHandlerName = "Child RGBW Switch" 
                	break
         		case "relaySwitch": 
                		deviceHandlerName = "Child Relay Switch" 
                	break
         		case "temperature": 
                		deviceHandlerName = "Child Temperature Sensor" 
                	break
         		case "humidity": 
                		deviceHandlerName = "Child Humidity Sensor" 
                	break
         		case "motion": 
                		deviceHandlerName = "Child Motion Sensor" 
                	break
         		case "water": 
                		deviceHandlerName = "Child Water Sensor" 
                	break
         		case "illuminance": 
                		deviceHandlerName = "Child Illuminance Sensor" 
                	break
         		case "illuminancergb": 
                		deviceHandlerName = "Child IlluminanceRGB Sensor" 
                	break
         		case "voltage": 
                		deviceHandlerName = "Child Voltage Sensor" 
                	break
         		case "smoke": 
                		deviceHandlerName = "Child Smoke Detector" 
                	break    
         		case "carbonMonoxide": 
                		deviceHandlerName = "Child Carbon Monoxide Detector" 
                	break    
         		case "alarm": 
                		deviceHandlerName = "Child Alarm" 
                	break    
         		case "doorControl": 
                		deviceHandlerName = "Child Door Control" 
                	break
         		case "ultrasonic": 
                		deviceHandlerName = "Child Ultrasonic Sensor" 
                	break
         		case "presence": 
                		deviceHandlerName = "Child Presence Sensor" 
                	break
         		case "power": 
                		deviceHandlerName = "Child Power Meter" 
                	break
         		case "energy": 
                		deviceHandlerName = "Child Energy Meter" 
                	break
         		case "servo": 
                		deviceHandlerName = "Child Servo" 
                	break
         		case "pressure": 
                		deviceHandlerName = "Child Pressure Measurement" 
                	break
         		case "radon": 
                		deviceHandlerName = "Child Radon Monitor" 
                	break
         		case "soundPressureLevel": 
                		deviceHandlerName = "Child Sound Pressure Level" 
                	break
         		case "valve": 
                		deviceHandlerName = "Child Valve" 
                	break
         		case "windowShade": 
                		deviceHandlerName = "Child Window Shade" 
                	break        
			default: 
                		log.error "No Child Device Handler case for ${deviceName}"
      		}
            if (deviceHandlerName != "") {
//                return addChildDevice(deviceHandlerName, "${device.deviceNetworkId}-${deviceName}${deviceNumber}", null,
//         			[completedSetup: true, label: "${device.displayName} (${deviceName}${deviceNumber})", 
//                	isComponent: false, componentName: "${deviceName}${deviceNumber}", componentLabel: "${deviceName} ${deviceNumber}"])
                return addChildDevice(deviceHandlerName, "${device.deviceNetworkId}-${deviceName}${deviceNumber}", null,
         			[completedSetup: true, label: "${device.displayName} (${deviceName}${deviceNumber})", 
                	isComponent: false])
        	}   
    	} catch (e) {
        	log.error "${deviceName}${deviceNumber} child device creation of type '${deviceHandlerName}' failed with error = ${e}"
        	log.error "Please delete/remove the offending child device in the ST Classic App, and then click Refresh on the Parent Device to have the child created again." 
            
            //state.alertMessage = "Child device creation failed. Please make sure that the '${deviceHandlerName}' is installed and published."
	    	//runIn(2, "sendAlert")
    	}
	} else 
    {
        state.alertMessage = "ST_Anything Parent Device has not yet been fully configured. Click the 'Gear' icon, enter data for all fields, and click 'Done'"
        runIn(2, "sendAlert")
    }
}

private sendAlert() {
   sendEvent(
      descriptionText: state.alertMessage,
	  eventType: "ALERT",
	  name: "childDeviceCreation",
	  value: "failed",
	  displayed: true,
   )
}

private boolean containsDigit(String s) {
    boolean containsDigit = false;

    if (s != null && !s.isEmpty()) {
		//log.debug "containsDigit .matches = ${s.matches(".*\\d+.*")}"
		containsDigit = s.matches(".*\\d+.*")
    }
    return containsDigit
}

def timeOutArduino() {
    //If the timeout expires before being reset, mark this Parent Device as 'not present' to allow action to be taken
    log.info "No update received from Arduino device in past ${timeOut} seconds"
    sendEvent(name: "presence", value: "not present", isStateChange: true, descriptionText: "No update received from Arduino device in past ${timeOut} seconds")
}

def deleteAllChildDevices() {
    log.info "Deleting all Child Devices"
    getChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
    state.numButtons = 0    
    sendEvent(name: "numberOfButtons", value: state.numButtons)
}

/*
def mywait(ms) {
    log.info "starting wait"
	def start = now()
	while (now() < start + ms) {
    	// hurry up and wait!
    }
    log.info "ending wait"
}
*/
