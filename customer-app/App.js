import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { COLORS } from './src/theme/colors';

// Screens
import HomeScreen from './src/views/screens/HomeScreen';
import BookingScreen from './src/views/screens/BookingScreen';
import NotificationScreen from './src/views/screens/NotificationScreen';
import ProfileScreen from './src/views/screens/ProfileScreen';
import LoginScreen from './src/views/screens/LoginScreen';
import RegisterScreen from './src/views/screens/RegisterScreen';
import OtpScreen from './src/views/screens/OtpScreen';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

/**
 * Main Tab Navigator - Giao diện chính sau khi đăng nhập
 */
function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName;
          if (route.name === 'Trang chủ') iconName = focused ? 'home' : 'home-outline';
          else if (route.name === 'Đặt bàn') iconName = focused ? 'calendar' : 'calendar-outline';
          else if (route.name === 'Thông báo') iconName = focused ? 'notifications' : 'notifications-outline';
          else if (route.name === 'Cá nhân') iconName = focused ? 'person' : 'person-outline';
          return <Ionicons name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: COLORS.primary,
        tabBarInactiveTintColor: 'gray',
        headerShown: false,
        tabBarStyle: {
          paddingBottom: 10,
          paddingTop: 10,
          height: 70,
          borderTopLeftRadius: 30,
          borderTopRightRadius: 30,
          position: 'absolute',
          borderTopWidth: 0,
          elevation: 20,
          shadowColor: '#000',
          shadowOffset: { width: 0, height: -10 },
          shadowOpacity: 0.1,
          shadowRadius: 10,
        }
      })}
    >
      <Tab.Screen name="Trang chủ" component={HomeScreen} />
      <Tab.Screen name="Đặt bàn" component={BookingScreen} />
      <Tab.Screen name="Thông báo" component={NotificationScreen} />
      <Tab.Screen name="Cá nhân" component={ProfileScreen} />
    </Tab.Navigator>
  );
}

import { useEffect, useState } from 'react';
import SocketService from './src/services/SocketService';
import { View, ActivityIndicator } from 'react-native';

/**
 * Root Stack Navigator - Quản lý Auth và App
 */
export default function App() {
  const [isReady, setIsReady] = useState(false);
  useEffect(() => {
    const initApp = async () => {
      await SocketService.loadToken();
      
      // Khởi chạy kết nối Socket
      SocketService.connect();
      
      // Đăng ký lắng nghe thông báo toàn cục
      SocketService.addListener((response) => {
        if (response.CommandType === 'NEW_NOTIFICATION') {
          const { title, message } = response.data;
          // Hiển thị thông báo nhanh (Toast hoặc Alert)
          // Ở đây dùng Alert cơ bản của React Native
          const { Alert } = require('react-native');
          Alert.alert(title, message);
        }
      });

      setIsReady(true);
    };
    initApp();
  }, []);

  if (!isReady) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: 'white' }}>
        <ActivityIndicator size="large" color={COLORS.primary} />
      </View>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator 
        screenOptions={{ headerShown: false }} 
        initialRouteName={SocketService.token ? "Main" : "Login"}
      >
        {/* Auth Screens */}
        <Stack.Screen name="Login" component={LoginScreen} />
        <Stack.Screen name="Register" component={RegisterScreen} />
        <Stack.Screen name="Otp" component={OtpScreen} />
        
        {/* Main App */}
        <Stack.Screen name="Main" component={MainTabs} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
