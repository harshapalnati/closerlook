# GBand Test App

A simple Android app to connect to G Band fitness trackers and read health data using the Veepoo SDK.

## Features

- **BLE Scanning** - Find nearby Bluetooth devices
- **Device Connection** - Connect to G Band with password authentication
- **Read Data** - Read all health metrics:
  - Steps
  - Battery level
  - Sleep data
  - Heart rate
  - Raw health data
- **Live Heart Rate** - Real-time heart rate monitoring
- **Detailed Logs** - See all connection and data read steps

## Requirements

- Android 7.0+ (API 24+)
- Android phone with Bluetooth LE support
- G Band / Veepoo compatible fitness tracker

## Installation

1. Download `GBandTestApp-debug.apk` from releases
2. Install on your Android phone
3. Grant Bluetooth and Location permissions when prompted

## Usage

1. **Open the app** - Make sure Bluetooth is enabled
2. **Tap Scan** - Find nearby BLE devices
3. **Tap Connect** - Connect to your G Band (default password: 0000)
4. **Tap Read** - Read all health data from device
5. **Tap Heart** - Start real-time heart rate monitoring
6. **Tap Disconnect** - Disconnect from device

## Building from Source

```bash
cd GBandTestApp
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```
GBandTestApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/gband/test/
│   │   │   └── MainActivity.java    # Main app code
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml  # UI layout
│   │   │   └── values/             # Colors, strings, themes
│   │   └── AndroidManifest.xml
│   └── libs/                      # SDK libraries
├── build.gradle
└── settings.gradle
```

## SDK Credits

This app uses the [Veepoo Protocol SDK](http://www.veepoo.com/) for Bluetooth communication.

## License

MIT License