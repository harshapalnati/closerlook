import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Platform } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { colors, fonts, spacing } from '../theme/theme';
import HomeScreen from '../screens/HomeScreen';
import DeviceScreen from '../screens/DeviceScreen';
import SettingsScreen from '../screens/SettingsScreen';
import { useHealthStore } from '../store/healthStore';

const Tab = createBottomTabNavigator();

// ── Simple icon components (no external icon dep needed) ──────────────────────
const HomeIcon = ({ focused }: { focused: boolean }) => (
  <View style={[styles.iconWrap, focused && styles.iconWrapActive]}>
    <Text style={[styles.iconText, focused && styles.iconTextActive]}>⊙</Text>
  </View>
);
const DeviceIcon = ({ focused }: { focused: boolean }) => (
  <View style={[styles.iconWrap, focused && styles.iconWrapActive]}>
    <Text style={[styles.iconText, focused && styles.iconTextActive]}>◈</Text>
  </View>
);
const SettingsIcon = ({ focused }: { focused: boolean }) => (
  <View style={[styles.iconWrap, focused && styles.iconWrapActive]}>
    <Text style={[styles.iconText, focused && styles.iconTextActive]}>⚙</Text>
  </View>
);

// ── Custom tab bar ────────────────────────────────────────────────────────────
function GBandTabBar({ state, descriptors, navigation }: any) {
  const isConnected = useHealthStore((s) => s.isConnected);

  return (
    <View style={styles.tabBar}>
      {state.routes.map((route: any, index: number) => {
        const { options } = descriptors[route.key];
        const focused = state.index === index;

        const labels: Record<string, string> = {
          Home: 'HOME',
          Device: 'DEVICE',
          Settings: 'SETTINGS',
        };

        const icons: Record<string, React.ReactNode> = {
          Home:     <HomeIcon focused={focused} />,
          Device:   <DeviceIcon focused={focused} />,
          Settings: <SettingsIcon focused={focused} />,
        };

        return (
          <TouchableOpacity
            key={route.key}
            style={styles.tabItem}
            activeOpacity={0.7}
            onPress={() => navigation.navigate(route.name)}>
            {icons[route.name]}
            <Text style={[styles.tabLabel, focused && styles.tabLabelActive]}>
              {labels[route.name]}
            </Text>
            {route.name === 'Device' && (
              <View style={[styles.statusDot, { backgroundColor: isConnected ? colors.green : colors.red }]} />
            )}
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

export default function BottomTabNavigator() {
  return (
    <Tab.Navigator
      tabBar={(props) => <GBandTabBar {...props} />}
      screenOptions={{ headerShown: false }}>
      <Tab.Screen name="Home"     component={HomeScreen} />
      <Tab.Screen name="Device"   component={DeviceScreen} />
      <Tab.Screen name="Settings" component={SettingsScreen} />
    </Tab.Navigator>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    flexDirection: 'row',
    backgroundColor: colors.bgCard,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingBottom: Platform.OS === 'ios' ? 20 : 8,
    paddingTop: 8,
  },
  tabItem: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  iconWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconWrapActive: {
    backgroundColor: colors.accentDim,
  },
  iconText: {
    fontSize: 18,
    color: colors.textMuted,
  },
  iconTextActive: {
    color: colors.accent,
  },
  tabLabel: {
    fontSize: fonts.xs,
    color: colors.textMuted,
    marginTop: 2,
    fontWeight: '600',
    letterSpacing: 0.8,
  },
  tabLabelActive: {
    color: colors.accent,
  },
  statusDot: {
    position: 'absolute',
    top: 4,
    right: '22%',
    width: 7,
    height: 7,
    borderRadius: 4,
  },
});
