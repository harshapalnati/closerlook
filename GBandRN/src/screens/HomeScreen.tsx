import React, { useEffect, useRef, useCallback } from 'react';
import {
  View, Text, ScrollView, StyleSheet, TouchableOpacity,
  ActivityIndicator, Alert, RefreshControl, StatusBar,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { GBand, GBandEmitter, EVENTS } from '../native/GBandModule';
import { useHealthStore } from '../store/healthStore';
import { colors, fonts, radius, spacing } from '../theme/theme';

// ── Helpers ───────────────────────────────────────────────────────────────────
const fmt = (val: number, fallback = '--') =>
  val > 0 ? String(val) : fallback;

const fmtFloat = (val: number, digits = 1, fallback = '--') =>
  val > 0 ? val.toFixed(digits) : fallback;

function stressLabel(s: number) {
  if (s < 0)  return { label: '--',       color: colors.textMuted };
  if (s < 30) return { label: 'LOW',      color: colors.green };
  if (s < 60) return { label: 'MODERATE', color: colors.yellow };
  return       { label: 'HIGH',           color: colors.red };
}

function sleepScoreLabel(q: number) {
  if (q <= 0)  return { label: '--',        color: colors.textMuted };
  if (q >= 80) return { label: 'GREAT',     color: colors.green };
  if (q >= 60) return { label: 'FAIR',      color: colors.yellow };
  return        { label: 'POOR',            color: colors.red };
}

// ── Metric Card ───────────────────────────────────────────────────────────────
interface MetricCardProps {
  label:       string;
  value:       string;
  unit?:       string;
  sub?:        string;
  accent?:     string;
  measuring?:  boolean;
  onMeasure?:  () => void;
  measureLabel?:string;
  supported?:  boolean | null; // null = unknown (device not connected)
}

function MetricCard({
  label, value, unit, sub, accent = colors.accent,
  measuring, onMeasure, measureLabel = 'MEASURE',
  supported = null,
}: MetricCardProps) {
  return (
    <View style={[styles.card, { borderLeftColor: accent }]}>
      <Text style={styles.cardLabel}>{label}</Text>
      <View style={styles.cardValueRow}>
        {measuring ? (
          <View style={styles.measuringRow}>
            <ActivityIndicator color={accent} size="small" />
            <Text style={[styles.measuringText, { color: accent }]}>  Measuring…</Text>
          </View>
        ) : (
          <>
            <Text style={[styles.cardValue, { color: accent }]}>{value}</Text>
            {unit ? <Text style={styles.cardUnit}>{unit}</Text> : null}
          </>
        )}
      </View>
      {sub ? <Text style={styles.cardSub}>{sub}</Text> : null}
      {onMeasure && supported !== false && (
        <TouchableOpacity
          style={[styles.measureBtn, measuring && styles.measureBtnActive, { borderColor: accent }]}
          onPress={onMeasure}
          activeOpacity={0.75}>
          <Text style={[styles.measureBtnText, { color: measuring ? colors.textPrimary : accent }]}>
            {measuring ? 'STOP' : measureLabel}
          </Text>
        </TouchableOpacity>
      )}
      {supported === false && (
        <Text style={styles.notSupported}>Not supported by this device</Text>
      )}
    </View>
  );
}

// ── Section header ────────────────────────────────────────────────────────────
function SectionHeader({ title }: { title: string }) {
  return <Text style={styles.sectionHeader}>{title}</Text>;
}

// ── Main Screen ───────────────────────────────────────────────────────────────
export default function HomeScreen() {
  const store = useHealthStore();
  const [refreshing, setRefreshing] = React.useState(false);
  const subRefs = useRef<any[]>([]);

  // Attach event listeners when connected
  useEffect(() => {
    if (!GBandEmitter) return;

    const subs = [
      GBandEmitter.addListener(EVENTS.HEART_RATE, (e) => store.setHeartRate(e.bpm)),
      GBandEmitter.addListener(EVENTS.SPO2,       (e) => store.setSpo2(e.value)),
      GBandEmitter.addListener(EVENTS.BP,         (e) => store.setBP(e.systolic, e.diastolic)),
      GBandEmitter.addListener(EVENTS.TEMPERATURE,(e) => store.setTemperature(e.celsius)),
      GBandEmitter.addListener(EVENTS.HRV,        (e) => store.setHRV(e.hrv)),
      GBandEmitter.addListener(EVENTS.WELLNESS_RESULT, (e) => {
        if (e.stress  !== undefined) store.setWellness(e.stress, e.fatigue ?? -1);
        if (e.hrv     !== undefined) store.setHRV(e.hrv);
        if (e.heartRate !== undefined) store.setHeartRate(e.heartRate);
        if (e.spo2    !== undefined) store.setSpo2(e.spo2);
        if (e.temp    !== undefined) store.setTemperature(e.temp);
        if (e.bpSystolic !== undefined) store.setBP(e.bpSystolic, e.bpDiastolic);
        store.setMeasuring('measuringWellness', false);
      }),
    ];
    subRefs.current = subs;
    return () => subs.forEach((s) => s.remove());
  }, []);

  const loadData = useCallback(async () => {
    if (!store.isConnected) return;
    try {
      const [steps, sleep, origin] = await Promise.all([
        GBand.readSteps(),
        GBand.readSleep(),
        GBand.readOriginData(),
      ]);
      store.setSteps(steps.steps, steps.distanceKm, steps.caloriesKcal);
      store.setSleep(sleep);
      if (origin.heartRate)      store.setHeartRate(origin.heartRate);
      if (origin.bpSystolic)     store.setBP(origin.bpSystolic, origin.bpDiastolic ?? 0);
      if (origin.respirationRate)store.setRespirationRate(origin.respirationRate);
      if (origin.spo2)           store.setSpo2(origin.spo2);
    } catch (e: any) {
      console.warn('loadData error', e?.message);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [store.isConnected]); // Zustand store setters are stable refs — no need in deps

  // Load on first connect
  useEffect(() => { if (store.isConnected) loadData(); }, [store.isConnected]);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  };

  // ── Heart rate toggle
  const toggleHeart = async () => {
    if (store.measuringHeart) {
      await GBand.stopHeartMeasure();
      store.setMeasuring('measuringHeart', false);
    } else {
      await GBand.startHeartMeasure();
      store.setMeasuring('measuringHeart', true);
    }
  };

  // ── SpO2 toggle
  const toggleSpo2 = async () => {
    if (store.measuringSpo2) {
      await GBand.stopSpo2Measure();
      store.setMeasuring('measuringSpo2', false);
    } else {
      await GBand.startSpo2Measure();
      store.setMeasuring('measuringSpo2', true);
    }
  };

  // ── BP toggle
  const toggleBp = async () => {
    if (store.measuringBp) {
      await GBand.stopBpMeasure();
      store.setMeasuring('measuringBp', false);
    } else {
      await GBand.startBpMeasure();
      store.setMeasuring('measuringBp', true);
    }
  };

  // ── Temp toggle
  const toggleTemp = async () => {
    if (store.measuringTemp) {
      await GBand.stopTempMeasure();
      store.setMeasuring('measuringTemp', false);
    } else {
      await GBand.startTempMeasure();
      store.setMeasuring('measuringTemp', true);
    }
  };

  // ── Wellness (Stress + Fatigue + HRV in one shot)
  const toggleWellness = async () => {
    if (store.measuringWellness) {
      await GBand.stopWellnessCheck();
      store.setMeasuring('measuringWellness', false);
    } else {
      await GBand.startWellnessCheck();
      store.setMeasuring('measuringWellness', true);
    }
  };

  const caps = store.capabilities;
  const stressInfo = stressLabel(store.stress);
  const sleepInfo  = sleepScoreLabel(store.sleep?.sleepQuality ?? 0);

  const sleepSub = store.sleep
    ? `Deep ${store.sleep.deepMinutes}m  ·  Light ${store.sleep.lightMinutes}m  ·  Awoke ${store.sleep.wakeCount}×`
    : undefined;

  const bpValue = store.bpSystolic > 0 && store.bpDiastolic > 0
    ? `${store.bpSystolic}/${store.bpDiastolic}`
    : '--';

  if (!store.isConnected) {
    return (
      <SafeAreaView style={styles.safe}>
        <StatusBar barStyle="light-content" backgroundColor={colors.bg} />
        <View style={styles.disconnectedCenter}>
          <Text style={styles.disconnectedIcon}>◈</Text>
          <Text style={styles.disconnectedTitle}>No Device Connected</Text>
          <Text style={styles.disconnectedSub}>
            Go to the Device tab to connect your G Band
          </Text>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" backgroundColor={colors.bg} />

      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text style={styles.headerGreeting}>Good {getTimeOfDay()},</Text>
          <Text style={styles.headerName}>{store.profile.name}</Text>
        </View>
        <View style={styles.batteryPill}>
          <Text style={styles.batteryText}>
            {store.battery >= 0 ? `⚡ ${store.battery}%` : '⚡ --'}
          </Text>
        </View>
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.accent} />}
        showsVerticalScrollIndicator={false}>

        {/* ── Activity ─────────────────────────────────────────────────── */}
        <SectionHeader title="ACTIVITY" />
        <View style={styles.row}>
          <View style={[styles.card, styles.cardThird, { borderLeftColor: colors.blue }]}>
            <Text style={styles.cardLabel}>STEPS</Text>
            <Text style={[styles.cardValue, { color: colors.blue }]}>{fmt(store.steps)}</Text>
            <Text style={styles.cardSub}>
              Goal: {store.profile.stepGoal.toLocaleString()}
            </Text>
          </View>
          <View style={[styles.card, styles.cardThird, { borderLeftColor: colors.green }]}>
            <Text style={styles.cardLabel}>DISTANCE</Text>
            <Text style={[styles.cardValue, { color: colors.green }]}>
              {fmtFloat(store.distanceKm)}
            </Text>
            <Text style={styles.cardSub}>km</Text>
          </View>
          <View style={[styles.card, styles.cardThird, { borderLeftColor: colors.yellow }]}>
            <Text style={styles.cardLabel}>CALORIES</Text>
            <Text style={[styles.cardValue, { color: colors.yellow }]}>
              {fmt(store.caloriesKcal)}
            </Text>
            <Text style={styles.cardSub}>kcal</Text>
          </View>
        </View>

        {/* ── Heart Rate ───────────────────────────────────────────────── */}
        <SectionHeader title="HEART" />
        <MetricCard
          label="HEART RATE"
          value={fmt(store.heartRate)}
          unit="BPM"
          accent={colors.accent}
          measuring={store.measuringHeart}
          onMeasure={toggleHeart}
        />

        {/* ── Blood Oxygen ─────────────────────────────────────────────── */}
        <SectionHeader title="BLOOD OXYGEN" />
        <MetricCard
          label="SpO₂"
          value={fmt(store.spo2)}
          unit="%"
          accent={colors.blue}
          measuring={store.measuringSpo2}
          onMeasure={toggleSpo2}
          supported={caps ? caps.spo2 : null}
        />

        {/* ── Blood Pressure ───────────────────────────────────────────── */}
        <SectionHeader title="BLOOD PRESSURE" />
        <MetricCard
          label="BLOOD PRESSURE"
          value={bpValue}
          unit={store.bpSystolic > 0 ? 'mmHg' : ''}
          accent={colors.purple}
          measuring={store.measuringBp}
          onMeasure={toggleBp}
          supported={caps ? caps.bp : null}
        />

        {/* ── Body Temperature ─────────────────────────────────────────── */}
        <SectionHeader title="TEMPERATURE" />
        <MetricCard
          label="BODY TEMP"
          value={fmtFloat(store.temperatureC)}
          unit={store.temperatureC > 0 ? '°C' : ''}
          accent={colors.yellow}
          measuring={store.measuringTemp}
          onMeasure={toggleTemp}
          supported={caps ? caps.temp : null}
        />

        {/* ── Respiration ──────────────────────────────────────────────── */}
        <SectionHeader title="RESPIRATORY" />
        <MetricCard
          label="RESP. RATE"
          value={fmt(store.respirationRate)}
          unit={store.respirationRate > 0 ? 'br/min' : ''}
          accent={colors.green}
          supported={caps ? caps.respRate : null}
        />

        {/* ── Wellness (Stress + HRV + Fatigue) ───────────────────────── */}
        <SectionHeader title="WELLNESS CHECK" />
        <View style={[styles.card, { borderLeftColor: colors.accent }]}>
          <Text style={styles.cardLabel}>WELLNESS CHECK</Text>
          <Text style={styles.cardSub}>Measures stress, fatigue, HRV, and more in one scan</Text>
          <View style={styles.wellnessGrid}>
            <View style={styles.wellnessItem}>
              <Text style={styles.wellnessItemLabel}>STRESS</Text>
              <Text style={[styles.wellnessItemValue, { color: stressInfo.color }]}>
                {store.stress >= 0 ? store.stress : '--'}
              </Text>
              <Text style={[styles.wellnessItemTag, { color: stressInfo.color }]}>
                {stressInfo.label}
              </Text>
            </View>
            <View style={styles.wellnessItem}>
              <Text style={styles.wellnessItemLabel}>FATIGUE</Text>
              <Text style={[styles.wellnessItemValue, { color: colors.yellow }]}>
                {store.fatigue >= 0 ? store.fatigue : '--'}
              </Text>
            </View>
            <View style={styles.wellnessItem}>
              <Text style={styles.wellnessItemLabel}>HRV</Text>
              <Text style={[styles.wellnessItemValue, { color: colors.green }]}>
                {fmt(store.hrv)}
              </Text>
              <Text style={styles.wellnessItemTag}>ms</Text>
            </View>
          </View>
          {store.measuringWellness ? (
            <View style={styles.measuringRow}>
              <ActivityIndicator color={colors.accent} size="small" />
              <Text style={[styles.measuringText, { color: colors.accent }]}>  Scanning… keep still</Text>
            </View>
          ) : null}
          <TouchableOpacity
            style={[styles.measureBtn, store.measuringWellness && styles.measureBtnActive,
              { borderColor: colors.accent }]}
            onPress={toggleWellness}
            activeOpacity={0.75}>
            <Text style={[styles.measureBtnText, {
              color: store.measuringWellness ? colors.textPrimary : colors.accent,
            }]}>
              {store.measuringWellness ? 'STOP' : 'START WELLNESS CHECK'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* ── Sleep ────────────────────────────────────────────────────── */}
        <SectionHeader title="SLEEP" />
        <View style={[styles.card, { borderLeftColor: colors.purple }]}>
          <Text style={styles.cardLabel}>LAST NIGHT</Text>
          <View style={styles.cardValueRow}>
            <Text style={[styles.cardValue, { color: colors.purple }]}>
              {store.sleep ? fmtFloat(store.sleep.totalMinutes / 60) : '--'}
            </Text>
            <Text style={styles.cardUnit}>hrs</Text>
          </View>
          {sleepSub ? <Text style={styles.cardSub}>{sleepSub}</Text> : null}
          {store.sleep && store.sleep.sleepQuality > 0 && (
            <View style={styles.sleepQualityRow}>
              <Text style={styles.cardSub}>Quality: </Text>
              <Text style={[styles.cardSub, { color: sleepInfo.color, fontWeight: '700' }]}>
                {sleepInfo.label} ({store.sleep.sleepQuality}/100)
              </Text>
            </View>
          )}
          <Text style={[styles.cardSub, { marginTop: spacing.sm }]}>
            Goal: {store.profile.sleepGoalH}h
          </Text>
        </View>

        <View style={{ height: spacing.xl }} />
      </ScrollView>
    </SafeAreaView>
  );
}

function getTimeOfDay() {
  const h = new Date().getHours();
  if (h < 12) return 'Morning';
  if (h < 17) return 'Afternoon';
  return 'Evening';
}

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  scroll: { flex: 1 },
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
  headerGreeting: { fontSize: fonts.sm, color: colors.textSecondary },
  headerName:     { fontSize: fonts.xl, color: colors.textPrimary, fontWeight: '700' },

  batteryPill: {
    backgroundColor: colors.bgElevated,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.xs,
    borderRadius: radius.full,
    borderWidth: 1,
    borderColor: colors.border,
  },
  batteryText: { fontSize: fonts.sm, color: colors.textSecondary },

  sectionHeader: {
    fontSize: fonts.xs,
    color: colors.textMuted,
    fontWeight: '700',
    letterSpacing: 1.5,
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },

  row: { flexDirection: 'row', gap: spacing.sm },

  card: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
    borderLeftWidth: 3,
    borderLeftColor: colors.accent,
  },
  cardThird: { flex: 1, padding: spacing.sm },

  cardLabel: {
    fontSize: fonts.xs,
    color: colors.textMuted,
    fontWeight: '700',
    letterSpacing: 1,
    marginBottom: spacing.xs,
  },
  cardValueRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 4 },
  cardValue:    { fontSize: fonts.xxl, fontWeight: '800', color: colors.accent },
  cardUnit:     { fontSize: fonts.md, color: colors.textSecondary, marginBottom: 4 },
  cardSub:      { fontSize: fonts.sm, color: colors.textSecondary, marginTop: spacing.xs },

  measuringRow: { flexDirection: 'row', alignItems: 'center', marginVertical: spacing.sm },
  measuringText:{ fontSize: fonts.md, fontWeight: '600' },

  measureBtn: {
    marginTop: spacing.sm,
    borderWidth: 1.5,
    borderRadius: radius.sm,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  measureBtnActive: { backgroundColor: colors.accentDim },
  measureBtnText: {
    fontSize: fonts.sm,
    fontWeight: '700',
    letterSpacing: 1,
  },

  notSupported: {
    fontSize: fonts.xs,
    color: colors.textMuted,
    fontStyle: 'italic',
    marginTop: spacing.xs,
  },

  wellnessGrid: {
    flexDirection: 'row',
    marginVertical: spacing.md,
    gap: spacing.sm,
  },
  wellnessItem:      { flex: 1, alignItems: 'center' },
  wellnessItemLabel: { fontSize: fonts.xs, color: colors.textMuted, letterSpacing: 0.8 },
  wellnessItemValue: { fontSize: fonts.xxl, fontWeight: '800', color: colors.textPrimary },
  wellnessItemTag:   { fontSize: fonts.xs, color: colors.textSecondary },

  sleepQualityRow: { flexDirection: 'row', alignItems: 'center' },

  disconnectedCenter: {
    flex: 1, alignItems: 'center', justifyContent: 'center', padding: spacing.xl,
  },
  disconnectedIcon:  { fontSize: 64, color: colors.textMuted, marginBottom: spacing.md },
  disconnectedTitle: { fontSize: fonts.xl, color: colors.textPrimary, fontWeight: '700', marginBottom: spacing.sm },
  disconnectedSub:   { fontSize: fonts.md, color: colors.textSecondary, textAlign: 'center', lineHeight: 22 },
});
