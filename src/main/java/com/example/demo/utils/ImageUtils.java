package com.example.demo.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageUtils {

//    public static String saveBase64Image(String base64) {
//        if (base64.contains(",")) {
//            base64 = base64.split(",")[1];
//        }
//
//        try {
//            byte[] imageBytes = Base64.getDecoder().decode(base64);
//            String fileName = System.currentTimeMillis() + ".jpg";
//            Path path = Paths.get("uploads/" + fileName);
//
//            Files.createDirectories(path.getParent());
//            Files.write(path, imageBytes);
//
//            return "http://10.0.2.2:8084/uploads/" + fileName;
//        } catch (IOException e) {
//            throw new RuntimeException("Không thể lưu ảnh", e);
//        }
//    }

    public static String saveBase64Image(String base64) {
        if (base64.contains(",")) {
            base64 = base64.split(",")[1];
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            String fileName = System.currentTimeMillis() + ".jpg";
            Path path = Paths.get("uploads/" + fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, imageBytes);
            return "http://10.0.2.2:8084/uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh", e);
        }
    }


}
