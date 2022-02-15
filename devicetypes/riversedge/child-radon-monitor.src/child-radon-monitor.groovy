/**
 *  Child Radon Monitor
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
 *    2022-02-13  Rivers Edge     Set up for WC measurement instead of hPa
 *
 * 
 */
metadata {
	definition (
    	name: "Child Radon Monitor", 
        namespace: "riversedge", 
        author: "Rivers Edge",
    ) {
        capability "atmosphericPressureMeasurement"
        capability "Alarm"
   		capability "Temperature Measurement"    
		capability "Sensor" 
        
        attribute "pressure", "Number"   //ST does not have a standard Capability for Pressure Measurement
	}

	simulator {
	}
    
	tiles(scale: 2) {
 		multiAttributeTile(name: "status", type: "generic", width: 6, height: 4) {
			tileAttribute("device.status", key: "PRIMARY_CONTROL") {
				attributeState "Off", label:'${name}', backgroundColor:"#B71C1C"
                attributeState "Low", label:'${name}', backgroundColor:"#FDD835"
                attributeState "OK", label:'${name}', backgroundColor:"#00C853"
                attributeState "High", label:'${name}', backgroundColor:"#FDD835"
                attributeState "Critical", label:'${name}', backgroundColor:"#B71C1C"
		   	}
		}
        
        valueTile("pressure", "device.pressure", width: 3, height: 2) {
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
         valueTile("temperature", "device.temperature", width: 3, height: 2) {
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

      main(["status", "pressure", "temperature"])
      details(["status", "pressure", "temperature"])
	}
}

def parse(String description) {
   log.debug "parse(${description}) called"
	def parts = description.split(" ")
    def montype  = parts.length>0?parts[0].trim():null
    def attrname  = parts.length>1?parts[1].trim():null
    def attrvalue = parts.length>2?parts[2].trim():null
    if (attrname && attrvalue) {
        def attrFloat = Float.valueOf(attrvalue);
        // Update device
        sendEvent(name: attrname, value: attrvalue)
        if (attrname.equals("pressure")) {        
            if (attrFloat < 0.5) {
            	sendEvent(name: "status", value: "Off")
                sendEvent(name: "alarm", value: "strobe")
            }
            else if (attrFloat < 1.2) {
            	sendEvent(name: "status", value: "Low")
                sendEvent(name: "alarm", value: "off")
            }
            else if (attrFloat < 1.7) {
            	sendEvent(name: "status", value: "OK")
                sendEvent(name: "alarm", value: "off")
            }
            else if (attrFloat < 2.0) {
            	sendEvent(name: "status", value: "High")
                sendEvent(name: "alarm", value: "off")
            }
            else {
            	sendEvent(name: "status", value: "Critical")
                sendEvent(name: "alarm", value: "strobe")
            }
            
            sendEvent(name: "atmosphericPressure", value: attrFloat)
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

def updated() {
   initialize()
}

def installed() {
   initialize()
}

def initialize() {
}
