package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Endpoint công khai - không cần xác thực
    private final String[] PUBLIC_ENDPOINTS = {
            // Auth - đăng nhập, đăng ký
            "/api/v1/auth/**",

            // Hiển thị sản phẩm, danh mục công khai
            "/api/v1/coffee/public/**",
            "/api/v1/categories/public/**",

            // Thông tin voucher công khai
            "/api/v1/voucher/public/**",
            "api/v1/room/**"
    };

    // Endpoint cho hình ảnh
    private final String[] PUBLIC_IMAGE_ENDPOINT = {
            "/uploads/**"
    };

    // Endpoint yêu cầu xác thực người dùng thông thường
    private final String[] USER_ENDPOINTS = {
            // Thông tin người dùng
            "/api/v1/users/profile/**",

            // Giỏ hàng, đặt hàng
            "/api/v1/cart/**",
            "/api/v1/orders/user/**",
            "/api/v1/order-items/user/**",

            // Địa chỉ người dùng
            "/api/v1/address/**",

            "api/v1/hotel/**",
            "/api/v1/locations",
            "/api/v1/locations",
    };

    // Endpoint dành cho quản trị viên
    private final String[] ADMIN_ENDPOINTS = {
            "/api/v1/admin/**",
            "/api/v1/coffee/admin/**",
            "/api/v1/categories/admin/**",
            "/api/v1/voucher/admin/**",
            "/api/v1/orders/admin/**",
            "/api/v1/users/admin/**",
            "/api/v1/amenity/**",
            "/api/v1/room-type/**",
            "/api/v1/room-images/**",
    };

    String signerKey = "9o75HYyiqLhhK91+pvVoDsJ3p+oRd6n3iapvj9Hx8uwvcqWIEVDcAgNnz7gG0rTX";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request ->
                request
                        // Cho phép truy cập không xác thực tới các endpoint công khai
                        .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(PUBLIC_IMAGE_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").permitAll()


                        // Yêu cầu vai trò ADMIN cho các endpoint quản trị
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")

                        // Yêu cầu xác thực cho tất cả các yêu cầu khác
                        .anyRequest().authenticated());

        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer -> jwtConfigurer.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        // This is the important line - set claim name to "scope"
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS384");  // Change to HS384
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS384)  // Change to match token generation
                .build();
    }
}

