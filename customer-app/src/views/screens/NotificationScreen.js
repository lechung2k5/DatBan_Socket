import React, { useState, useEffect } from 'react';
import { 
  ScrollView, 
  View, 
  Text, 
  SafeAreaView, 
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import SocketService from '../../services/SocketService';
import ApiService from '../../services/ApiService';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledScrollView = styled(ScrollView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTouchableOpacity = styled(TouchableOpacity);

const NotificationScreen = () => {
  const [activeTab, setActiveTab] = useState('Hệ thống');
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetchNotifications = async () => {
    try {
      const phone = SocketService.userProfile?.soDT;
      if (!phone) {
          setLoading(false);
          return;
      }
      const res = await ApiService.getNotifications(phone);
      if (res.statusCode === 200) {
        setNotifications(res.data);
      }
    } catch (err) {
      console.error('[NotificationScreen] Error fetching:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchNotifications();

    // Lắng nghe thông báo Real-time
    const unsubscribe = SocketService.addListener((response) => {
      if (response.CommandType === 'NEW_NOTIFICATION') {
        // Thêm vào đầu danh sách
        setNotifications(prev => [response.data, ...prev]);
      }
    });

    return () => unsubscribe();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchNotifications();
  };

  const getIcon = (type) => {
    switch (type) {
      case 'BOOKING': return 'calendar-check';
      case 'PROMO': return 'ticket-percent';
      default: return 'bell-outline';
    }
  };

  const getColor = (type) => {
    switch (type) {
      case 'BOOKING': return '#800000';
      case 'PROMO': return '#D35400';
      default: return '#2C3E50';
    }
  };

  const formatTime = (timeStr) => {
    try {
        const date = new Date(timeStr);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' ' + date.toLocaleDateString();
    } catch (e) {
        return timeStr;
    }
  };

  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <StyledView className="px-6 pt-4 flex-1">
        
        {/* Header */}
        <StyledView className="flex-row justify-between items-center mb-6">
          <StyledView className="flex-row items-center">
             <StyledView className="w-12 h-12 rounded-full bg-red-50 justify-center items-center overflow-hidden border-2 border-red-900">
                <Ionicons name="person" size={24} color="#800000" />
             </StyledView>
             <StyledView className="ml-3">
                <StyledText className="text-xl font-bold text-red-900">
                  {SocketService.userProfile?.tenKH || 'Khách hàng'}
                </StyledText>
             </StyledView>
          </StyledView>
          <StyledTouchableOpacity onPress={onRefresh} className="p-2">
             <Ionicons name="reload-outline" size={24} color={COLORS.text} />
          </StyledTouchableOpacity>
        </StyledView>

        {/* Title */}
        <StyledText className="text-4xl font-bold mb-8 text-gray-800">Thông báo</StyledText>

        {/* Tabs */}
        <StyledView className="flex-row bg-gray-100 p-1.5 rounded-2xl mb-8">
           <StyledTouchableOpacity 
            onPress={() => setActiveTab('Hệ thống')}
            className={`flex-1 py-3 rounded-xl items-center ${activeTab === 'Hệ thống' ? 'bg-red-900 shadow-md' : ''}`}
           >
              <StyledText className={`font-bold text-base ${activeTab === 'Hệ thống' ? 'text-white' : 'text-gray-500'}`}>Hệ thống</StyledText>
           </StyledTouchableOpacity>
           <StyledTouchableOpacity 
            onPress={() => setActiveTab('Ưu đãi')}
            className={`flex-1 py-3 rounded-xl items-center ${activeTab === 'Ưu đãi' ? 'bg-red-900 shadow-md' : ''}`}
           >
              <StyledText className={`font-bold text-base ${activeTab === 'Ưu đãi' ? 'text-white' : 'text-gray-500'}`}>Ưu đãi</StyledText>
           </StyledTouchableOpacity>
        </StyledView>

        {/* List */}
        {loading ? (
            <StyledView className="flex-1 justify-center items-center">
                <ActivityIndicator size="large" color="#800000" />
            </StyledView>
        ) : (
            <StyledScrollView 
                showsVerticalScrollIndicator={false} 
                className="flex-1"
                refreshControl={
                    <RefreshControl refreshing={refreshing} onRefresh={onRefresh} colors={["#800000"]} />
                }
            >
                {notifications.length > 0 ? (
                    notifications
                    .filter(n => activeTab === 'Hệ thống' ? n.type !== 'PROMO' : n.type === 'PROMO')
                    .map((notif, index) => (
                    <StyledTouchableOpacity 
                        key={notif.notificationId || index}
                        className="bg-white rounded-3xl p-5 mb-4 border border-gray-100 flex-row items-center shadow-sm"
                    >
                        <StyledView className="w-14 h-14 rounded-full justify-center items-center mr-4" style={{ backgroundColor: getColor(notif.type) + '15' }}>
                            <MaterialCommunityIcons name={getIcon(notif.type)} size={28} color={getColor(notif.type)} />
                        </StyledView>
                        <StyledView className="flex-1">
                            <StyledView className="flex-row justify-between items-start">
                                <StyledText className="text-lg font-bold text-gray-800 w-[90%]" numberOfLines={1}>{notif.title}</StyledText>
                                {!notif.isRead && <StyledView className="w-2.5 h-2.5 rounded-full bg-red-600 mt-2" />}
                            </StyledView>
                            <StyledText className="text-gray-500 text-sm mt-1 leading-5" numberOfLines={2}>{notif.message}</StyledText>
                            <StyledText className="text-gray-400 text-xs mt-2 font-semibold">{formatTime(notif.createdAt)}</StyledText>
                        </StyledView>
                    </StyledTouchableOpacity>
                    ))
                ) : (
                    <StyledView className="items-center justify-center py-20">
                        <Ionicons name="notifications-off-outline" size={64} color="#CCC" />
                        <StyledText className="text-gray-400 mt-4 text-lg">Không có thông báo nào</StyledText>
                    </StyledView>
                )}
                <StyledView className="h-20" />
            </StyledScrollView>
        )}

      </StyledView>
    </StyledSafeAreaView>
  );
};

export default NotificationScreen;
