package com.mipl.firstmqtt

interface MqttOperations {
    fun publishMessage(topic: String, message: String)
    fun subscribeToTopic(topic: String)
    fun unsubscribeFromTopic(topic: String)
    fun getMqttConnectionStatus(): Boolean
}

