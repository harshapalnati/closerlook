# GBand Health App — Feature Backlog

> Track features planned, in progress, and done.
> Update this file as features are implemented.

---

## ⚠️ Hardware Reality Check

The SDK supports all features listed below, but **not every G Band device has every sensor**.
Confirmed working features come from the test app (`closerlook` repo).
Everything else depends on what `FunctionDeviceSupportData` reports for YOUR specific band.

**The Device tab now shows a live "What Your Band Supports" card after connecting** — green ✓ = confirmed by hardware, grey ✗ = not available on this device. Use that as the ground truth before building any feature.

| Feature | Confirmed on G Band? |
|---------|---------------------|
| Heart Rate (live + history) | ✅ Yes — test app proves it |
| Steps / Distance / Calories | ✅ Yes — test app proves it |
| Sleep (total/deep/light/wake) | ✅ Yes — test app proves it |
| Battery | ✅ Yes — test app proves it |
| SpO2 | ⚠️ Likely — SDK has it, test app has the UI but never populated it |
| Blood Pressure | ❓ Check device capabilities card |
| Body Temperature | ❓ Check device capabilities card |
| HRV | ❓ Check device capabilities card |
| Stress / Fatigue (MiniCheckup) | ❓ Check device capabilities card |
| Respiratory Rate | ❓ Check device capabilities card |
| ECG | ❓ Check device capabilities card (probably not on G Band) |
| Sleep Quality Score | ❓ Field exists in SleepData but may return 0 if unsupported |
| Precision Sleep / REM | ❓ Check device capabilities card |

---

## ✅ Implemented

| Feature | Where | Notes |
|---------|-------|-------|
| Heart Rate (live measure) | Home → Heart card | `startDetectHeart()` |
| Heart Rate (history) | Home → Heart card | Via `readOriginData3()` |
| Steps | Home → Activity section | `readSportStep()` |
| Distance (km) | Home → Activity section | `readSportStep()` |
| Calories (kcal) | Home → Activity section | `readSportStep()` |
| Sleep Total Time | Home → Sleep card | `readSleepData()` |
| Sleep Deep | Home → Sleep card | `readSleepData()` |
| Sleep Light | Home → Sleep card | `readSleepData()` |
| Sleep Wake Count | Home → Sleep card | `readSleepData()` |
| **Sleep Quality Score** | Home → Sleep card | `SleepData.getSleepQulity()` — color-coded |
| SpO2 / Blood Oxygen (live) | Home → Vitals | `startDetectSPO2H()` — with health interpretation |
| Blood Pressure (live) | Home → Vitals | `startDetectBP()` — with hypertension warnings |
| **Body Temperature** | Home → Wellness | `startDetectTempture()` — fever alerts |
| **HRV** | Home → Wellness | Via `IOriginData3Listener` + `MiniCheckupResultData` |
| **Respiratory Rate** | Home → Wellness | Via `IOriginData3Listener.getResRates()` |
| **Stress Level** | Home → Wellness Check | `startMiniCheckup()` — 0–100 score |
| **Fatigue** | Home → Wellness Check | `startMiniCheckup()` — 0–100 score |
| Battery % | Home → top badge | `readBattery()` |
| BLE Scan + Connect | Device tab | `startScanDevice()` + `connectDevice()` |
| Device info (name, MAC, firmware) | Device tab | From `PwdData` |
| User profile (name, age, sex, height, weight) | Settings tab | SharedPreferences |
| Daily goals (steps, sleep) | Settings tab | SharedPreferences |
| Sync profile to device | Settings tab | `syncPersonInfo()` — improves calorie accuracy |

---

## 🔲 Backlog — Not Yet Implemented

### High Priority (valuable for 40–80 year olds)

| Feature | SDK Method | Effort | Notes |
|---------|-----------|--------|-------|
| **REM Sleep** | `SleepData.getSleepLine()` | Small | Parse sleepLine string: precision mode `2`=REM. Only works on devices with `precisionSleep` flag |
| **Sleep Apnea Events** | `IOriginData3Listener` → `OriginData3.apneaResults` + `hypoxiaTimes` | Small | Show overnight apnea event count in sleep card — very relevant for 40–80 |
| **ECG** | `startDetectECG()` → `EcgDetectResult` | Medium | Full waveform + 8-point diagnosis array, QTc, average HR/HRV. Great for AFib detection |
| **Heart Rate History Chart** | Already reading data | Medium | Plot last 24h HR readings from origin data on a simple line chart |
| **Sleep History (7 nights)** | `readSleepDataFromDay()` | Medium | Show week-over-week sleep trend |
| **Step Goal Progress Ring** | Already have step goal | Small | Visual progress ring showing steps vs daily goal |
| **Cardiac Load** | `IOriginData3Listener` → `OriginData3.cardiacLoads` | Small | Heart strain indicator — show as low/medium/high |

