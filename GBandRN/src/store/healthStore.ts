import { create } from 'zustand';

// ── Types ─────────────────────────────────────────────────────────────────────
export interface FoundDevice {
  name: string;
  mac:  string;
  rssi: number;
}

export interface Capabilities {
  bp:             boolean;
  spo2:           boolean;
  temp:           boolean;
  ecg:            boolean;
  hrv:            boolean;
  precisionSleep: boolean;
  stress:         boolean;
  respRate:       boolean;
  bloodGlucose:   boolean;
}

export interface SleepData {
  totalMinutes:  number;
  deepMinutes:   number;
  lightMinutes:  number;
  wakeCount:     number;
  sleepQuality:  number; // 0-100
}

export interface UserProfile {
  name:       string;
  age:        number;
  heightCm:   number;
  weightKg:   number;
  gender:     'male' | 'female';
  sleepGoalH: number; // target sleep hours
  stepGoal:   number;
}

export interface HealthState {
  // Device connection
  isConnected:      boolean;
  isConnecting:     boolean;
  deviceName:       string;
  deviceMac:        string;
  firmware:         string;
  battery:          number;  // -1 = unknown
  capabilities:     Capabilities | null;
  foundDevices:     FoundDevice[];
  scanStatus:       'idle' | 'scanning' | 'stopped';
  connectionLog:    string[];

  // Activity
  steps:            number;
  distanceKm:       number;
  caloriesKcal:     number;

  // Vitals — live readings
  heartRate:        number;
  spo2:             number;
  bpSystolic:       number;
  bpDiastolic:      number;
  temperatureC:     number;
  respirationRate:  number;
  hrv:              number;

  // Wellness (from MiniCheckup)
  stress:           number;  // 0-100, -1 = unknown
  fatigue:          number;  // 0-100, -1 = unknown

  // Sleep
  sleep:            SleepData | null;

  // Measurement states
  measuringHeart:   boolean;
  measuringSpo2:    boolean;
  measuringBp:      boolean;
  measuringTemp:    boolean;
  measuringWellness:boolean;

  // User profile
  profile: UserProfile;

  // Actions
  setConnected:     (connected: boolean) => void;
  setConnecting:    (connecting: boolean) => void;
  setDeviceInfo:    (name: string, mac: string, firmware: string) => void;
  setBattery:       (pct: number) => void;
  setCapabilities:  (caps: Capabilities) => void;
  addFoundDevice:   (device: FoundDevice) => void;
  clearFoundDevices:() => void;
  setScanStatus:    (s: 'idle' | 'scanning' | 'stopped') => void;
  addLog:           (msg: string) => void;
  clearLog:         () => void;

  setSteps:         (steps: number, dist: number, cal: number) => void;
  setHeartRate:     (bpm: number) => void;
  setSpo2:          (val: number) => void;
  setBP:            (sys: number, dia: number) => void;
  setTemperature:   (c: number) => void;
  setRespirationRate:(r: number) => void;
  setHRV:           (hrv: number) => void;
  setWellness:      (stress: number, fatigue: number) => void;
  setSleep:         (data: SleepData) => void;

  setMeasuring:     (key: 'measuringHeart' | 'measuringSpo2' | 'measuringBp' | 'measuringTemp' | 'measuringWellness', val: boolean) => void;
  updateProfile:    (partial: Partial<UserProfile>) => void;
  resetReadings:    () => void;
}

// ── Store ─────────────────────────────────────────────────────────────────────
export const useHealthStore = create<HealthState>((set) => ({
  isConnected:       false,
  isConnecting:      false,
  deviceName:        '',
  deviceMac:         '',
  firmware:          '',
  battery:           -1,
  capabilities:      null,
  foundDevices:      [],
  scanStatus:        'idle',
  connectionLog:     [],

  steps:             0,
  distanceKm:        0,
  caloriesKcal:      0,

  heartRate:         0,
  spo2:              0,
  bpSystolic:        0,
  bpDiastolic:       0,
  temperatureC:      0,
  respirationRate:   0,
  hrv:               0,

  stress:            -1,
  fatigue:           -1,

  sleep:             null,

  measuringHeart:    false,
  measuringSpo2:     false,
  measuringBp:       false,
  measuringTemp:     false,
  measuringWellness: false,

  profile: {
    name:       'Your Name',
    age:        55,
    heightCm:   170,
    weightKg:   70,
    gender:     'male',
    sleepGoalH: 8,
    stepGoal:   8000,
  },

  setConnected:      (connected) => set({ isConnected: connected }),
  setConnecting:     (connecting) => set({ isConnecting: connecting }),
  setDeviceInfo:     (name, mac, firmware) => set({ deviceName: name, deviceMac: mac, firmware }),
  setBattery:        (battery) => set({ battery }),
  setCapabilities:   (capabilities) => set({ capabilities }),
  addFoundDevice:    (device) => set((s) => ({ foundDevices: [...s.foundDevices, device] })),
  clearFoundDevices: () => set({ foundDevices: [] }),
  setScanStatus:     (scanStatus) => set({ scanStatus }),
  addLog:            (msg) => set((s) => ({ connectionLog: [...s.connectionLog.slice(-49), `${new Date().toLocaleTimeString()}  ${msg}`] })),
  clearLog:          () => set({ connectionLog: [] }),

  setSteps:          (steps, distanceKm, caloriesKcal) => set({ steps, distanceKm, caloriesKcal }),
  setHeartRate:      (heartRate) => set({ heartRate }),
  setSpo2:           (spo2) => set({ spo2 }),
  setBP:             (bpSystolic, bpDiastolic) => set({ bpSystolic, bpDiastolic }),
  setTemperature:    (temperatureC) => set({ temperatureC }),
  setRespirationRate:(respirationRate) => set({ respirationRate }),
  setHRV:            (hrv) => set({ hrv }),
  setWellness:       (stress, fatigue) => set({ stress, fatigue }),
  setSleep:          (sleep) => set({ sleep }),

  setMeasuring:      (key, val) => set({ [key]: val }),
  updateProfile:     (partial) => set((s) => ({ profile: { ...s.profile, ...partial } })),

  resetReadings: () => set({
    steps: 0, distanceKm: 0, caloriesKcal: 0,
    heartRate: 0, spo2: 0, bpSystolic: 0, bpDiastolic: 0,
    temperatureC: 0, respirationRate: 0, hrv: 0,
    stress: -1, fatigue: -1, sleep: null, battery: -1,
    capabilities: null,
  }),
}));
