/*  ****************  NOAA Hourly Weather Forecast Device Driver  ****************
 *
 *  importUrl: https://github.com/robstitt/Hubitat-NOAA-Hourly-Weather-Forecast-Information/raw/main/NOAA%20Hourly%20Weather%20Forecast%20Device%20Driver.groovy
 *
 *  Copyright 2019 Aaron Ward
 *  Copyright 2021-2024 Robert L. Stitt
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
 * Last Update: 01/23/2024
 */

metadata {
   definition (
      name: "NOAA Hourly Weather Forecast Device",
      namespace: "robstitt",
      author: "Robert L. Stitt"
      //importUrl: "https://github.com/robstitt/Hubitat-NOAA-Hourly-Weather-Forecast-Information/raw/main/NOAA%20Hourly%20Weather%20Forecast%20Device%20Driver.groovy"
       ) {
      command "initialize"
      command "refresh"
      capability "Actuator"
      capability "Refresh"
      attribute "CurTemp", "number"
      attribute "CurTempUnit", "string"
      attribute "CurWindSpeed", "string"
      attribute "CurWindDirection", "string"
      attribute "CurPrecipPct", "number"
      attribute "CurPrecipType", "string"
      attribute "CurPeriodStarts", "date"
      attribute "CurPeriodEnds", "date"
      attribute "CurIconURL", "string"
      attribute "CurSummary", "string"
      attribute "CurDetail", "string"

      attribute "NextTemp", "number"
      attribute "NextTempUnit", "string"
      attribute "NextWindSpeed", "string"
      attribute "NextWindDirection", "string"
      attribute "NextPrecipPct", "number"
      attribute "NextPrecipType", "string"
      attribute "NextPeriodStarts", "date"
      attribute "NextPeriodEnds", "date"
      attribute "NextIconURL", "string"
      attribute "NextSummary", "string"
      attribute "NextDetail", "string"

      attribute "ShortTermTempLow", "number"
      attribute "ShortTermTempHigh", "number"
      attribute "ShortTermTempUnit", "string"
      attribute "ShortTermWindSpeed", "string"
      attribute "ShortTermWindDirection", "string"
      attribute "ShortTermPrecipPct", "number"
      attribute "ShortTermPrecipType", "string"
      attribute "ShortTermPeriodStarts", "date"
      attribute "ShortTermPeriodEnds", "date"
      attribute "ShortTermIconURL", "string"
      attribute "ShortTermSummary", "string"
      attribute "ShortTermDetail", "string"
      }

   preferences() {
      input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: false)
   }
}

def initialize() {
   log.info "NOAA Hourly Weather Forecast Information Device Driver Initializing."
   refresh()
}

def updated() {
   refresh()
}

def installed(){
   log.info "NOAA Hourly Weather Forecast Information Device has been Installed."

   sendEvent(name: "CurTemp", value: "", displayed: true)
   sendEvent(name: "CurTempUnit", value: "", displayed: true)
   sendEvent(name: "CurWindSpeed", value: "", displayed: true)
   sendEvent(name: "CurWindDirection", value: "", displayed: true)
   sendEvent(name: "CurPrecipPct", value: "", displayed: true)
   sendEvent(name: "CurPrecipType", value: "", displayed: true)
   sendEvent(name: "CurPeriodStarts", value: "", displayed: true)
   sendEvent(name: "CurPeriodEnds", value: "", displayed: true)
   sendEvent(name: "CurIconURL", value: "", displayed: true)
   sendEvent(name: "CurSummary", value: "", displayed: true)
   sendEvent(name: "CurDetail", value: "", displayed: true)

   sendEvent(name: "NextTemp", value: "", displayed: true)
   sendEvent(name: "NextTempUnit", value: "", displayed: true)
   sendEvent(name: "NextWindSpeed", value: "", displayed: true)
   sendEvent(name: "NextWindDirection", value: "", displayed: true)
   sendEvent(name: "NextPrecipPct", value: "", displayed: true)
   sendEvent(name: "NextPrecipType", value: "", displayed: true)
   sendEvent(name: "NextPeriodStarts", value: "", displayed: true)
   sendEvent(name: "NextPeriodEnds", value: "", displayed: true)
   sendEvent(name: "NextIconURL", value: "", displayed: true)
   sendEvent(name: "NextSummary", value: "", displayed: true)
   sendEvent(name: "NextDetail", value: "", displayed: true)

   sendEvent(name: "ShortTermtTempLow", value: "", displayed: true)
   sendEvent(name: "ShortTermtTempHigh", value: "", displayed: true)
   sendEvent(name: "ShortTermTempUnit", value: "", displayed: true)
   sendEvent(name: "ShortTermtWindSpeed", value: "", displayed: true)
   sendEvent(name: "ShortTermWindDirection", value: "", displayed: true)
   sendEvent(name: "ShortTermPrecipPct", value: "", displayed: true)
   sendEvent(name: "ShortTermPrecipType", value: "", displayed: true)
   sendEvent(name: "ShortTermPeriodStarts", value: "", displayed: true)
   sendEvent(name: "ShortTermPeriodEnds", value: "", displayed: true)
   sendEvent(name: "ShortTermIconURL", value: "", displayed: true)
   sendEvent(name: "ShortTermSummary", value: "", displayed: true)
   sendEvent(name: "ShortTermDetail", value: "", displayed: true)
}

