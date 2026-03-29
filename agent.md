# Agent Guide — GBand Health App

## What This App Is
A health companion Android app for the **G Band** wearable (Veepoo hardware).
We use the **HBandSDK** (VeepooSDK) — pre-built AARs, no source, callback-based API.
Read CLAUDE.md for the full SDK reference, color system, and architecture.

## Primary Goals
1. Display real health data from the G Band device in a clean, readable UI
2. Target users aged **40–80** — accessibility is non-negotiable
3. WHOOP-inspired dark aesthetic — premium feel without complexity
4. 3-tab bottom nav: **Home** (data) | **Device** (connect) | **Settings** (profile)

## Metrics We Display (Home Tab)

### Section 1 — Heart
- **Current Heart Rate** (bpm) — live from `startDetectHeart()`
- Visual: large number, red accent, pulsing dot when live

### Section 2 — Activity (Today)
- **Steps** — from `readSportStep()` → `SportData.getStep()`
- **Distance** — km from `SportData.getDis()`
- **Calories** — kcal from `SportData.getKcal()`
- Visual: 3-column row of metric tiles

### Section 3 — Sleep (Last Night)
- **Total sleep** hours:minutes from `SleepData.getAllSleepTime()`
- **Deep sleep** minutes from `SleepData.getDeepSleepTime()`
- **Light sleep** minutes from `SleepData.getLowSleepTime()`
- **Wake count** from `SleepData.getWakeCount()`
- Visual: horizontal bar breakdown

### Section 4 — Vitals
- **SpO2** % — from `startDetectSPO2H()` or stored origin data
- **Blood Pressure** — systolic/diastolic from `startDetectBP()`
- Visual: 2-column cards

### Section 5 — Device Status (subtle footer)
- Battery % — always visible when connected

## Device Tab
- Connection status hero (large "Connected" / "Searching..." text)
- Scan button — triggers `startScanDevice()`, shows found device list
- Connect button — calls `connectDevice()` → `confirmDevicePwd()`
- Device info card (name, MAC, firmware version from `PwdData`)
- Battery level
- Disconnect button (red, bottom)

## Settings Tab
### Profile
- Name (text input, stored in SharedPreferences)
- Age (number input, 18–100)
- Sex (radio: Male / Female)

### Body Parameters (used for SDK accuracy)
- Height (cm, 100–220) — passed to `syncPersonInfo()`
- Weight (kg, 30–200) — passed to `syncPersonInfo()`

### Goals
- Daily step goal (default 8000)
- Sleep target (hours, default 8)

## Code Patterns to Follow

### Calling SDK (always serial, never concurrent)
```java
// Pattern: write response → data listener
VPOperateManager.getInstance().readSportStep(
    code -> { /* write response, ignore */ },
    sportData -> runOnUiThread(() -> {
        AppData.getInstance().steps = sportData.getStep();
        // update UI
    })
);
```

### Updating UI from callbacks
```java
// ALWAYS wrap UI updates in runOnUiThread
runOnUiThread(() -> tvHeartRate.setText(String.valueOf(bpm)));
```

### AppData pattern
```java
// DeviceFragment writes:
AppData.getInstance().heartRate = value;
AppData.getInstance().isConnected = true;

// HomeFragment reads on resume:
@Override
public void onResume() {
    super.onResume();
    refreshUI();
}
```

### SharedPreferences for user profile
```java
SharedPreferences prefs = requireContext()
    .getSharedPreferences("user_profile", Context.MODE_PRIVATE);
prefs.edit().putInt("age", 65).apply();
```

## What NOT to Do
- Do NOT show fake/simulated data — only display real SDK readings
- Do NOT add recovery/strain scores without HRV data from device
- Do NOT use concurrent SDK calls (device protocol is serial)
- Do NOT use text below 16sp anywhere
- Do NOT use light backgrounds — dark theme only
- Do NOT add features not in CLAUDE.md without discussing first
- Do NOT store sensitive health data outside SharedPreferences for now (no DB)

## Accessibility Checklist (run before any PR)
- [ ] All text ≥ 16sp
- [ ] All buttons ≥ 56dp tall
- [ ] Color contrast ≥ 4.5:1 for all text
- [ ] No gesture-only interactions
- [ ] Labels use plain language (no abbreviations without explanation)
- [ ] Connection state always clearly visible

## File Ownership
| File | Owner | Purpose |
|------|-------|---------|
| `AppData.java` | shared | Singleton state bus |
| `MainActivity.java` | infra | Nav host only |
| `HomeFragment.java` | feature | Reads AppData, renders UI |
| `DeviceFragment.java` | feature | Owns all SDK calls |
| `SettingsFragment.java` | feature | Reads/writes SharedPreferences |
| `colors.xml` | design | Single source of truth for palette |
| `themes.xml` | design | Dark theme definition |
