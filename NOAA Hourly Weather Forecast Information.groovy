/*  **************** NOAA Hourly Weather Forecast Information ****************
 *
 *  Hubitat Import URL: https://github.com/robstitt/Hubitat-NOAA-Hourly-Weather-Forecast-Information/raw/main/NOAA%20Hourly%20Weather%20Forecast%20Information.groovy
 *
 *  Copyright 2019 Aaron Ward
 *  Copyright 2021-2025 Robert L. Stitt
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
 *  Change history:
 *
 *    Created: 10/30/2021
 *    Updated: 03/22/2022 (Added "Feature-Flags" workaround for old/cached data)
 *    Updated: 01/23/2024 (Added "ShortTerm" forecast information)
 *    Updated (1.1.001): 01/20/2025 (Added ",[misfire:ignore]" to runIn calls
 *
 *---------------------------------------------------------
 * The ShortTerm values are set as follows:
 *
 * The "window time range" starts with the current time and ends "ShortTermHours" later.
 *
 * Only forecast periods (returned by NOAA) that start before the end of the window time range and which also end after
 * after the start of the window time range are used (in other words, all forecast periods that are fully or partially
 * within the window time range will be used).
 *
 * The start and end times will reflect the starting and ending date/times for earliest and latest forecast periods,
 * respectively, that are fully or partially within the target window.
 *
 * PrecipPct - Highest percentage probabilty of all periods in the window
 * PrecipType - Precipitation type for the period in the window with hightest percentage probability
 * TempHigh - Highest forecast temp in all periods in the window
 * TempLow  - Lowest forecast temp in all periods in the window
 * TempUnit - The unit from the first period in the window with a non-blank temp unit (if any)
 * WindSpeed - the highest wind speed of all periods in the window
 * WindDirection - The direction associated with the highest wind speed in the window
 * IconURL - The IconURL from the first period in the window with a non-blank IconURL (if any)
 * ShortForecast - The first non-blank Short Forecast of all periods in the window
 * DetailedForecast - The first non-blank Detailed forecast of all periods in the window
 * Starts - This is the actual "start" date/time for the first forecast period (fully or partially) within the window
 * Ends - This is the actual "end" date/time for the last forecast period (fully or partially) within the window
 *---------------------------------------------------------
 *
 */

static String version() { return "1.1.001" }

import groovy.transform.Field
import groovy.json.*
import java.util.regex.*
import java.text.SimpleDateFormat
import java.text.ParseException
import java.util.Date
import groovy.time.TimeCategory
import groovy.time.*

definition(
   name:"NOAA Hourly Weather Forecast Information",
   namespace: "robstitt",
   author: "Rob Stitt",
   description: "NOAA Hourly Weather Forecast Information Application",
   category: "Weather",
   iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
   iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
   iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
   documentationLink: "https://github.com/robstitt/Hubitat-NOAA-Hourly-Weather-Forecast-Information/raw/main/README.md",
   singleInstance: true,
   oauth: false,
   pausable: true)

preferences {
   page name: "mainPage", title: "", install: true, uninstall: false
   page name: "ConfigPage", title: "", install: false, uninstall: false, nextPage: "mainPage"
   page name: "DebugPage", title: "", install: false, uninstall: true, nextPage: "mainPage"
}

@Field static Map CurrentHourForecast=[:]
@Field static Map NextHourForecast=[:]
@Field static Map ShortTermForecast=[:]

def mainPage() {
   dynamicPage(name: "mainPage") {
      installCheck()
      if((String)state.appInstalled == 'COMPLETE') {
         section(UIsupport("logo","")) {
            if(whatPoll) href(name: "ConfigPage", title: "${UIsupport("configured","")} Weather Information Settings", required: false, page: "ConfigPage", description: "Settings for the weather information")
            else  href(name: "ConfigPage", title: "${UIsupport("attention","")} Weather Information Settings", required: false, page: "ConfigPage", description: "Change default settings for weather information settings")

            href(name: "DebugPage", title: "Debugging", required: false, page: "DebugPage", description: "Debug and Test Options")
            paragraph UIsupport("line","")
            paragraph UIsupport("footer","")
         }
      }
   }
}

