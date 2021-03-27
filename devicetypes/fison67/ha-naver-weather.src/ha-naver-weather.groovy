/**
 *  HA Naver Weather (v0.1.0)
 *
 *  Authors
 *   - sunghn.kim@gmail.com
 *  Copyright 2018-2021
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
 */

import groovy.json.JsonSlurper

metadata {
    definition (name: "HA Naver Weather", namespace: "fison67", author: "sunghn-kim", ocfDeviceType: "x.com.st.d.airqualitysensor") {
        capability "Dust Sensor"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Ultraviolet Index"
        capability "Refresh"

        attribute "lastCheckin", "Date"
    }
}

def setHASetting(url, password, deviceId) {
    state.app_url = url
    state.app_pwd = password
    state.entity_id = deviceId
    state.hasSetStatusMap = true

    sendEvent(name: "ha_url", value: state.app_url, displayed: false)
}

def setStatusMap(object) {
    log.debug "[HA Naver Weather][setStatusMap()] Received object:\n" + object

    def attributes = object.attr != null ? object.attr : object.attributes

    sendEvent(name: "entity_id", value: state.entity_id, displayed: false)
    sendEvent(name: "lastCheckin", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone), displayed: false)

    sendEvent(name: "temperature", value: attributes.현재온도.replace("°C", ""), unit:"°C")
    sendEvent(name: "humidity", value: attributes.현재습도.replace("%", ""), unit: "%")
    sendEvent(name: "dustLevel", value: attributes.미세먼지.replace("㎍/m³", ""), unit:"㎍/m³")
    sendEvent(name: "fineDustLevel", value: attributes.초미세먼지.replace("㎍/m³", ""), unit:"㎍/m³")
    sendEvent(name: "ultravioletIndex", value: attributes.자외선지수)
}

def refresh() {
    def options = [
        "method": "GET",
        "path": "/api/states/${state.entity_id}",
        "headers": [
            "HOST": parent._getServerURL(),
            "Authorization": "Bearer " + parent._getPassword(),
            "Content-Type": "application/json"
        ]
    ]

    sendCommand(options, callback)
}

def callback(physicalgraph.device.HubResponse hubResponse) {
    try {
        setStatusMap(new JsonSlurper().parseText(parseLanMessage(hubResponse.description).body))
    } catch (e) {
        log.error "[HA Naver Weather][callback()] Exception caught while parsing data: " + e;
    }
}

def sendCommand(options, _callback) {
    sendHubCommand(new physicalgraph.device.HubAction(options, null, [callback: _callback]))
}
