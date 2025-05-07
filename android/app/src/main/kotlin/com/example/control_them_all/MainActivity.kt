package com.example.control_them_all

import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class MainActivity : FlutterActivity(), MessageClient.OnMessageReceivedListener {

    private val METHOD_CHANNEL = "sensor_channel"
    private val EVENT_CHANNEL = "sensor_result_stream"
    private val TAG = "FlutterWear"

    private var eventSink: EventChannel.EventSink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "✅ MainActivity created")
    }

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // 🔹 MethodChannel: Flutter → Native (send command)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler { call, result ->
                if (call.method == "sendSensorCommand") {
                    val sensorType = call.argument<Int>("sensorType") ?: 1
                    val mode = call.argument<String>("mode") ?: "snapshot"
                    val duration = call.argument<Int>("duration") ?: 3000

                    Log.d(TAG, "📤 Sending command: type=$sensorType, mode=$mode, duration=$duration")
                    sendMessageToWatch(sensorType, mode, duration)
                    result.success("Message sent")
                } else {
                    result.notImplemented()
                }
            }

        // 🔹 EventChannel: Native → Flutter (stream sensor_result)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                    Log.d(TAG, "🎧 Flutter started listening for sensor_result")
                    eventSink = events
                }

                override fun onCancel(arguments: Any?) {
                    Log.d(TAG, "🛑 Flutter stopped listening")
                    eventSink = null
                }
            })
    }

    private fun sendMessageToWatch(sensorType: Int, mode: String, duration: Int) {
        val json = JSONObject().apply {
            put("sensorType", sensorType)
            put("mode", mode)
            put("duration", duration)
        }

        val message = json.toString().toByteArray()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(applicationContext).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ No connected Wear OS nodes found")
                    return@launch
                }

                for (node in nodes) {
                    Log.d(TAG, "✅ Found node: ${node.displayName}")
                    Wearable.getMessageClient(applicationContext)
                        .sendMessage(node.id, "/start_sensor", message)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Message sent to ${node.displayName}")
                        }
                        .addOnFailureListener {
                            Log.e(TAG, "❌ Failed to send message: ${it.message}")
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception sending message: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📡 Registering message listener")
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "🔌 Unregistering message listener")
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        val data = String(event.data)

        Log.d(TAG, "📬 Received on path: $path")
        Log.d(TAG, "📦 Raw data: $data")

        if (path == "/sensor_result") {
            eventSink?.success(data)
            Log.d(TAG, "🚀 Forwarded to Flutter via EventChannel")
        }
    }
}