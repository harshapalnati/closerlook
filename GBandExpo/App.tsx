import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import BottomTabNavigator from './src/navigation/BottomTabNavigator';
import { colors } from './src/theme/theme';

const GBandTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    background: colors.bg,
    card: colors.bgCard,
    text: colors.textPrimary,
    border: colors.border,
    primary: colors.accent,
    notification: colors.accent,
  },
};

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer theme={GBandTheme}>
        <StatusBar style="light" />
        <BottomTabNavigator />
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
