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
import BookingDetailScreen from './src/views/screens/BookingDetailScreen';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

/**
 * Main Tab Navigator - Giao diện chính sau khi đăng nhập
 */
function MainTabs({ unreadCount, setUnreadCount }) {
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
      <Tab.Screen 
        name="Thông báo" 
        options={{ 
            tabBarBadge: unreadCount > 0 ? unreadCount : null,
            tabBarBadgeStyle: { backgroundColor: '#FF0000', color: 'white', fontSize: 10 }
        }}
      >
        {props => <NotificationScreen {...props} setUnreadCount={setUnreadCount} />}
      </Tab.Screen>
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
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    const initApp = async () => {
      await SocketService.loadToken();
      SocketService.connect();

      // Lấy số lượng chưa đọc ban đầu
      const profile = SocketService.userProfile;
      if (profile && profile.soDT) {
        try {
            const res = await ApiService.getNotifications(profile.soDT);
            if (res.statusCode === 200) {
                setUnreadCount(res.data.filter(n => !n.isRead).length);
            }
        } catch (e) {}
      }
      
      // Đăng ký lắng nghe thông báo toàn cục
      SocketService.addListener((response) => {
        if (response.CommandType === 'NEW_NOTIFICATION') {
          setUnreadCount(prev => prev + 1);
          const { title, message, type } = response.data;
          const { Alert } = require('react-native');
          
          // Tìm mã hóa đơn trong tin nhắn
          const match = message.match(/(HD\d+)/);
          const maHD = match ? match[0] : null;

          if (maHD && (type === 'BOOKING' || type === 'UPDATE' || type === 'SYSTEM')) {
            Alert.alert(
              title, 
              message,
              [
                { text: 'Đóng', style: 'cancel' },
                { text: 'Xem ngay', onPress: () => {
                   Alert.alert('Thông báo', 'Vui lòng vào tab Thông báo để xem chi tiết đơn ' + maHD);
                }}
              ]
            );
          } else {
            Alert.alert(title, message);
          }
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
        <Stack.Screen name="Main">
          {props => <MainTabs {...props} unreadCount={unreadCount} setUnreadCount={setUnreadCount} />}
        </Stack.Screen>
        <Stack.Screen name="BookingDetail" component={BookingDetailScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
