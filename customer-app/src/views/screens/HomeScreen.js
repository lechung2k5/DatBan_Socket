import React from 'react';
import { 
  ScrollView, 
  View, 
  Text, 
  SafeAreaView, 
  TextInput, 
  TouchableOpacity,
  Dimensions,
  Image 
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import { useEffect, useState } from 'react';
import ApiService from '../../services/ApiService';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledScrollView = styled(ScrollView);
const StyledTextInput = styled(TextInput);
const StyledTouchableOpacity = styled(TouchableOpacity);
const StyledView = styled(View);
const StyledText = styled(Text);

const { width } = Dimensions.get('window');

const HomeScreen = () => {
  const [menuItems, setMenuItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [filteredItems, setFilteredItems] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();

    // 🔥 CƠ CHẾ REAL-TIME (POLLING): Tự động cập nhật dữ liệu mỗi 30 giây
    const pollingInterval = setInterval(fetchData, 30000);

    return () => clearInterval(pollingInterval);
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const menuRes = await ApiService.getMenu();
      if (menuRes.statusCode === 200) {
        setMenuItems(menuRes.data);
        setFilteredItems(menuRes.data);
      }

      const catRes = await ApiService.getCategories();
      if (catRes.statusCode === 200) {
        setCategories(catRes.data);
      }
    } catch (err) {
      console.error('Error fetching data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (text) => {
    setSearchQuery(text);
    filterItems(text, selectedCategory);
  };

  const handleCategorySelect = (catId) => {
    setSelectedCategory(catId);
    filterItems(searchQuery, catId);
  };

  const filterItems = (query, catId) => {
    let filtered = [...menuItems];
    
    if (catId !== 'ALL') {
      filtered = filtered.filter(item => item.maDM === catId);
    }
    
    if (query) {
      filtered = filtered.filter(item => 
        item.tenMon.toLowerCase().includes(query.toLowerCase())
      );
    }
    
    setFilteredItems(filtered);
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
                  Xin chào, Quý khách
                </StyledText>
             </StyledView>
          </StyledView>
          <StyledTouchableOpacity className="p-2">
             <Ionicons name="search-outline" size={24} color={COLORS.text} />
          </StyledTouchableOpacity>
        </StyledView>

        {/* Search Bar */}
        <StyledView className="flex-row items-center bg-gray-100 rounded-full px-4 py-3 mb-6 border border-gray-100">
          <Ionicons name="search-outline" size={20} color="#999" />
          <StyledTextInput 
            placeholder="Tìm món ăn, chi nhánh..." 
            className="flex-1 ml-2 text-base"
            placeholderTextColor="#999"
            value={searchQuery}
            onChangeText={handleSearch}
          />
        </StyledView>

        {/* Status Card (Appointment) */}
        <StyledView className="bg-orange-50 p-6 rounded-[24px] flex-row items-center mb-8 border border-orange-100 shadow-sm">
           <StyledView className="w-12 h-12 rounded-2xl bg-orange-200 justify-center items-center mr-4">
              <Ionicons name="calendar" size={24} color="#D35400" />
           </StyledView>
           <StyledView>
              <StyledText className="text-xs font-semibold text-orange-600 tracking-wider uppercase">THÔNG BÁO LỊCH TRÌNH</StyledText>
              <StyledText className="text-lg font-bold mt-1" style={{ color: '#5D4037' }}>Bạn có lịch hẹn lúc 19:00 hôm nay</StyledText>
           </StyledView>
        </StyledView>

        {/* Quick Menu Icons */}
        <StyledView className="flex-row justify-between mb-10">
           {[
             { name: 'Đặt bàn', icon: 'cellphone-dock', color: '#FAD7D4' },
             { name: 'Ưu đãi', icon: 'ticket-percent', color: '#FFF3E0' },
             { name: 'Thực đơn', icon: 'silverware-fork-knife', color: '#F2F2F2' },
             { name: 'Liên hệ', icon: 'face-agent', color: '#F2F2F2' }
           ].map((item, index) => (
             <StyledTouchableOpacity key={index} className="items-center">
                <StyledView className="w-16 h-16 rounded-2xl justify-center items-center mb-2" style={{ backgroundColor: item.color }}>
                   <MaterialCommunityIcons name={item.icon} size={28} color={index === 0 ? COLORS.primary : COLORS.text} />
                </StyledView>
                <StyledText className="text-sm font-semibold text-gray-700">{item.name}</StyledText>
             </StyledTouchableOpacity>
           ))}
        </StyledView>

        {/* Banner Section */}
        <StyledView className="bg-red-900 rounded-[30px] p-6 mb-10 overflow-hidden relative" style={{ height: 180 }}>
           <StyledView className="z-10 w-2/3">
              <StyledView className="bg-orange-200 self-start px-3 py-1 rounded-full mb-3">
                 <StyledText className="text-xs font-bold text-orange-800">Ưu đãi Đặc biệt</StyledText>
              </StyledView>
              <StyledText className="text-white text-3xl font-bold leading-tight">Giảm 20% cho thành viên mới</StyledText>
              <StyledText className="text-red-200 text-sm mt-2">Áp dụng cho hóa đơn từ 1.000.000đ</StyledText>
           </StyledView>
           {/* Decorative Circle */}
           <StyledView className="absolute right-[-20] top-[-20] w-40 h-40 rounded-full bg-red-800 opacity-30" />
           <StyledView className="absolute right-10 bottom-[-30] w-32 h-32 rounded-full bg-red-800 opacity-20" />
        </StyledView>
        {/* Categories Section */}
        <StyledView className="mb-6">
           <StyledText className="text-xl font-bold text-gray-800 mb-4">Danh mục</StyledText>
           <StyledScrollView 
            horizontal 
            showsHorizontalScrollIndicator={false} 
            contentContainerStyle={{ paddingRight: 20 }}
           >
          <StyledTouchableOpacity 
            onPress={() => handleCategorySelect('ALL')}
            className={`${selectedCategory === 'ALL' ? 'bg-red-900' : 'bg-gray-50'} px-6 py-3 rounded-2xl mr-3 shadow-md ${selectedCategory === 'ALL' ? 'shadow-red-900/20' : 'border border-gray-100'}`}
          >
             <StyledText className={`${selectedCategory === 'ALL' ? 'text-white' : 'text-gray-600'} font-bold`}>Tất cả</StyledText>
          </StyledTouchableOpacity>
          {categories.map((cat, index) => (
            <StyledTouchableOpacity 
              key={cat.maDM || index} 
              onPress={() => handleCategorySelect(cat.maDM)}
              className={`${selectedCategory === cat.maDM ? 'bg-red-900' : 'bg-gray-50'} px-6 py-3 rounded-2xl mr-3 border border-gray-100 shadow-sm`}
            >
               <StyledText className={`${selectedCategory === cat.maDM ? 'text-white' : 'text-gray-600'} font-bold`}>{cat.tenDM}</StyledText>
            </StyledTouchableOpacity>
          ))}
           </StyledScrollView>
        </StyledView>

        {/* Featured Section */}
        <StyledView className="flex-row justify-between items-center mb-4">
           <StyledText className="text-2xl font-bold text-gray-800">Món ngon phải thử</StyledText>
           <StyledTouchableOpacity className="flex-row items-center">
              <StyledText className="text-primary font-bold text-sm mr-1" style={{ color: COLORS.primary }}>Xem tất cả</StyledText>
              <Ionicons name="chevron-forward" size={16} color={COLORS.primary} />
           </StyledTouchableOpacity>
        </StyledView>

        {/* Dish List */}
        {filteredItems.map((item, index) => (
          <StyledView key={item.maMon || index} className="bg-white rounded-[24px] shadow-lg shadow-black/10 overflow-hidden mb-6 border border-gray-100">
             <StyledView className="relative">
                <View className="w-full h-56">
                   <Image 
                     source={item.hinhAnhUrl ? { uri: item.hinhAnhUrl } : require('../../../assets/images/pho_wagyu.png')} 
                     style={{ width: '100%', height: '100%' }}
                     resizeMode="cover"
                   />
                </View>
                <StyledView className="absolute top-4 right-4 bg-white/90 px-2 py-1 rounded-full flex-row items-center">
                   <Ionicons name="star" size={14} color="#F1C40F" />
                   <StyledText className="text-xs font-bold ml-1 text-gray-800">4.9</StyledText>
                </StyledView>
             </StyledView>
             <StyledView className="p-5">
                <StyledText className="text-xl font-bold text-gray-800">{item.tenMon}</StyledText>
                <StyledText className="text-gray-500 text-sm mt-2 leading-5" numberOfLines={2}>
                   {item.moTa || "Món ăn thơm ngon, bổ dưỡng được chế biến từ nguyên liệu tươi sạch mỗi ngày."}
                </StyledText>
                <StyledView className="flex-row justify-between items-center mt-4">
                   <StyledText className="text-xl font-bold text-gray-800">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.giaBan)}
                   </StyledText>
                   <StyledTouchableOpacity className="w-10 h-10 rounded-full bg-red-900 justify-center items-center">
                      <Ionicons name="add" size={24} color="white" />
                   </StyledTouchableOpacity>
                </StyledView>
             </StyledView>
          </StyledView>
        ))}

        {filteredItems.length === 0 && !loading && (
          <StyledView className="items-center py-10">
            <StyledText className="text-gray-400">Không tìm thấy món ăn nào</StyledText>
          </StyledView>
        )}

        <StyledView className="h-20" />
      </StyledScrollView>
    </StyledSafeAreaView>
  );
};

export default HomeScreen;
