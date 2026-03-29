import React, { useState } from 'react';
import {
  View, Text, ScrollView, StyleSheet, TextInput,
  TouchableOpacity, StatusBar,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useHealthStore } from '../store/healthStore';
import { colors, fonts, radius, spacing } from '../theme/theme';

// ── Input row ─────────────────────────────────────────────────────────────────
function InputRow({
  label, value, onChangeText, keyboardType = 'default', unit,
}: {
  label: string;
  value: string;
  onChangeText: (v: string) => void;
  keyboardType?: 'default' | 'numeric' | 'decimal-pad';
  unit?: string;
}) {
  return (
    <View style={styles.inputRow}>
      <Text style={styles.inputLabel}>{label}</Text>
      <View style={styles.inputWrap}>
        <TextInput
          style={styles.input}
          value={value}
          onChangeText={onChangeText}
          keyboardType={keyboardType}
          placeholderTextColor={colors.textMuted}
          selectionColor={colors.accent}
        />
        {unit && <Text style={styles.inputUnit}>{unit}</Text>}
      </View>
    </View>
  );
}

// ── Gender picker ─────────────────────────────────────────────────────────────
function GenderPicker({
  value, onChange,
}: { value: 'male' | 'female'; onChange: (v: 'male' | 'female') => void }) {
  return (
    <View style={styles.inputRow}>
      <Text style={styles.inputLabel}>GENDER</Text>
      <View style={styles.genderRow}>
        {(['male', 'female'] as const).map((g) => (
          <TouchableOpacity
            key={g}
            style={[styles.genderBtn, value === g && styles.genderBtnActive]}
            onPress={() => onChange(g)}
            activeOpacity={0.8}>
            <Text style={[styles.genderBtnText, value === g && styles.genderBtnTextActive]}>
              {g === 'male' ? 'Male' : 'Female'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );
}

// ── Section header ────────────────────────────────────────────────────────────
function SectionHeader({ title }: { title: string }) {
  return <Text style={styles.sectionHeader}>{title}</Text>;
}

// ── BMI helper ────────────────────────────────────────────────────────────────
function calcBMI(heightCm: number, weightKg: number): { bmi: string; label: string; color: string } {
  if (heightCm <= 0 || weightKg <= 0) return { bmi: '--', label: '', color: colors.textMuted };
  const h = heightCm / 100;
  const bmi = weightKg / (h * h);
  let label = '', color = colors.green;
  if      (bmi < 18.5) { label = 'Underweight'; color = colors.blue; }
  else if (bmi < 25)   { label = 'Normal';       color = colors.green; }
  else if (bmi < 30)   { label = 'Overweight';   color = colors.yellow; }
  else                 { label = 'Obese';         color = colors.red; }
  return { bmi: bmi.toFixed(1), label, color };
}

// ── Main Screen ───────────────────────────────────────────────────────────────
export default function SettingsScreen() {
  const profile = useHealthStore((s) => s.profile);
  const updateProfile = useHealthStore((s) => s.updateProfile);

  // Local editable copies
  const [name,       setName]       = useState(profile.name);
  const [age,        setAge]        = useState(String(profile.age));
  const [heightCm,   setHeightCm]   = useState(String(profile.heightCm));
  const [weightKg,   setWeightKg]   = useState(String(profile.weightKg));
  const [gender,     setGender]     = useState<'male' | 'female'>(profile.gender);
  const [sleepGoalH, setSleepGoalH] = useState(String(profile.sleepGoalH));
  const [stepGoal,   setStepGoal]   = useState(String(profile.stepGoal));
  const [saved,      setSaved]      = useState(false);

  const bmi = calcBMI(Number(heightCm), Number(weightKg));

  const handleSave = () => {
    updateProfile({
      name:       name.trim() || 'Your Name',
      age:        Number(age)        || 55,
      heightCm:   Number(heightCm)   || 170,
      weightKg:   Number(weightKg)   || 70,
      gender,
      sleepGoalH: Number(sleepGoalH) || 8,
      stepGoal:   Number(stepGoal)   || 8000,
    });
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" backgroundColor={colors.bg} />

      <View style={styles.header}>
        <Text style={styles.headerTitle}>SETTINGS</Text>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">

        {/* ── Profile ────────────────────────────────────────────────── */}
        <SectionHeader title="PROFILE" />
        <View style={styles.card}>
          <InputRow label="NAME"   value={name}   onChangeText={setName} />
          <InputRow label="AGE"    value={age}    onChangeText={setAge}    keyboardType="numeric" unit="yrs" />
          <GenderPicker value={gender} onChange={setGender} />
        </View>

        {/* ── Body parameters ────────────────────────────────────────── */}
        <SectionHeader title="BODY" />
        <View style={styles.card}>
          <InputRow label="HEIGHT" value={heightCm} onChangeText={setHeightCm} keyboardType="numeric" unit="cm" />
          <InputRow label="WEIGHT" value={weightKg} onChangeText={setWeightKg} keyboardType="decimal-pad" unit="kg" />

          {/* BMI preview */}
          <View style={styles.bmiRow}>
            <Text style={styles.bmiLabel}>BMI (estimated)</Text>
            <View style={styles.bmiRight}>
              <Text style={[styles.bmiValue, { color: bmi.color }]}>{bmi.bmi}</Text>
              {bmi.label ? (
                <Text style={[styles.bmiTag, { color: bmi.color }]}>{bmi.label}</Text>
              ) : null}
            </View>
          </View>
        </View>

        {/* ── Goals ──────────────────────────────────────────────────── */}
        <SectionHeader title="DAILY GOALS" />
        <View style={styles.card}>
          <InputRow
            label="SLEEP GOAL"
            value={sleepGoalH}
            onChangeText={setSleepGoalH}
            keyboardType="numeric"
            unit="hrs"
          />
          <InputRow
            label="STEP GOAL"
            value={stepGoal}
            onChangeText={setStepGoal}
            keyboardType="numeric"
            unit="steps"
          />
        </View>

        {/* ── Health tips for 40-80 year olds ───────────────────────── */}
        <SectionHeader title="HEALTH TIPS" />
        <View style={styles.tipsCard}>
          <TipRow icon="❤️" text="Aim for 7–9 hours of sleep per night." />
          <TipRow icon="🚶" text="Walk at least 6,000–8,000 steps daily." />
          <TipRow icon="💧" text="Stay hydrated — drink 6–8 glasses of water." />
          <TipRow icon="🩺" text="Resting heart rate of 60–100 bpm is normal." />
          <TipRow icon="🫀" text="SpO₂ should be 95% or above. Below 92% — call a doctor." />
          <TipRow icon="🧘" text="High stress over time affects heart and sleep. Check weekly." />
          <TipRow icon="🌡️" text="Normal body temp: 36.1°C – 37.2°C." />
        </View>

        {/* ── Save button ────────────────────────────────────────────── */}
        <TouchableOpacity
          style={[styles.saveBtn, saved && styles.saveBtnDone]}
          onPress={handleSave}
          activeOpacity={0.8}>
          <Text style={styles.saveBtnText}>
            {saved ? '✓  SAVED' : 'SAVE PROFILE'}
          </Text>
        </TouchableOpacity>

        <View style={{ height: spacing.xl }} />
      </ScrollView>
    </SafeAreaView>
  );
}

function TipRow({ icon, text }: { icon: string; text: string }) {
  return (
    <View style={styles.tipRow}>
      <Text style={styles.tipIcon}>{icon}</Text>
      <Text style={styles.tipText}>{text}</Text>
    </View>
  );
}

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = StyleSheet.create({
  safe:          { flex: 1, backgroundColor: colors.bg },
  scroll:        { flex: 1 },
  scrollContent: { padding: spacing.md },

  header: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  headerTitle: { fontSize: fonts.lg, color: colors.textPrimary, fontWeight: '800', letterSpacing: 2 },

  sectionHeader: {
    fontSize: fonts.xs,
    color: colors.textMuted,
    fontWeight: '700',
    letterSpacing: 1.5,
    marginTop: spacing.lg,
    marginBottom: spacing.sm,
  },

  card: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },

  inputRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  inputLabel: { fontSize: fonts.sm, color: colors.textSecondary, fontWeight: '600', letterSpacing: 0.5 },
  inputWrap:  { flexDirection: 'row', alignItems: 'center', gap: 4 },
  input: {
    fontSize: fonts.md,
    color: colors.textPrimary,
    textAlign: 'right',
    minWidth: 80,
    paddingVertical: 2,
  },
  inputUnit: { fontSize: fonts.sm, color: colors.textMuted },

  genderRow: { flexDirection: 'row', gap: spacing.sm },
  genderBtn: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderRadius: radius.full,
    borderWidth: 1.5,
    borderColor: colors.border,
  },
  genderBtnActive:    { borderColor: colors.accent, backgroundColor: colors.accentDim },
  genderBtnText:      { fontSize: fonts.sm, color: colors.textSecondary, fontWeight: '600' },
  genderBtnTextActive:{ color: colors.textPrimary },

  bmiRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: spacing.sm,
    marginTop: spacing.xs,
    backgroundColor: colors.bgElevated,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.sm,
  },
  bmiLabel: { fontSize: fonts.sm, color: colors.textSecondary },
  bmiRight: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  bmiValue: { fontSize: fonts.xl, fontWeight: '800' },
  bmiTag:   { fontSize: fonts.sm, fontWeight: '600' },

  tipsCard: {
    backgroundColor: colors.bgCard,
    borderRadius: radius.md,
    padding: spacing.md,
    gap: spacing.sm,
  },
  tipRow:  { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm },
  tipIcon: { fontSize: fonts.md, width: 24 },
  tipText: { flex: 1, fontSize: fonts.sm, color: colors.textSecondary, lineHeight: 20 },

  saveBtn: {
    marginTop: spacing.lg,
    backgroundColor: colors.accent,
    borderRadius: radius.sm,
    paddingVertical: spacing.md,
    alignItems: 'center',
  },
  saveBtnDone:  { backgroundColor: colors.green },
  saveBtnText:  { fontSize: fonts.md, color: colors.textPrimary, fontWeight: '700', letterSpacing: 1 },
});
