package com.gband.test;

/**
 * Singleton state bus. DeviceFragment writes connection state.
 * HomeFragment writes/reads health readings.
 * All fields are set to sentinel "no data" values until populated from device.
 */
public class AppData {

    private static AppData instance;

    private AppData() {}

    public static AppData getInstance() {
        if (instance == null) {
            instance = new AppData();
        }
        return instance;
    }

    // ── Connection ──────────────────────────────────────────────────────────
    public boolean isConnected    = false;
    public String  deviceName     = "";
    public String  deviceMac      = "";
    public String  firmwareVersion = "";

    // ── Device capability flags (from FunctionDeviceSupportData on connect) ──
    // All default false until device reports its capabilities.
    // Use these to HIDE/DISABLE UI for features the device doesn't have.
    public boolean supportsBp          = false;   // Blood Pressure sensor
    public boolean supportsSpo2        = false;   // SpO2 / blood oxygen
    public boolean supportsTemp        = false;   // Body temperature sensor
    public boolean supportsEcg         = false;   // ECG
    public boolean supportsHrv         = false;   // HRV
    public boolean supportsPrecisionSleep = false; // REM sleep stages
    public boolean supportsStress      = false;   // Stress detection
    public boolean supportsRespRate    = false;   // Respiratory rate (breathFunction)
    public boolean supportsBloodGlucose = false;  // Blood glucose sensor
    public boolean capabilitiesLoaded  = false;   // true once flags have been read

    // ── Battery ──────────────────────────────────────────────────────────────
    public int batteryPercent = -1;       // -1 = unknown

    // ── Heart Rate ───────────────────────────────────────────────────────────
    public int heartRate = -1;            // bpm, -1 = no data

    // ── Activity ──────────────────────────────────────────────────────────────
    public int    steps        = -1;
    public double distanceKm   = -1;
    public double caloriesKcal = -1;

    // ── Sleep ────────────────────────────────────────────────────────────────
    public int sleepTotalMinutes = -1;
    public int sleepDeepMinutes  = -1;
    public int sleepLightMinutes = -1;
    public int sleepRemMinutes   = -1;    // REM (precision sleep mode only)
    public int sleepWakeCount    = -1;
    public int sleepQuality      = -1;    // 0–100 quality score from device
    public int sleepApneaEvents  = -1;    // overnight apnea event count

    // ── Vitals ───────────────────────────────────────────────────────────────
    public int   spo2Percent  = -1;       // blood oxygen %
    public int   bpSystolic   = -1;       // mmHg systolic
    public int   bpDiastolic  = -1;       // mmHg diastolic
    public float temperature  = -1f;      // °C body temperature
    public int   respirationRate = -1;    // breaths per minute

    // ── Wellness / Recovery ───────────────────────────────────────────────────
    public int hrv         = -1;          // Heart Rate Variability (ms)
    public int stressLevel = -1;          // 0–100 stress score from MiniCheckup
    public int fatigue     = -1;          // fatigue degree from MiniCheckup

    /** Reset all health readings — call on disconnect */
    public void resetReadings() {
        batteryPercent   = -1;
        heartRate        = -1;
        steps            = -1;
        distanceKm       = -1;
        caloriesKcal     = -1;
        sleepTotalMinutes = -1;
        sleepDeepMinutes = -1;
        sleepLightMinutes = -1;
        sleepRemMinutes  = -1;
        sleepWakeCount   = -1;
        sleepQuality     = -1;
        sleepApneaEvents = -1;
        spo2Percent      = -1;
        bpSystolic       = -1;
        bpDiastolic      = -1;
        temperature      = -1f;
        respirationRate  = -1;
        hrv              = -1;
        stressLevel      = -1;
        fatigue          = -1;
    }
}
