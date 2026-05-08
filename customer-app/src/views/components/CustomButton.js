import React from 'react';
import { TouchableOpacity, Text } from 'react-native';
import { styled } from 'nativewind';

const StyledTouchableOpacity = styled(TouchableOpacity);
const StyledText = styled(Text);

/**
 * CustomButton - Component nút bấm dùng chung (Tailwind)
 */
const CustomButton = ({ title, onPress, className = '', loading = false }) => {
  return (
    <StyledTouchableOpacity 
      onPress={onPress}
      disabled={loading}
      className={`bg-primary p-4 rounded-xl items-center active:opacity-80 ${className} ${loading ? 'opacity-70' : ''}`}
    >
      <StyledText className="text-white font-bold text-lg">
        {loading ? 'Đang xử lý...' : title}
      </StyledText>
    </StyledTouchableOpacity>
  );
};

export default CustomButton;
