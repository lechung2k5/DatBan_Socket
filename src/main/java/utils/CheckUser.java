package utils;
import dao.EmployeeDAO;
import entity.NhanVien;
import java.util.List;
public class CheckUser {
    public static void main(String[] args) {
        EmployeeDAO dao = new EmployeeDAO();
        System.out.println("--- LISTING ALL EMPLOYEES ---");
        List<NhanVien> all = dao.findAll();
        for (NhanVien e : all) {
            System.out.println("ID: " + e.getMaNV() + " | Pass: " + e.getMatKhau() + " | Name: " + e.getHoTen());
        }
        NhanVien nv = dao.findByUsername("NV000");
        if (nv != null) {
            System.out.println("\nSearch NV000: FOUND ✓");
        } else {
        System.out.println("\nSearch NV000: NOT FOUND ✗");
    }
}
}