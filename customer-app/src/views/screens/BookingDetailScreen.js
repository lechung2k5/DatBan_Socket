import React, { useState, useEffect, useCallback, useMemo } from 'react';
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
  TextInput,
  StatusBar
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

// --- Sub-components for better performance ---

const OrderItem = React.memo(({ item }) => {
    if (!item) return null;
    return (
        <StyledView className="bg-white rounded-2xl p-4 mb-3 flex-row justify-between items-center shadow-sm border border-gray-100">
            <StyledView className="flex-1">
                <StyledText className="font-bold text-gray-800 text-base">{item.tenMon || 'Món ăn'}</StyledText>
                <StyledText className="text-gray-400 text-xs">Đơn giá: {(item.donGia || 0).toLocaleString()}đ</StyledText>
            </StyledView>
            <StyledView className="items-end">
                <StyledText className="font-bold text-red-900">x{item.soLuong || 0}</StyledText>
                <StyledText className="font-bold text-gray-800">{( (item.soLuong || 0) * (item.donGia || 0) ).toLocaleString()}đ</StyledText>
            </StyledView>
        </StyledView>
    );
});

const BookingDetailScreen = ({ route, navigation }) => {
  const { maHD } = route.params;
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  
  // Modals state
  const [showMenuModal, setShowMenuModal] = useState(false);
  const [showTableModal, setShowTableModal] = useState(false);
  const [availableTables, setAvailableTables] = useState([]);
  const [menuItems, setMenuItems] = useState([]);
  
  // Temporary cart for adding items
  const [tempCart, setTempCart] = useState([]);

  const fetchDetail = useCallback(async (showLoading = true) => {
    try {
      if (showLoading) setLoading(true);
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
      if (navigation.canGoBack()) {
        navigation.goBack();
      } else {
        navigation.navigate('Trang chủ');
      }
    } finally {
      if (showLoading) setLoading(false);
    }
  }, [maHD, navigation]);

  const fetchMenuData = async () => {
    try {
      const menuRes = await ApiService.getMenu();
      if (menuRes.statusCode === 200) setMenuItems(menuRes.data);
    } catch (err) {}
  };

  useEffect(() => {
    fetchDetail();
    fetchMenuData();
    
    // 🔥 Lắng nghe Socket để cập nhật real-time thay vì Polling
    const socketHandler = (event) => {
        if (!event) return; // Phòng vệ rỗng
        // Hỗ trợ cả format từ Java (event.CommandType) và Node.js (event.action hoặc CommandType lồng trong data)
        const cmd = event.CommandType || event.action;
        if (cmd === 'UPDATE_INVOICE' && (event.affectedId === maHD || (event.data && event.data.maHD === maHD))) {
            fetchDetail(false); // Cập nhật ngầm, không hiện loading
        }
    };

    SocketService.addListener(socketHandler);

    return () => {
        SocketService.removeListener(socketHandler);
    };
  }, [maHD, fetchDetail]);

  const canEdit = useMemo(() => 
    booking && booking.trangThai !== 'DangSuDung' && booking.trangThai !== 'DaThanhToan'
  , [booking]);

  const handleConfirmAddItems = async () => {
    if (tempCart.length === 0) {
        setShowMenuModal(false);
        return;
    }
    
    try {
      setUpdating(true);
      // Kết hợp món cũ và món mới
      const currentItems = booking.chiTietHoaDon || [];
      let updated = [...currentItems];
      
      tempCart.forEach(newItem => {
          const existing = updated.find(i => i.maMon === newItem.maMon);
          if (existing) {
              existing.soLuong += newItem.soLuong;
          } else {
              updated.push(newItem);
          }
      });

      const res = await ApiService.updateInvoice({
        maHD: maHD,
        itemsJson: JSON.stringify(updated),
        source: 'mobile'
      });
      
      if (res.statusCode === 200) {
        Alert.alert('Thành công', 'Đã cập nhật danh sách món ăn');
        setTempCart([]);
        setShowMenuModal(false);
        fetchDetail(false);
      }
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể cập nhật món ăn');
    } finally {
      setUpdating(false);
    }
  };

  const handleChangeTable = async (newTableId) => {
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
        fetchDetail(false);
      }
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể đổi bàn');
    } finally {
      setUpdating(false);
    }
  };

  const handleCancelBooking = () => {
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

  const openTablePicker = async () => {
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

  const addToTempCart = (item) => {
      setTempCart(prev => {
          const existing = prev.find(i => i.maMon === item.maMon);
          if (existing) {
              return prev.map(i => i.maMon === item.maMon ? { ...i, soLuong: i.soLuong + 1 } : i);
          }
          return [...prev, { ...item, donGia: item.donGia || item.giaBan || 0, soLuong: 1 }];
      });
  };

  if (loading || !booking) {
    return (
      <StyledView className="flex-1 justify-center items-center bg-white">
        <ActivityIndicator size="large" color="#800000" />
      </StyledView>
    );
  }

  const getStatusColor = (status) => {
      switch(status) {
          case 'ChoXacNhan': return '#D35400';
          case 'Dat': return '#27AE60';
          case 'DangSuDung': return '#2980B9';
          case 'DaHuy': return '#C0392B';
          default: return '#2C3E50';
      }
  };

  const getStatusLabel = (status) => {
    switch(status) {
        case 'ChoXacNhan': return 'CHỜ XÁC NHẬN';
        case 'Dat': return 'ĐÃ XÁC NHẬN';
        case 'DangSuDung': return 'ĐANG PHỤC VỤ';
        case 'DaThanhToan': return 'ĐÃ THANH TOÁN';
        case 'DaHuy': return 'ĐÃ HỦY';
        default: return status;
    }
  };

  return (
    <StyledSafeAreaView className="flex-1 bg-gray-50">
      <StatusBar barStyle="dark-content" />
      
      {/* Header */}
      <StyledView className="px-6 py-4 bg-white flex-row items-center justify-between border-b border-gray-100">
        <StyledTouchableOpacity onPress={() => navigation.goBack()} className="w-10 h-10 items-center justify-center rounded-full bg-gray-50">
          <Ionicons name="chevron-back" size={24} color="#800000" />
        </StyledTouchableOpacity>
        <StyledText className="text-lg font-bold text-red-900">Chi tiết đơn đặt</StyledText>
        <StyledView className="w-10" />
      </StyledView>

      <StyledScrollView showsVerticalScrollIndicator={false} className="flex-1 px-5 pt-4">
        {/* Main Info Card */}
        <StyledView className="bg-white rounded-[32px] p-6 mb-5 shadow-sm border border-gray-100 overflow-hidden">
            <StyledView className="flex-row justify-between items-start mb-6">
                <StyledView>
                    <StyledText className="text-gray-400 text-[10px] font-bold tracking-widest uppercase mb-1">Mã hóa đơn</StyledText>
                    <StyledText className="text-2xl font-black text-gray-800">{booking.maHD}</StyledText>
                </StyledView>
                <StyledView className="px-4 py-1.5 rounded-full" style={{ backgroundColor: getStatusColor(booking.trangThai) + '15' }}>
                    <StyledText className="font-bold text-[10px]" style={{ color: getStatusColor(booking.trangThai) }}>
                        {getStatusLabel(booking.trangThai)}
                    </StyledText>
                </StyledView>
            </StyledView>

            <StyledView className="flex-row justify-between items-center mb-6">
                <StyledView className="flex-row items-center">
                    <StyledView className="w-12 h-12 bg-red-50 rounded-2xl items-center justify-center mr-3">
                        <MaterialCommunityIcons name="silverware-fork-knife" size={24} color="#800000" />
                    </StyledView>
                    <StyledView>
                        <StyledText className="text-gray-400 text-[10px] font-bold uppercase">Bàn phục vụ</StyledText>
                        <StyledText className="text-lg font-black text-red-900">
                            {booking.ban?.maBan || booking.maBan || booking.tableId || 'N/A'}
                        </StyledText>
                    </StyledView>
                </StyledView>
                <StyledView className="items-end">
                    <StyledText className="text-gray-400 text-[10px] font-bold uppercase">Số khách</StyledText>
                    <StyledText className="text-lg font-black text-gray-800">{booking.soKhach || 0} người</StyledText>
                </StyledView>
            </StyledView>

            <StyledView className="flex-row bg-gray-50 rounded-2xl p-4 justify-between">
                <StyledView className="items-center flex-1 border-r border-gray-200">
                    <StyledText className="text-[9px] text-gray-400 font-bold uppercase mb-1">Giờ vào</StyledText>
                    <StyledText className="text-sm font-bold text-gray-700">
                        {booking.gioVao && typeof booking.gioVao === 'string' ? booking.gioVao.split('T')[1]?.substring(0, 5) : '--:--'}
                    </StyledText>
                </StyledView>
                <StyledView className="items-center flex-1">
                    <StyledText className="text-[9px] text-gray-400 font-bold uppercase mb-1">Ngày đặt</StyledText>
                    <StyledText className="text-sm font-bold text-gray-700">
                         {booking.gioVao && typeof booking.gioVao === 'string' ? booking.gioVao.split('T')[0].split('-').reverse().join('/') : '--/--/----'}
                    </StyledText>
                </StyledView>
            </StyledView>
        </StyledView>

        {/* Quick Actions */}
        {canEdit ? (
            <StyledView className="flex-row mb-6">
                <StyledTouchableOpacity 
                    onPress={openTablePicker}
                    className="bg-white p-4 rounded-2xl flex-1 mr-2 flex-row items-center justify-center border border-gray-100 shadow-sm"
                >
                    <Ionicons name="swap-horizontal" size={18} color="#800000" />
                    <StyledText className="text-red-900 font-bold ml-2 text-xs">Đổi bàn</StyledText>
                </StyledTouchableOpacity>
                <StyledTouchableOpacity 
                    onPress={() => setShowMenuModal(true)}
                    className="bg-red-900 p-4 rounded-2xl flex-1 mx-2 flex-row items-center justify-center shadow-md"
                >
                    <Ionicons name="add" size={18} color="white" />
                    <StyledText className="text-white font-bold ml-2 text-xs">Thêm món</StyledText>
                </StyledTouchableOpacity>
                <StyledTouchableOpacity 
                    onPress={handleCancelBooking}
                    className="bg-white p-4 rounded-2xl flex-1 ml-2 flex-row items-center justify-center border border-gray-100 shadow-sm"
                >
                    <Ionicons name="close-circle-outline" size={18} color="#C0392B" />
                    <StyledText className="text-red-600 font-bold ml-2 text-xs">Hủy</StyledText>
                </StyledTouchableOpacity>
            </StyledView>
        ) : (
            <StyledView className="bg-amber-50 p-4 rounded-2xl mb-6 border border-amber-100 flex-row items-center">
                <Ionicons name="information-circle" size={20} color="#D35400" />
                <StyledText className="text-amber-800 ml-2 font-bold text-[10px]">
                    Đơn hàng đã được khóa để phục vụ hoặc thanh toán.
                </StyledText>
            </StyledView>
        )}

        {/* Items List */}
        <StyledView className="flex-row justify-between items-center mb-4">
            <StyledText className="text-lg font-black text-gray-800">Danh sách món ăn</StyledText>
            <StyledView className="bg-red-100 px-2 py-0.5 rounded-md">
                <StyledText className="text-red-900 font-bold text-[10px]">{booking.chiTietHoaDon?.length || 0} món</StyledText>
            </StyledView>
        </StyledView>

        {booking.chiTietHoaDon?.length > 0 ? (
            booking.chiTietHoaDon.map((item, index) => (
                <OrderItem key={`${item.maMon}-${index}`} item={item} />
            ))
        ) : (
            <StyledView className="bg-white rounded-3xl p-10 items-center border border-dashed border-gray-200">
                <Ionicons name="restaurant-outline" size={40} color="#EEE" />
                <StyledText className="text-gray-300 mt-2 font-bold">Chưa chọn món ăn</StyledText>
            </StyledView>
        )}

        {/* Billing Summary */}
        <StyledView className="bg-white rounded-[32px] p-6 mt-4 mb-10 shadow-lg border border-gray-100">
            <StyledText className="text-base font-black mb-4 text-gray-800">Tổng kết thanh toán</StyledText>
            
            <StyledView className="space-y-3">
                <StyledView className="flex-row justify-between">
                    <StyledText className="text-gray-400 text-xs">Tiền món ăn</StyledText>
                    <StyledText className="text-gray-700 font-bold text-xs">{booking.tongCongMonAn?.toLocaleString()}đ</StyledText>
                </StyledView>
                <StyledView className="flex-row justify-between">
                    <StyledText className="text-gray-400 text-xs">Phí dịch vụ (5%)</StyledText>
                    <StyledText className="text-gray-700 font-bold text-xs">{booking.phiDichVu?.toLocaleString()}đ</StyledText>
                </StyledView>
                <StyledView className="flex-row justify-between">
                    <StyledText className="text-gray-400 text-xs">Thuế VAT (8%)</StyledText>
                    <StyledText className="text-gray-700 font-bold text-xs">{booking.thueVAT?.toLocaleString()}đ</StyledText>
                </StyledView>
                {booking.tienCoc > 0 && (
                    <StyledView className="flex-row justify-between">
                        <StyledText className="text-blue-500 text-xs">Đã đặt cọc</StyledText>
                        <StyledText className="text-blue-600 font-bold text-xs">-{booking.tienCoc?.toLocaleString()}đ</StyledText>
                    </StyledView>
                )}
                {booking.khuyenMai > 0 && (
                    <StyledView className="flex-row justify-between">
                        <StyledText className="text-green-500 text-xs">Ưu đãi</StyledText>
                        <StyledText className="text-green-600 font-bold text-xs">-{booking.khuyenMai?.toLocaleString()}đ</StyledText>
                    </StyledView>
                )}
            </StyledView>

            <StyledView className="h-[1px] bg-gray-100 my-5" />

            <StyledView className="flex-row justify-between items-center">
                <StyledView>
                    <StyledText className="text-[10px] text-gray-400 font-bold uppercase">Cần thanh toán</StyledText>
                    <StyledText className="text-2xl font-black text-red-900">{(booking.tongTienThanhToan || 0).toLocaleString()}đ</StyledText>
                </StyledView>
                <StyledView className="w-12 h-12 bg-green-50 rounded-full items-center justify-center">
                    <Ionicons name="checkmark-shield" size={24} color="#27AE60" />
                </StyledView>
            </StyledView>
        </StyledView>

        <StyledView className="h-10" />
      </StyledScrollView>

      {/* Modern Menu Modal */}
      <Modal visible={showMenuModal} animationType="slide" transparent={true}>
          <StyledView className="flex-1 bg-black/60 justify-end">
              <StyledView className="bg-white rounded-t-[40px] h-[85%] px-6 pt-8">
                  <StyledView className="flex-row justify-between items-center mb-6">
                      <StyledView>
                        <StyledText className="text-2xl font-black text-gray-800">Thêm món ăn</StyledText>
                        <StyledText className="text-gray-400 text-xs">Chọn các món bạn muốn dùng thêm</StyledText>
                      </StyledView>
                      <StyledTouchableOpacity 
                        onPress={() => { setShowMenuModal(false); setTempCart([]); }}
                        className="w-10 h-10 bg-gray-100 rounded-full items-center justify-center"
                      >
                          <Ionicons name="close" size={24} color="gray" />
                      </StyledTouchableOpacity>
                  </StyledView>

                  <FlatList 
                    data={menuItems}
                    keyExtractor={item => item.maMon}
                    showsVerticalScrollIndicator={false}
                    className="flex-1"
                    renderItem={({ item }) => {
                        const inCart = tempCart.find(i => i.maMon === item.maMon);
                        return (
                            <StyledView className="flex-row items-center mb-4 bg-gray-50 p-3 rounded-3xl border border-gray-100">
                                <Image 
                                    source={{ uri: item.hinhAnhUrl || 'https://via.placeholder.com/100' }} 
                                    className="w-20 h-20 rounded-2xl" 
                                />
                                <StyledView className="flex-1 px-4">
                                    <StyledText className="font-bold text-gray-800 text-base" numberOfLines={1}>{item.tenMon}</StyledText>
                                    <StyledText className="text-red-900 font-black mt-1">{(item.donGia || item.giaBan || 0).toLocaleString()}đ</StyledText>
                                </StyledView>
                                <StyledView className="flex-row items-center">
                                    {inCart && (
                                        <StyledText className="mr-3 font-bold text-red-900">x{inCart.soLuong}</StyledText>
                                    )}
                                    <StyledTouchableOpacity 
                                        onPress={() => addToTempCart(item)}
                                        className="w-10 h-10 bg-red-900 rounded-2xl items-center justify-center shadow-sm"
                                    >
                                        <Ionicons name="add" size={24} color="white" />
                                    </StyledTouchableOpacity>
                                </StyledView>
                            </StyledView>
                        );
                    }}
                  />

                  {/* Confirm Section */}
                  <StyledView className="py-6 border-t border-gray-100 flex-row items-center justify-between">
                      <StyledView>
                          <StyledText className="text-gray-400 text-xs font-bold uppercase">Món mới chọn</StyledText>
                          <StyledText className="text-lg font-black text-gray-800">{tempCart.reduce((sum, i) => sum + i.soLuong, 0)} món</StyledText>
                      </StyledView>
                      <StyledTouchableOpacity 
                        onPress={handleConfirmAddItems}
                        className="bg-red-900 px-8 py-4 rounded-2xl shadow-lg"
                      >
                          <StyledText className="text-white font-black">Xác nhận thêm</StyledText>
                      </StyledTouchableOpacity>
                  </StyledView>
              </StyledView>
          </StyledView>
      </Modal>

      {/* Table Picker Modal */}
      <Modal visible={showTableModal} animationType="fade" transparent={true}>
          <StyledView className="flex-1 bg-black/50 justify-center px-6">
              <StyledView className="bg-white rounded-[40px] p-6 max-h-[70%]">
                  <StyledView className="flex-row justify-between items-center mb-6">
                      <StyledText className="text-xl font-black text-gray-800">Chọn bàn trống</StyledText>
                      <StyledTouchableOpacity onPress={() => setShowTableModal(false)}>
                          <Ionicons name="close" size={24} color="gray" />
                      </StyledTouchableOpacity>
                  </StyledView>
                  <FlatList 
                    data={availableTables}
                    keyExtractor={item => item.maBan}
                    numColumns={3}
                    renderItem={({ item }) => (
                        <StyledTouchableOpacity 
                            onPress={() => handleChangeTable(item.maBan)}
                            className="m-2 flex-1 aspect-square bg-red-50 rounded-2xl justify-center items-center border border-red-100"
                        >
                            <MaterialCommunityIcons name="table-chair" size={28} color="#800000" />
                            <StyledText className="font-black text-red-900 mt-1">{item.maBan}</StyledText>
                        </StyledTouchableOpacity>
                    )}
                  />
              </StyledView>
          </StyledView>
      </Modal>

      {updating && (
          <StyledView className="absolute inset-0 bg-black/30 justify-center items-center">
              <StyledView className="bg-white p-6 rounded-3xl shadow-xl">
                <ActivityIndicator size="large" color="#800000" />
                <StyledText className="mt-3 font-bold text-gray-600">Đang cập nhật...</StyledText>
              </StyledView>
          </StyledView>
      )}
    </StyledSafeAreaView>
  );
};

export default BookingDetailScreen;
