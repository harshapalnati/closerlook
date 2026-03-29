import React, { useEffect, useRef } from 'react';
import {
  View, Text, ScrollView, StyleSheet, TouchableOpacity,
  ActivityIndicator, PermissionsAndroid, Platform, StatusBar,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { GBand, GBandEmitter, EVENTS } from '../native/GBandModule';
import { useHealthStore, FoundDevice, Capabilities } from '../store/healthStore';
import { colors, fonts, radius, spacing } from '../theme/theme';

// ── Permission helper ─────────────────────────────────────────────────────────
async function requestBlePermissions(): Promise<boolean> {
  if (Platform.OS !== 'android') return true;
  try {
    if (Platform.Version >= 31) {
      const results = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
      ]);
      return Object.values(results).every(
        (r) => r === PermissionsAndroid.RESULTS.GRANTED,
      );
    } else {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      );
      return result === PermissionsAndroid.RESULTS.GRANTED;
    }
  } catch {
    return false;
  }
}

// ── Capability row ─────────────────────────────────────────────────────────────
function CapRow({ label, supported }: { label: string; supported: boolean }) {
  return (
    <View style={styles.capRow}>
      <Text style={[styles.capDot, { color: supported ? colors.green : colors.textMuted }]}>
        {supported ? '✓' : '✗'}
      </Text>
      <Text style={[styles.capLabel, { color: supported ? colors.textPrimary : colors.textMuted }]}>
        {label}
      </Text>
    </View>
  );
}

// ── Device row in scan list ────────────────────────────────────────────────────
function DeviceRow({ device, onConnect }: { device: FoundDevice; onConnect: () => void }) {
  return (
    <TouchableOpacity style={styles.deviceRow} onPress={onConnect} activeOpacity={0.75}>
      <View style={styles.deviceRowLeft}>
        <Text style={styles.deviceRowName}>{device.name}</Text>
        <Text style={styles.deviceRowMac}>{device.mac}</Text>
      </View>
      <View style={styles.deviceRowRight}>
        <Text style={styles.rssiText}>{device.rssi} dBm</Text>
        <Text style={styles.connectArrow}>›</Text>
      </View>
    </TouchableOpacity>
  );
}

