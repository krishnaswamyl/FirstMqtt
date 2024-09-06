package com.mipl.firstmqtt

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment

class adminFragment  : Fragment() {
    private lateinit var mqttOperations: MqttOperations
    private lateinit var publishEditText: EditText
    private lateinit var publishButton: Button
    private lateinit var subscribeEditText: EditText
    private lateinit var subscribeButton: Button
    private lateinit var messageTextView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.adminfragment, container, false)

        publishEditText = view.findViewById(R.id.publishEditText)
        publishButton = view.findViewById(R.id.publishButton)
        subscribeEditText = view.findViewById(R.id.subscribeEditText)
        subscribeButton = view.findViewById(R.id.subscribeButton)
        messageTextView = view.findViewById(R.id.messageTextView)

        publishButton.setOnClickListener { publishMessage() }
        subscribeButton.setOnClickListener { subscribeTopic() }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MqttOperations) {
            mqttOperations = context
        } else {
            throw RuntimeException("$context must implement MqttOperations")
        }
    }

    private fun publishMessage() {
        val topic = "channels/2643660/publish/fields/field1"
        val message = publishEditText.text.toString()
        mqttOperations.publishMessage(topic, message)
    }

    private fun subscribeTopic() {
        val topic = subscribeEditText.text.toString()
        mqttOperations.subscribeToTopic(topic)
    }

    fun updateReceivedMessage(message: String) {
        messageTextView.text = message
    }
}