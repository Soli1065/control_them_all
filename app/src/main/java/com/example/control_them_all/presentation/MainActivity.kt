package com.example.control_them_all.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val TAG = "WatchMain"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate")
        println("✅ MainActivity created")

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.d(TAG, "Found ${sensorList.size} sensors")
        println("✅ Found ${sensorList.size} sensors")

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "sensor_list") {
                    composable("sensor_list") {
                        SensorListScreen(sensorList, navController)
                    }

                    composable(
                        "sensor_detail/{sensorType}/{sensorName}",
                        arguments = listOf(
                            navArgument("sensorType") { type = NavType.IntType },
                            navArgument("sensorName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val sensorType = backStackEntry.arguments?.getInt("sensorType") ?: -1
                        val sensorName = backStackEntry.arguments?.getString("sensorName") ?: ""
                        Log.d(TAG, "Navigating to sensor_detail: type=$sensorType, name=$sensorName")
                        println("➡️ sensor_detail: $sensorType - $sensorName")
                        SensorDetailScreen(sensorType, sensorName, sensorManager)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorListScreen(sensors: List<Sensor>, navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Sensors") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            items(sensors) { sensor ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Log.d("WatchMain", "Clicked: ${sensor.name} (type: ${sensor.type})")
                            println("🟢 Clicked sensor: ${sensor.name}")
                            navController.navigate("sensor_detail/${sensor.type}/${sensor.name}")
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Text(text = sensor.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Type: ${sensor.type}, Vendor: ${sensor.vendor}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDetailScreen(sensorType: Int, sensorName: String, sensorManager: SensorManager) {
    val TAG = "SensorScreen"
    val values = remember { mutableStateListOf<Float>() }
    DisposableEffect(sensorType) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                values.clear()
                values.addAll(event.values.toList())
                Log.d("SensorScreen", "Sensor [$sensorType] updated: ${event.values.joinToString(",")}")
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            Log.e("SensorScreen", "Sensor not found for type: $sensorType")
            println("❌ Sensor not found: $sensorType")
        } else {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            Log.d("SensorScreen", "✅ Listener registered for sensor: $sensorType")
        }

        onDispose {
            sensorManager.unregisterListener(listener)
            Log.d("SensorScreen", "🛑 Listener unregistered for sensor: $sensorType")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(sensorName) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Real-time sensor values:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            values.forEachIndexed { index, value ->
                Text("Value[$index]: $value", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}