// ── Main Screen ───────────────────────────────────────────────────────────────
export default function DeviceScreen() {
  const store = useHealthStore();
  const subRefs = useRef<any[]>([]);

  useEffect(() => {
    if (!GBandEmitter) return;

    const subs = [
      GBandEmitter.addListener(EVENTS.SCAN_STATUS, (e) => {
        store.setScanStatus(e.status === 'scanning' ? 'scanning' : 'stopped');
        if (e.status === 'scanning') store.addLog('Scan started');
        if (e.status === 'stopped')  store.addLog('Scan stopped');
      }),
      GBandEmitter.addListener(EVENTS.DEVICE_FOUND, (e: FoundDevice) => {
        const already = store.foundDevices.some((d) => d.mac === e.mac);
        if (!already) {
          store.addFoundDevice(e);
          store.addLog(`Found: ${e.name} (${e.mac})`);
        }
      }),
      GBandEmitter.addListener(EVENTS.CONNECTION_STATE, (e) => {
        store.addLog(`State: ${e.state}${e.reason ? ` — ${e.reason}` : ''}`);
        if (e.state === 'failed' || e.state === 'auth_failed') {
          store.setConnecting(false);
        }
        if (e.state === 'disconnected') {
          store.setConnected(false);
          store.setConnecting(false);
          store.resetReadings();
        }
      }),
      GBandEmitter.addListener(EVENTS.CONNECTED, (e) => {
        store.setDeviceInfo(e.name, e.mac, e.firmware);
        store.setConnected(true);
        store.setConnecting(false);
        store.addLog(`Connected! Firmware: ${e.firmware || 'unknown'}`);
        // Read battery after connect
        GBand.readBattery()
          .then((pct) => store.setBattery(pct))
          .catch(() => {});
      }),
      GBandEmitter.addListener(EVENTS.CAPABILITIES, (caps: Capabilities) => {
        store.setCapabilities(caps);
        store.addLog('Device capabilities loaded');
      }),
    ];
    subRefs.current = subs;
    return () => subs.forEach((s) => s.remove());
  }, []);

  const handleScan = async () => {
    const granted = await requestBlePermissions();
    if (!granted) {
      store.addLog('Bluetooth permission denied');
      return;
    }
    store.clearFoundDevices();
    store.clearLog();
    try {
      await GBand.startScan();
    } catch (e: any) {
      store.addLog(`Scan error: ${e?.message}`);
    }
  };

  const handleConnect = async (device: FoundDevice) => {
    store.setConnecting(true);
    store.addLog(`Connecting to ${device.name}…`);
    try {
      await GBand.connectDevice(device.mac, device.name);
    } catch (e: any) {
      store.addLog(`Connect error: ${e?.message}`);
      store.setConnecting(false);
    }
  };

  const handleDisconnect = async () => {
    await GBand.disconnect();
  };

  const caps = store.capabilities;

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" backgroundColor={colors.bg} />

      <View style={styles.header}>
        <Text style={styles.headerTitle}>DEVICE</Text>
        {/* Live status dot */}
        <View style={styles.statusRow}>
          <View style={[styles.statusDot, {
            backgroundColor: store.isConnected ? colors.green
              : store.isConnecting ? colors.yellow
              : colors.red,
          }]} />
          <Text style={[styles.statusText, {
            color: store.isConnected ? colors.green
              : store.isConnecting ? colors.yellow
              : colors.red,
          }]}>
            {store.isConnected ? 'Connected'
              : store.isConnecting ? 'Connecting…'
              : 'Not Connected'}
          </Text>
        </View>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}>

        {/* ── Connected device info ──────────────────────────────────── */}
        {store.isConnected && (
          <View style={styles.card}>
            <Text style={styles.sectionLabel}>PAIRED DEVICE</Text>
            <Text style={styles.deviceName}>{store.deviceName || 'G Band'}</Text>
            <View style={styles.infoGrid}>
              <InfoItem label="MAC" value={store.deviceMac || '—'} />
              <InfoItem label="FIRMWARE" value={store.firmware || '—'} />
              <InfoItem
                label="BATTERY"
                value={store.battery >= 0 ? `${store.battery}%` : '—'}
                valueColor={
                  store.battery >= 50 ? colors.green
                  : store.battery >= 20 ? colors.yellow
                  : colors.red
                }
              />
            </View>
            <TouchableOpacity style={styles.disconnectBtn} onPress={handleDisconnect} activeOpacity={0.75}>
              <Text style={styles.disconnectBtnText}>DISCONNECT</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* ── Device capabilities ────────────────────────────────────── */}
        {caps && (
          <View style={styles.card}>
            <Text style={styles.sectionLabel}>HARDWARE CAPABILITIES</Text>
            <Text style={styles.capsNote}>
              These are features confirmed by YOUR device — not assumptions.
            </Text>
            <CapRow label="Blood Pressure"         supported={caps.bp} />
            <CapRow label="Blood Oxygen (SpO₂)"    supported={caps.spo2} />
            <CapRow label="Body Temperature"        supported={caps.temp} />
            <CapRow label="HRV"                     supported={caps.hrv} />
            <CapRow label="Stress Detection"        supported={caps.stress} />
            <CapRow label="Respiratory Rate"        supported={caps.respRate} />
            <CapRow label="ECG"                     supported={caps.ecg} />
            <CapRow label="Precision Sleep (REM)"   supported={caps.precisionSleep} />
            <CapRow label="Blood Glucose"           supported={caps.bloodGlucose} />
          </View>
        )}

        {/* ── Scan section ───────────────────────────────────────────── */}
        {!store.isConnected && (
          <View style={styles.card}>
            <Text style={styles.sectionLabel}>FIND YOUR G BAND</Text>
            <Text style={styles.scanHint}>
              Make sure your band is nearby and Bluetooth is on.
            </Text>
            <TouchableOpacity
              style={[styles.scanBtn, store.scanStatus === 'scanning' && styles.scanBtnActive]}
              onPress={handleScan}
              disabled={store.isConnecting || store.scanStatus === 'scanning'}
              activeOpacity={0.8}>
              {store.scanStatus === 'scanning'
                ? <ActivityIndicator color={colors.textPrimary} size="small" />
                : null}
              <Text style={styles.scanBtnText}>
                {store.scanStatus === 'scanning' ? '  SCANNING…' : 'SCAN FOR DEVICES'}
              </Text>
            </TouchableOpacity>
          </View>
        )}

        {/* ── Found devices list ─────────────────────────────────────── */}
        {store.foundDevices.length > 0 && !store.isConnected && (
          <View style={styles.card}>
            <Text style={styles.sectionLabel}>FOUND DEVICES</Text>
            {store.isConnecting && (
              <View style={styles.connectingRow}>
                <ActivityIndicator color={colors.yellow} size="small" />
                <Text style={styles.connectingText}>  Connecting…</Text>
              </View>
            )}
            {store.foundDevices.map((d) => (
              <DeviceRow
                key={d.mac}
                device={d}
                onConnect={() => handleConnect(d)}
              />
            ))}
          </View>
        )}

        {/* ── Connection log ─────────────────────────────────────────── */}
        {store.connectionLog.length > 0 && (
          <View style={styles.logCard}>
            <Text style={styles.sectionLabel}>LOG</Text>
            {store.connectionLog.map((line, i) => (
              <Text key={i} style={styles.logLine}>{line}</Text>
            ))}
          </View>
        )}

        <View style={{ height: spacing.xl }} />
      </ScrollView>
    </SafeAreaView>
  );
}

