package com.example.control_them_all.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class SensorCommandService : WearableListenerService() {

    private lateinit var sensorManager: SensorManager
    private var currentListener: SensorEventListener? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("SensorService", "Service created")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = String(messageEvent.data)
        Log.d("SensorService", "Message received on path: $path")
        Log.d("SensorService", "Raw message: $data")

        if (path == "/start_sensor") {
            try {
                val json = JSONObject(data)
                val sensorType = json.getInt("sensorType")
                val mode = json.optString("mode", "snapshot")
                val duration = json.optLong("duration", 3000L)

                Log.d("SensorService", "Parsed command: sensorType=$sensorType, mode=$mode, duration=$duration")

                val sensor = sensorManager.getDefaultSensor(sensorType)

                if (sensor == null) {
                    Log.e("SensorService", "Sensor not available: $sensorType")
                    return
                }

                // Remove any existing listeners
                currentListener?.let {
                    sensorManager.unregisterListener(it)
                    Log.d("SensorService", "Previous listener unregistered")
                }

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        Log.d("SensorService", "Sensor data received: ${event.values.joinToString(",")}")

                        val result = JSONObject().apply {
                            put("sensorType", sensorType)
                            put("values", event.values.joinToString(","))
                        }

                        // Send result back
                        Wearable.getMessageClient(this@SensorCommandService)
                            .sendMessage(
                                messageEvent.sourceNodeId,
                                "/sensor_result",
                                result.toString().toByteArray()
                            )
                            .addOnSuccessListener {
                                Log.d("SensorService", "Sensor result sent back to phone")
                            }
                            .addOnFailureListener {
                                Log.e("SensorService", "Failed to send result: ${it.message}")
                            }

                        sensorManager.unregisterListener(this)
                        currentListener = null
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                currentListener = listener
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                Log.d("SensorService", "Sensor listener registered for type: $sensorType")

            } catch (e: Exception) {
                Log.e("SensorService", "Error processing command: ${e.message}")
            }
        }
    }
}