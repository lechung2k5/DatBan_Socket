package db;

import dao.*;
import entity.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.*;

public class DataImporter {
    private static final String SQL_FILE = "scriptSQL.sql";
    private final TableDAO tableDAO = new TableDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final MenuDAO menuDAO = new MenuDAO();
    private final PromoDAO promoDAO = new PromoDAO();
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final Map<String, HoaDon> invoices = new HashMap<>();
    private final Map<String, NhanVien> employees = new HashMap<>();
    private final Map<String, String> passwords = new HashMap<>();
    private final Map<String, String> roles = new HashMap<>();
    private final Map<String, String> usernames = new HashMap<>();

    public void importData() {
        System.out.println("[IMPORT] Báº¯t Äáº§u import dữ liá»u từ " + SQL_FILE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(SQL_FILE), "UTF-16LE"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.startsWith("INSERT [dbo].[Ban]")) {
                    parseBan(line);
                } else if (line.startsWith("INSERT [dbo].[NhanVien]")) {
                    parseNhanVien(line);
                } else if (line.startsWith("INSERT [dbo].[TaiKhoan]")) {
                    parseTaiKhoan(line);
                } else if (line.startsWith("INSERT [dbo].[MonAn]")) {
                    parseMonAn(line);
                } else if (line.startsWith("INSERT [dbo].[UuDai]")) {
                    parseUuDai(line);
                } else if (line.startsWith("INSERT [dbo].[KhachHang]")) {
                    parseKhachHang(line);
                } else if (line.startsWith("INSERT [dbo].[HoaDon]")) {
                    parseHoaDon(line);
                } else if (line.startsWith("INSERT [dbo].[ChiTietHoaDon]")) {
                    parseChiTietHoaDon(line);
                }
            }
            saveEmployees();
            saveInvoices();
            System.out.println("[IMPORT] HoÃ n táº¥t import dữ liá»u.");
        } catch (Exception e) {
            System.err.println("[IMPORT] Lá»i: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> extractValues(String line) {
        List<String> values = new ArrayList<>();
        int start = line.indexOf("VALUES (") + 8;
        int end = line.lastIndexOf(")");
        String content = line.substring(start, end);
        boolean inQuotes = false;
        int inCast = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                sb.append(c);
            } else if (!inQuotes && c == '(') {
                inCast++;
                sb.append(c);
            } else if (!inQuotes && c == ')') {
                inCast--;
                sb.append(c);
            } else if (!inQuotes && inCast == 0 && c == ',') {
                values.add(sb.toString().trim());
                sb = new StringBuilder();
                if (i + 1 < content.length() && content.charAt(i + 1) == ' ')
                    i++;
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());
        return values;
    }

    private String clean(String val) {
        if (val == null || val.equalsIgnoreCase("NULL"))
            return null;
        if (val.startsWith("N'"))
            val = val.substring(2, val.length() - 1);
        else if (val.startsWith("'"))
            val = val.substring(1, val.length() - 1);
        else if (val.startsWith("CAST(N'")) {
            val = val.substring(val.indexOf("'") + 1, val.lastIndexOf("'"));
        } else if (val.startsWith("CAST(")) {
            val = val.substring(val.indexOf("(") + 1, val.indexOf(" AS"));
        }
        return val;
    }

    private void parseBan(String line) {
        List<String> vals = extractValues(line);
        Ban b = new Ban();
        b.setMaBan(clean(vals.get(0)));
        b.setViTri(clean(vals.get(1)));
        b.setSucChua(Integer.parseInt(vals.get(2)));
        String loai = clean(vals.get(3));
        if (loai != null)
            b.setLoaiBan(LoaiBan.valueOf(loai));
        String tt = clean(vals.get(4));
        if (tt != null)
            b.setTrangThai(TrangThaiBan.fromDbValue(tt));
        tableDAO.insert(b);
    }

    private void parseNhanVien(String line) {
        List<String> vals = extractValues(line);
        NhanVien nv = new NhanVien();
        nv.setMaNV(clean(vals.get(0)));
        nv.setHoTen(clean(vals.get(1)));
        nv.setSdt(clean(vals.get(2)));
        nv.setEmail(clean(vals.get(3)));
        nv.setNgaySinh(clean(vals.get(4)));
        nv.setDiaChi(clean(vals.get(5)));
        nv.setGioiTinh(vals.get(6).equals("1") ? "Nam" : "Nữ");
        nv.setTrangThai(vals.get(7).equals("1") ? "Äang làm" : "Nghỉ");
        nv.setCaLamYeuThich(clean(vals.get(9)));
        employees.put(nv.getMaNV(), nv);
    }

    private void parseTaiKhoan(String line) {
        List<String> vals = extractValues(line);
        String user = clean(vals.get(0));
        String pass = clean(vals.get(1));
        String role = clean(vals.get(2));
        String maNV = clean(vals.get(3));
        usernames.put(maNV, user);
        passwords.put(maNV, pass);
        roles.put(maNV, role);
    }

    private void saveEmployees() {
        for (NhanVien nv : employees.values()) {
            nv.setMatKhau(passwords.getOrDefault(nv.getMaNV(), "123"));
            nv.setUsername_entity(usernames.getOrDefault(nv.getMaNV(), nv.getMaNV()));
            String role = roles.getOrDefault(nv.getMaNV(), "NhanVien");
            if (role.equalsIgnoreCase("Admin") || role.equalsIgnoreCase("QuanLy"))
                nv.setChucVu("QuanLy");
            else
                nv.setChucVu("NhanVien");
            employeeDAO.upsert(nv);
        }
    }

    private void parseMonAn(String line) {
        List<String> vals = extractValues(line);
        MonAn ma = new MonAn();
        ma.setMaMon(clean(vals.get(0)));
        ma.setTenMon(clean(vals.get(1)));
        ma.setGiaBan(Double.parseDouble(clean(vals.get(3))));
        ma.setMaDM(clean(vals.get(4)));
        menuDAO.insert(ma);
    }

    private void parseUuDai(String line) {
        List<String> vals = extractValues(line);
        UuDai ud = new UuDai();
        ud.setMaUuDai(clean(vals.get(0)));
        ud.setTenUuDai(clean(vals.get(1)));
        ud.setMoTa(clean(vals.get(2)));
        ud.setGiaTri(Double.parseDouble(clean(vals.get(3))));
        String ns = clean(vals.get(4));
        if (ns != null)
            ud.setNgayBatDau(LocalDate.parse(ns));
        String ne = clean(vals.get(5));
        if (ne != null)
            ud.setNgayKetThuc(LocalDate.parse(ne));
        promoDAO.insert(ud);
    }

    private void parseKhachHang(String line) {
        List<String> vals = extractValues(line);
        KhachHang kh = new KhachHang();
        kh.setMaKH(clean(vals.get(0)));
        kh.setTenKH(clean(vals.get(1)));
        kh.setSoDT(clean(vals.get(2)));
        kh.setEmail(clean(vals.get(3)));
        String ndk = clean(vals.get(4));
        if (ndk != null)
            kh.setNgayDangKy(LocalDate.parse(ndk));
        kh.setThanhVien(clean(vals.get(5)));
        kh.setDiaChi(clean(vals.get(6)));
        kh.setDiemTichLuy(0);
        customerDAO.insert(kh);
    }

    private void parseHoaDon(String line) {
        List<String> vals = extractValues(line);
        HoaDon hd = new HoaDon();
        hd.setMaHD(clean(vals.get(0)));
        String nl = clean(vals.get(1));
        if (nl != null)
            hd.setNgayLap(LocalDateTime.parse(nl));
        String pt = clean(vals.get(3));
        if (pt != null) {
            if (pt.equalsIgnoreCase("ViDienTu"))
                hd.setHinhThucTT(PTTThanhToan.VI_DIEN_TU);
            else if (pt.equalsIgnoreCase("NganHang"))
                hd.setHinhThucTT(PTTThanhToan.NGAN_HANG);
            else
                hd.setHinhThucTT(PTTThanhToan.TIEN_MAT);
        }
        hd.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
        hd.setMaBan(clean(vals.get(7)));
        hd.setTenNhanVien(clean(vals.get(8)));
        KhachHang kh = new KhachHang();
        kh.setSoDT(clean(vals.get(9)));
        hd.setKhachHang(kh);
        String tc = clean(vals.get(10));
        if (tc != null)
            hd.setTienCoc(Double.parseDouble(tc));
        String gv = clean(vals.get(5));
        if (gv != null)
            hd.setGioVao(LocalDateTime.parse(gv));
        String gr = clean(vals.get(6));
        if (gr != null)
            hd.setGioRa(LocalDateTime.parse(gr));
        invoices.put(hd.getMaHD(), hd);
    }

    private void parseChiTietHoaDon(String line) {
        List<String> vals = extractValues(line);
        String maHD = clean(vals.get(0));
        if (invoices.containsKey(maHD)) {
            ChiTietHoaDon ct = new ChiTietHoaDon();
            ct.setTenMon(clean(vals.get(1)));
            ct.setSoLuong(Integer.parseInt(vals.get(2)));
            ct.setThanhTien(Double.parseDouble(clean(vals.get(3))));
            ct.setDonGia(ct.getThanhTien() / ct.getSoLuong());
            invoices.get(maHD).getChiTietHoaDon().add(ct);
        }
    }

    private void saveInvoices() {
        for (HoaDon hd : invoices.values()) {
            double total = hd.getChiTietHoaDon().stream().mapToDouble(ChiTietHoaDon::getThanhTien).sum();
            hd.setTongCongMonAn(total);
            invoiceDAO.insert(hd, hd.getChiTietHoaDon());
        }
    }

    public static void main(String[] args) {
        new DataImporter().importData();
    }
}