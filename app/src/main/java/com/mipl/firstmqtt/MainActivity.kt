package com.mipl.firstmqtt

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), MqttOperations {

    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var publishButton: Button
    private lateinit var messageInput: EditText
    private lateinit var statusText: TextView
    private lateinit var subscriptionTextField: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var mqttClient: MqttClient

    // ThingSpeak MQTT connection details
    private val serverUri = "ssl://mqtt3.thingspeak.com:8883"
    private val clientId = "CyYmCjA0NB4qCi0vAB0TBxE" // Replace with your MQTT Username
    private val mqttPassword = "y1cF1SfV2JkIO9DTZ4xVwzPP" // Replace with your MQTT API Key
    private val channelId = "2643660" // Replace with your Channel ID
    private val publishTopic = "channels/$channelId/publish/fields/field1"
    private val subscribeTopic = "channels/$channelId/subscribe/fields/field2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        publishButton = findViewById(R.id.publishButton)
        messageInput = findViewById(R.id.messageInput)
        statusText = findViewById(R.id.statusText)
        subscriptionTextField=findViewById(R.id.subscriptionTextField)
        progressBar = findViewById(R.id.progressBar)

        connectButton.setOnClickListener { connectToThingSpeak() }
        disconnectButton.setOnClickListener { disconnectFromThingSpeak() }
        publishButton.setOnClickListener { publishMessage() }
        val adminButton: Button = findViewById(R.id.adminButton)
        adminButton.setOnClickListener {
            openAdminFragment()
        }

        updateConnectionState(false)
    }

    private fun connectToThingSpeak() {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            statusText.text = "Connecting..."
            withContext(Dispatchers.IO) {
                try {
                    mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
                    val options = MqttConnectOptions().apply {
                        userName = clientId
                        password = mqttPassword.toCharArray()
                        socketFactory = SSLSocketFactory.getDefault()
                        isAutomaticReconnect = true
                        isCleanSession = true
                        connectionTimeout = 60
                        setKeepAliveInterval(30)
                    }

                    mqttClient.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {
                            //showToast("Connection to ThingSpeak lost: ${cause?.message}")
                            updateConnectionState(false)
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            message?.let {
                                val payload = String(it.payload)
                                updateTextField(payload)
                                runOnUiThread {
                                    updateCurrentFragmentWithMessage(topic, payload)
                                }
                            }
                            showToast("Message received: ${message?.toString()}")
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                            showToast("Message delivered")
                        }
                    })

                    mqttClient.connect(options)
                    mqttClient.subscribe(subscribeTopic)
                    withContext(Dispatchers.Main) {
                        showToast("Connected to ThingSpeak successfully!")
                        updateConnectionState(true)
                    }
                } catch (e: MqttException) {
                    withContext(Dispatchers.Main) {
                        showToast("Error connecting to ThingSpeak: ${e.message}")
                        updateConnectionState(false)
                    }
                    e.printStackTrace()
                }
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun updateCurrentFragmentWithMessage(topic: String?, payload: String) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        when (val fragment:Fragment? = currentFragment) {

            is adminFragment -> topic?.let { fragment.updateReceivedMessage(it,payload) } // If AdminFragment also needs to handle messages
            // Add other fragment types here if they need to receive MQTT messages
        }
    }

    private fun disconnectFromThingSpeak() {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            statusText.text = "Disconnecting..."
            withContext(Dispatchers.IO) {
                try {
                    mqttClient.disconnect()
                    withContext(Dispatchers.Main) {
                        showToast("Disconnected from ThingSpeak")
                        updateConnectionState(false)
                    }
                } catch (e: MqttException) {
                    withContext(Dispatchers.Main) {
                        showToast("Error disconnecting: ${e.message}")
                    }
                    e.printStackTrace()
                }
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun publishMessage() {
        val message = messageInput.text.toString()
        if (message.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val mqttMessage = MqttMessage(message.toByteArray())
                    mqttClient.publish(publishTopic, mqttMessage)
                    withContext(Dispatchers.Main) {
                        showToast("Message published")
                        messageInput.text.clear()
                    }
                } catch (e: MqttException) {
                    withContext(Dispatchers.Main) {
                        showToast("Error publishing message: ${e.message}")
                    }
                    e.printStackTrace()
                }
            }
        } else {
            showToast("Please enter a message to publish")
        }
    }

    private fun updateConnectionState(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                statusText.text = "Connected"
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
                publishButton.isEnabled = true
            } else {
                statusText.text = "Disconnected"
                connectButton.isEnabled = true
                disconnectButton.isEnabled = false
                publishButton.isEnabled = false
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTextField(message: String) {
        runOnUiThread {
            subscriptionTextField.text = message
        }
    }

    override fun publishMessage(topic: String, message: String) {
        if (!getMqttConnectionStatus()) {
            println("MQTT Client is not connected")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mqttMessage = MqttMessage(message.toByteArray())
                mqttClient.publish(topic, mqttMessage)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    override fun subscribeToTopic(topic: String) {
        if (!getMqttConnectionStatus()) {
            println("MQTT Client is not connected")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mqttClient.subscribe(topic)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    override fun unsubscribeFromTopic(topic: String) {
        if (!getMqttConnectionStatus()) {
            println("MQTT Client is not connected")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mqttClient.unsubscribe(topic)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    override fun getMqttConnectionStatus(): Boolean {
        // Implementation
        //return ::mqttClient.isInitialized && mqttClient?.isConnected == true
        return ::mqttClient.isInitialized && mqttClient.isConnected
    }
    private fun openAdminFragment() {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, adminFragment())
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromThingSpeak()
    }
}