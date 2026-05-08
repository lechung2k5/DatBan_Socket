import React, { useState, useEffect } from 'react';
import { 
  ScrollView, 
  View, 
  Text, 
  SafeAreaView, 
  TouchableOpacity,
  TextInput,
  Dimensions,
  Image,
  Alert,
  Modal,
  FlatList,
  ActivityIndicator
} from 'react-native';
import { styled } from 'nativewind';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { COLORS } from '../../theme/colors';
import DateTimePicker from '@react-native-community/datetimepicker';
import ApiService from '../../services/ApiService';
import SocketService from '../../services/SocketService';
import { BookingModel } from '../../models/BookingModel';

const StyledSafeAreaView = styled(SafeAreaView);
const StyledScrollView = styled(ScrollView);
const StyledView = styled(View);
const StyledText = styled(Text);
const StyledTouchableOpacity = styled(TouchableOpacity);
const StyledTextInput = styled(TextInput);

const BookingScreen = () => {
  const [paymentModalVisible, setPaymentModalVisible] = useState(false);
  const [currentInvoice, setCurrentInvoice] = useState(null);
  const [selectedZone, setSelectedZone] = useState('Tầng trệt');
  const [guestCount, setGuestCount] = useState(4);
  const [bookingDate, setBookingDate] = useState(new Date());
  const [bookingTime, setBookingTime] = useState('18:00');
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [ghiChu, setGhiChu] = useState('');
  const [availableTables, setAvailableTables] = useState([]);
  const [showTableModal, setShowTableModal] = useState(false);
  const [selectedTable, setSelectedTable] = useState(null);
  const [loading, setLoading] = useState(false);

  // New fields for name, phone, and menu
  const [customerName, setCustomerName] = useState(
    SocketService.userProfile?.name || SocketService.userProfile?.tenKH || ''
  );
  const [customerPhone, setCustomerPhone] = useState(
    SocketService.userProfile?.phone || SocketService.userProfile?.soDT || ''
  );
  const [menuItems, setMenuItems] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('CAT001');
  const [showMenuModal, setShowMenuModal] = useState(false);
  const [selectedDishes, setSelectedDishes] = useState([]); // Array of { ...item, soLuong: 1 }

  useEffect(() => {
    fetchMenuData();
  }, []);

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
    } catch (err) {
      console.error('Lỗi tải thực đơn:', err);
    }
  };

  const zones = [
    { name: 'Tầng trệt', icon: 'stairs' },
    { name: 'Tầng 1', icon: 'office-building' },
    { name: 'Phòng riêng', icon: 'door-open' }
  ];

  const timeSlots = ['18:00', '18:30', '19:00', '19:30', '20:00', '20:30', '21:00'];

  const onDateChange = (event, selectedDate) => {
    setShowDatePicker(false);
    if (selectedDate) {
      setBookingDate(selectedDate);
    }
  };

  const handleSearchTables = async () => {
    // Fallback if state is empty but profile has it
    const phone = customerPhone || SocketService.userProfile?.phone || SocketService.userProfile?.soDT;
    const name = customerName || SocketService.userProfile?.name || SocketService.userProfile?.tenKH;

    if (!phone || phone.length < 10) {
        Alert.alert('Thông báo', 'Hệ thống chưa có số điện thoại của bạn. Vui lòng cập nhật trong mục Cá nhân.');
        return;
    }
    if (!name) {
        Alert.alert('Thông báo', 'Hệ thống chưa có tên của bạn. Vui lòng cập nhật trong mục Cá nhân.');
        return;
    }
    try {
      setLoading(true);
      const res = await ApiService.getTables();
      if (res.statusCode === 200) {
        const filtered = res.data.filter(table => 
          table.trangThai === 'Trong' && 
          table.sucChua >= guestCount &&
          (selectedZone === 'Tất cả' || table.viTri === selectedZone)
        );
        setAvailableTables(filtered);
        setShowTableModal(true);
      }
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể tìm thấy danh sách bàn trống');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = (dish) => {
    setSelectedDishes(prev => {
        const existing = prev.find(d => d.maMon === dish.maMon);
        if (existing) {
            return prev.map(d => d.maMon === dish.maMon ? { ...d, soLuong: d.soLuong + 1 } : d);
        }
        return [...prev, { ...dish, soLuong: 1 }];
    });
  };

  const handleUpdateQuantity = (maMon, delta) => {
    setSelectedDishes(prev => {
        return prev.map(d => {
            if (d.maMon === maMon) {
                const newQty = Math.max(0, d.soLuong + delta);
                return { ...d, soLuong: newQty };
            }
            return d;
        }).filter(d => d.soLuong > 0);
    });
  };
  const getDepositAmount = (table) => {
    if (!table) return 0;
    const COC_MAC_DINH = 150000;
    const PHI_PHONG_RIENG = 100000;
    
    let total = COC_MAC_DINH;
    // Kiểm tra nếu là phòng riêng (maLoaiBan === 'PHONG')
    if (table.maLoaiBan === 'PHONG' || table.viTri === 'Phòng riêng') {
        total += PHI_PHONG_RIENG;
    }
    return total;
  };

  const handleCreateRequest = async () => {
    if (!selectedTable) {
      Alert.alert('Thông báo', 'Vui lòng chọn bàn bạn muốn đặt');
      return;
    }
    if (selectedDishes.length === 0) {
        Alert.alert('Thông báo', 'Vui lòng chọn ít nhất một món ăn');
        setShowMenuModal(true);
        return;
    }

    try {
      setLoading(true);
      const booking = new BookingModel({
        customerId: SocketService.token || 'GUEST',
        customerPhone: customerPhone,
        customerName: customerName,
        maBan: selectedTable.maBan,
        ngayDat: bookingDate.toISOString().split('T')[0],
        gioDat: bookingTime,
        soKhach: guestCount,
        ghiChu: ghiChu,
        items: selectedDishes.map(d => ({
            maMon: d.maMon,
            tenMon: d.tenMon,
            soLuong: d.soLuong,
            donGia: d.giaBan,
            thanhTien: d.giaBan * d.soLuong
        }))
      });

      const res = await SocketService.request('CREATE_ORDER', booking.toJavaData(getDepositAmount(selectedTable)));

      if (res.statusCode === 200) {
        // Lấy mã HD trực tiếp từ phản hồi (data là maHD)
        const maHD = res.data;
        
        // Cập nhật currentInvoice với các thông tin tối thiểu cần thiết cho PaymentModal
        setCurrentInvoice({
            maHD: maHD,
            maBan: selectedTable.maBan,
            tienCoc: getDepositAmount(selectedTable)
        });
        
        setShowTableModal(false);
        setPaymentModalVisible(true);
      }
    } catch (err) {
      console.error(err);
      Alert.alert('Lỗi', 'Không thể tạo yêu cầu đặt bàn: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const resetForm = () => {
    setSelectedTable(null);
    setSelectedDishes([]);
    setGhiChu('');
    setCurrentInvoice(null);
    setAvailableTables([]);
  };

  const handleConfirmPayment = async () => {
    if (!currentInvoice) return;

    try {
      setLoading(true);
      const res = await SocketService.request('CONFIRM_DEPOSIT', {
          maHD: currentInvoice.maHD,
          maBan: currentInvoice.maBan
      });

      if (res.statusCode === 200) {
        setPaymentModalVisible(false);
        
        Alert.alert('Thành công', 'Đặt bàn của bạn đã được xác nhận!', [
          { text: 'OK', onPress: () => resetForm() }
        ]);
      }
    } catch (err) {
      Alert.alert('Lỗi', 'Không thể xác nhận thanh toán');
    } finally {
      setLoading(false);
    }
  };

  // VietQR generation URL
  const qrUrl = (currentInvoice && selectedTable) 
    ? `https://img.vietqr.io/image/MB-0968686868-compact2.png?amount=${getDepositAmount(selectedTable)}&addInfo=DATBAN%20${currentInvoice.invoiceId}&accountName=QUAN%20AN%20ANTIGRAVITY` 
    : '';

  return (
    <StyledSafeAreaView className="flex-1 bg-white">
      <StyledScrollView showsVerticalScrollIndicator={false} className="px-6 pt-4">
        
        <StyledView className="mb-8">
           <StyledText className="text-4xl font-bold" style={{ color: COLORS.primary }}>
             Chào {SocketService.userProfile?.name || 'bạn'},
           </StyledText>
           <StyledText className="text-gray-500 text-base mt-2 leading-6">
              Hãy chọn thời gian và món ăn yêu thích của bạn.
           </StyledText>
        </StyledView>

        {/* Section: Khu vực */}
        <StyledView className="mb-8">
           <StyledView className="flex-row items-center mb-4">
              <Ionicons name="home-outline" size={22} color={COLORS.primary} />
              <StyledText className="text-2xl font-bold ml-3 text-gray-800">Khu Vực Phục Vụ</StyledText>
           </StyledView>
           
           <StyledView className="flex-row justify-between">
              {zones.map((zone) => (
                <StyledTouchableOpacity 
                  key={zone.name}
                  onPress={() => setSelectedZone(zone.name)}
                  className={`w-[30%] aspect-square rounded-[24px] items-center justify-center border-2 ${selectedZone === zone.name ? 'bg-orange-50' : 'bg-gray-50 border-gray-50'}`}
                  style={{ borderColor: selectedZone === zone.name ? COLORS.accent : '#F9F9F9' }}
                >
                   <MaterialCommunityIcons 
                    name={zone.icon} 
                    size={32} 
                    color={selectedZone === zone.name ? COLORS.text : '#AAA'} 
                   />
                   <StyledText className={`text-sm font-bold mt-2 text-center ${selectedZone === zone.name ? 'text-gray-800' : 'text-gray-400'}`}>
                      {zone.name}
                   </StyledText>
                </StyledTouchableOpacity>
              ))}
           </StyledView>
        </StyledView>

        {/* Section: Thời gian */}
        <StyledView className="mb-8">
           <StyledView className="flex-row items-center mb-4">
              <Ionicons name="calendar-outline" size={22} color={COLORS.primary} />
              <StyledText className="text-2xl font-bold ml-3 text-gray-800">Thời Gian</StyledText>
           </StyledView>

           <StyledText className="text-sm font-bold text-gray-500 mb-2 ml-1">Ngày đến</StyledText>
           <StyledTouchableOpacity 
              onPress={() => setShowDatePicker(true)}
              className="bg-gray-50 rounded-2xl p-4 flex-row justify-between items-center mb-4 border border-gray-100"
            >
               <StyledView className="flex-row items-center">
                  <Ionicons name="calendar" size={20} color={COLORS.text} />
                  <StyledText className="text-lg font-bold ml-3 text-gray-800">
                    {bookingDate.toLocaleDateString('vi-VN')}
                  </StyledText>
               </StyledView>
               <Ionicons name="calendar-outline" size={20} color="#AAA" />
            </StyledTouchableOpacity>

            {showDatePicker && (
              <DateTimePicker
                value={bookingDate}
                mode="date"
                display="default"
                onChange={onDateChange}
                minimumDate={new Date()}
              />
            )}

           <StyledText className="text-sm font-bold text-gray-500 mb-2 ml-1">Giờ đến</StyledText>
           <StyledView className="flex-row flex-wrap justify-between">
              {timeSlots.map((time) => (
                <StyledTouchableOpacity 
                  key={time}
                  onPress={() => setBookingTime(time)}
                  className={`w-[31%] py-3 rounded-xl mb-3 border ${bookingTime === time ? 'bg-orange-50 border-orange-200' : 'bg-white border-gray-100'}`}
                >
                   <StyledText className={`text-center font-bold text-base ${bookingTime === time ? 'text-gray-800' : 'text-gray-400'}`}>
                      {time}
                   </StyledText>
                </StyledTouchableOpacity>
              ))}
           </StyledView>
        </StyledView>

        {/* Section: Số khách */}
        <StyledView className="mb-8">
           <StyledView className="flex-row items-center mb-4">
              <Ionicons name="people-outline" size={24} color={COLORS.primary} />
              <StyledText className="text-2xl font-bold ml-3 text-gray-800">Số Khách</StyledText>
           </StyledView>
           <StyledView className="bg-gray-50 rounded-[24px] p-4 flex-row items-center justify-between border border-gray-100">
              <StyledTouchableOpacity 
                onPress={() => setGuestCount(Math.max(1, guestCount - 1))}
                className="w-14 h-14 rounded-2xl bg-white shadow-sm items-center justify-center"
              >
                 <Ionicons name="remove" size={28} color={COLORS.text} />
              </StyledTouchableOpacity>
              <StyledText className="text-4xl font-bold text-gray-800">{guestCount}</StyledText>
              <StyledTouchableOpacity 
                onPress={() => setGuestCount(guestCount + 1)}
                className="w-14 h-14 rounded-2xl bg-white shadow-sm items-center justify-center"
              >
                 <Ionicons name="add" size={28} color={COLORS.text} />
              </StyledTouchableOpacity>
           </StyledView>
        </StyledView>

        {/* Section: Yêu cầu đặc biệt */}
        <StyledView className="mb-8">
           <StyledView className="flex-row items-center mb-4">
              <Ionicons name="create-outline" size={22} color={COLORS.primary} />
              <StyledText className="text-2xl font-bold ml-3 text-gray-800">Yêu Cầu Đặc Biệt</StyledText>
           </StyledView>
           <StyledView className="bg-gray-50 rounded-[24px] p-6 border border-gray-100">
              <StyledTextInput 
                placeholder="Ví dụ: Ghế em bé, Dị ứng hải sản, Kỷ niệm ngày cưới..."
                multiline
                numberOfLines={2}
                className="text-base text-gray-600 leading-6"
                placeholderTextColor="#AAA"
                value={ghiChu}
                onChangeText={setGhiChu}
              />
           </StyledView>
        </StyledView>

        {/* Section: Thực đơn */}
        <StyledView className="mb-8">
           <StyledView className="flex-row items-center justify-between mb-4">
              <StyledView className="flex-row items-center">
                <Ionicons name="restaurant-outline" size={22} color={COLORS.primary} />
                <StyledText className="text-2xl font-bold ml-3 text-gray-800">Thực Đơn</StyledText>
              </StyledView>
              <StyledTouchableOpacity onPress={() => setShowMenuModal(true)}>
                 <StyledText className="text-blue-600 font-bold">Chọn món {selectedDishes.length > 0 ? `(${selectedDishes.length})` : ''}</StyledText>
              </StyledTouchableOpacity>
           </StyledView>
           {selectedDishes.length > 0 ? (
             <StyledView className="bg-gray-50 rounded-3xl p-4 border border-gray-100">
               {selectedDishes.map((item) => (
                 <StyledView key={item.maMon} className="flex-row justify-between items-center mb-2">
                    <StyledText className="text-gray-700 flex-1">{item.tenMon}</StyledText>
                    <StyledText className="text-gray-400 mx-2">x{item.soLuong}</StyledText>
                    <StyledText className="font-bold text-gray-800">{(item.giaBan * item.soLuong).toLocaleString()}đ</StyledText>
                 </StyledView>
               ))}
               <StyledView className="border-t border-gray-200 mt-2 pt-2 flex-row justify-between">
                  <StyledText className="font-bold">Tổng cộng:</StyledText>
                  <StyledText className="font-bold text-red-700">
                    {selectedDishes.reduce((sum, d) => sum + (d.giaBan * d.soLuong), 0).toLocaleString()}đ
                  </StyledText>
               </StyledView>
             </StyledView>
           ) : (
             <StyledTouchableOpacity onPress={() => setShowMenuModal(true)} className="bg-gray-50 rounded-3xl p-6 border-2 border-dashed border-gray-200 items-center">
                <Ionicons name="add-circle-outline" size={32} color="#AAA" />
                <StyledText className="text-gray-400 mt-2">Bấm để chọn món ăn trước</StyledText>
             </StyledTouchableOpacity>
           )}
        </StyledView>

        {/* Button */}
        <StyledTouchableOpacity 
          onPress={handleSearchTables}
          disabled={loading}
          className="bg-red-900 w-full py-5 rounded-2xl flex-row items-center justify-center shadow-lg shadow-red-900/40"
        >
          {loading ? (
            <ActivityIndicator color="white" />
          ) : (
            <>
              <Ionicons name="search" size={24} color="white" />
              <StyledText className="text-white text-lg font-bold ml-2">Tìm bàn trống</StyledText>
            </>
          )}
        </StyledTouchableOpacity>

        <StyledView className="h-20" />
      </StyledScrollView>

      {/* Modal: Chọn bàn */}
      <Modal visible={showTableModal} animationType="slide" transparent>
        <StyledView className="flex-1 bg-black/50 justify-end">
          <StyledView className="bg-white h-[80%] rounded-t-[40px] p-6">
            <StyledView className="flex-row justify-between items-center mb-6">
               <StyledText className="text-2xl font-bold text-gray-800">Chọn bàn trống</StyledText>
               <StyledTouchableOpacity onPress={() => setShowTableModal(false)}>
                  <Ionicons name="close" size={28} color="#666" />
               </StyledTouchableOpacity>
            </StyledView>

            <FlatList 
              data={availableTables}
              keyExtractor={(item) => item.maBan}
              renderItem={({item}) => (
                <StyledTouchableOpacity 
                  onPress={() => setSelectedTable(item)}
                  className={`p-4 rounded-2xl mb-4 border-2 flex-row justify-between items-center ${selectedTable?.maBan === item.maBan ? 'bg-orange-50 border-orange-400' : 'bg-gray-50 border-gray-100'}`}
                >
                  <StyledView className="flex-row items-center">
                    <StyledView className="w-12 h-12 rounded-xl bg-white items-center justify-center mr-4">
                       <MaterialCommunityIcons name="table-chair" size={24} color={COLORS.primary} />
                    </StyledView>
                    <StyledView>
                       <StyledText className="text-lg font-bold text-gray-800">{item.maBan}</StyledText>
                       <StyledText className="text-gray-500 text-sm">{item.viTri} • {item.sucChua} chỗ</StyledText>
                    </StyledView>
                  </StyledView>
                  {selectedTable?.maBan === item.maBan && (
                    <Ionicons name="checkmark-circle" size={24} color={COLORS.primary} />
                  )}
                </StyledTouchableOpacity>
              )}
              ListEmptyComponent={() => (
                <StyledView className="items-center py-10">
                   <Ionicons name="alert-circle-outline" size={60} color="#DDD" />
                   <StyledText className="text-gray-400 mt-4 text-center">Rất tiếc, không còn bàn nào trống phù hợp với yêu cầu của bạn.</StyledText>
                </StyledView>
              )}
            />

            <StyledTouchableOpacity 
              onPress={handleCreateRequest}
              disabled={!selectedTable || loading}
              className={`w-full py-5 rounded-2xl items-center mt-4 ${!selectedTable ? 'bg-gray-300' : 'bg-red-900 shadow-lg shadow-red-900/40'}`}
            >
               <StyledText className="text-white text-lg font-bold">Tiếp theo: Thanh toán cọc</StyledText>
            </StyledTouchableOpacity>
          </StyledView>
        </StyledView>
      </Modal>
      {/* Modal: Chọn món ăn */}
      <Modal visible={showMenuModal} animationType="slide" transparent>
        <StyledView className="flex-1 bg-black/50 justify-end">
          <StyledView className="bg-white h-[90%] rounded-t-[40px] p-6">
            <StyledView className="flex-row justify-between items-center mb-6">
               <StyledView>
                  <StyledText className="text-2xl font-bold text-gray-800">Chọn món trước</StyledText>
                  <StyledText className="text-gray-400">Tiết kiệm thời gian khi đến quán</StyledText>
               </StyledView>
               <StyledTouchableOpacity onPress={() => setShowMenuModal(false)}><Ionicons name="close" size={28} color="#666" /></StyledTouchableOpacity>
            </StyledView>

            <StyledView className="flex-row mb-6">
               <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                  {categories.map(cat => (
                    <StyledTouchableOpacity 
                      key={cat.maDM} 
                      onPress={() => setSelectedCategory(cat.maDM)}
                      className={`px-6 py-3 rounded-full mr-3 ${selectedCategory === cat.maDM ? 'bg-red-900' : 'bg-gray-100'}`}
                    >
                       <StyledText className={`font-bold ${selectedCategory === cat.maDM ? 'text-white' : 'text-gray-500'}`}>{cat.tenDM}</StyledText>
                    </StyledTouchableOpacity>
                  ))}
               </ScrollView>
            </StyledView>

            <FlatList 
              data={menuItems.filter(i => i.maDM === selectedCategory)}
              keyExtractor={(item) => item.maMon}
              renderItem={({item}) => {
                const cartItem = selectedDishes.find(d => d.maMon === item.maMon);
                return (
                  <StyledView className="flex-row items-center mb-6 bg-gray-50 p-3 rounded-3xl border border-gray-100">
                     <Image source={{ uri: item.hinhAnhUrl }} className="w-20 h-20 rounded-2xl" />
                     <StyledView className="flex-1 ml-4">
                        <StyledText className="text-lg font-bold text-gray-800">{item.tenMon}</StyledText>
                        <StyledText className="text-red-700 font-bold">{item.giaBan.toLocaleString()}đ</StyledText>
                     </StyledView>
                     {cartItem ? (
                       <StyledView className="flex-row items-center bg-white rounded-2xl p-1 shadow-sm">
                          <StyledTouchableOpacity onPress={() => handleUpdateQuantity(item.maMon, -1)} className="w-8 h-8 items-center justify-center"><Ionicons name="remove" size={20} /></StyledTouchableOpacity>
                          <StyledText className="mx-3 font-bold">{cartItem.soLuong}</StyledText>
                          <StyledTouchableOpacity onPress={() => handleUpdateQuantity(item.maMon, 1)} className="w-8 h-8 items-center justify-center"><Ionicons name="add" size={20} /></StyledTouchableOpacity>
                       </StyledView>
                     ) : (
                       <StyledTouchableOpacity onPress={() => handleAddToCart(item)} className="bg-red-900 w-10 h-10 rounded-2xl items-center justify-center shadow-lg shadow-red-900/30">
                          <Ionicons name="add" size={24} color="white" />
                       </StyledTouchableOpacity>
                     )}
                  </StyledView>
                );
              }}
            />

            <StyledTouchableOpacity onPress={() => setShowMenuModal(false)} className="bg-red-900 w-full py-5 rounded-2xl items-center mt-4">
               <StyledText className="text-white text-lg font-bold">Xong ({selectedDishes.length} món)</StyledText>
            </StyledTouchableOpacity>
          </StyledView>
        </StyledView>
      </Modal>

      {/* Modal: QR Payment */}
      <Modal visible={paymentModalVisible} animationType="fade" transparent>
        <StyledView className="flex-1 bg-black/70 justify-center items-center p-6">
           <StyledView className="bg-white w-full rounded-[40px] p-8 items-center">
              <StyledText className="text-2xl font-bold text-gray-800 mb-2">Thanh toán cọc</StyledText>
              <StyledText className="text-gray-500 mb-6 text-center">
                Vui lòng chuyển khoản {getDepositAmount(selectedTable).toLocaleString()}đ để hoàn tất đặt bàn.
              </StyledText>
              
              <StyledView className="bg-white p-4 rounded-3xl border border-gray-100 shadow-xl mb-6">
                 <Image 
                   source={{ uri: qrUrl }}
                   style={{ width: 250, height: 250 }}
                   resizeMode="contain"
                 />
              </StyledView>

              <StyledView className="w-full bg-gray-50 p-4 rounded-2xl mb-8">
                 <StyledView className="flex-row justify-between mb-2">
                    <StyledText className="text-gray-400">Số tiền:</StyledText>
                    <StyledText className="font-bold text-gray-800">{getDepositAmount(selectedTable).toLocaleString()} VNĐ</StyledText>
                 </StyledView>
                 <StyledView className="flex-row justify-between">
                    <StyledText className="text-gray-400">Mã đơn:</StyledText>
                    <StyledText className="font-bold text-red-700">{currentInvoice?.invoiceId}</StyledText>
                 </StyledView>
              </StyledView>

              <StyledTouchableOpacity 
                onPress={handleConfirmPayment}
                disabled={loading}
                className="bg-green-600 w-full py-5 rounded-2xl items-center shadow-lg shadow-green-600/30"
              >
                {loading ? <ActivityIndicator color="white" /> : <StyledText className="text-white text-lg font-bold">Tôi đã thanh toán</StyledText>}
              </StyledTouchableOpacity>

              <StyledTouchableOpacity 
                onPress={() => setPaymentModalVisible(false)}
                className="mt-4"
              >
                 <StyledText className="text-gray-400 font-bold">Hủy giao dịch</StyledText>
              </StyledTouchableOpacity>
           </StyledView>
        </StyledView>
      </Modal>
    </StyledSafeAreaView>
  );
};

export default BookingScreen;
