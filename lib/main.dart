import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MaterialApp(home: SensorControlScreen()));
}

class SensorControlScreen extends StatefulWidget {
  const SensorControlScreen({Key? key}) : super(key: key);

  @override
  State<SensorControlScreen> createState() => _SensorControlScreenState();
}

class _SensorControlScreenState extends State<SensorControlScreen> {
  static const platform = MethodChannel('sensor_channel');

  final Map<String, int> sensorOptions = {
    'Accelerometer': 1,
    'Gyroscope': 4,
    'Magnetometer': 2,
    'Light': 5,
    'Heart Rate': 21,
    'Rotation Vector': 11,
  };

  String selectedSensor = 'Accelerometer';
  String? sensorResult;
  bool isLoading = false;

  Future<void> requestSensorData() async {
    final sensorType = sensorOptions[selectedSensor]!;
    setState(() {
      isLoading = true;
      sensorResult = null;
    });

    try {
      final result = await platform.invokeMethod('sendSensorCommand', {
        'sensorType': sensorType,
        'mode': 'snapshot',
        'duration': 3000,
      });

      // Result from native just says "Message sent", actual data comes later
      debugPrint('✅ Command sent, waiting for data...');
    } catch (e) {
      debugPrint('❌ Failed to send command: $e');
    } finally {
      setState(() {
        isLoading = false;
      });
    }
  }

  // Placeholder until we add EventChannel
  Widget buildResultWidget() {
    return sensorResult != null
        ? Text('Sensor Result:\n$sensorResult',
        style: const TextStyle(fontSize: 16))
        : const Text('No data received yet.');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sensor Request')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            DropdownButton<String>(
              value: selectedSensor,
              isExpanded: true,
              onChanged: (newValue) {
                if (newValue != null) {
                  setState(() => selectedSensor = newValue);
                }
              },
              items: sensorOptions.keys
                  .map((label) => DropdownMenuItem(
                value: label,
                child: Text(label),
              ))
                  .toList(),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: isLoading ? null : requestSensorData,
              child: isLoading
                  ? const CircularProgressIndicator()
                  : const Text('Request Sensor Data'),
            ),
            const SizedBox(height: 30),
            buildResultWidget(),
          ],
        ),
      ),
    );
  }
}