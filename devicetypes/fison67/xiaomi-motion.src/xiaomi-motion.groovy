/**
 *  Xiaomi Motion (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
*/
 
import groovy.json.JsonSlurper

metadata {
	definition (name: "Xiaomi Motion", namespace: "fison67", author: "fison67") {
        capability "Motion Sensor"
        capability "Illuminance Measurement"
        capability "Configuration"
        capability "Sensor"
        capability "Battery"
        capability "Refresh"
         
        attribute "lastMotion", "Date"

        attribute "lastCheckin", "Date"
	command "reset"	
         
	}


	simulator {
	}
	preferences {
		input "motionReset", "number", title: "Motion Reset Time", description: "", value:120, displayDuringSetup: true
	}


	tiles(scale: 2) {
		multiAttributeTile(name:"motion", type: "generic", width: 6, height: 4){
			tileAttribute ("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label:'motion', icon:"http://postfiles1.naver.net/MjAxODA0MDNfMTAz/MDAxNTIyNzI0MDQ3OTU1.KlL6RhQNyk29a6B2xLdYi8f7mWkZ_hDJmvLTcUYxFUog.zOxJRz6RrrZsUTkFj8BefZycoyKxoL0Eeq7Ep6Pdxw0g.PNG.shin4299/motion_on1.png?type=w3", backgroundColor:"#00a0dc"
				attributeState "inactive", label:'no motion', icon:"http://postfiles5.naver.net/MjAxODA0MDNfMTky/MDAxNTIyNzIzMDU3MTM4.mWDrfCVxx5OgUmoCZos7CkVgVY8jm3Ho4WgWeFnMbhMg.MB0MzqQCJM80xAFZ19imwE9AnHQ58Px2gHAOr9DSJLQg.PNG.shin4299/motion_off.png?type=w3", backgroundColor:"#ffffff"
			}
            tileAttribute("device.battery", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Battery: ${currentValue}%\n')
            }		
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'\nLast Update: ${currentValue}')
            }
		}
        
        valueTile("illuminance", "device.illuminance", width: 2, height: 2) {
            state "val", label:'${currentValue}', defaultState: true,
                backgroundColors:[
                    [value: 100, color: "#153591"],
                    [value: 200, color: "#1e9cbb"],
                    [value: 300, color: "#90d2a7"],
                    [value: 600, color: "#44b621"],
                    [value: 900, color: "#f1d801"],
                    [value: 1200, color: "#d04e00"],
                    [value: 1500, color: "#bc2323"]
                ]
        }
        
                standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", action:"reset", label: "Reset Motion", icon:"st.motion.motion.active"
        }

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
        valueTile("lastMotion_label", "", decoration: "flat") {
            state "default", label:'Last\nMotion'
        }
        valueTile("lastMotion", "device.lastMotion", decoration: "flat", width: 3, height: 1) {
            state "default", label:'${currentValue}'
        }		
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setStatus(params){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
 	switch(params.key){
    case "motion":
        sendEvent(name:"motion", value: (params.data == "true" ? "active" : "inactive") )
        if (settings.motionReset == null || settings.motionReset == "" ) settings.motionReset = 120
        if (params.data == "true") runIn(settings.motionReset, stopMotion)
	if (params.data == "true") sendEvent(name: "lastMotion", value: now)	

		
    	break;
    case "batteryLevel":
    	sendEvent(name:"battery", value: params.data)
    	break;
    case "illuminance":
    	sendEvent(name:"illuminance", value: params.data )
    	break;
    }
    
    updateLastTime()
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
        
        sendEvent(name:"battery", value: jsonObj.properties.batteryLevel)
        sendEvent(name:"motion", value: jsonObj.properties.motion == true ? "active" : "inactive")
        
        if(jsonObj.properties.illuminance != null && jsonObj.properties.illuminance != ""){
        	sendEvent(name:"illuminance", value: jsonObj.properties.illuminance.value + jsonObj.properties.illuminance.unit)
        }
      
        updateLastTime()

    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
}

def updateLastTime(){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
}

def stopMotion() {
   sendEvent(name:"motion", value:"inactive")
}

def reset() {
   sendEvent(name:"motion", value:"inactive")
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/control",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}
