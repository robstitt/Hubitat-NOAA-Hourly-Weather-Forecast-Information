/*  ****************  NOAA Weather Alert Device Driver  ****************
 *
 *  importUrl: xxx
 *
 *  Copyright 2019 Aaron Ward
 *  Copyright 2021 Robert L. Stitt
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 * Last Update: 01/21/2021
 */

metadata {
   definition (
      name: "NOAA Weather Alert Device",
      namespace: "robstitt",
      author: "Robert L. Stitt"
      //importUrl: "https://raw.githubusercontent.com/imnotbob/Hubitat-4/master/NOAA/NOAA-Tile-Driver.groovy"
       ) {
      command "initialize"
      command "refresh"
      capability "Actuator"
      capability "Refresh"
      attribute "Text", "string"
      attribute "Level", "string"
      attribute "Urgency", "string"
      attribute "Certainty", "string"
      attribute "Severity", "string"
      attribute "EventType", "string"
      attribute "Expires", "string"
      }

   preferences() {
      input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: false)
   }
}

def initialize() {
   log.info "NOAA Weather Alert Device Driver Initializing."
   refresh()
}

def updated() {
   refresh()
}

def installed(){
   log.info "NOAA Weather Alert Device has been Installed."
   sendEvent(name: "Text", value: "None", displayed: true)
   sendEvent(name: "Level", value: "", displayed: true)
   sendEvent(name: "Urgency", value: "", displayed: true)
   sendEvent(name: "Certainty", value: "", displayed: true)
   sendEvent(name: "Severity", value: "", displayed: true)
   sendEvent(name: "EventType", value: "", displayed: true)
   sendEvent(name: "Expires", value: "", displayed: true)
}

void logsOff(){
   log.warn "Debug logging disabled."
   device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def refresh() {
   if(logEnable) log.info "Getting most severe weather alert data from parent NOAA Weather Alert Monitor App"

   Map noaaData = [:]

   try { noaaData = (Map)parent.getAlertDevice() }
   catch (e) {if(logEnable) log.warn "Error getting most severe weather alert data from parent NOAA Weather Alert Monitor App: $e"}
 
   if(noaaData.text==null) {
      noaaData = [text:"None", level:"-", urgency:"-", certainty:"-", severity:"-", eventtype:"-", expires:"-"]
      if(logEnable) log.warn "Null alert data received from the parent NOAA Weather Alert Monitor App"
   }

    if(logEnable) {
        if (noaaData.text!="None") {
            log.info "Most severe alert received from the parent NOAA Weather Alert Monitor App: : ${noaaData.level}(${noaaData.text})"
        } else {
            log.info "Clearing alert based on no active alert received from the parent NOAA Weather Alert Monitor App"
        }
    }
    
    sendEvent(name: "Text", value: noaaData.text, displayed: true)
    sendEvent(name: "Level", value: noaaData.level, displayed: true)
    sendEvent(name: "Urgency", value: noaaData.urgency, displayed: true)
    sendEvent(name: "Certainty", value: noaaData.certainty, displayed: true)
    sendEvent(name: "Severity", value: noaaData.severity, displayed: true)
    sendEvent(name: "EventType", value: noaaData.eventtype, displayed: true)
    sendEvent(name: "Expires", value: noaaData.expires, displayed: true)
}
