import { BookingModel } from '../models/BookingModel';
import { Alert } from 'react-native';

/**
 * BookingController - Xử lý tương tác giữa UI và Dữ liệu
 */
export const BookingController = {
  
  /**
   * Xử lý yêu cầu đặt bàn từ View
   */
  processBooking: async (formData) => {
    const booking = new BookingModel(
      formData.name,
      formData.phone,
      formData.guests,
      formData.time
    );

    if (!booking.isValid()) {
      return {
        success: false,
        message: 'Thông tin không hợp lệ. Vui lòng kiểm tra lại Tên và Số điện thoại.'
      };
    }

    try {
      // Giả lập gửi qua Socket (Sẽ kết nối thật ở phần Service)
      console.log('Sending to Socket:', booking.toSocketData());
      
      // Ở đây ta có thể gọi: SocketService.send(booking.toSocketData())
      
      return {
        success: true,
        message: `Chào ${booking.name}, yêu cầu của bạn đã được gửi đến nhà hàng!`
      };
    } catch (error) {
      return {
        success: false,
        message: 'Lỗi kết nối Server: ' + error.message
      };
    }
  }
};