void logsOff(){
   log.warn "Debug logging disabled."
   device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def refresh() {
   if(logEnable) log.info "Getting the hourly forecast data from parent NOAA Hourly Weather Forecast Information App"

   Map CurHourForecast = [:]

   try {
      CurHourForecast = (Map)parent.getForecastCurrentDevice()
   }
   catch (e) {
      if(logEnable) log.warn "Error getting the current hour forecast data from parent NOAA Hourly Weather Forecast Information App: $e"
      CurHourForecast = [:]
   }

   if(CurHourForecast.equals([:])) {
      if(logEnable) log.warn "No current hour forecast data received from the parent NOAA Weather Alert Monitor App"
   } else {
      if(logEnable) log.info "Current hour forecast data received from parent NOAA Hourly Weather Forecast Information App."
      sendEvent(name: "CurTemp", value: CurHourForecast.Temp, displayed: true)
      sendEvent(name: "CurTempUnit", value: CurHourForecast.TempUnit, displayed: true)
      sendEvent(name: "CurWindSpeed", value: CurHourForecast.WindSpeed, displayed: true)
      sendEvent(name: "CurWindDirection", value: CurHourForecast.WindDirection, displayed: true)
      sendEvent(name: "CurPrecipPct", value: CurHourForecast.PrecipPct, displayed: true)
      sendEvent(name: "CurPrecipType", value: CurHourForecast.PrecipType, displayed: true)
      sendEvent(name: "CurPeriodStarts", value: CurHourForecast.Starts, displayed: true)
      sendEvent(name: "CurPeriodEnds", value: CurHourForecast.Ends, displayed: true)
      sendEvent(name: "CurIconURL", value: CurHourForecast.IconURL, displayed: true)
      sendEvent(name: "CurSummary", value: CurHourForecast.ShortForecast, displayed: true)
      sendEvent(name: "CurDetail", value: CurHourForecast.DetailedForecast, displayed: true)
   }

   Map NextHourForecast = [:]

   try {
      NextHourForecast = (Map)parent.getForecastNextDevice()
   }
   catch (e) {
      if(logEnable) log.warn "Error getting the next hour forecast data from parent NOAA Hourly Weather Forecast Information App: $e"
      NextHourForecast = [:]
   }

   if(NextHourForecast.equals([:])) {
      if(logEnable) log.warn "No next hour forecast data received from the parent NOAA Weather Alert Monitor App"
   } else {
      if(logEnable) log.info "Next hour forecast data received from parent NOAA Hourly Weather Forecast Information App."
      sendEvent(name: "NextTemp", value: NextHourForecast.Temp, displayed: true)
      sendEvent(name: "NextTempUnit", value: NextHourForecast.TempUnit, displayed: true)
      sendEvent(name: "NextWindSpeed", value: NextHourForecast.WindSpeed, displayed: true)
      sendEvent(name: "NextWindDirection", value: NextHourForecast.WindDirection, displayed: true)
      sendEvent(name: "NextPrecipPct", value: NextHourForecast.PrecipPct, displayed: true)
      sendEvent(name: "NextPrecipType", value: NextHourForecast.PrecipType, displayed: true)
      sendEvent(name: "NextPeriodStarts", value: NextHourForecast.Starts, displayed: true)
      sendEvent(name: "NextPeriodEnds", value: NextHourForecast.Ends, displayed: true)
      sendEvent(name: "NextIconURL", value: NextHourForecast.IconURL, displayed: true)
      sendEvent(name: "NextSummary", value: NextHourForecast.ShortForecast, displayed: true)
      sendEvent(name: "NextDetail", value: NextHourForecast.DetailedForecast, displayed: true)
   }

   Map ShortTermForecast = [:]

   try {
      ShortTermForecast = (Map)parent.getForecastShortTermDevice()
   }
   catch (e) {
      if(logEnable) log.warn "Error getting the short term forecast data from parent NOAA Hourly Weather Forecast Information App: $e"
      ShortTermForecast = [:]
   }

   if(ShortTermForecast.equals([:])) {
      if(logEnable) log.warn "No short term forecast data received from the parent NOAA Weather Alert Monitor App"
   } else {
      if(logEnable) log.info "Short term forecast data received from parent NOAA Hourly Weather Forecast Information App."
      sendEvent(name: "ShortTermTempLow", value: ShortTermForecast.TempLow, displayed: true)
      sendEvent(name: "ShortTermTempHigh", value: ShortTermForecast.TempHigh, displayed: true)
      sendEvent(name: "ShortTermTempUnit", value: ShortTermForecast.TempUnit, displayed: true)
      sendEvent(name: "ShortTermWindSpeed", value: ShortTermForecast.WindSpeed, displayed: true)
      sendEvent(name: "ShortTermWindDirection", value: ShortTermForecast.WindDirection, displayed: true)
      sendEvent(name: "ShortTermPrecipPct", value: ShortTermForecast.PrecipPct, displayed: true)
      sendEvent(name: "ShortTermPrecipType", value: ShortTermForecast.PrecipType, displayed: true)
      sendEvent(name: "ShortTermPeriodStarts", value: ShortTermForecast.Starts, displayed: true)
      sendEvent(name: "ShortTermPeriodEnds", value: ShortTermForecast.Ends, displayed: true)
      sendEvent(name: "ShortTermIconURL", value: ShortTermForecast.IconURL, displayed: true)
      sendEvent(name: "ShortTermSummary", value: ShortTermForecast.ShortForecast, displayed: true)
      sendEvent(name: "ShortTermDetail", value: ShortTermForecast.DetailedForecast, displayed: true)
   }
}
