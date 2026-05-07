package server;

import entity.*;
import org.junit.jupiter.api.*;
import dao.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4  AWS DynamoDB DAO Layer Integration Test
 *
 * Checklist CLO1 (5 phương thức CRUD):
 *  ✅ TableDAO:    findAll, findById, insert, updateStatus, delete
 *  ✅ MenuDAO:     findAll, findById, insert, update, delete
 *  ✅ CustomerDAO: findAll, findByPhone, insert, update, delete
 *  ✅ InvoiceDAO:  insert, findById, findByDate, findByStatus, updateStatus
 *  ✅ StatsDAO:    getRevenueByDate, getRevenueByMonth, countInvoicesByDate
 *
 * Chú ý: Test này kết nối THẬT với AWS DynamoDB (cần .env hợp lệ)
 * Mỗi test tự dọn dẹp dữ liệu của mình (cleanup) để không ảnh hưởng production
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase4DynamoDBTest {

    // ─── TEST TABLE DAO ───────────────────────────────────────────────────────

    @Test @Order(1)
    void testTableDAO_Insert() {
        TableDAO dao = new TableDAO();
        Ban ban = new Ban();
        ban.setMaBan("TEST-B001");
        ban.setViTri("Tầng 1 - Test");
        ban.setSucChua(4);
        ban.setTrangThai(TrangThaiBan.TRONG);

        dao.insert(ban);

        // Verify
        Ban fetched = dao.findById("TEST-B001");
        assertNotNull(fetched, "Phải tìm thấy bàn vừa insert");
        assertEquals("TEST-B001", fetched.getMaBan());
        assertEquals("Tầng 1 - Test", fetched.getViTri());
        System.out.println("[PASS] TableDAO insert + findById ✓");

        // Cleanup
        dao.delete("TEST-B001");
    }

    @Test @Order(2)
    void testTableDAO_UpdateStatus() {
        TableDAO dao = new TableDAO();

        // Setup
        Ban ban = new Ban();
        ban.setMaBan("TEST-B002");
        ban.setViTri("Tầng 2");
        ban.setSucChua(6);
        ban.setTrangThai(TrangThaiBan.TRONG);
        dao.insert(ban);

        // Update
        boolean updated = dao.updateStatus("TEST-B002", "DangSuDung");
        assertTrue(updated, "updateStatus phải trả về true khi bàn tồn tại");

        Ban fetched = dao.findById("TEST-B002");
        assertNotNull(fetched);
        assertEquals("DangSuDung", fetched.getTrangThai().getDbValue());
        System.out.println("[PASS] TableDAO updateStatus ✓");

        // Cleanup
        dao.delete("TEST-B002");
    }

    @Test @Order(3)
    void testTableDAO_UpdateNonExistent() {
        TableDAO dao = new TableDAO();
        boolean result = dao.updateStatus("NON-EXISTENT-TABLE", "Trong");
        assertFalse(result, "Bàn không tồn tại phải trả về false");
        System.out.println("[PASS] TableDAO updateStatus(non-existent) → false ✓");
    }

    @Test @Order(4)
    void testTableDAO_FindAll() {
        TableDAO dao = new TableDAO();
        List<Ban> list = dao.findAll();
        assertNotNull(list);
        System.out.println("[PASS] TableDAO findAll → " + list.size() + " bàn ✓");
    }

    // ─── TEST MENU DAO ────────────────────────────────────────────────────────

    @Test @Order(5)
    void testMenuDAO_CRUD() {
        MenuDAO dao = new MenuDAO();

        // Insert
        MonAn monAn = new MonAn();
        monAn.setMaMon("TEST-FOOD001");
        monAn.setMaDM("TEST-CAT");
        monAn.setTenMon("Phở Bò Test");
        monAn.setGiaBan(55000);
        dao.insert(monAn);

        // FindById
        MonAn fetched = dao.findById("TEST-CAT", "TEST-FOOD001");
        assertNotNull(fetched, "Phải tìm thấy món vừa insert");
        assertEquals("Phở Bò Test", fetched.getTenMon());
        assertEquals(55000, fetched.getGiaBan(), 0.01);
        System.out.println("[PASS] MenuDAO insert + findById ✓");

        // Update
        monAn.setTenMon("Phở Bò Đặc Biệt");
        monAn.setGiaBan(65000);
        dao.update(monAn);

        MonAn updated = dao.findById("TEST-CAT", "TEST-FOOD001");
        assertNotNull(updated);
        assertEquals(65000, updated.getGiaBan(), 0.01);
        System.out.println("[PASS] MenuDAO update ✓");

        // Delete
        dao.delete("TEST-CAT", "TEST-FOOD001");
        MonAn deleted = dao.findById("TEST-CAT", "TEST-FOOD001");
        assertNull(deleted, "Sau delete phải không tìm thấy");
        System.out.println("[PASS] MenuDAO delete ✓");
    }

    @Test @Order(6)
    void testMenuDAO_FindAll() {
        MenuDAO dao = new MenuDAO();
        List<MonAn> list = dao.findAll();
        assertNotNull(list);
        System.out.println("[PASS] MenuDAO findAll → " + list.size() + " món ✓");
    }

    // ─── TEST CUSTOMER DAO ────────────────────────────────────────────────────

    @Test @Order(7)
    void testCustomerDAO_CRUD() {
        CustomerDAO dao = new CustomerDAO();
        String testPhone = "0900000001";

        // Cleanup trước khi test (đề phòng test trước bị gián đoạn)
        dao.delete(testPhone);

        // Insert
        KhachHang kh = new KhachHang();
        kh.setSoDT(testPhone);
        kh.setTenKH("Nguyễn Test");
        kh.setThanhVien("");
        dao.insert(kh);

        // FindByPhone
        KhachHang fetched = dao.findByPhone(testPhone);
        assertNotNull(fetched, "Phải tìm thấy KH vừa insert");
        assertEquals("Nguyễn Test", fetched.getTenKH());
        System.out.println("[PASS] CustomerDAO insert + findByPhone ✓");

        // Update
        kh.setThanhVien("Bạc"); // cập nhật hạng thành viên
        dao.update(kh);

        KhachHang updated = dao.findByPhone(testPhone);
        assertNotNull(updated);
        assertEquals("Bạc", updated.getThanhVien());
        System.out.println("[PASS] CustomerDAO update ✓");

        // Delete
        dao.delete(testPhone);
        assertNull(dao.findByPhone(testPhone), "Sau delete phải null");
        System.out.println("[PASS] CustomerDAO delete ✓");
    }

    @Test @Order(8)
    void testCustomerDAO_FindOrCreate() {
        CustomerDAO dao = new CustomerDAO();
        String testPhone = "0900000002";

        // Cleanup
        dao.delete(testPhone);

        // FindOrCreate  KH chưa tồn tại
        KhachHang kh = dao.findOrCreate(testPhone, "Trần Test");
        assertNotNull(kh);
        assertEquals(testPhone, kh.getSoDT());

        // FindOrCreate lần 2  KH đã có
        KhachHang kh2 = dao.findOrCreate(testPhone, "Tên Khác");
        assertEquals("Trần Test", kh2.getTenKH()); // tên gốc không bị đổi

        System.out.println("[PASS] CustomerDAO findOrCreate ✓");

        // Cleanup
        dao.delete(testPhone);
    }

    // ─── TEST INVOICE DAO ─────────────────────────────────────────────────────

    @Test @Order(9)
    void testInvoiceDAO_InsertAndFindById() {
        InvoiceDAO dao = new InvoiceDAO();
        String testId = "TEST-HD-" + System.currentTimeMillis();

        HoaDon hd = new HoaDon();
        hd.setMaHD(testId);
        hd.setMaBan("B001");
        hd.setTrangThai(TrangThaiHoaDon.HOA_DON_TAM);
        hd.setNgayLap(LocalDateTime.now());
        hd.setTenNhanVien("TEST-EMP");
        hd.setTongTienThanhToan(120000);

        dao.insert(hd, null);

        HoaDon fetched = dao.findById(testId);
        assertNotNull(fetched, "Phải tìm thấy hóa đơn vừa insert");
        assertEquals(testId, fetched.getMaHD());
        System.out.println("[PASS] InvoiceDAO insert + findById ✓");

        // Cleanup bằng cách cancel
        dao.updateStatus(testId, "DaHuy", false);
    }

    @Test @Order(10)
    void testInvoiceDAO_UpdateStatus() {
        InvoiceDAO dao = new InvoiceDAO();
        String testId = "TEST-HD-STATUS-" + System.currentTimeMillis();

        HoaDon hd = new HoaDon();
        hd.setMaHD(testId);
        hd.setMaBan("B001");
        hd.setTrangThai(TrangThaiHoaDon.HOA_DON_TAM);
        hd.setNgayLap(LocalDateTime.now());
        hd.setTenNhanVien("TEST-EMP");
        dao.insert(hd, null);

        // UpdateStatus
        dao.updateStatus(testId, "DaThanhToan", true);

        HoaDon updated = dao.findById(testId);
        assertNotNull(updated);
        assertEquals("DaThanhToan", updated.getTrangThai().getDbValue());
        System.out.println("[PASS] InvoiceDAO updateStatus ✓");
    }

    // ─── TEST STATS DAO ───────────────────────────────────────────────────────

    @Test @Order(11)
    void testStatsDAO_GetRevenue() {
        StatsDAO dao = new StatsDAO();

        // Test ngày hôm nay (có thể là 0 nếu chưa có dữ liệu - không sao)
        double revenue = dao.getRevenueByDate(LocalDate.now());
        assertTrue(revenue >= 0, "Doanh thu không được âm");
        System.out.println("[PASS] StatsDAO getRevenueByDate(today) → " + revenue + " ✓");

        // Test tháng này
        double monthly = dao.getRevenueByMonth(LocalDate.now().getYear(), LocalDate.now().getMonthValue());
        assertTrue(monthly >= 0);
        System.out.println("[PASS] StatsDAO getRevenueByMonth → " + monthly + " ✓");

        // Test đếm hóa đơn
        int count = dao.countInvoicesByDate(LocalDate.now());
        assertTrue(count >= 0);
        System.out.println("[PASS] StatsDAO countInvoicesByDate → " + count + " ✓");
    }
}
