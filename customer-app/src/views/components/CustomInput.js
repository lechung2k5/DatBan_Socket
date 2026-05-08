import React from 'react';
import { View, Text, TextInput } from 'react-native';
import { styled } from 'nativewind';

const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTextInput = styled(TextInput);

/**
 * CustomInput - Component ô nhập liệu dùng chung (Tailwind)
 */
const CustomInput = ({ label, value, onChangeText, placeholder, keyboardType = 'default', className = '' }) => {
  return (
    <StyledView className={`mb-4 ${className}`}>
      {label && <StyledText className="text-secondary font-semibold mb-1 text-sm">{label}</StyledText>}
      <StyledTextInput 
        className="border border-gray-300 rounded-lg p-3 text-secondary bg-white focus:border-primary"
        placeholder={placeholder}
        value={value}
        onChangeText={onChangeText}
        keyboardType={keyboardType}
        placeholderTextColor="#9CA3AF"
      />
    </StyledView>
  );
};

export default CustomInput;
