import React from 'react';
import { 
  ScrollView, 
  View, 
  Text, 
  SafeAreaView, 
  TouchableOpacity,
  Image 
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledScrollView = styled(ScrollView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTouchableOpacity = styled(TouchableOpacity);

import SocketService from '../../services/SocketService';
import { useNavigation } from '@react-navigation/native';

const ProfileScreen = () => {
  const navigation = useNavigation();
  const profile = SocketService.userProfile || {};

  const handleLogout = async () => {
    await SocketService.setToken(null, null);
    // Reset to Login screen
    navigation.reset({
      index: 0,
      routes: [{ name: 'Login' }],
    });
  };

  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <StyledScrollView showsVerticalScrollIndicator={false} className="px-4 pt-4">
        
        {/* Header */}
        <StyledView className="flex-row justify-between items-center mb-6">
          <StyledView className="flex-row items-center">
             <View className="w-12 h-12">
                <Image 
                   source={require('../../../assets/images/logo.png')} 
                   style={{ width: '100%', height: '100%' }}
                   resizeMode="contain"
                 />
             </View>
             <StyledView className="ml-3">
                <StyledText className="text-xl font-bold" style={{ color: COLORS.primary }}>
                   {profile.tenKH || 'Quý khách'}
                </StyledText>
             </StyledView>
          </StyledView>
          <StyledTouchableOpacity className="p-2">
             <Ionicons name="search-outline" size={24} color={COLORS.text} />
          </StyledTouchableOpacity>
        </StyledView>

        {/* Membership Card */}
        <StyledView className="bg-red-900 rounded-[30px] p-6 mb-8 shadow-xl shadow-red-900/40 relative overflow-hidden">
           <StyledView className="flex-row justify-between">
              <StyledView>
                 <StyledText className="text-white/80 text-sm font-semibold">Thành viên</StyledText>
                 <StyledView className="bg-orange-200 self-start px-3 py-1 rounded-full mt-2 flex-row items-center">
                    <Ionicons name="ribbon" size={12} color="#884210" />
                    <StyledText className="text-[10px] font-bold text-orange-800 ml-1">
                      {profile.thanhVien?.toUpperCase() || 'MEMBER'}
                    </StyledText>
                 </StyledView>
              </StyledView>
              <StyledView className="items-end">
                 <StyledText className="text-white/80 text-sm font-semibold">Điểm tích lũy</StyledText>
                 <StyledText className="text-white text-4xl font-bold mt-1">
                   {profile.diemTichLuy?.toLocaleString() || '0'}
                 </StyledText>
              </StyledView>
           </StyledView>

           <StyledView className="mt-10">
              <StyledText className="text-white/90 text-xs font-semibold">
                 ID: <StyledText className="text-orange-200">{profile.maKH || 'N/A'}</StyledText>
              </StyledText>
              <StyledView className="w-full h-2 bg-red-800/50 rounded-full mt-3 overflow-hidden">
                 <StyledView className="h-full bg-orange-300 rounded-full" style={{ width: '30%' }} />
              </StyledView>
           </StyledView>

           <StyledView className="absolute right-[-20] top-[-20] w-32 h-32 rounded-full bg-white/5" />
        </StyledView>

        {/* Main Actions Cards */}
        <StyledView className="flex-row justify-between mb-8">
           <StyledTouchableOpacity className="bg-white rounded-[24px] p-6 items-center flex-1 mr-3 shadow-md shadow-black/5 border border-gray-100">
              <StyledView className="w-12 h-12 rounded-full bg-gray-100 justify-center items-center mb-3">
                 <Ionicons name="person-outline" size={24} color={COLORS.primary} />
              </StyledView>
              <StyledText className="text-base font-bold text-center text-gray-800">Thông tin cá nhân</StyledText>
           </StyledTouchableOpacity>
           
           <StyledTouchableOpacity className="bg-white rounded-[24px] p-6 items-center flex-1 ml-3 shadow-md shadow-black/5 border border-gray-100">
              <StyledView className="w-12 h-12 rounded-full bg-gray-100 justify-center items-center mb-3">
                 <Ionicons name="settings-outline" size={24} color={COLORS.primary} />
              </StyledView>
              <StyledText className="text-base font-bold text-center text-gray-800">Cài đặt</StyledText>
           </StyledTouchableOpacity>
        </StyledView>

        {/* Activity Section */}
        <StyledView className="mb-6">
           <StyledText className="text-gray-500 font-bold text-xs uppercase tracking-widest mb-4 ml-1">Hoạt động</StyledText>
           
           <StyledView className="bg-white rounded-[24px] shadow-sm border border-gray-100 overflow-hidden">
              {[
                { title: 'Lịch sử đặt bàn', icon: 'history', type: 'MaterialCommunityIcons' },
                { title: 'Lịch sử điểm', icon: 'star-outline', type: 'Ionicons' },
                { title: 'Lịch sử hóa đơn', icon: 'receipt-outline', type: 'Ionicons' }
              ].map((item, index) => (
                <StyledTouchableOpacity 
                  key={index} 
                  className={`flex-row items-center justify-between p-5 ${index !== 2 ? 'border-b border-gray-50' : ''}`}
                >
                   <StyledView className="flex-row items-center">
                      <StyledView className="w-10 h-10 rounded-full bg-gray-100 justify-center items-center mr-4">
                         {item.type === 'Ionicons' ? (
                           <Ionicons name={item.icon} size={20} color="#444" />
                         ) : (
                           <MaterialCommunityIcons name={item.icon} size={20} color="#444" />
                         )}
                      </StyledView>
                      <StyledText className="text-lg font-semibold text-gray-800">{item.title}</StyledText>
                   </StyledView>
                   <Ionicons name="chevron-forward" size={20} color="#CCC" />
                </StyledTouchableOpacity>
              ))}
           </StyledView>
        </StyledView>

        {/* Logout Button */}
        <StyledTouchableOpacity 
          onPress={handleLogout}
          className="bg-red-50 flex-row items-center justify-center p-5 rounded-2xl border border-red-100 mb-10"
        >
           <Ionicons name="log-out-outline" size={22} color={COLORS.primary} />
           <StyledText className="text-lg font-bold ml-2" style={{ color: COLORS.primary }}>Đăng xuất</StyledText>
        </StyledTouchableOpacity>

        <StyledView className="h-20" />
      </StyledScrollView>
    </StyledSafeAreaView>
  );
};

export default ProfileScreen;
