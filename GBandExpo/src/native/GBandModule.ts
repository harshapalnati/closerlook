import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { GBandModule: NativeGBand } = NativeModules;

if (!NativeGBand) {
  console.warn(`GBandModule native module not found on ${Platform.OS}. Make sure the app was built with the native bridge.`);
}

// Safe stub — all methods return rejected promises if native module is missing
const stub = new Proxy({}, {
  get: (_t, prop) => {
    if (prop === 'addListener' || prop === 'removeListeners') return () => {};
    return () => Promise.reject(new Error(`GBandModule not available on ${Platform.OS}`));
  },
});

export const GBandModule = NativeGBand ?? stub;

export const GBandEmitter = NativeGBand
  ? new NativeEventEmitter(NativeGBand)
  : null;

// ── Event name constants ──────────────────────────────────────────────────────
export const EVENTS = {
  SCAN_STATUS:      'onScanStatus',       // { status: 'scanning'|'stopped'|'canceled' }
  DEVICE_FOUND:     'onDeviceFound',      // { name, mac, rssi }
  CONNECTION_STATE: 'onConnectionState',  // { state: 'ble_connected'|'failed'|'auth_failed'|'disconnected', code?, reason? }
  CONNECTED:        'onConnected',        // { mac, name, firmware }
  CAPABILITIES:     'onCapabilities',     // { bp, spo2, temp, ecg, hrv, precisionSleep, stress, respRate, bloodGlucose }
  HEART_RATE:       'onHeartRate',        // { bpm }
  SPO2:             'onSpo2',             // { value }
  BP:               'onBP',              // { systolic, diastolic }
  TEMPERATURE:      'onTemperature',      // { celsius }
  HRV:              'onHRV',             // { hrv }
  WELLNESS_RESULT:  'onWellnessResult',   // { stress, fatigue, hrv, heartRate, spo2, temp, bpSystolic, bpDiastolic }
} as const;

// ── Typed wrappers ────────────────────────────────────────────────────────────
export const GBand = {
  startScan: (): Promise<void>            => GBandModule.startScan(),
  stopScan:  (): Promise<void>            => GBandModule.stopScan(),

  connectDevice: (mac: string, name: string): Promise<void> =>
    GBandModule.connectDevice(mac, name),
  disconnect: (): Promise<void>           => GBandModule.disconnect(),

  readBattery: (): Promise<number>        => GBandModule.readBattery(),
  readSteps:   (): Promise<{ steps: number; distanceKm: number; caloriesKcal: number }> =>
    GBandModule.readSteps(),
  readSleep: (): Promise<{
    totalMinutes: number; deepMinutes: number; lightMinutes: number;
    wakeCount: number; sleepQuality: number;
  }> => GBandModule.readSleep(),
  readOriginData: (): Promise<{
    heartRate?: number; bpSystolic?: number; bpDiastolic?: number;
    respirationRate?: number; spo2?: number;
  }> => GBandModule.readOriginData(),

  startHeartMeasure: (): Promise<void>    => GBandModule.startHeartMeasure(),
  stopHeartMeasure:  (): Promise<void>    => GBandModule.stopHeartMeasure(),

  startSpo2Measure: (): Promise<void>     => GBandModule.startSpo2Measure(),
  stopSpo2Measure:  (): Promise<void>     => GBandModule.stopSpo2Measure(),

  startBpMeasure: (): Promise<void>       => GBandModule.startBpMeasure(),
  stopBpMeasure:  (): Promise<void>       => GBandModule.stopBpMeasure(),

  startTempMeasure: (): Promise<void>     => GBandModule.startTempMeasure(),
  stopTempMeasure:  (): Promise<void>     => GBandModule.stopTempMeasure(),

  startWellnessCheck: (): Promise<void>   => GBandModule.startWellnessCheck(),
  stopWellnessCheck:  (): Promise<void>   => GBandModule.stopWellnessCheck(),
};