def ConfigPage() {
   dynamicPage(name: "ConfigPage") {
      section(UIsupport("logo","")) {
         input name: "whatPoll", type: "enum", title: "Poll Frequency: ", required: true, multiple: false, submitOnChange: true,
            options: [
               "1": "1 Minute",
               "5": "5 Minutes",
               "10": "10 Minutes",
               "15": "15 Minutes",
               "30": "30 Minutes"
            ], defaultValue: "30"

         setRefresh()

         input name: "shortTermHours", type: "number", range: "1..48", title: "Short Term Forecast Period (hours from current time)?", require: true, multiple: false, defaultValue: 5, submitOnChange: true

         input name: "useCustomCords", type: "bool", title: "Use Custom Coordinates?", require: false, defaultValue: false, submitOnChange: true

         if(useCustomCords) {
            paragraph "The default coordinates are acquired from your Hubitat Hub.  Enter your custom coordinates:"
            input name:"customlatitude", type:"text", title: "Latitude coordinate:", require: false, defaultValue: "${location.latitude}", submitOnChange: true
            input name:"customlongitude", type:"text", title: "Longitude coordinate:", require: false, defaultValue: "${location.longitude}", submitOnChange: true
         }

         main()
      }
   }
}

def DebugPage() {
   dynamicPage(name: "DebugPage") {
      section(UIsupport("logo","")) {
         paragraph UIsupport("header", " Debug and Test Options")

         input "logEnable", "bool", title: "Enable Normal Logging?", required: false, defaultValue: true, submitOnChange: true

         input "debugEnable", "bool", title: "Enable Debug Logging?", required: false, defaultValue: false, submitOnChange: true

         input "init", "bool", title: "Reset the current application state?", required: false, submitOnChange: true, defaultValue: false
         if(init) {
            app.updateSetting("init",[value:"false",type:"bool"])
            if (logEnable) log.warn "NOAA Hourly Weather Forecast Information application state has been reset."
            initialize()
         }

         input "testAPI", "bool", title: "Invoke and test the NOAA Hourly Weather Information API now?", required: false, submitOnChange: true, defaultValue: false
         if(testAPI) {
            app.updateSetting("testAPI",[value:"false",type:"bool"])

            getForecast()

            Date date = new Date()
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm a")

            String temp = "<hr><br>Current poll of Weather API: ${sdf.format(date)}<br/><br/>URI: <a href='${state.wxURI}' target=_blank>${state.wxURI}</a><br><br>"
            paragraph temp

            String testConfig = ""

            testConfig += "<b>Current Hour</b>"
            testConfig += "<table border=1px>"
            testConfig += "<tr><td>Temp</td><td>${CurrentHourForecast.Temp} ${CurrentHourForecast.TempUnit}</td></tr>"
            testConfig += "<tr><td>Wind</td><td>${CurrentHourForecast.WindSpeed} ${CurrentHourForecast.WindDirection}</td></tr>"
            testConfig += "<tr><td>Precipitation</td><td>${CurrentHourForecast.PrecipType} ${CurrentHourForecast.PrecipPct}%</td></tr>"
            testConfig += "<tr><td>Times</td><td>From: ${CurrentHourForecast.Starts}<br/>To: ${CurrentHourForecast.Ends}</td></tr>"
            testConfig += "<tr><td>Summary</td><td>${CurrentHourForecast.ShortForecast}</td></tr>"
            testConfig += "<tr><td>Details</td><td>${CurrentHourForecast.DetailedForecast}</td></tr>"
            testConfig += "</table><br/>"

            testConfig += "<b>Next Hour</b>"
            testConfig += "<table border=1px>"
            testConfig += "<tr><td>Temp</td><td>${NextHourForecast.Temp} ${NextHourForecast.TempUnit}</td></tr>"
            testConfig += "<tr><td>Wind</td><td>${NextHourForecast.WindSpeed} ${NextHourForecast.WindDirection}</td></tr>"
            testConfig += "<tr><td>Precipitation</td><td>${NextHourForecast.PrecipType} ${NextHourForecast.PrecipPct}%</td></tr>"
            testConfig += "<tr><td>Times</td><td>From: ${NextHourForecast.Starts}<br/>To: ${NextHourForecast.Ends}</td></tr>"
            testConfig += "<tr><td>Summary</td><td>${NextHourForecast.ShortForecast}</td></tr>"
            testConfig += "<tr><td>Details</td><td>${NextHourForecast.DetailedForecast}</td></tr>"
            testConfig += "</table><br/>"

            testConfig += "<b>Short Term</b>"
            testConfig += "<table border=1px>"
            testConfig += "<tr><td>Min Temp</td><td>${ShortTermForecast.TempLow}  ${ShortTermForecast.TempUnit}</td></tr>"
            testConfig += "<tr><td>Max Temp</td><td>${ShortTermForecast.TempHigh} ${ShortTermForecast.TempUnit}</td></tr>"
            testConfig += "<tr><td>Max Wind</td><td>${ShortTermForecast.WindSpeed} ${ShortTermForecast.WindDirection}</td></tr>"
            testConfig += "<tr><td>Precipitation</td><td>${ShortTermForecast.PrecipType} ${ShortTermForecast.PrecipPct}% (max)</td></tr>"
            testConfig += "<tr><td>Times</td><td>From: ${ShortTermForecast.Starts}<br/>To: ${ShortTermForecast.Ends}</td></tr>"
            testConfig += "<tr><td>Summary</td><td>${ShortTermForecast.ShortForecast}</td></tr>"
            testConfig += "<tr><td>Details</td><td>${ShortTermForecast.DetailedForecast}</td></tr>"
            testConfig += "</table>"

            paragraph testConfig

            callRefreshForecastDevice()
            setRefresh()
         }
      }
   }
}

