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
            "/api/v1/auth/**",
            "/api/v1/coffee/public/**",
            "/api/v1/categories/public/**",
            "/api/v1/voucher/public/**",
            "api/v1/room/**",
            "/api/v1/payments/health",
            "/api/v1/payments/*/status",
            "/api/v1/payments/*/check-momo",
            "/api/v1/payments/*/test/success",
            "/api/v1/payments/*/qr",
            "/api/payments/callback/**",
            "/api/payments/config/debug"
    };

    // Endpoint cho hình ảnh
    private final String[] PUBLIC_IMAGE_ENDPOINT = {
            "/uploads/**"
    };

    // Endpoint yêu cầu xác thực người dùng thông thường
    private final String[] USER_ENDPOINTS = {
            "/api/v1/users/profile/**",
            "/api/v1/cart/**",
            "/api/v1/orders/user/**",
            "/api/v1/order-items/user/**",
            "/api/v1/address/**",
            "api/v1/hotel/**",
            "/api/v1/locations"
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
            "/api/v1/room-images/**",
            // ❌ REMOVED: "/api/v1/reviews/**",
            // ❌ REMOVED: "/api/v1/favorites/**",
            // ❌ REMOVED: "/api/v1/bookings/**", ← MOVED TO SPECIFIC RULES
            "/api/v1/payments/admin/**",
            "api/v1/admin/dashboard/**"
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
                        // ✅ Public endpoints
                        .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/payments/**").permitAll()
                        .requestMatchers("/api/v1/payments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/callback").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payments/callback").permitAll()
                        .requestMatchers(PUBLIC_IMAGE_ENDPOINT).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()

                        // ✅ BOOKINGS ENDPOINTS - Granular permissions
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/pending").authenticated()       // USER can view their pending bookings
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/confirmed").authenticated()     // USER can view their confirmed bookings
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/completed").authenticated()     // USER can view their completed bookings
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/cancelled").authenticated()     // USER can view their cancelled bookings
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/user/**").authenticated()       // USER can view their own bookings
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/*").authenticated()             // USER can view specific booking details
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").authenticated()              // USER can create bookings
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/cancel").authenticated()      // USER can cancel their own bookings

                        // Only ADMIN can manage all bookings
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/admin/**").hasRole("ADMIN")     // Admin view all bookings
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/approve").hasRole("ADMIN")    // Admin approve bookings
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bookings/*/reject").hasRole("ADMIN")     // Admin reject bookings
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bookings/**").hasRole("ADMIN")       // Admin delete bookings

                        // ✅ ROOM TYPE ENDPOINTS
                        .requestMatchers(HttpMethod.GET, "/api/v1/room-type/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/room-type/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/room-type/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/room-type/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/room-type/**").hasRole("ADMIN")

                        // ✅ REVIEWS ENDPOINTS
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/reviews/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/**").hasRole("ADMIN")

                        // ✅ FAVORITES ENDPOINTS
                        .requestMatchers(HttpMethod.GET, "/api/v1/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/favorites/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/favorites/**").hasRole("ADMIN")

                        // ✅ Payment endpoints
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/payments/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/payments/*").hasRole("ADMIN")

                        // ✅ USER endpoints
                        .requestMatchers(USER_ENDPOINTS).authenticated()

                        // ✅ ADMIN endpoints
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")

                        // ✅ All other requests need authentication
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
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS384");
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS384)
                .build();
    }
}