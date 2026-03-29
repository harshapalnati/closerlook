import React from 'react';
import { DarkTheme, NavigationContainer } from '@react-navigation/native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import BottomTabNavigator from './src/navigation/BottomTabNavigator';
import { colors } from './src/theme/theme';

const GBandTheme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary:      colors.accent,
    background:   colors.bg,
    card:         colors.bgCard,
    text:         colors.textPrimary,
    border:       colors.border,
    notification: colors.accent,
  },
};

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer theme={GBandTheme}>
        <BottomTabNavigator />
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
