package dao;
import db.ConnectDB;
import entity.CaTruc;
import entity.NhanVien;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
public class ShiftDAO {
    public List<CaTruc> findWeeklyByEmployee(LocalDate startOfWeek, String maNV) {
        List<CaTruc> list = new ArrayList<>();
        LocalDate endOfWeek = startOfWeek.plusDays(6);
        String sql = "SELECT CT.*, NV.tenNV FROM CaTruc CT " +
        "JOIN NhanVien NV ON CT.maNV = NV.maNV " +
        "WHERE CT.ngay BETWEEN ? AND ? AND CT.maNV = ?";
        try (Connection con = ConnectDB.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(startOfWeek));
            ps.setDate(2, Date.valueOf(endOfWeek));
            ps.setString(3, maNV);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NhanVien nv = new NhanVien();
                    nv.setMaNV(rs.getString("maNV"));
                    nv.setHoTen(rs.getString("tenNV"));
                    CaTruc ca = new CaTruc(
                    rs.getString("maCa"),
                    rs.getDate("ngay").toLocalDate(),
                    rs.getTime("gioBatDau").toLocalTime(),
                    rs.getTime("gioKetThuc").toLocalTime(),
                    nv
                    );
                    list.add(ca);
                }
            }
        } catch (SQLException e) {
        e.printStackTrace();
    }
    return list;
}
public List<CaTruc> findMonthlyByEmployee(String maNV, LocalDate startOfMonth, LocalDate endOfMonth) {
    List<CaTruc> list = new ArrayList<>();
    String sql = "SELECT CT.*, NV.tenNV FROM CaTruc CT " +
    "JOIN NhanVien NV ON CT.maNV = NV.maNV " +
    "WHERE CT.ngay BETWEEN ? AND ? AND CT.maNV = ?";
    try (Connection con = ConnectDB.getConnection();
    PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setDate(1, Date.valueOf(startOfMonth));
        ps.setDate(2, Date.valueOf(endOfMonth));
        ps.setString(3, maNV);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NhanVien nv = new NhanVien();
                nv.setMaNV(rs.getString("maNV"));
                nv.setHoTen(rs.getString("tenNV"));
                CaTruc ca = new CaTruc(
                rs.getString("maCa"),
                rs.getDate("ngay").toLocalDate(),
                rs.getTime("gioBatDau").toLocalTime(),
                rs.getTime("gioKetThuc").toLocalTime(),
                nv
                );
                list.add(ca);
            }
        }
    } catch (SQLException e) {
    e.printStackTrace();
}
return list;
}
public double getScheduledHours(String maNV, LocalDate start, LocalDate end) {
    long totalMinutes = 0;
    String sql = "SELECT gioBatDau, gioKetThuc FROM CaTruc WHERE maNV = ? AND ngay BETWEEN ? AND ?";
    try (Connection con = ConnectDB.getConnection();
    PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, maNV);
        ps.setDate(2, Date.valueOf(start));
        ps.setDate(3, Date.valueOf(end));
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LocalTime begin = rs.getTime("gioBatDau").toLocalTime();
                LocalTime endT = rs.getTime("gioKetThuc").toLocalTime();
                Duration d = Duration.between(begin, endT);
                if (d.isNegative()) d = d.plusHours(24);
                totalMinutes += d.toMinutes();
            }
        }
    } catch (SQLException e) {
    e.printStackTrace();
}
return totalMinutes / 60.0;
}
public List<CaTruc> findAllByWeek(LocalDate startOfWeek) {
    List<CaTruc> list = new ArrayList<>();
    LocalDate endOfWeek = startOfWeek.plusDays(6);
    String sql = "SELECT CT.*, NV.tenNV FROM CaTruc CT " +
    "JOIN NhanVien NV ON CT.maNV = NV.maNV " +
    "WHERE CT.ngay BETWEEN ? AND ?";
    try (Connection con = ConnectDB.getConnection();
    PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setDate(1, Date.valueOf(startOfWeek));
        ps.setDate(2, Date.valueOf(endOfWeek));
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                NhanVien nv = new NhanVien();
                nv.setMaNV(rs.getString("maNV"));
                nv.setHoTen(rs.getString("tenNV"));
                CaTruc ca = new CaTruc(
                rs.getString("maCa"),
                rs.getDate("ngay").toLocalDate(),
                rs.getTime("gioBatDau").toLocalTime(),
                rs.getTime("gioKetThuc").toLocalTime(),
                nv
                );
                list.add(ca);
            }
        }
    } catch (SQLException e) {
    e.printStackTrace();
}
return list;
}
public void upsert(CaTruc ca) throws SQLException {
    if (ca.getMaCa() == null || ca.getMaCa().isEmpty()) {
        ca.setMaCa(generateNewId());
    }
    String sql = "IF EXISTS (SELECT 1 FROM CaTruc WHERE maCa = ?) " +
    "UPDATE CaTruc SET ngay=?, gioBatDau=?, gioKetThuc=?, maNV=? WHERE maCa=? " +
    "ELSE INSERT INTO CaTruc (maCa, ngay, gioBatDau, gioKetThuc, maNV) VALUES (?,?,?,?,?)";
    try (Connection con = ConnectDB.getConnection();
    PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, ca.getMaCa());
        ps.setDate(2, Date.valueOf(ca.getNgay()));
        ps.setTime(3, Time.valueOf(ca.getGioBatDau()));
        ps.setTime(4, Time.valueOf(ca.getGioKetThuc()));
        ps.setString(5, ca.getNhanVien().getMaNV());
        ps.setString(6, ca.getMaCa());
        ps.setString(7, ca.getMaCa());
        ps.setDate(8, Date.valueOf(ca.getNgay()));
        ps.setTime(9, Time.valueOf(ca.getGioBatDau()));
        ps.setTime(10, Time.valueOf(ca.getGioKetThuc()));
        ps.setString(11, ca.getNhanVien().getMaNV());
        ps.executeUpdate();
    }
}
public void delete(String maCa) throws SQLException {
    String sql = "DELETE FROM CaTruc WHERE maCa = ?";
    try (Connection con = ConnectDB.getConnection();
    PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, maCa);
        ps.executeUpdate();
    }
}
private String generateNewId() {
    String sql = "SELECT TOP 1 maCa FROM CaTruc ORDER BY maCa DESC";
    try (Connection con = ConnectDB.getConnection();
    Statement st = con.createStatement();
    ResultSet rs = st.executeQuery(sql)) {
        if (rs.next()) {
            String lastId = rs.getString(1);
            int num = Integer.parseInt(lastId.substring(2)) + 1;
            return String.format("CA%03d", num);
        }
    } catch (Exception e) {
    e.printStackTrace();
}
return "CA001";
}
}