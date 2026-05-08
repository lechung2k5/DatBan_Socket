/**
 * BookingModel - Quản lý dữ liệu đặt bàn (Client-side)
 */
export class BookingModel {
  constructor(data = {}) {
    this.customerId = data.customerId || '';
    this.customerPhone = data.customerPhone || '';
    this.customerName = data.customerName || '';
    this.maBan = data.maBan || '';
    this.ngayDat = data.ngayDat || new Date().toISOString().split('T')[0];
    this.gioDat = data.gioDat || '18:00';
    this.soKhach = data.soKhach || 2;
    this.ghiChu = data.ghiChu || '';
    this.items = data.items || [];
  }

  // Kiểm tra dữ liệu trước khi gửi lên Node.js Backend
  isValid() {
    return (
      this.maBan.length > 0 && 
      this.soKhach > 0 && 
      this.ngayDat.length > 0 &&
      this.gioDat.length > 0
    );
  }

  // Format dữ liệu chuẩn để gửi qua REST API (Node.js)
  toApiData() {
    return {
      customerId: this.customerId,
      customerPhone: this.customerPhone,
      customerName: this.customerName,
      maBan: this.maBan,
      ngayDat: this.ngayDat,
      gioDat: this.gioDat,
      soKhach: this.soKhach,
      ghiChu: this.ghiChu,
      items: this.items,
      total: this.items.reduce((sum, it) => sum + (it.donGia * it.soLuong), 0)
    };
  }

  // Format dữ liệu chuẩn để gửi qua WebSocket (Java)
  toJavaData(tienCoc = 0) {
    return {
      hoaDon: {
        maBan: this.maBan,
        khachHang: {
          maKH: this.customerPhone,
          soDT: this.customerPhone,
          tenKH: this.customerName
        },
        gioVao: `${this.ngayDat}T${this.gioDat}:00`,
        soKhach: this.soKhach,
        ghiChu: this.ghiChu,
        tienCoc: tienCoc,
        trangThai: 'ChoXacNhan', // Trạng thái ban đầu
        loaiHoaDon: 'DatTruoc'
      },
      chiTiet: this.items.map(it => ({
        maMon: it.maMon,
        tenMon: it.tenMon,
        soLuong: it.soLuong,
        donGia: it.donGia,
        thanhTien: it.donGia * it.soLuong
      }))
    };
  }
}