// Main Application Routines
def main() {
    // Get the current forecast
    getForecast()
    callRefreshForecastDevice()
    setRefresh()
}

void callRefreshForecastDevice(){
   def noaaForecastDevice = getChildDevice("NOAAhourlywxforecast")
   if(noaaForecastDevice) {
      noaaForecastDevice.refresh()
   } else {
      if(logEnable) log.warn "No child forecast device found to refresh"
   }
}

void getForecast() {
   Map result = getWeatherForecast()

   if(result) {
      if (debugEnable) log.debug "Weather API returned ${result.properties.periods.size().toString()} entries"

      if (result.properties.periods.size() >= 2) {
         CurrentHourForecast = [:]
         NextHourForecast = [:]

         if (1 > shortTermHours) shortTermHours = 1

         Integer ShortTermPrecippct = -1
         String  ShortTermPrecippctstr = "0"
         String  ShortTermPreciptype = "None"
         Integer ShortTermTempHigh = -9999
         Integer ShortTermTempLow = 9999
         String  ShortTermTempUnit = "-"
         Integer ShortTermWindSpeedInt = -1
         String  ShortTermWindSpeed = "-"
         String  ShortTermWindDirection = "-"
         String  ShortTermIconURL = ""
         String  ShortTermForecastShort = "-"
         String  ShortTermForecastDetailed = "-"
         Date    ShortTermdtperiodstarts = new Date()
         Date    ShortTermdtperiodends
         Date    ShortTermdtperiodstartsActual = new Date()
         Date    ShortTermdtperiodendsActual = new Date()
         boolean exitloop = false

         if (debugEnable) log.debug "Short Term Starts at: ${ShortTermdtperiodstarts.toString()}"
         if (debugEnable) log.debug "Short Term Period length: ${shortTermHours.toString()}"

         Integer tmpHours = shortTermHours.toInteger()
         use ( groovy.time.TimeCategory) {
            ShortTermdtperiodends = ShortTermdtperiodstarts + tmpHours.hours
         }

         if (debugEnable) log.debug "Short Term Ends at (Date): ${ShortTermdtperiodends.toString()}"

//         for(i=0; i<Math.min(result.properties.periods.size(),2);i++) {
         for(i=0; (i<result.properties.periods.size() && !exitloop);i++) {
            if(debugEnable) log.debug "In weather period loop, i=${i.toString()}"

            Integer periodnumber
            Integer periodtemp
            String  periodtempunit
            String  periodwindspeed
            Integer periodwindspeedint
            String  periodwinddirection
            String  periodiconurl
            String  periodforecastshort
            String  periodforecastdetailed
            Integer periodprecippct
            String  periodprecippctstr
            String  periodpreciptype
            String  temppreciptype
            String  periodstarts
            String  periodends
            Date    dtperiodstarts
            Date    dtperiodends
            Matcher parseiconurl

            periodnumber           = (Integer)result.properties.periods[i].number
            periodtemp             = (Integer)result.properties.periods[i].temperature
            periodtempunit         = (String)result.properties.periods[i].temperatureUnit
            periodwindspeed        = (String)result.properties.periods[i].windSpeed
            periodwinddirection    = (String)result.properties.periods[i].windDirection
            periodiconurl          = (String)result.properties.periods[i].icon
            periodforecastshort    = (String)result.properties.periods[i].shortForecast
            periodforecastdetailed = (String)result.properties.periods[i].detailedForecast
            periodstarts           = (String)result.properties.periods[i].startTime
            periodends             = (String)result.properties.periods[i].endTime

            try {
               Matcher match
               if ((match = periodwindspeed =~ /(\d+)/)) {
                  String extractspeed = match.group(1)

                  if (extractspeed == "") {
                     periodwindspeedint = -1
                  } else {
                     periodwindspeedint = extractspeed.toInteger()
                  }
               } else {
                  periodwindspeedint = -1
               }
            }
            catch (e) {
               log.error "Error parsing weather forecast wind speed for period ${periodnumber.toString()}: ${periodwindspeed}."
               periodwindspeedint = -1
            }

            if (periodwindspeed=="") periodwindspeed = "-"
            if (periodwinddirection=="") periodwinddirection = "-"
            if (periodforecastshort=="") periodforecastshort = "-"
            if (periodforecastdetailed=="") periodforecastdetailed = "-"

            parseiconurl = periodiconurl =~ /\/([^\/,?]*)(,{0,1})([0-9]*)\?[^\/]./

            periodpreciptype    = "None"
            periodprecippctstr  = "0"

            if (parseiconurl.size() > 0) {
               if (parseiconurl[0].size() > 1) temppreciptype     = parseiconurl[0][1]
               if (parseiconurl[0].size() > 3) periodprecippctstr = parseiconurl[0][3]
            }

            if (periodprecippctstr=="") periodprecippctstr = "0"

            if (periodprecippctstr!="0") periodpreciptype = temppreciptype

            try {
               periodprecippct = periodprecippctstr as Integer
            }
            catch (e) {
               log.error "Error parsing weather forecast precipitation percent for period ${periodnumber.toString()}: ${periodprecippctstr}."
               periodprecippct = 0
            }

            if(debugEnable) log.debug "Period: ${periodnumber.toString()}, Start: ${periodstarts}, End: ${periodends}, Temp: ${periodtemp.toString()} ${periodtempunit}, Wind: ${periodwindspeed} ${periodwinddirection}"
            if(debugEnable) log.debug "...Precip Type: ${periodpreciptype}, Precip %: ${periodprecippct.toString()}"
            if(debugEnable) log.debug "...Forecast: ${periodforecastshort}"

            try {
               dtperiodstarts = Date.parse("yyyy-MM-dd'T'HH:mm:ssXXX", periodstarts)
            }
            catch (e) {
               log.error "Error parsing weather forecast start date for period ${periodnumber.toString()}: ${periodstarts}."
               dtperiodtstarts = curtime
            }

            try {
               dtperiodends = Date.parse("yyyy-MM-dd'T'HH:mm:ssXXX", periodends)
            }
            catch (e) {
               log.error "Error parsing weather forecast end date for period ${periodnumber.toString()}: ${periodends} (assuming 1 hour from now)."
               dtperiodends = curtime + 1.hour
            }

            if (periodnumber==1) {
               CurrentHourForecast.Temp = periodtemp
               CurrentHourForecast.TempUnit = periodtempunit
               CurrentHourForecast.WindSpeed = periodwindspeed
               CurrentHourForecast.WindDirection = periodwinddirection
               CurrentHourForecast.IconURL = periodiconurl
               CurrentHourForecast.ShortForecast = periodforecastshort
               CurrentHourForecast.DetailedForecast = periodforecastdetailed
               CurrentHourForecast.PrecipPct = periodprecippct
               CurrentHourForecast.PrecipType = periodpreciptype
               CurrentHourForecast.Starts = dtperiodstarts
               CurrentHourForecast.Ends = dtperiodends
            } else if (periodnumber==2) {
               NextHourForecast.Temp = periodtemp
               NextHourForecast.TempUnit = periodtempunit
               NextHourForecast.WindSpeed = periodwindspeed
               NextHourForecast.WindDirection = periodwinddirection
               NextHourForecast.IconURL = periodiconurl
               NextHourForecast.ShortForecast = periodforecastshort
               NextHourForecast.DetailedForecast = periodforecastdetailed
               NextHourForecast.PrecipPct = periodprecippct
               NextHourForecast.PrecipType = periodpreciptype
               NextHourForecast.Starts = dtperiodstarts
               NextHourForecast.Ends = dtperiodends
            }

            if ((dtperiodstarts<=ShortTermdtperiodends) && (dtperiodends>=ShortTermdtperiodstarts)) {
               if (ShortTermdtperiodstartsActual > dtperiodstarts) ShortTermdtperiodstartsActual = dtperiodstarts
               if (dtperiodends > ShortTermdtperiodendsActual) ShortTermdtperiodendsActual = dtperiodends

               if (0>ShortTermPrecippct) {
                   ShortTermPrecippct = periodprecippct
                   ShortTermPrecippctstr = periodprecippctstr
               }

               if (ShortTermPreciptype=="None") ShortTermPreciptype = periodpreciptype

               if (ShortTermTempHigh==-9999)    ShortTermTempHigh   = periodtemp

               if (ShortTermTempLow==9999)      ShortTermTempLow    = periodtemp

               if ((ShortTermTempUnit=="-") && (periodtempunit != "")) ShortTermTempUnit = periodtempunit

               if ((ShortTermWindSpeed=="-") && (periodwindspeedint>=0)) {
                   ShortTermWindSpeedInt = periodwindspeedint
                   ShortTermWindSpeed = periodwindspeed
                   ShortTermWindDirection = periodwinddirection
               } else if ((periodwindspeedint >=0) && (ShortTermWindSpeedInt < periodwindspeedint)) {
                   ShortTermWindSpeedInt = periodwindspeedint
                   ShortTermWindSpeed = periodwindspeed
                   ShortTermWindDirection = periodwinddirection
               }

               if (ShortTermPrecippct < periodprecippct) {
                  ShortTermPrecippct    = periodprecippct
                  ShortTermPrecippctstr = periodprecippctstr
                  ShortTermPreciptype   = periodpreciptype
               }

               if (ShortTermTempHigh < periodtemp) {
                  ShortTermTempHigh = periodtemp
                  ShortTermTempUnit = periodtempunit
               }

               if (ShortTermTempLow > periodtemp) {
                  ShortTermTempLow = periodtemp
                  ShortTermTempUnit = periodtempunit
               }

               if ((ShortTermIconURL == "") && (periodiconurl != "")) ShortTermIconURL = periodiconurl

               if ((ShortTermForecastShort == "-") && (periodforecastshort != "-")) ShortTermForecastShort = periodforecastshort

               if ((ShortTermForecastDetailed == "-") && (periodforecastdetailed != "-")) ShortTermForecastDetailed = periodforecastdetailed
            }

            if ((dtperiodstarts>ShortTermdtperiodends) && (i>2)) exitloop = true

         } //end of for statement


         ShortTermForecast = [:]

         ShortTermForecast.TempHigh = ShortTermTempHigh
         ShortTermForecast.TempLow  = ShortTermTempLow
         ShortTermForecast.TempUnit = ShortTermTempUnit
         ShortTermForecast.WindSpeed  = ShortTermWindSpeed
         ShortTermForecast.WindDirection  = ShortTermWindDirection
         ShortTermForecast.IconURL = ShortTermIconUrl
         ShortTermForecast.ShortForecast = ShortTermForecastShort
         ShortTermForecast.DetailedForecast = ShortTermForecastDetailed
         ShortTermForecast.PrecipPct = ShortTermPrecippct
         ShortTermForecast.PrecipType = ShortTermPreciptype
         ShortTermForecast.Starts = ShortTermdtperiodstartsActual
         ShortTermForecast.Ends = ShortTermdtperiodendsActual
      }
   }

   if(result==null) { // deal with network outages and errors by only making updates if something was returned
      if(logEnable) log.warn "No data returned from the weather forecast request"
   }
}

