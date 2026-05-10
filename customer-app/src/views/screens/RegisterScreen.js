import React, { useState } from 'react';
import { 
  View, 
  Text, 
  SafeAreaView, 
  TextInput, 
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  Image,
  Alert 
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import CryptoJS from 'crypto-js';
import SocketService from '../../services/SocketService';
import { useEffect } from 'react';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTextInput = styled(TextInput);
const StyledTouchableOpacity = styled(TouchableOpacity);
const StyledScrollView = styled(ScrollView);

const RegisterScreen = ({ navigation }) => {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    phone: '',
    email: '',
    diaChi: '',
    password: '',
    confirmPassword: ''
  });

  useEffect(() => {
    SocketService.connect();
    
    let isMounted = true;
    const unsubscribe = SocketService.addListener((res) => {
      if (!isMounted || !res) return; // Phòng vệ rỗng và unmounted
      
      if (res.statusCode === 200) {
        if (res.data && res.data.otp) {
          setLoading(false);
          navigation.navigate('Otp', { 
            email: formData.email, 
            serverOtp: res.data.otp,
            userData: {
                ...formData,
                password: CryptoJS.SHA256(formData.password).toString()
            }
          });
        }
      } else {
        setLoading(false);
        Alert.alert('Lỗi', res.message || 'Không thể gửi OTP');
      }
    });

    return () => {
        isMounted = false;
        if (typeof unsubscribe === 'function') unsubscribe();
        else SocketService.removeListener(unsubscribe);
    };
  }, [formData, navigation]);

  const handleRegister = () => {
    const { name, phone, email, diaChi, password, confirmPassword } = formData;

    if (!name || !phone || !email || !diaChi || !password) {
      Alert.alert('Thông báo', 'Vui lòng nhập đầy đủ thông tin bắt buộc.');
      return;
    }

    if (password !== confirmPassword) {
      Alert.alert('Lỗi', 'Mật khẩu xác nhận không khớp.');
      return;
    }

    setLoading(true);
    // Yêu cầu server gửi OTP thực qua email
    SocketService.send('SEND_OTP', { email });
  };

  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <StyledScrollView contentContainerStyle={{ flexGrow: 1 }} showsVerticalScrollIndicator={false}>
          <StyledView className="flex-1 px-8 pt-10 pb-10 items-center justify-center">
            
            {/* Logo */}
            <StyledView className="mb-6 w-32 h-32">
               <Image 
                source={require('../../../assets/images/logo.png')} 
                style={{ width: '100%', height: '100%' }}
                resizeMode="contain"
               />
            </StyledView>

            {/* Title */}
            <StyledText className="text-3xl font-bold text-gray-900 mb-2">Đăng ký</StyledText>
            <StyledText className="text-gray-500 text-base mb-8 text-center px-4">Tham gia cùng chúng tôi để nhận nhiều ưu đãi hấp dẫn.</StyledText>

            {/* Form Card */}
            <StyledView className="w-full bg-white p-8 rounded-[32px] shadow-2xl shadow-black/10 border border-gray-50">
               
               {/* Name Input */}
               <StyledView className="mb-5">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Họ và Tên *</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="person-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="VD: Lê Chung"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        value={formData.name}
                        onChangeText={(text) => setFormData({...formData, name: text})}
                     />
                  </StyledView>
               </StyledView>

               {/* Phone Input */}
               <StyledView className="mb-5">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Số điện thoại *</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="call-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="09xx xxx xxx"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        keyboardType="phone-pad"
                        value={formData.phone}
                        onChangeText={(text) => setFormData({...formData, phone: text})}
                     />
                  </StyledView>
               </StyledView>

               {/* Email Input */}
               <StyledView className="mb-5">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Email *</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="mail-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="lechung@gmail.com"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        keyboardType="email-address"
                        autoCapitalize="none"
                        value={formData.email}
                        onChangeText={(text) => setFormData({...formData, email: text})}
                     />
                  </StyledView>
               </StyledView>

               {/* Address Input */}
               <StyledView className="mb-5">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Địa chỉ *</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="location-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="Số 12, Nguyễn Văn Bảo, Gò Vấp"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        value={formData.diaChi}
                        onChangeText={(text) => setFormData({...formData, diaChi: text})}
                     />
                  </StyledView>
               </StyledView>

               {/* Password Input */}
               <StyledView className="mb-5">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Mật khẩu *</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="lock-closed-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="••••••••"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        secureTextEntry
                        value={formData.password}
                        onChangeText={(text) => setFormData({...formData, password: text})}
                     />
                  </StyledView>
               </StyledView>

               {/* Confirm Password Input */}
               <StyledView className="mb-5">
                  <StyledText className="text-sm font-bold text-gray-800 mb-2 ml-1">Xác nhận mật khẩu *</StyledText>
                  <StyledView className="flex-row items-center bg-gray-50 border border-gray-100 rounded-2xl px-4 py-4">
                     <Ionicons name="shield-checkmark-outline" size={20} color="#666" />
                     <StyledTextInput 
                        placeholder="••••••••"
                        placeholderTextColor="#999"
                        className="flex-1 ml-3 text-base text-gray-800"
                        secureTextEntry
                        value={formData.confirmPassword}
                        onChangeText={(text) => setFormData({...formData, confirmPassword: text})}
                     />
                  </StyledView>
               </StyledView>

               {/* Register Button */}
               <StyledTouchableOpacity 
                  onPress={handleRegister}
                  disabled={loading}
                  className={`bg-red-900 w-full py-5 rounded-2xl mt-4 shadow-lg shadow-red-900/30 items-center ${loading ? 'opacity-70' : ''}`}
               >
                  <StyledText className="text-white text-lg font-bold">
                    {loading ? 'Đang gửi mã OTP...' : 'Tạo tài khoản'}
                  </StyledText>
               </StyledTouchableOpacity>

            </StyledView>

            {/* Footer */}
            <StyledView className="flex-row mt-8">
               <StyledText className="text-gray-500">Đã có tài khoản? </StyledText>
               <StyledTouchableOpacity onPress={() => navigation.navigate('Login')}>
                  <StyledText className="font-bold" style={{ color: COLORS.primary }}>Đăng nhập ngay</StyledText>
               </StyledTouchableOpacity>
            </StyledView>

          </StyledView>
        </StyledScrollView>
      </KeyboardAvoidingView>
    </StyledSafeAreaView>
  );
};

export default RegisterScreen;
