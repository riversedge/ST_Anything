/**
 *  Radon Monitor
 *
 *  Copyright 2022
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
	definition (
    	name: "Radon Monitor", 
        namespace: "riversedge", 
        author: "Rivers Edge",
		"mnmn": "SmartThings",
    	"vid": "SmartThings-smartthings-Radon_Monitor"
    ) {
        capability "Signal Strength"
        capability "Presence Sensor"  //used to determine is the Arduino microcontroller is still reporting data or not
        capability "publicdouble60310.customRadonMonitor"
        capability "afterwatch06989.refresh"
        
        command "sendData", ["string"]
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
    
    /*
    tiles(scale: 2) {
 		multiAttributeTile(name: "wcstatus", type: "generic", width: 6, height: 4) {
			tileAttribute("device.wcstatus", key: "PRIMARY_CONTROL") {
				attributeState "Off", label:'${name}', backgroundColor:"#B71C1C"
                attributeState "Low", label:'${name}', backgroundColor:"#FDD835"
                attributeState "OK", label:'${name}', backgroundColor:"#00C853"
                attributeState "High", label:'${name}', backgroundColor:"#FDD835"
                attributeState "Critical", label:'${name}', backgroundColor:"#B71C1C"
		   	}
		}
        
        valueTile("pressure", "device.pressure", width: 2, height: 2) {
            state("pressure", label:'${currentValue} WC',
                backgroundColors:[
                    [value: 0.5, color: "#B71C1C"],
                    [value: 1.2, color: "#FDD835"],
                    [value: 1.7, color: "#00C853"],
                    [value: 2.0, color: "#FDD835"],
                    [value: 2.5, color: "#B71C1C"]
                ]
              )
         }
         valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label:'${currentValue} WC',
                backgroundColors:[
                    [value: 0.5, color: "#B71C1C"],
                    [value: 1.2, color: "#FDD835"],
                    [value: 1.7, color: "#00C853"],
                    [value: 2.0, color: "#FDD835"],
                    [value: 2.5, color: "#B71C1C"]
                ]
              )
         }
         valueTile("rssi", "device.rssi", width: 2, height: 2) {
            state("rssi", label:'${currentValue}')
         }

      main(["wcstatus", "pressure"])
      details(["wcstatus", "pressure", "temperature"])
	}
    */
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

		if (name.startsWith("rssi")) {
			//log.debug "In parse: RSSI name = ${name}, value = ${value}"
           	results = createEvent(name: name, value: value, displayed: false)
            log.debug "RSSI result: " + results
			return results
        }
        
        if (name.startsWith("radon")) {
        log.debug "parse(${description}) called"
            def attrname  = value
            def attrvalue = value2
            if (attrname && attrvalue) {
                def attrFloat = Float.valueOf(attrvalue);
                // Update device
                if (attrname.equals("temperature")) {
                   results = createEvent(name: attrname, value: attrvalue, unit: "F");
                   log.debug "Temp Result: " + results
                   return results
                }
                else if (attrname.equals("pressure")) {   
                    if (attrFloat < 0.5) {
                        sendEvent(name: "wcstatus", value: "Off")
                        //sendEvent(name: "alarm", value: "strobe")
                    }
                    else if (attrFloat < 1.2) {
                        sendEvent(name: "wcstatus", value: "Low")
                        //sendEvent(name: "alarm", value: "off")
                    }
                    else if (attrFloat < 1.7) {
                        sendEvent(name: "wcstatus", value: "OK")
                        //sendEvent(name: "alarm", value: "off")
                    }
                    else if (attrFloat < 2.0) {
                        sendEvent(name: "wcstatus", value: "High")
                        //sendEvent(name: "alarm", value: "off")
                    }
                    else {
                        sendEvent(name: "wcstatus", value: "Critical")
                        ///sendEvent(name: "alarm", value: "strobe")
                    }
                    results = createEvent(name: attrname, value: attrvalue, unit: "WC");
                    
                    //sendEvent(name: "atmosphericPressure", value: attrFloat)
                    log.debug "Pressure Result: " + results
                    return results
                 }
                 log.debug("Setting " + attrname + " = " + attrvalue);

        //        // Update lastUpdated date and time
        //        def nowDay = new Date().format("MMM dd", location.timeZone)
        //        def nowTime = new Date().format("h:mm a", location.timeZone)
        //        sendEvent(name: "lastUpdated", value: nowDay + " at " + nowTime, displayed: false)
            }
            else {
                log.debug "Missing either name or value.  Cannot parse!"
            }
        }
        else
        {
            results = createEvent(name: name, value: value)
            log.debug "Other Result: " + results
            return results
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

    //state.numButtons = 0
    //sendEvent(name: "numberOfButtons", value: state.numButtons)
}

def uninstalled() {
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