Map getForecastCurrentDevice() {
   if(debugEnable) log.debug "Returning forecast for the current hour to the child device"
   return CurrentHourForecast
}

Map getForecastNextDevice() {
   if(debugEnable) log.debug "Returning forecast for the next hour to the child device"
   return NextHourForecast
}

Map getForecastShortTermDevice() {
   if(debugEnable) log.debug "Returning forecast for the short term to the child device"
   return ShortTermForecast
}

// Device creation and status updhandlers
void createChildDevices() {
   try {
      if (!getChildDevice("NOAAhourlywxforecast")) {
         if (logEnable) log.info "Creating device: NOAA Hourly Weather Forecast Device"
         addChildDevice("robstitt", "NOAA Hourly Weather Forecast Device", "NOAAhourlywxforecast", 1234, ["name": "NOAA Hourly Weather Forecast Device", isComponent: false])
      }
   }
   catch (e) { log.error "Couldn't create child device. ${e}" }
}

void cleanupChildDevices() {
   try {
      for(device in getChildDevices()) deleteChildDevice(device.deviceNetworkId)
   }
   catch (e) { log.error "Couldn't clean up child devices." }
}

// Application Support Routines

Map getGridpointInfo() {
   // Determine if custom coordinates have been selected

   String latitude
   String longitude

   if(useCustomCords) {
      latitude = "${customlatitude}".toString()
      longitude = "${customlongitude}".toString()
   } else {
      latitude = "${location.latitude}".toString()
      longitude = "${location.longitude}".toString()
   }

   String wxURI = "https://api.weather.gov/points/${latitude},${longitude}".toString()
   Map result = [:]

   state.wxURI = wxURI
   if(debugEnable) log.debug "URI: <a href='${wxURI}' target=_blank>${wxURI}</a>"

   // Stuff a non-sensical date into the "Feature-Flags" parameter (which weather.gov apparently ignores) to force it to NOT use old, cached data
   Date ffdate = new Date()
   SimpleDateFormat ffsdf = new SimpleDateFormat("yyyyMMddHHmmssS")
   String ffvalue = "tempworkaround-${ffsdf.format(ffdate)}"
   Map getHeaders = [ "Feature-Flags": ffvalue]

   if(debugEnable) log.debug "Connecting to weather.gov service."
   Map requestParams =  [
      uri:"${wxURI}",
      headers: getHeaders,
      requestContentType:"application/json",
      contentType:"application/json"
   ]

   try {
      httpGet(requestParams)  { response -> result = response.data }
   }
   catch (e) {
      log.error "The API Weather.gov did not return a response (unable to map latitude/longitude to NOAA grid coordinates), exception: $e"
   }

   return result
}

