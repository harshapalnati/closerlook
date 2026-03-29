# GBand Health App — CLAUDE.md

## Project Overview
Android health companion app for the **G Band** wearable device (Veepoo-based hardware).
Built using the **HBandSDK** (VeepooSDK) via pre-packaged AARs in `GBandTestApp/app/libs/`.
Target users: **40–80 year olds** — prioritize readability, simplicity, and accessibility.

## Repository Structure
```
closerlook/
├── CLAUDE.md                  ← you are here
├── agent.md                   ← agent behavior guide
└── GBandTestApp/              ← Android project root
    └── app/
        ├── libs/              ← SDK AARs (DO NOT modify)
        │   ├── vpbluetooth-1.18.aar
        │   ├── vpprotocol-2.3.47.15.aar
        │   └── ...
        └── src/main/
            ├── java/com/gband/test/
            │   ├── MainActivity.java          ← bottom nav host
            │   ├── AppData.java               ← shared state singleton
            │   └── fragments/
            │       ├── HomeFragment.java      ← health dashboard
            │       ├── DeviceFragment.java    ← BLE connectivity
            │       └── SettingsFragment.java  ← user profile
            ├── res/
            │   ├── layout/
            │   │   ├── activity_main.xml
            │   │   ├── fragment_home.xml
            │   │   ├── fragment_device.xml
            │   │   └── fragment_settings.xml
            │   ├── menu/bottom_nav_menu.xml
            │   └── values/
            │       ├── colors.xml             ← WHOOP palette
            │       ├── themes.xml             ← dark theme
            │       └── strings.xml
            └── AndroidManifest.xml
```

## SDK: HBandSDK / VeepooSDK
- **Org:** https://github.com/HBandSDK
- **Entry point:** `VPOperateManager.getInstance()` — singleton, ALL device ops go here
- **Architecture:** Callback/listener-based async. Operations must be **serialized** (no concurrent calls)
- **Init:** `VPOperateManager.getInstance().init(context)` — call once in MainActivity.onCreate()
- **Password:** Default device password is `"0000"`

### Supported Health Metrics
| Metric | SDK Method | Data Model |
|--------|-----------|------------|
| Steps | `readSportStep()` | `SportData.getStep()` |
| Distance (km) | `readSportStep()` | `SportData.getDis()` |
| Calories (kcal) | `readSportStep()` | `SportData.getKcal()` |
| Heart Rate (live) | `startDetectHeart()` / `stopDetectHeart()` | `HeartData.getRateValue()` |
| Heart Rate (history) | `readOriginData()` | `OriginData.getRateValue()` |
| Blood Pressure | `startDetectBP()` / `stopDetectBP()` | `OriginData.getHighValue()` / `.getLowValue()` |
| SpO2 | `startDetectSPO2H()` / `stopDetectSPO2H()` | via `IOriginData3Listener` |
| Sleep | `readSleepData(days)` | `SleepData` — total, deep, light, REM, awake |
| Battery | `readBattery()` | `BatteryData.getBatteryPercent()` |
| HRV | `readOriginData()` (IOriginData3) | `HRVOriginData` |
| Body Temp | `startDetectTempture()` | `TemptureDetectData` |
| Stress/Fatigue | `startMiniCheckup()` | `MiniCheckupResultData` |
| Respiration | `readOriginData()` (IOriginData3) | `OriginData3.resRates` |

### Sleep Stages (sleepLine string)
- **Standard mode** (5-min intervals): `0`=light, `1`=deep, `2`=awake
- **Precision mode** (1-min intervals): `0`=deep, `1`=light, `2`=REM, `3`=insomnia, `4`=awake

### No Native Support For
- Recovery score (infer from HRV + sleep quality)
- Strain score (infer from steps + HR data)

## App Architecture

### Navigation
3-tab bottom navigation:
1. **Home** — health metrics dashboard
2. **Device** — BLE connection management
3. **Settings** — user profile & body parameters

### State Management
`AppData.java` — singleton that stores all live readings.
Fragments read from `AppData` on `onResume()` and update UI.
The `DeviceFragment` drives all BLE connection logic and writes to `AppData`.
`HomeFragment` has a "Refresh" button to trigger data reads.

### Fragment Communication
- All fragments get `AppData.getInstance()` for reading state
- `DeviceFragment` calls SDK methods and stores results in `AppData`
- `HomeFragment` reads `AppData` and renders metrics
- `SettingsFragment` reads/writes `UserPrefs` (SharedPreferences wrapper)

## Design System — WHOOP Dark Theme

### Color Palette
```
Background:      #0D0D0F   (near black)
Surface:         #1A1A1F   (card backgrounds)
Card Elevated:   #252530
Accent Red:      #E8384F   (WHOOP signature, primary actions)
Green (good):    #02B875   (healthy readings)
Yellow (caution):#F5B731   (moderate / warning)
Red (alert):     #E8384F   (danger readings)
Blue (info):     #3B9EFF
Text Primary:    #FFFFFF
Text Secondary:  #9B9EA6
Text Tertiary:   #5A5C65
Divider:         #2A2A35
Bottom Nav Bg:   #131318
```

### Typography (40-80 year old accessibility)
- Metric values: **44sp** bold
- Card headers: **16sp** regular, secondary color
- Section labels: **20sp** semi-bold
- Body text: **18sp** regular
- Captions: **14sp** secondary color
- Minimum touch targets: **56dp** height

### Card Anatomy
Each metric card:
- `#1A1A1F` background, `16dp` corner radius
- Label (secondary color, 16sp) at top
- Big value (white, 44sp bold) center
- Unit (secondary color, 14sp) below value
- Optional color-coded dot indicator (green/yellow/red)

## Key Implementation Rules
1. Always call `VPOperateManager.getInstance().stopScanDevice()` before starting a new scan
2. Always `disconnectWatch()` before re-scanning
3. After `connectDevice()` + notify success → wait 1000ms → call `confirmDevicePwd()`
4. All UI updates must be on the main thread: `runOnUiThread()`
5. Check `isConnected` flag before calling any read/detect methods
6. Request BLUETOOTH_SCAN + BLUETOOTH_CONNECT + ACCESS_FINE_LOCATION at runtime (Android 12+)
7. Never hardcode MAC addresses — always scan and connect dynamically

## Target User Considerations (40–80 Years Old)
- **Large text** everywhere — no text below 16sp
- **High contrast** — white on dark backgrounds
- **Simple labels** — "Heart Rate" not "HR bpm"
- **Clear status** — explicit connected/disconnected indicators
- **No swipe gestures** for navigation
- **Generous padding** — minimum 16dp, cards 20dp internal padding
- **Large touch targets** — buttons minimum 56dp tall
- **Plain language** — "Your sleep last night" not technical jargon
