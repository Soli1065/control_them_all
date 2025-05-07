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
  static const methodChannel = MethodChannel('sensor_channel');
  static const eventChannel = EventChannel('sensor_result_stream');

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
  Stream<dynamic>? sensorStream;

  @override
  void initState() {
    super.initState();

    // Start listening for native messages
    sensorStream = eventChannel.receiveBroadcastStream();
    sensorStream!.listen((event) {
      debugPrint('🎯 Sensor result received: $event');
      setState(() {
        sensorResult = event;
        isLoading = false;
      });
    }, onError: (error) {
      debugPrint('❌ Error from native: $error');
    });
  }

  Future<void> requestSensorData() async {
    final sensorType = sensorOptions[selectedSensor]!;
    setState(() {
      isLoading = true;
      sensorResult = null;
    });

    try {
      await methodChannel.invokeMethod('sendSensorCommand', {
        'sensorType': sensorType,
        'mode': 'snapshot',
        'duration': 3000,
      });

      debugPrint('📤 Sensor command sent.');
    } catch (e) {
      debugPrint('❌ Failed to send command: $e');
      setState(() {
        isLoading = false;
      });
    }
  }

  Widget buildResultWidget() {
    if (isLoading) {
      return const CircularProgressIndicator();
    }

    return sensorResult != null
        ? Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Sensor Result:',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        Text(sensorResult!, style: const TextStyle(fontSize: 16)),
      ],
    )
        : const Text('No data received yet.', style: TextStyle(fontSize: 16));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sensor Request (Phone)')),
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
              child: const Text('Request Sensor Data'),
            ),
            const SizedBox(height: 30),
            buildResultWidget(),
          ],
        ),
      ),
    );
  }
}