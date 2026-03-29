// Web stub for react-native-safe-area-context
import React from 'react';
import { View } from 'react-native';

export const SafeAreaProvider = ({ children }) => React.createElement(View, { style: { flex: 1 } }, children);
export const SafeAreaView = ({ children, style }) => React.createElement(View, { style: [{ flex: 1 }, style] }, children);
export const useSafeAreaInsets = () => ({ top: 0, bottom: 0, left: 0, right: 0 });
export const SafeAreaInsetsContext = React.createContext({ top: 0, bottom: 0, left: 0, right: 0 });
export default { SafeAreaProvider, SafeAreaView, useSafeAreaInsets };
