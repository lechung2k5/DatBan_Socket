package entity;

import java.io.Serializable;

/**
 * MonAn - Thực thể món ăn trong thực đơn
 */
public class MonAn implements Serializable {
    private static final long serialVersionUID = 1L;

    private String maMon;
    private String tenMon;
    private byte[] hinhAnh; // Dữ liệu ảnh dạng byte (legacy)
    private String hinhAnhUrl; // URL ảnh từ Cloudinary
    private double giaBan;
    private String maDM;

    // Trường bổ trợ (không lưu DB trực tiếp trên bảng MenuItems nhưng có thể map từ
    // categoryId)
    private String tenDanhMuc;

    public MonAn() {
    }

    public MonAn(String maMon, String tenMon, byte[] hinhAnh, double giaBan, String maDM) {
        this.maMon = maMon;
        this.tenMon = tenMon;
        this.hinhAnh = hinhAnh;
        this.giaBan = giaBan;
        this.maDM = maDM;
    }

    public MonAn(String maMon, String tenMon, double giaBan) {
        this.maMon = maMon;
        this.tenMon = tenMon;
        this.giaBan = giaBan;
    }

    // Getters and Setters
    public String getMaMon() {
        return maMon;
    }

    public void setMaMon(String maMon) {
        this.maMon = maMon;
    }

    public String getTenMon() {
        return tenMon;
    }

    public void setTenMon(String tenMon) {
        this.tenMon = tenMon;
    }

    public byte[] getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(byte[] hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public String getHinhAnhUrl() {
        return hinhAnhUrl;
    }

    public void setHinhAnhUrl(String hinhAnhUrl) {
        this.hinhAnhUrl = hinhAnhUrl;
    }

    public double getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(double giaBan) {
        this.giaBan = giaBan;
    }

    public String getMaDM() {
        return maDM;
    }

    public void setMaDM(String maDM) {
        this.maDM = maDM;
    }

    public String getTenDanhMuc() {
        return tenDanhMuc;
    }

    public void setTenDanhMuc(String tenDanhMuc) {
        this.tenDanhMuc = tenDanhMuc;
    }
}