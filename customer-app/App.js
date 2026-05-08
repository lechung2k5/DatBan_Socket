import React, { useEffect, useState } from 'react';
import { View, ActivityIndicator, Alert } from 'react-native';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { COLORS } from './src/theme/colors';

// Services
import ApiService from './src/services/ApiService';
import SocketService from './src/services/SocketService';

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
            if (res && res.statusCode === 200) {
                setUnreadCount(res.data.filter(n => !n.isRead).length);
            }
        } catch (e) {
            console.log('[Init] Error fetching notifications:', e);
        }
      }
      
      // Đăng ký lắng nghe thông báo toàn cục qua Socket (nếu kết nối được)
      SocketService.addListener((response) => {
        if (response.CommandType === 'NEW_NOTIFICATION') {
          setUnreadCount(prev => prev + 1);
          const eventData = response.data || response; // Hỗ trợ cả 2 định dạng
          const { title, message, type } = eventData;
          
          // Hiển thị Alert khi có thông báo mới
          Alert.alert(title || "Thông báo", message || "Bạn có thông báo mới");
        }
      });

      setIsReady(true);
    };
    initApp();

    // 🔥 CƠ CHẾ REAL-TIME (POLLING): Tự động kiểm tra database mỗi 10 giây
    const pollingInterval = setInterval(async () => {
      const profile = SocketService.userProfile;
      if (profile && profile.soDT) {
        try {
          // Sử dụng ApiService trực tiếp (đã import ở top-level)
          const res = await ApiService.getNotifications(profile.soDT);
          if (res && res.statusCode === 200) {
            const newNotifications = res.data || [];
            const currentUnread = newNotifications.filter(n => !n.isRead).length;
            
            setUnreadCount(prev => {
                if (currentUnread > prev) {
                    console.log('[Polling] Phát hiện thông báo mới!');
                }
                return currentUnread;
            });
          }
        } catch (e) {
          console.log('[Polling] Lỗi khi kiểm tra thông báo:', e.message);
        }
      }
    }, 10000); // 10 giây một lần

    return () => {
        clearInterval(pollingInterval);
    };
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
