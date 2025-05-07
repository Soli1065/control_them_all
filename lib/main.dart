import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  final channel = const MethodChannel('sensor_channel');

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Watch Sensor Trigger',
      home: Scaffold(
        appBar: AppBar(title: const Text('Send Command to Watch')),
        body: Center(
          child: ElevatedButton(
            onPressed: () async {
              try {
                final result = await channel.invokeMethod('sendSensorCommand', {
                  'sensorType': 1,     // Accelerometer
                  'mode': 'snapshot',  // Stream mode not yet supported
                  'duration': 3000     // Optional, for future use
                });
                print("Command result: $result");
              } catch (e) {
                print("Error sending command: $e");
              }
            },
            child: const Text('Send Start Sensor Command'),
          ),
        ),
      ),
    );
  }
}