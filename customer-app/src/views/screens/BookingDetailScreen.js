import React, { useState, useEffect } from 'react';
import { 
  ScrollView, 
  View, 
  Text, 
  SafeAreaView, 
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Modal,
  FlatList,
  Image,
  TextInput
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
const StyledTextInput = styled(TextInput);

const BookingDetailScreen = ({ route, navigation }) => {
  const { maHD } = route.params;
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  
  // Modals
  const [showMenuModal, setShowMenuModal] = useState(false);
  const [showTableModal, setShowTableModal] = useState(false);
  const [availableTables, setAvailableTables] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('');

  useEffect(() => {
    fetchDetail();
    fetchMenuData();
    
    const unsubscribe = SocketService.on('UPDATE_INVOICE', (data) => {
        if (data === maHD || data === `[ORDER]:${maHD}`) {
            fetchDetail();
        }
    });

    return () => unsubscribe();
  }, [maHD]);

  const fetchDetail = async () => {
    try {
      setLoading(true);
      const res = await ApiService.getInvoiceDetail(maHD);
      if (res.statusCode === 200) {
        setBooking(res.data);
      } else {
        Alert.alert('Lỗi', res.message || 'Không thể lấy thông tin đơn hàng');
        navigation.goBack();
      }
    } catch (err) {
      console.error('Error fetching detail:', err);
      Alert.alert('Lỗi', 'Không thể tìm thấy hóa đơn hoặc lỗi kết nối');
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  };

  const fetchMenuData = async () => {
    try {
      const [menuRes, catRes] = await Promise.all([
        ApiService.getMenu(),
        ApiService.getCategories()
      ]);
      if (menuRes.statusCode === 200) setMenuItems(menuRes.data);
      if (catRes.statusCode === 200) {
          setCategories(catRes.data);
          if (catRes.data.length > 0) setSelectedCategory(catRes.data[0].maDM);
      }
    } catch (err) {}
  };

  const canEdit = booking && booking.trangThai !== 'DangSuDung' && booking.trangThai !== 'DaThanhToan';

  const handleUpdateItems = async (newItems) => {
    if (!canEdit) {
        Alert.alert('Thông báo', 'Đơn hàng đang phục vụ hoặc đã thanh toán, không thể chỉnh sửa.');
        return;
    }
    try {
      setUpdating(true);
      const res = await ApiService.updateInvoice({
        maHD: maHD,
        itemsJson: JSON.stringify(newItems),
        source: 'mobile'
      });
      if (res.statusCode === 200) {
        Alert.alert('Thành công', 'Đã cập nhật danh sách món ăn');
        setShowMenuModal(false);
      }
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể cập nhật món ăn');
    } finally {
      setUpdating(false);
    }
  };

  const handleChangeTable = async (newTableId) => {
    if (!canEdit) {
        Alert.alert('Thông báo', 'Đơn hàng đang phục vụ hoặc đã thanh toán, không thể đổi bàn.');
        return;
    }
    try {
      setUpdating(true);
      const res = await ApiService.updateInvoice({
        maHD: maHD,
        maBan: newTableId,
        source: 'mobile'
      });
      if (res.statusCode === 200) {
        Alert.alert('Thành công', 'Đã đổi bàn sang ' + newTableId);
        setShowTableModal(false);
      }
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể đổi bàn');
    } finally {
      setUpdating(false);
    }
  };

  const handleCancelBooking = () => {
    if (!canEdit) {
        Alert.alert('Thông báo', 'Đơn hàng đang phục vụ hoặc đã thanh toán, không thể hủy.');
        return;
    }
    Alert.alert(
        'Xác nhận hủy',
        'Bạn có chắc chắn muốn hủy đơn đặt bàn này không?',
        [
            { text: 'Không', style: 'cancel' },
            { text: 'Hủy ngay', style: 'destructive', onPress: async () => {
                try {
                    setUpdating(true);
                    const res = await SocketService.request('DELETE_INVOICE', { maHD });
                    if (res.statusCode === 200) {
                        Alert.alert('Thành công', 'Đã hủy đơn đặt bàn');
                        navigation.navigate('Trang chủ');
                    }
                } catch (err) {
                    Alert.alert('Lỗi', 'Không thể hủy đơn');
                } finally {
                    setUpdating(false);
                }
            }}
        ]
    );
  };

  const fetchAvailableTables = async () => {
      try {
        setUpdating(true);
        const res = await SocketService.request('GET_TABLES');
        if (res.statusCode === 200) {
            setAvailableTables(res.data.filter(t => t.trangThai === 'Trong'));
            setShowTableModal(true);
        }
      } catch (err) {
          Alert.alert('Lỗi', 'Không thể lấy danh sách bàn trống');
      } finally {
          setUpdating(false);
      }
  };

  if (loading || !booking) {
    return (
      <StyledView className="flex-1 justify-center items-center bg-white">
        <ActivityIndicator size="large" color="#800000" />
        <StyledText className="mt-4 text-gray-500">Đang tải chi tiết...</StyledText>
      </StyledView>
    );
  }

  const getStatusColor = (status) => {
      switch(status) {
          case 'ChoXacNhan': return '#D35400';
          case 'Dat': return '#27AE60';
          case 'DaHuy': return '#C0392B';
          default: return '#2C3E50';
      }
  };

  return (
    <StyledSafeAreaView className="flex-1 bg-gray-50">
      {/* Header */}
      <StyledView className="px-6 py-4 bg-white flex-row items-center border-b border-gray-100">
        <StyledTouchableOpacity onPress={() => navigation.goBack()} className="p-2 -ml-2">
          <Ionicons name="chevron-back" size={28} color="#800000" />
        </StyledTouchableOpacity>
        <StyledText className="text-xl font-bold ml-2 text-red-900">Chi tiết đơn đặt</StyledText>
      </StyledView>

      <StyledScrollView showsVerticalScrollIndicator={false} className="flex-1 px-6 pt-6">
        {/* Status Card */}
        <StyledView className="bg-white rounded-3xl p-6 mb-6 shadow-sm border border-gray-100">
            <StyledView className="flex-row justify-between items-center mb-4">
                <StyledText className="text-gray-400 font-semibold">MÃ ĐƠN HÀNG</StyledText>
                <StyledView className="px-3 py-1 rounded-full" style={{ backgroundColor: getStatusColor(booking.trangThai) + '20' }}>
                    <StyledText className="font-bold text-xs" style={{ color: getStatusColor(booking.trangThai) }}>
                        {booking.trangThai === 'ChoXacNhan' ? 'CHỜ XÁC NHẬN' : booking.trangThai.toUpperCase()}
                    </StyledText>
                </StyledView>
            </StyledView>
            <StyledView className="flex-row justify-between items-center mb-2">
                <StyledText className="text-2xl font-bold text-gray-800">{booking.maHD}</StyledText>
                <StyledView className="items-end">
                    <StyledText className="text-gray-400 text-[10px]">NGÀY ĐẶT</StyledText>
                    <StyledText className="text-sm font-semibold text-gray-600">
                        {booking.ngayLap ? booking.ngayLap.split('T')[0].split('-').reverse().join('/') : 
                         booking.gioVao ? booking.gioVao.split('T')[0].split('-').reverse().join('/') : '--/--/----'}
                    </StyledText>
                </StyledView>
            </StyledView>
            <StyledView className="h-[1px] bg-gray-100 my-4" />
            <StyledView className="flex-row justify-between">
                <StyledView>
                    <StyledText className="text-gray-400 text-xs mb-1">BÀN</StyledText>
                    <StyledText className="text-lg font-bold text-red-900">{booking.ban?.maBan || booking.maBan || 'N/A'}</StyledText>
                </StyledView>
                <StyledView className="items-end">
                    <StyledView className="flex-row">
                        <StyledView className="mr-6 items-center">
                            <StyledText className="text-gray-400 text-[10px] mb-1">GIỜ VÀO</StyledText>
                            <StyledText className="text-base font-bold text-gray-800">
                                {booking.gioVao ? booking.gioVao.split('T')[1]?.substring(0, 5) : '--:--'}
                            </StyledText>
                        </StyledView>
                        <StyledView className="items-center">
                            <StyledText className="text-gray-400 text-[10px] mb-1">GIỜ RA</StyledText>
                            <StyledText className="text-base font-bold text-gray-800">
                                {booking.gioRa ? booking.gioRa.split('T')[1]?.substring(0, 5) : '--:--'}
                            </StyledText>
                        </StyledView>
                    </StyledView>
                </StyledView>
            </StyledView>

            {booking.trangThai === 'DaThanhToan' && (
                <StyledView className="mt-6 p-4 bg-green-50 rounded-2xl border border-green-100 items-center">
                    <Ionicons name="heart" size={24} color="#27AE60" />
                    <StyledText className="text-green-800 font-bold mt-2 text-center">
                        Cảm ơn quý khách đã tin tưởng và sử dụng dịch vụ của nhà hàng chúng tôi!
                    </StyledText>
                </StyledView>
            )}
        </StyledView>

        {/* Action Buttons */}
        {!canEdit && (
            <StyledView className="bg-amber-50 p-4 rounded-2xl mb-6 border border-amber-200 flex-row items-center">
                <Ionicons name="lock-closed" size={20} color="#D35400" />
                <StyledText className="text-amber-800 ml-2 font-semibold text-xs">
                    Đơn hàng đã được khóa (Đang phục vụ hoặc Đã thanh toán)
                </StyledText>
            </StyledView>
        )}

        <StyledView className="flex-row justify-between mb-8">
            <StyledTouchableOpacity 
                onPress={canEdit ? fetchAvailableTables : null}
                style={{ opacity: canEdit ? 1 : 0.5 }}
                className="bg-red-50 p-4 rounded-2xl flex-1 mr-2 items-center border border-red-100"
            >
                <Ionicons name="swap-horizontal" size={24} color="#800000" />
                <StyledText className="text-red-900 font-bold mt-1 text-xs">Đổi bàn</StyledText>
            </StyledTouchableOpacity>
            <StyledTouchableOpacity 
                onPress={canEdit ? () => setShowMenuModal(true) : null}
                style={{ opacity: canEdit ? 1 : 0.5 }}
                className="bg-red-50 p-4 rounded-2xl flex-1 mx-2 items-center border border-red-100"
            >
                <Ionicons name="add-circle-outline" size={24} color="#800000" />
                <StyledText className="text-red-900 font-bold mt-1 text-xs">Thêm món</StyledText>
            </StyledTouchableOpacity>
            <StyledTouchableOpacity 
                onPress={canEdit ? handleCancelBooking : null}
                style={{ opacity: canEdit ? 1 : 0.5 }}
                className="bg-red-50 p-4 rounded-2xl flex-1 ml-2 items-center border border-red-100"
            >
                <Ionicons name="trash-outline" size={24} color="#C0392B" />
                <StyledText className="text-red-600 font-bold mt-1 text-xs">Hủy bàn</StyledText>
            </StyledTouchableOpacity>
        </StyledView>

        {/* Dishes List */}
        <StyledText className="text-xl font-bold mb-4 text-gray-800">Món ăn đã đặt</StyledText>
        {booking.chiTietHoaDon && booking.chiTietHoaDon.length > 0 ? (
            booking.chiTietHoaDon.map((item, index) => (
                <StyledView key={index} className="bg-white rounded-2xl p-4 mb-3 flex-row justify-between items-center shadow-sm border border-gray-100">
                    <StyledView className="flex-1">
                        <StyledText className="font-bold text-gray-800 text-base">{item.tenMon}</StyledText>
                        <StyledText className="text-gray-400 text-xs">Đơn giá: {item.donGia?.toLocaleString()}đ</StyledText>
                    </StyledView>
                    <StyledView className="items-end">
                        <StyledText className="font-bold text-red-900">x{item.soLuong}</StyledText>
                        <StyledText className="font-bold text-gray-800">{(item.soLuong * item.donGia)?.toLocaleString()}đ</StyledText>
                    </StyledView>
                </StyledView>
            ))
        ) : (
            <StyledView className="bg-white rounded-2xl p-8 items-center border border-dashed border-gray-300">
                <Ionicons name="fast-food-outline" size={48} color="#CCC" />
                <StyledText className="text-gray-400 mt-2">Chưa có món ăn nào</StyledText>
            </StyledView>
        )}

        {/* Payment Summary */}
        <StyledView className="bg-white rounded-3xl p-6 mt-6 mb-6 shadow-sm border border-gray-100">
            <StyledText className="text-lg font-bold mb-4 text-gray-800">Chi tiết thanh toán</StyledText>
            
            <StyledView className="flex-row justify-between mb-2">
                <StyledText className="text-gray-500">Tạm tính (Món ăn)</StyledText>
                <StyledText className="text-gray-800 font-semibold">{booking.tongCongMonAn?.toLocaleString()}đ</StyledText>
            </StyledView>

            {booking.phiDichVu > 0 && (
                <StyledView className="flex-row justify-between mb-2">
                    <StyledText className="text-gray-500">Phí dịch vụ</StyledText>
                    <StyledText className="text-gray-800 font-semibold">{booking.phiDichVu?.toLocaleString()}đ</StyledText>
                </StyledView>
            )}

            <StyledView className="flex-row justify-between mb-2">
                <StyledText className="text-gray-500">Thuế (VAT)</StyledText>
                <StyledText className="text-gray-800 font-semibold">{booking.thueVAT?.toLocaleString()}đ</StyledText>
            </StyledView>

            {booking.tienCoc > 0 && (
                <StyledView className="flex-row justify-between mb-2">
                    <StyledText className="text-blue-600">Đã đặt cọc</StyledText>
                    <StyledText className="text-blue-600 font-semibold">-{booking.tienCoc?.toLocaleString()}đ</StyledText>
                </StyledView>
            )}

            {booking.khuyenMai > 0 && (
                <StyledView className="flex-row justify-between mb-2">
                    <StyledText className="text-green-600">Ưu đãi / Giảm giá</StyledText>
                    <StyledText className="text-green-600 font-semibold">-{booking.khuyenMai?.toLocaleString()}đ</StyledText>
                </StyledView>
            )}

            <StyledView className="h-[1px] bg-gray-100 my-4" />

            <StyledView className="flex-row justify-between">
                <StyledText className="text-xl font-bold text-gray-800">TỔNG CỘNG</StyledText>
                <StyledText className="text-xl font-bold text-red-900">{booking.tongTienThanhToan?.toLocaleString()}đ</StyledText>
            </StyledView>
            
            {booking.trangThai === 'DaThanhToan' && (
                <StyledView className="mt-4 pt-4 border-t border-gray-50 items-center">
                    <StyledText className="text-gray-400 text-xs">Phương thức: {booking.hinhThucTT || 'Tiền mặt'}</StyledText>
                </StyledView>
            )}
        </StyledView>

        <StyledView className="h-16" />
      </StyledScrollView>

      {/* Menu Modal (Simplified) */}
      <Modal visible={showMenuModal} animationType="slide">
          <StyledSafeAreaView className="flex-1 bg-white">
              <StyledView className="p-6 flex-row justify-between items-center border-b border-gray-100">
                  <StyledText className="text-2xl font-bold text-red-900">Thêm món ăn</StyledText>
                  <StyledTouchableOpacity onPress={() => setShowMenuModal(false)}>
                      <Ionicons name="close" size={32} color="gray" />
                  </StyledTouchableOpacity>
              </StyledView>
              <FlatList 
                data={menuItems}
                keyExtractor={item => item.maMon}
                renderItem={({ item }) => (
                    <StyledTouchableOpacity 
                        onPress={() => {
                            const currentItems = booking.chiTietHoaDon || [];
                            const existing = currentItems.find(i => i.maMon === item.maMon);
                            let updated;
                            if (existing) {
                                updated = currentItems.map(i => i.maMon === item.maMon ? { ...i, soLuong: i.soLuong + 1 } : i);
                            } else {
                                updated = [...currentItems, { ...item, soLuong: 1 }];
                            }
                            handleUpdateItems(updated);
                        }}
                        className="p-4 border-b border-gray-50 flex-row"
                    >
                        <Image source={{ uri: item.hinhAnh || 'https://via.placeholder.com/100' }} className="w-16 h-16 rounded-xl mr-4" />
                        <StyledView className="flex-1 justify-center">
                            <StyledText className="font-bold text-gray-800">{item.tenMon}</StyledText>
                            <StyledText className="text-red-900 font-bold">{item.donGia?.toLocaleString()}đ</StyledText>
                        </StyledView>
                        <Ionicons name="add-circle" size={32} color="#800000" />
                    </StyledTouchableOpacity>
                )}
              />
          </StyledSafeAreaView>
      </Modal>

      {/* Table Modal (Simplified) */}
      <Modal visible={showTableModal} animationType="slide">
          <StyledSafeAreaView className="flex-1 bg-white">
              <StyledView className="p-6 flex-row justify-between items-center border-b border-gray-100">
                  <StyledText className="text-2xl font-bold text-red-900">Chọn bàn mới</StyledText>
                  <StyledTouchableOpacity onPress={() => setShowTableModal(false)}>
                      <Ionicons name="close" size={32} color="gray" />
                  </StyledTouchableOpacity>
              </StyledView>
              <FlatList 
                data={availableTables}
                keyExtractor={item => item.maBan}
                numColumns={3}
                renderItem={({ item }) => (
                    <StyledTouchableOpacity 
                        onPress={() => handleChangeTable(item.maBan)}
                        className="m-2 flex-1 aspect-square bg-gray-50 rounded-2xl justify-center items-center border border-gray-100"
                    >
                        <Ionicons name="restaurant" size={24} color="#800000" />
                        <StyledText className="font-bold text-red-900 mt-1">{item.maBan}</StyledText>
                    </StyledTouchableOpacity>
                )}
              />
          </StyledSafeAreaView>
      </Modal>

      {updating && (
          <StyledView className="absolute inset-0 bg-black/20 justify-center items-center">
              <ActivityIndicator size="large" color="white" />
          </StyledView>
      )}
    </StyledSafeAreaView>
  );
};

export default BookingDetailScreen;
