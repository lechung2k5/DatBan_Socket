import React, { useState, useEffect, useCallback } from 'react';
import { 
  ScrollView, 
  View, 
  Text, 
  SafeAreaView, 
  TouchableOpacity,
  Image,
  Modal,
  FlatList,
  ActivityIndicator,
  StatusBar
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import SocketService from '../../services/SocketService';
import ApiService from '../../services/ApiService';
import { useNavigation } from '@react-navigation/native';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledScrollView = styled(ScrollView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTouchableOpacity = styled(TouchableOpacity);

const ProfileScreen = () => {
  const navigation = useNavigation();
  const [profile, setProfile] = useState(SocketService.userProfile || {});
  const [showHistory, setShowHistory] = useState(false);
  const [invoices, setInvoices] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  useEffect(() => {
    // 🔥 REAL-TIME: Lắng nghe cập nhật profile (điểm tích lũy, hạng thành viên)
    const socketHandler = (event) => {
        // Nếu có sự kiện liên quan đến khách hàng hoặc thanh toán, cập nhật profile
        if (event.CommandType === 'UPDATE_CUSTOMER' && event.affectedId === profile.soDT) {
            refreshProfile();
        }
        if (event.CommandType === 'CHECK_OUT' && event.params?.customerPhone === profile.soDT) {
            refreshProfile();
        }
    };

    SocketService.addListener(socketHandler);
    return () => SocketService.removeListener(socketHandler);
  }, [profile.soDT]);

  const refreshProfile = async () => {
      console.log('[Profile] Refreshing profile data...');
      try {
          const res = await ApiService.getUserProfile(profile.soDT || profile.maKH);
          if (res.statusCode === 200) {
              const newProfile = res.data;
              setProfile(newProfile);
              // Cập nhật lại trong SocketService để các màn hình khác cũng có dữ liệu mới
              SocketService.userProfile = newProfile;
          }
      } catch (err) {
          console.error('[Profile] Refresh failed:', err);
      }
  };

  const handleLogout = async () => {
    await SocketService.setToken(null, null);
    navigation.reset({
      index: 0,
      routes: [{ name: 'Login' }],
    });
  };

  const fetchInvoiceHistory = async () => {
    try {
      setLoadingHistory(true);
      setShowHistory(true);
      const res = await ApiService.getInvoicesByCustomer(profile.soDT || profile.maKH);
      if (res.statusCode === 200) {
        // 🔥 LỌC: Không hiển thị hóa đơn đã hủy
        const activeInvoices = (res.data || []).filter(hd => hd.trangThai !== 'DaHuy' && hd.trangThai !== 'Cancelled');
        
        // Sắp xếp hóa đơn mới nhất lên đầu
        const sorted = activeInvoices.sort((a, b) => {
            const dateA = new Date(a.gioVao || a.ngayLap);
            const dateB = new Date(b.gioVao || b.ngayLap);
            return dateB - dateA;
        });
        setInvoices(sorted);
      }
    } catch (err) {
      console.error('Error fetching invoices:', err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const getStatusColor = (status) => {
    switch(status) {
        case 'ChoXacNhan': return '#D35400';
        case 'Dat': return '#27AE60';
        case 'DangSuDung': return '#2980B9';
        case 'DaThanhToan': return '#2C3E50';
        case 'DaHuy': return '#C0392B';
        default: return '#95A5A6';
    }
  };

  const renderInvoiceItem = ({ item }) => (
    <StyledTouchableOpacity 
        onPress={() => {
            console.log('[Profile] Navigating to detail of invoice:', item.maHD);
            setShowHistory(false);
            navigation.navigate('BookingDetail', { maHD: item.maHD });
        }}
        className="bg-gray-50 mb-4 p-5 rounded-3xl border border-gray-100 flex-row justify-between items-center"
    >
        <StyledView className="flex-1">
            <StyledView className="flex-row items-center mb-1">
                <StyledText className="font-black text-gray-800 text-lg mr-2">{item.maHD || 'N/A'}</StyledText>
                <StyledView className="px-2 py-0.5 rounded-md" style={{ backgroundColor: getStatusColor(item.trangThai) + '15' }}>
                    <StyledText className="text-[8px] font-bold" style={{ color: getStatusColor(item.trangThai) }}>
                        {(item.trangThai || 'unknown').toUpperCase()}
                    </StyledText>
                </StyledView>
            </StyledView>
            <StyledText className="text-gray-400 text-[10px]">
                {item.gioVao && typeof item.gioVao === 'string' ? item.gioVao.split('T')[0].split('-').reverse().join('/') : ''} • Bàn {item.maBan || 'N/A'}
            </StyledText>
        </StyledView>
        <StyledView className="items-end">
            <StyledText className="font-black text-red-900 text-base">
                {(item.tongTienThanhToan || 0).toLocaleString()}đ
            </StyledText>
            <Ionicons name="chevron-forward" size={16} color="#CCC" />
        </StyledView>
    </StyledTouchableOpacity>
  );

  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <StatusBar barStyle="dark-content" />
      <StyledScrollView showsVerticalScrollIndicator={false} className="px-6 pt-6">
        
        {/* User Info Header */}
        <StyledView className="flex-row items-center mb-8">
            <StyledView className="w-20 h-20 rounded-[30px] bg-red-50 items-center justify-center border-4 border-white shadow-sm">
                <Ionicons name="person" size={40} color="#800000" />
            </StyledView>
            <StyledView className="ml-5">
                <StyledText className="text-2xl font-black text-gray-800">{profile.tenKH || 'Khách hàng'}</StyledText>
                <StyledText className="text-gray-400 font-bold">{profile.soDT || 'Chưa cập nhật SĐT'}</StyledText>
            </StyledView>
        </StyledView>

        {/* Loyalty Points Card */}
        <StyledView className="bg-red-900 rounded-[40px] p-8 mb-8 shadow-2xl shadow-red-900/40 relative overflow-hidden">
            <StyledView className="flex-row justify-between items-start z-10">
                <StyledView>
                    <StyledText className="text-white/70 text-[10px] font-black uppercase tracking-widest mb-2">Hạng thành viên</StyledText>
                    <StyledView className="bg-white/20 self-start px-4 py-1.5 rounded-full flex-row items-center border border-white/20">
                        <MaterialCommunityIcons name="crown" size={16} color="#F1C40F" />
                        <StyledText className="text-xs font-black text-white ml-2">
                            {profile.thanhVien?.toUpperCase() || 'MEMBER'}
                        </StyledText>
                    </StyledView>
                </StyledView>
                <StyledView className="items-end">
                    <StyledText className="text-white/70 text-[10px] font-black uppercase tracking-widest mb-1">Điểm tích lũy</StyledText>
                    <StyledText className="text-white text-5xl font-black italic">
                        {profile.diemTichLuy?.toLocaleString() || '0'}
                    </StyledText>
                </StyledView>
            </StyledView>

            <StyledView className="mt-12 z-10">
                <StyledView className="flex-row justify-between items-end mb-2">
                    <StyledText className="text-white/80 text-[10px] font-bold">Tiến trình nâng hạng</StyledText>
                    <StyledText className="text-white text-[10px] font-bold">{(profile.diemTichLuy || 0)} / 1000</StyledText>
                </StyledView>
                <StyledView className="w-full h-2.5 bg-black/20 rounded-full overflow-hidden">
                    <StyledView 
                        className="h-full bg-orange-400 rounded-full" 
                        style={{ width: `${Math.min(((profile.diemTichLuy || 0) / 1000) * 100, 100)}%` }} 
                    />
                </StyledView>
            </StyledView>

            {/* Decorative circles */}
            <StyledView className="absolute -right-10 -bottom-10 w-48 h-48 rounded-full bg-white/5" />
            <StyledView className="absolute -left-5 top-5 w-20 h-20 rounded-full bg-red-400/10" />
        </StyledView>

        {/* Activity Section */}
        <StyledText className="text-gray-400 font-black text-[10px] uppercase tracking-widest mb-4 ml-2">Hoạt động của tôi</StyledText>
        
        <StyledTouchableOpacity 
            onPress={fetchInvoiceHistory}
            className="bg-white rounded-[32px] p-6 mb-8 flex-row items-center justify-between border border-gray-100 shadow-sm"
        >
            <StyledView className="flex-row items-center">
                <StyledView className="w-14 h-14 bg-blue-50 rounded-2xl items-center justify-center mr-4">
                    <Ionicons name="receipt" size={26} color="#2980B9" />
                </StyledView>
                <StyledView>
                    <StyledText className="text-lg font-black text-gray-800">Lịch sử hóa đơn</StyledText>
                    <StyledText className="text-gray-400 text-[10px] font-bold">Xem lại tất cả các đơn hàng đã đặt</StyledText>
                </StyledView>
            </StyledView>
            <Ionicons name="chevron-forward" size={24} color="#EEE" />
        </StyledTouchableOpacity>

        {/* Other Info */}
        <StyledView className="bg-gray-50 rounded-[32px] p-6 mb-8 border border-gray-100">
            <StyledView className="flex-row items-center mb-5">
                <Ionicons name="mail-outline" size={20} color="#666" />
                <StyledText className="ml-3 text-gray-600 font-bold">{profile.email || 'Chưa cập nhật email'}</StyledText>
            </StyledView>
            <StyledView className="flex-row items-center">
                <Ionicons name="location-outline" size={20} color="#666" />
                <StyledText className="ml-3 text-gray-600 font-bold" numberOfLines={1}>
                    {profile.diaChi || 'Chưa cập nhật địa chỉ'}
                </StyledText>
            </StyledView>
        </StyledView>

        {/* Logout */}
        <StyledTouchableOpacity 
          onPress={handleLogout}
          className="flex-row items-center justify-center p-6 rounded-3xl bg-red-50 border border-red-100 mb-20"
        >
           <Ionicons name="log-out" size={22} color="#800000" />
           <StyledText className="text-lg font-black ml-2 text-red-900">Đăng xuất</StyledText>
        </StyledTouchableOpacity>

        <StyledView className="h-10" />
      </StyledScrollView>

      {/* Invoice History Modal */}
      <Modal visible={showHistory} animationType="slide" transparent={true}>
          <StyledView className="flex-1 bg-black/60 justify-end">
              <StyledView className="bg-white rounded-t-[40px] h-[85%] px-6 pt-8">
                  <StyledView className="flex-row justify-between items-center mb-6">
                      <StyledView>
                        <StyledText className="text-2xl font-black text-gray-800">Lịch sử hóa đơn</StyledText>
                        <StyledText className="text-gray-400 text-xs">Danh sách các đơn hàng của bạn</StyledText>
                      </StyledView>
                      <StyledTouchableOpacity 
                        onPress={() => setShowHistory(false)}
                        className="w-10 h-10 bg-gray-100 rounded-full items-center justify-center"
                      >
                          <Ionicons name="close" size={24} color="gray" />
                      </StyledTouchableOpacity>
                  </StyledView>

                  {loadingHistory ? (
                      <StyledView className="flex-1 justify-center items-center">
                          <ActivityIndicator size="large" color="#800000" />
                      </StyledView>
                  ) : (
                      <FlatList 
                        data={invoices}
                        keyExtractor={item => item.maHD}
                        renderItem={renderInvoiceItem}
                        showsVerticalScrollIndicator={false}
                        ListEmptyComponent={
                            <StyledView className="flex-1 justify-center items-center mt-20">
                                <Ionicons name="receipt-outline" size={80} color="#EEE" />
                                <StyledText className="text-gray-400 font-bold mt-4">Bạn chưa có hóa đơn nào</StyledText>
                            </StyledView>
                        }
                      />
                  )}
              </StyledView>
          </StyledView>
      </Modal>
    </StyledSafeAreaView>
  );
};

export default ProfileScreen;