Map getWeatherForecast() {
   // Obtain the gridpoint information from the lat/long location

   String wxstation
   String gridpointx
   String gridpointy

   Map result = [:]

   Map gridresult = getGridpointInfo()

   if(gridresult) {
      if (debugEnable) log.debug "Weather API returned grid point information"

      if (gridresult.properties.gridId==null) {
         log.error "Error converting latitude, longitude into NOAA grid point information (no office ID returned)"
         return result
      }

      if (gridresult.properties.gridX==null) {
         log.error "Error converting latitude, longitude into NOAA grid point information (no gridX value returned)"
         return result
      }

      if (gridresult.properties.gridY==null) {
         log.error "Error converting latitude, longitude into NOAA grid point information (no gridY value returned)"
         return result
      }
   }
   else {
         log.error "Error converting latitude, longitude into NOAA grid point information (no data was returned)"
         return result
   }

   wxstation = gridresult.properties.gridId
   gridpointx = gridresult.properties.gridX
   gridpointy = gridresult.properties.gridY

   if(debugEnable) log.debug "The specified latitude,longitude corresponds to Office=${wxstation}, X=${gridpointx}, Y=${gridpointy}"


   String wxURI = "https://api.weather.gov/gridpoints/${wxstation}/${gridpointx},${gridpointy}/forecast/hourly".toString()

   state.wxURI = wxURI
   if(debugEnable) log.debug "URI: <a href='${wxURI}' target=_blank>${wxURI}</a>"

   if(debugEnable) log.debug "Connecting to weather.gov service."

   // Stuff a non-sensical date into the "Feature-Flags" parameter (which weather.gov apparently ignores) to force it to NOT use old, cached data
   Date ffdate = new Date()
   SimpleDateFormat ffsdf = new SimpleDateFormat("yyyyMMddHHmmssS")
   String ffvalue = "tempworkaround-${ffsdf.format(ffdate)}"
   Map getHeaders = [ "Feature-Flags": ffvalue]

   if(debugEnable) log.debug "Using workaround 'Feature-Flags:${ffvalue}' header value to force bypassing of old, cached data."

   Map requestParams =  [
      uri:"${wxURI}",
      headers:getHeaders,
      requestContentType:"application/json",
      contentType:"application/json"
   ]

   try {
      httpGet(requestParams)  { response -> result = response.data }
   }
   catch (e) {
      if(logEnable) log.warn "The API Weather.gov did not return a response (retaining previous results), exception: $e"
   }

   return result
}

