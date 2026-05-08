import React from 'react';
import { 
  View, 
  Text, 
  SafeAreaView, 
  TextInput, 
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Image 
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import SocketService from '../../services/SocketService';
import { useEffect, useState } from 'react';
import CryptoJS from 'crypto-js';
import { Alert } from 'react-native';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTextInput = styled(TextInput);
const StyledTouchableOpacity = styled(TouchableOpacity);
const StyledScrollView = styled(ScrollView);

const LoginScreen = ({ navigation }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    SocketService.connect();
    const unsubscribe = SocketService.addListener((res) => {
      if (res.CommandType === 'CUSTOMER_LOGIN' || res.statusCode !== undefined) {
        setLoading(false);
        if (res.statusCode === 200) {
          // Đăng nhập thành công, lưu token và toàn bộ profile khách hàng
          if (res.data && res.data.token) {
            const customerData = res.data.customer || {};
            // Chuẩn hóa profile để dễ dùng nhưng vẫn giữ nguyên data gốc
            const profile = {
              ...customerData,
              name: customerData.tenKH,
              phone: customerData.soDT
            };
            SocketService.setToken(res.data.token, profile);
          }
          navigation.navigate('Main');
        } else if (res.statusCode === 400 || res.statusCode === 401) {
          Alert.alert('Lỗi', res.message || 'Đăng nhập thất bại');
        }
      }
    });
    return unsubscribe;
  }, []);

  const handleLogin = () => {
    if (!email || !password) {
      Alert.alert('Thông báo', 'Vui lòng nhập email và mật khẩu');
      return;
    }

    setLoading(true);
    const hashedPassword = CryptoJS.SHA256(password).toString();
    SocketService.send('CUSTOMER_LOGIN', { email, password: hashedPassword });
  };
  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <StyledScrollView contentContainerStyle={{ flexGrow: 1 }} showsVerticalScrollIndicator={false}>
          <StyledView className="flex-1 px-8 pt-20 pb-10 items-center justify-center">
            
            {/* Logo */}
            <StyledView className="mb-10 w-40 h-40">
               <Image 
                source={require('../../../assets/images/logo.png')} 
                style={{ width: '100%', height: '100%' }}
                resizeMode="contain"
               />
            </StyledView>

            {/* Title */}
            <StyledText className="text-4xl font-bold text-gray-900 mb-2">Đăng nhập</StyledText>
            <StyledText className="text-gray-500 text-base mb-12">Chào mừng bạn quay trở lại hệ thống.</StyledText>

            {/* Form Card */}
            <StyledView className="w-full bg-white p-8 rounded-[32px] shadow-2xl shadow-black/10 border border-gray-50">
               
               {/* Email Input */}
               <StyledView className="mb-6">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Email</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="mail-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="quanly@nhahang.com"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        keyboardType="email-address"
                        autoCapitalize="none"
                        value={email}
                        onChangeText={setEmail}
                     />
                  </StyledView>
               </StyledView>

               {/* Password Input */}
               <StyledView className="mb-4">
                  <StyledView className="flex-row justify-between items-center mb-2">
                     <StyledText className="text-sm font-bold text-gray-800 ml-1">Mật khẩu</StyledText>
                     <StyledTouchableOpacity>
                        <StyledText className="text-xs font-bold" style={{ color: COLORS.primary }}>Quên mật khẩu?</StyledText>
                     </StyledTouchableOpacity>
                  </StyledView>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="lock-closed-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="••••••••"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        secureTextEntry
                        value={password}
                        onChangeText={setPassword}
                     />
                  </StyledView>
               </StyledView>

               {/* Login Button */}
               <StyledTouchableOpacity 
                  onPress={handleLogin}
                  disabled={loading}
                  className={`bg-red-900 w-full py-5 rounded-2xl mt-6 shadow-lg shadow-red-900/30 items-center ${loading ? 'opacity-70' : ''}`}
               >
                  <StyledText className="text-white text-lg font-bold">
                    {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
                  </StyledText>
               </StyledTouchableOpacity>

               {/* Divider */}
               <StyledView className="flex-row items-center my-8">
                  <StyledView className="flex-1 h-[1px] bg-gray-100" />
                  <StyledText className="mx-4 text-xs font-bold text-gray-400">HOẶC</StyledText>
                  <StyledView className="flex-1 h-[1px] bg-gray-100" />
               </StyledView>

               {/* Google Login */}
               <StyledTouchableOpacity className="w-full flex-row items-center justify-center py-4 rounded-2xl border border-gray-100 bg-white shadow-sm">
                  <View className="w-5 h-5 mr-3">
                    <Image 
                        source={{ uri: 'https://cdn-icons-png.flaticon.com/512/2991/2991148.png' }} 
                        style={{ width: '100%', height: '100%' }}
                    />
                  </View>
                  <StyledText className="text-gray-700 text-base font-bold">Đăng nhập bằng Google</StyledText>
               </StyledTouchableOpacity>

            </StyledView>

            {/* Footer */}
            <StyledView className="flex-row mt-10">
               <StyledText className="text-gray-500">Chưa có tài khoản? </StyledText>
               <StyledTouchableOpacity onPress={() => navigation.navigate('Register')}>
                  <StyledText className="font-bold" style={{ color: COLORS.primary }}>Đăng ký ngay</StyledText>
               </StyledTouchableOpacity>
            </StyledView>

            <StyledText className="text-gray-300 text-[10px] mt-12">© 2024 Quản lý nhà hàng. Bảo mật tuyệt đối.</StyledText>

          </StyledView>
        </StyledScrollView>
      </KeyboardAvoidingView>
    </StyledSafeAreaView>
  );
};

export default LoginScreen;
