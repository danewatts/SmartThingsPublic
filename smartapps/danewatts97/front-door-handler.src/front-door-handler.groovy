/**
 *  Frontdoor Handler
 *
 *  Copyright 2017 Dane Watts
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
definition(
        name: "Frontdoor Handler",
        namespace: "danewatts97",
        author: "Dane Watts",
        description: "Handling the frontdoor sensor actions",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Door sensor") {
        input "contact", "capability.contactSensor", required: true
    }
    section("And notify me if it's open for more than this many minutes (default 1). Set 0 to disable") {
        input "openThreshold", "number", description: "Number of minutes", required: false
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe()
}

def subscribe() {
    subscribe(contact, "contact.open", sensorOpenHandler)
    subscribe(contact, "contact.closed", sensorClosedHandler)
}

def sensorOpenHandler(evt) {
    log.trace "[sensorOpenHandler]: ${contact.displayName} sensor opened at ${evt.date}"
    log.trace "[sensorOpenHandler]: Mode set as: ${location.mode}"
    String eventMessage = "${evt.displayName} has been opened"
    switch(location.mode){
        case "Home":
            sendPushNotification(eventMessage)
            break
        case "Asleep":
            alarm()
            break
        case "Away":
            sendPushNotification(eventMessage)
            triggerAlexaAlarm()
            break
        case "Night":
            sendPushNotification(eventMessage)
        default:
            log.debug("[sensorOpenHandler]: Doing nothing")
            break
    }
    if(openThreshold != 0) {
        def delay = (openThreshold != null && openThreshold != "") ? openThreshold * 60 : 60
        runIn(delay, doorOpenTooLong, [overwrite: true])
    }
}

def doorOpenTooLong() {
    log.trace "[doorOpenTooLong]"
    def contactState = contact.currentState("contact")
    if (contactState.value == "open") {
        sendPushNotification("${contact.displayName} has been open for too long")
    } else {
        log.warn "[doorOpenTooLong]: in incorrect state - contactState.value=${contactState.value}"
    }
}

def sensorClosedHandler(evt) {
    log.trace "[sensorClosedHandler]: ($evt.name: $evt.value)"
    unschedule(doorOpenTooLong)
}

def sendPushNotification(notificationMessage) {
    log.trace "[sendPushNotification]: Sending mobile notification"
    sendNotification(notificationMessage, [method: "push"])
}

def triggerAlexaAlarm() {
    //log.debug "[triggerAlexaAlarm]: Triggering alexa alarm"
}



// TODO: Trigger alexa response