package com.example.control_them_all

import android.os.Bundle
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import android.util.Log

class MainActivity: FlutterActivity() {
    private val CHANNEL = "sensor_channel"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
                call, result ->
            if (call.method == "sendSensorCommand") {
                val sensorType = call.argument<Int>("sensorType") ?: 1
                val mode = call.argument<String>("mode") ?: "snapshot"
                val duration = call.argument<Int>("duration") ?: 3000

                sendMessageToWatch(sensorType, mode, duration)
                result.success("Message sent")
            } else {
                result.notImplemented()
            }
        }
    }

    private fun sendMessageToWatch(sensorType: Int, mode: String, duration: Int) {
        val json = JSONObject().apply {
            put("sensorType", sensorType)
            put("mode", mode)
            put("duration", duration)
        }

        val message = json.toString().toByteArray()

        Thread {
            Wearable.getNodeClient(this)
                .connectedNodes
                .addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        for (node in nodes) {
                            Wearable.getMessageClient(this)
                                .sendMessage(node.id, "/start_sensor", message)
                                .addOnSuccessListener {
                                    Log.d("FlutterWear", "Message sent to ${node.displayName}")
                                }
                                .addOnFailureListener {
                                    Log.e("FlutterWear", "Failed to send message: ${it.message}")
                                }
                        }
                    } else {
                        Log.e("FlutterWear", "No connected nodes")
                    }
                }
                .addOnFailureListener {
                    Log.e("FlutterWear", "Failed to get connected nodes: ${it.message}")
                }
        }.start()
    }
}