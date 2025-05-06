package com.example.controlthemall_watch.service


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
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = String(messageEvent.data)

        if (path == "/start_sensor") {
            try {
                val json = JSONObject(data)
                val sensorType = json.getInt("sensorType")
                val mode = json.optString("mode", "snapshot")
                val duration = json.optLong("duration", 3000L)

                Log.d("SensorService", "Received command for sensor: $sensorType, mode: $mode")

                val sensor = sensorManager.getDefaultSensor(sensorType)

                if (sensor != null) {
                    currentListener?.let { sensorManager.unregisterListener(it) }

                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            val result = JSONObject().apply {
                                put("sensorType", sensorType)
                                put("values", event.values.joinToString(","))
                            }

                            Wearable.getMessageClient(this@SensorCommandService)
                                .sendMessage(
                                    messageEvent.sourceNodeId,
                                    "/sensor_result",
                                    result.toString().toByteArray()
                                )

                            sensorManager.unregisterListener(this)
                            currentListener = null
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }

                    currentListener = listener
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } catch (e: Exception) {
                Log.e("SensorService", "Error parsing sensor command: ${e.message}")
            }
        }
    }
}