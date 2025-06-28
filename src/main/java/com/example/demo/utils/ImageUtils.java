package com.example.demo.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class ImageUtils {

    private static final String UPLOAD_DIR = "uploads/";
    private static final String BASE_URL = "http://10.0.2.2:8084/uploads/";

    /**
     * Lưu base64 thành file và trả về URL ngắn
     * INPUT: base64 string (có thể có prefix "data:image/...")
     * OUTPUT: URL ngắn để lưu vào database
     */
    public static String saveBase64Image(String base64) {
        if (base64 == null || base64.trim().isEmpty()) {
            throw new IllegalArgumentException("Base64 string không thể null hoặc rỗng");
        }

        try {
            System.out.println("🔍 === DEBUG saveBase64Image START ===");
            System.out.println("🔍 Input length: " + base64.length());
            System.out.println("🔍 Input starts with 'data:': " + base64.startsWith("data:"));
            System.out.println("🔍 Input preview: " + base64.substring(0, Math.min(100, base64.length())) + "...");

            // ✅ Loại bỏ data URL prefix nếu có
            String base64Data = base64;
            if (base64.contains(",")) {
                String[] parts = base64.split(",");
                if (parts.length >= 2) {
                    base64Data = parts[1];
                    System.out.println("🔍 Removed data URL prefix");
                }
            }

            System.out.println("🔍 Clean base64 length: " + base64Data.length());

            // ✅ Validate base64
            if (base64Data.trim().isEmpty()) {
                throw new IllegalArgumentException("Base64 data rỗng sau khi xử lý");
            }

            // ✅ Decode base64 thành bytes
            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(base64Data);
                System.out.println("🔍 Decoded bytes length: " + imageBytes.length);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Base64 không hợp lệ: " + e.getMessage());
            }

            // ✅ Validate file size (max 10MB)
            if (imageBytes.length > 10 * 1024 * 1024) {
                throw new RuntimeException("File quá lớn: " + (imageBytes.length / 1024 / 1024) + "MB (max 10MB)");
            }

            if (imageBytes.length == 0) {
                throw new RuntimeException("File rỗng sau khi decode");
            }

            // ✅ Tạo tên file unique
            String fileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + ".jpg";
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path filePath = uploadPath.resolve(fileName);

            System.out.println("🔍 File path: " + filePath.toAbsolutePath());

            // ✅ Tạo thư mục nếu chưa tồn tại
            Files.createDirectories(uploadPath);

            // ✅ Lưu file vào disk
            Files.write(filePath, imageBytes);

            // ✅ Verify file đã được lưu
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File không được lưu thành công: " + filePath);
            }

            long savedFileSize = Files.size(filePath);
            System.out.println("🔍 File saved with size: " + savedFileSize + " bytes");

            // ✅ Tạo URL ngắn để trả về
            String imageUrl = BASE_URL + fileName;

            System.out.println("✅ File saved successfully: " + fileName);
            System.out.println("📎 URL created: " + imageUrl);
            System.out.println("📏 URL length: " + imageUrl.length() + " characters");
            System.out.println("🔍 === DEBUG saveBase64Image END ===");

            // ✅ QUAN TRỌNG: Luôn trả về URL ngắn, KHÔNG BAO GIỜ trả về base64
            return imageUrl;

        } catch (IOException e) {
            System.err.println("❌ IO Error in saveBase64Image: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lưu file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in saveBase64Image: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi xử lý ảnh: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem string có phải là base64 hay URL
     */
    public static boolean isBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.startsWith("data:") || (!str.startsWith("http") && str.length() > 100);
    }

    /**
     * Kiểm tra xem string có phải là URL hay không
     */
    public static boolean isUrl(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.startsWith("http://") || str.startsWith("https://");
    }
}