/**
 * Smoke test — verifies the app tree renders without throwing.
 * Native modules (GBandModule, AsyncStorage) are mocked.
 */
import 'react-native';
import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

// Mock react-native-screens
jest.mock('react-native-screens', () => ({ enableScreens: jest.fn() }));

// Mock AsyncStorage
jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);

// Mock the native GBandModule
jest.mock('react-native', () => {
  const rn = jest.requireActual('react-native');
  rn.NativeModules.GBandModule = {
    addListener: jest.fn(),
    removeListeners: jest.fn(),
    startScan: jest.fn(() => Promise.resolve()),
    stopScan: jest.fn(() => Promise.resolve()),
    connectDevice: jest.fn(() => Promise.resolve()),
    disconnect: jest.fn(() => Promise.resolve()),
    readBattery: jest.fn(() => Promise.resolve(85)),
    readSteps: jest.fn(() => Promise.resolve({ steps: 0, distanceKm: 0, caloriesKcal: 0 })),
    readSleep: jest.fn(() => Promise.resolve({ totalMinutes: 0, deepMinutes: 0, lightMinutes: 0, wakeCount: 0, sleepQuality: 0 })),
    readOriginData: jest.fn(() => Promise.resolve({})),
    startHeartMeasure: jest.fn(() => Promise.resolve()),
    stopHeartMeasure: jest.fn(() => Promise.resolve()),
    startSpo2Measure: jest.fn(() => Promise.resolve()),
    stopSpo2Measure: jest.fn(() => Promise.resolve()),
    startBpMeasure: jest.fn(() => Promise.resolve()),
    stopBpMeasure: jest.fn(() => Promise.resolve()),
    startTempMeasure: jest.fn(() => Promise.resolve()),
    stopTempMeasure: jest.fn(() => Promise.resolve()),
    startWellnessCheck: jest.fn(() => Promise.resolve()),
    stopWellnessCheck: jest.fn(() => Promise.resolve()),
  };
  return rn;
});

import App from '../App';

test('renders without crashing', async () => {
  await ReactTestRenderer.act(async () => {
    ReactTestRenderer.create(<App />);
  });
});