### Medium Priority

| Feature | SDK Method | Effort | Notes |
|---------|-----------|--------|-------|
| **Alarms on device** | `addAlarm2()` / `readAlarm2()` | Medium | Set wake-up alarms from the app |
| **Sedentary reminder** | `settingLongSeat()` / `readLongSeat()` | Small | Alert user to stand up after sitting too long |
| **Drink water reminder** | `readDrinkData()` | Small | Hydration reminders |
| **Auto heart rate detection toggle** | `changeCustomSetting()` → `isOpenAutoHeartDetect` | Small | Turn on/off automatic background HR monitoring |
| **Auto SpO2 detection toggle** | `changeCustomSetting()` → auto SpO2 | Small | Background SpO2 monitoring during sleep |
| **Heart Rate warning thresholds** | `settingHeartWarning()` / `readHeartWarning()` | Small | Set upper/lower HR alert limits |
| **Device language setting** | `settingDeviceLanguage()` | Small | Set language on band display |
| **Screen brightness** | `settingScreenLight()` | Small | Control band screen brightness |
| **Find phone** | `settingFindPhoneListener()` | Small | Vibrate phone from band |
| **Weather push to device** | `setWeatherData()` | Medium | Show weather on band |

### Lower Priority / Device-Dependent

| Feature | SDK Method | Effort | Notes |
|---------|-----------|--------|-------|
| **Blood Glucose** | `OriginData3.bloodGlucose` | Small | Only on devices with glucose sensor — check `FunctionDeviceSupportData` |
| **Blood Components** | `OriginData3.bloodComponent` | Small | Cholesterol, triglycerides — specialized hardware only |
| **Body Composition** | `MiniCheckupDetailData` | Medium | BMI, body fat %, muscle mass, bone mass — extended MiniCheckup |
| **Women's Health / Cycle** | `settingWomenState()` | Medium | Menstrual cycle tracking |
| **OTA Firmware Update** | `checkVersionAndFile()` + `startOad()` | Large | In-app firmware upgrade flow |
| **Watch face customization** | `UiUpdateUtil` / JL SDK | Large | Change the band's watch face from the app |
| **ECG diagnosis display** | `EcgDetectResult.diagnosis` | Medium | Show the 8 diagnosis results in plain language |
| **GPS tracking** | AGPS feature | Large | Location + route tracking during walks |
| **Notification mirroring** | `settingSocialMsg()` | Medium | Mirror phone notifications (calls, SMS, apps) to band |
| **SOS / Emergency** | `settingSOSListener()` | Medium | Emergency alert from band |

---

## 🐛 Known Issues / Technical Debt

| Issue | Impact | Fix |
|-------|--------|-----|
| `IOriginData3Listener` class name — verify against AAR | Build error if wrong | Open External Libraries in Android Studio and find exact name in vpprotocol AAR |
| `ISpo2HDataListener` / `Spo2HData.getOxygen()` — verify method name | Build error | Same — check AAR |
| `IBPDataListener` / `BpData` — verify class names | Build error | Same |
| `ITemptureDataListener` / `TemptureDetectData.getTempture()` — verify | Build error | Same |
| `IMiniCheckupDataListener` / `MiniCheckupResultData` method names — verify | Build error | Same |
| `IOriginData3Listener.onOriginData3Change` — verify exact callback signature | Build error | Same |
| `HRVOriginData.getHrv()` — verify method name | Build error | Same |
| Refresh chain: if `readSportStep` fires listener multiple times, next steps may start too early | Minor | Wrap with single-fire boolean guard if needed |

---

## 📋 UX / Design Backlog

| Item | Priority |
|------|----------|
| Loading skeleton/shimmer while data loads | Medium |
| Empty state illustrations (no data yet) | Low |
| Pull-to-refresh gesture on Home | Medium |
| Haptic feedback on measurement start/stop | Low |
| History graphs (HR, BP, sleep trends) | High |
| Daily summary card at top of Home | Medium |
| Notifications for health alerts (high BP, low SpO2) | High |
| Dark/light mode toggle (currently dark only) | Low |
| Onboarding screen (first launch walkthrough) | Medium |
| Accessibility: font size setting in app | High — for 40–80 users |
