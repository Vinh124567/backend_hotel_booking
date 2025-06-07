package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Thêm cấu hình cho uploads - đây là phần cần bổ sung
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600); // Cache trong 1 giờ

        // Giữ lại cấu hình hiện có
        registry.addResourceHandler(
                        "/img/**",
                        "/css/**",
                        "/libs/**")
                .addResourceLocations(
                        "classpath:/static/img/",
                        "classpath:/static/css/",
                        "classpath:/static/assets/",
                        "classpath:/static/libs/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Mở rộng CORS để cho phép tất cả các request từ các nguồn khác nhau
        registry.addMapping("/**") // Áp dụng cho tất cả các endpoint, bao gồm cả uploads
                .allowedOrigins("*") // Cho phép tất cả các origin
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .maxAge(3600); // Thời gian lưu cache preflight request
    }
}