void checkState() {
   if(whatPoll==null) app.updateSetting("whatPoll",[value:"30",type:"enum"])
   if(logEnable==null) app.updateSetting("logEnable",[value:"false",type:"bool"])
   if(debugEnable==null) app.updateSetting("debugEnable",[value:"false",type:"bool"])
   if(logMinutes==null) app.updateSetting("logMinutes",[value:15,type:"number"])
   if(logMinutes==null) app.updateSetting("logMinutes",[value:15,type:"number"])
}

void installCheck(){
   state.appInstalled = app.getInstallationState()
   if((String)state.appInstalled != 'COMPLETE'){
      section{paragraph "Please hit 'Done' to install ${app.label} "}
   }
}

void initialize() {
    checkState()
    unschedule()
    createChildDevices()
    callRefreshForecastDevice()
    runIn(2,main,[misfire:ignore])
    runIn(5,setRefresh,[misfire:ignore])
}

void setRefresh() {
   unschedule()

   Integer myPoll=30
   if(whatPoll!=null) myPoll=whatPoll.toInteger()
   if (debugEnable) log.debug "Polling interval set to ${myPoll} minute(s)"
   switch(myPoll) {
      case 1:
         runEvery1Minute(main)
         break
      case 5:
         runEvery5Minutes(main)
         break
      case 10:
         runEvery10Minutes(main)
         break
      case 15:
         runEvery15Minutes(main)
         break
      default:
         runEvery30Minutes(main)
         break
   }
}

