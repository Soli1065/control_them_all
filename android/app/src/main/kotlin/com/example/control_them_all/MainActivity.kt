package com.example.control_them_all

import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import org.json.JSONObject
import com.google.android.gms.tasks.await

class MainActivity : FlutterActivity(), MessageClient.OnMessageReceivedListener {

    private val CHANNEL = "sensor_channel"
    private val TAG = "FlutterWear"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "✅ MainActivity created")
    }

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "sendSensorCommand") {
                val sensorType = call.argument<Int>("sensorType") ?: 1
                val mode = call.argument<String>("mode") ?: "snapshot"
                val duration = call.argument<Int>("duration") ?: 3000

                Log.d(TAG, "Preparing to send sensor command: type=$sensorType, mode=$mode, duration=$duration")
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
        Log.d(TAG, "Sending message payload: ${json.toString()}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Wearable.getNodeClient(applicationContext).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.e(TAG, "❌ No connected Wear OS nodes found")
                    return@launch
                }

                for (node in nodes) {
                    Log.d(TAG, "✅ Found node: ${node.displayName} (${node.id})")

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
                Log.e(TAG, "❌ Error sending message to watch: ${e.message}")
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
        Log.d(TAG, "📬 Message received on path: $path")
        Log.d(TAG, "📦 Data: $data")

        if (path == "/sensor_result") {
            println("🎯 Sensor Result Received: $data")
            // TODO: Pass this to Flutter (via EventChannel or state)
        }
    }
}