function InfoItem({
  label, value, valueColor,
}: { label: string; value: string; valueColor?: string }) {
  return (
    <View style={styles.infoItem}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={[styles.infoValue, valueColor ? { color: valueColor } : {}]}>{value}</Text>
    </View>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = StyleSheet.create({
  safe:          { flex: 1, backgroundColor: colors.bg },
  scroll:        { flex: 1 },
  scrollContent: { padding: spacing.md },

  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  headerTitle: { fontSize: fonts.lg, color: colors.textPrimary, fontWeight: '800', letterSpacing: 2 },
  statusRow:   { flexDirection: 'row', alignItems: 'center' },
  statusDot:   { width: 8, height: 8, borderRadius: 4, marginRight: 6 },
  statusText:  { fontSize: fonts.sm, fontWeight: '600' },

  card: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  sectionLabel: {
    fontSize: fonts.xs,
    color: colors.textMuted,
    fontWeight: '700',
    letterSpacing: 1.5,
    marginBottom: spacing.sm,
  },
  deviceName: {
    fontSize: fonts.xl,
    color: colors.textPrimary,
    fontWeight: '700',
    marginBottom: spacing.sm,
  },
  infoGrid:    { gap: spacing.xs, marginBottom: spacing.md },
  infoItem:    { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 4,
                 borderBottomWidth: 1, borderBottomColor: colors.border },
  infoLabel:   { fontSize: fonts.sm, color: colors.textSecondary },
  infoValue:   { fontSize: fonts.sm, color: colors.textPrimary, fontWeight: '600' },

  disconnectBtn: {
    borderWidth: 1.5, borderColor: colors.red,
    borderRadius: radius.sm, paddingVertical: spacing.sm, alignItems: 'center',
  },
  disconnectBtnText: { fontSize: fonts.sm, color: colors.red, fontWeight: '700', letterSpacing: 1 },

  capsNote: { fontSize: fonts.sm, color: colors.textSecondary, marginBottom: spacing.sm, fontStyle: 'italic' },
  capRow:   { flexDirection: 'row', alignItems: 'center', paddingVertical: 5,
              borderBottomWidth: 1, borderBottomColor: colors.border },
  capDot:   { fontSize: fonts.md, fontWeight: '700', width: 24 },
  capLabel: { fontSize: fonts.md },

  scanHint: { fontSize: fonts.sm, color: colors.textSecondary, marginBottom: spacing.md },
  scanBtn:  {
    flexDirection: 'row', justifyContent: 'center', alignItems: 'center',
    backgroundColor: colors.accent, borderRadius: radius.sm, paddingVertical: spacing.md,
  },
  scanBtnActive:  { backgroundColor: colors.accentDim },
  scanBtnText:    { fontSize: fonts.md, color: colors.textPrimary, fontWeight: '700', letterSpacing: 1 },

  connectingRow: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.sm },
  connectingText:{ fontSize: fonts.md, color: colors.yellow },

  deviceRow: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingVertical: spacing.sm, borderBottomWidth: 1, borderBottomColor: colors.border,
  },
  deviceRowLeft:  {},
  deviceRowName:  { fontSize: fonts.md, color: colors.textPrimary, fontWeight: '600' },
  deviceRowMac:   { fontSize: fonts.xs, color: colors.textMuted },
  deviceRowRight: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  rssiText:       { fontSize: fonts.xs, color: colors.textSecondary },
  connectArrow:   { fontSize: fonts.xl, color: colors.accent },

  logCard: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  logLine: { fontSize: fonts.xs, color: colors.textSecondary, fontFamily: 'monospace', lineHeight: 18 },
});
