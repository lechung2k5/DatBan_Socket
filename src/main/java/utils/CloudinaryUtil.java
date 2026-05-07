package utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import db.EnvConfig;

import java.io.File;
import java.util.Map;

/**
 * CloudinaryUtil - Tiện ích tải ảnh lên Cloudinary
 */
public class CloudinaryUtil {
    private static Cloudinary cloudinary;

    private static Cloudinary getCloudinary() {
        if (cloudinary == null) {
            String cloudName = EnvConfig.cloudinaryCloudName();
            String apiKey = EnvConfig.cloudinaryApiKey();
            String apiSecret = EnvConfig.cloudinaryApiSecret();
            
            System.out.println("[Cloudinary] Đang khởi tạo với CloudName: " + cloudName);
            
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }
        return cloudinary;
    }

    /**
     * Tải ảnh lên Cloudinary và trả về URL
     * @param filePath Đường dẫn file cục bộ
     * @param publicId Tên định danh trên Cloudinary (vd: maMonAn)
     * @return URL của ảnh đã tải lên
     */
    public static String uploadImage(String filePath, String publicId) {
        try {
            if (EnvConfig.cloudinaryCloudName() == null) {
                System.err.println("[Cloudinary] Lỗi: Chưa cấu hình CLOUDINARY_CLOUD_NAME trong .env");
                return null;
            }
            Map uploadResult = getCloudinary().uploader().upload(new File(filePath),
                    ObjectUtils.asMap("public_id", publicId));
            String url = (String) uploadResult.get("secure_url");
            System.out.println("[Cloudinary] Upload thành công: " + url);
            return url;
        } catch (Exception e) {
            System.err.println("[Cloudinary] Lỗi upload (File: " + filePath + "): " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lấy URL ảnh dựa trên publicId (nếu đã biết)
     */
    public static String getImageUrl(String publicId) {
        return getCloudinary().url().generate(publicId);
    }
}