void installed() {
   if(debugEnable) log.debug "Installed with settings: ${settings}"
   initialize()
}

void updated() {
   if(debugEnable) log.debug "Updated with settings: ${settings}"
   initialize()
}

void uninstalled() {
   cleanupChildDevices()
}

static String UIsupport(String type, String txt) {
   switch(type) {
      case "logo":
         return "<table border=0><thead><tr><th><img border=0 style='max-width:100px' src='https://raw.githubusercontent.com/imnotbob/Hubitat-4/master/NOAA/Support/NOAA.png'></th><th style='padding:10px' align=left><font style='font-size:34px;color:#1A77C9;font-weight: bold'>NOAA Hourly Weather Forecast Information</font><br><font style='font-size:14px;font-weight: none'>This application provides the current and next hour's weather forecast information.</font></tr></thead></table><br><hr style='margin-top:-15px;background-color:#1A77C9; height: 1px; border: 0;'></hr>"
         break
      case "line":
         return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
         break
      case "header":
         return "<div style='color:#ffffff;font-weight: bold;background-color:#1A7BC7;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${txt}</div>"
         break
      case "footer":
         return "<div style='color:#1A77C9;text-align:center'>App/Driver v${version()}<br>Written by: Robert L. Stitt; Based on code originally developed by: Aaron Ward<br></div>"
         break
      case "configured":
         return "<img border=0 style='max-width:15px' src='https://raw.githubusercontent.com/imnotbob/Hubitat-4/master/support/images/Checked.svg'>"
         break
      case "attention":
         return "<img border=0 style='max-width:15px' src='https://raw.githubusercontent.com/imnotbob/Hubitat-4/master/support/images/Attention.svg'>"
         break
   }
}
