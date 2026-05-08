import React, { useState, useRef } from 'react';
import { 
  View, 
  Text, 
  SafeAreaView, 
  TextInput, 
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform 
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import SocketService from '../../services/SocketService';
import { Alert } from 'react-native';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTextInput = styled(TextInput);
const StyledTouchableOpacity = styled(TouchableOpacity);

const OtpScreen = ({ route, navigation }) => {
  const { email, serverOtp, userData } = route.params || {};
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const inputs = useRef([]);

  const handleChange = (text, index) => {
    const newOtp = [...otp];
    newOtp[index] = text;
    setOtp(newOtp);

    if (text && index < 5) {
      inputs.current[index + 1].focus();
    }
  };

  const handleKeyPress = (e, index) => {
    if (e.nativeEvent.key === 'Backspace' && !otp[index] && index > 0) {
      inputs.current[index - 1].focus();
    }
  };

  const handleVerify = () => {
    const enteredOtp = otp.join('');
    if (enteredOtp.length < 6) {
      Alert.alert('Thông báo', 'Vui lòng nhập đầy đủ mã OTP.');
      return;
    }

    if (enteredOtp === serverOtp) {
      // Mã đúng, tiến hành đăng ký tài khoản thật lên Server
      setLoading(true);
      
      const unsubscribe = SocketService.addListener((res) => {
        setLoading(false);
        if (res.statusCode === 200) {
          Alert.alert('Thành công', 'Tài khoản của bạn đã được khởi tạo!', [
            { text: 'Đăng nhập ngay', onPress: () => navigation.navigate('Login') }
          ]);
        } else {
          Alert.alert('Lỗi', res.message || 'Không thể đăng ký tài khoản.');
        }
        unsubscribe();
      });

      // Gửi lệnh REGISTER_CUSTOMER với thông tin đã nhập
      SocketService.send('REGISTER_CUSTOMER', userData);
    } else {
      Alert.alert('Lỗi', 'Mã xác thực không chính xác. Vui lòng thử lại.');
    }
  };

  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={{ flex: 1 }}
      >
        <StyledView className="flex-1 px-8 pt-20 items-center">
          
          {/* Icon */}
          <StyledView className="w-20 h-20 rounded-full bg-orange-50 justify-center items-center mb-8">
             <Ionicons name="mail-open-outline" size={40} color={COLORS.accent} />
          </StyledView>

          {/* Title */}
          <StyledText className="text-3xl font-bold text-gray-900 mb-2">Xác thực OTP</StyledText>
          <StyledText className="text-gray-500 text-base text-center mb-12 px-4">
             Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra và nhập mã bên dưới.
          </StyledText>

          {/* OTP Inputs */}
          <StyledView className="flex-row justify-between w-full mb-12">
             {otp.map((digit, index) => (
               <StyledTextInput 
                key={index}
                ref={(ref) => (inputs.current[index] = ref)}
                className="w-[14%] aspect-square bg-gray-50 border border-gray-100 rounded-2xl text-center text-2xl font-bold text-gray-800"
                keyboardType="number-pad"
                maxLength={1}
                value={digit}
                onChangeText={(text) => handleChange(text, index)}
                onKeyPress={(e) => handleKeyPress(e, index)}
                style={{ borderColor: digit ? COLORS.primary : '#F3F4F6' }}
               />
             ))}
          </StyledView>

          {/* Verify Button */}
          <StyledTouchableOpacity 
            onPress={handleVerify}
            disabled={loading}
            className={`bg-red-900 w-full py-5 rounded-2xl shadow-lg shadow-red-900/30 items-center mb-8 ${loading ? 'opacity-70' : ''}`}
          >
             <StyledText className="text-white text-lg font-bold">
                {loading ? 'Đang hoàn tất...' : 'Xác nhận mã'}
             </StyledText>
          </StyledTouchableOpacity>

          {/* Resend */}
          <StyledView className="flex-row">
             <StyledText className="text-gray-500">Chưa nhận được mã? </StyledText>
             <StyledTouchableOpacity>
                <StyledText className="font-bold" style={{ color: COLORS.primary }}>Gửi lại sau 59s</StyledText>
             </StyledTouchableOpacity>
          </StyledView>

        </StyledView>
      </KeyboardAvoidingView>
    </StyledSafeAreaView>
  );
};

export default OtpScreen;
