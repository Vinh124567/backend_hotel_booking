package com.example.demo.service.auth;

import com.example.demo.dto.auth.AuthenticationRequestDTO;
import com.example.demo.dto.auth.IntrospectRequest;
import com.example.demo.dto.auth.IntrospectResponse;
import com.example.demo.dto.user.ChangePasswordRequest;
import com.example.demo.entity.User;
import com.example.demo.entity.Role;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.AuthRepository;
import com.example.demo.response.AuthenticationResponse;
import com.example.demo.service.auth.AuthenticationService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.experimental.NonFinal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.StringJoiner;

@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthRepository authRepository;

    @NonFinal
    protected static final String SIGNER_KEY = "9o75HYyiqLhhK91+pvVoDsJ3p+oRd6n3iapvj9Hx8uwvcqWIEVDcAgNnz7gG0rTX";

    // Thêm key riêng cho refresh token để tăng tính bảo mật
    @NonFinal
    protected static final String REFRESH_SIGNER_KEY = "R3fr3shT0k3nS1gn3rK3y_S3cur3!@9o75HYyiqLhhK91+pvVoDsJ3p+oRd6n3";

    public AuthenticationResponse Authenticate(AuthenticationRequestDTO authenticationRequestDTO) {
        // Tìm người dùng theo username hoặc email
        var admin = authRepository
                .findByUsernameOrEmail(authenticationRequestDTO.getUsername(), authenticationRequestDTO.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(authenticationRequestDTO.getPassword(), admin.getPasswordHash());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(admin);
        var refreshToken = generateRefreshToken(admin);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }


    private String generateToken(User user) {
        String issuer = System.getProperty("spring.application.name");

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS384);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issueTime(new Date())
//                .expirationTime(new Date(
//                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
//                ))
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    // Thêm phương thức mới để tạo refresh token
    private String generateRefreshToken(User user) {
        String issuer = System.getProperty("spring.application.name");

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS384);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issueTime(new Date())
                // Thời hạn refresh token dài hơn access token
                .expirationTime(new Date(
                        Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli()
                ))
                .claim("type", "refresh")
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(REFRESH_SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner joiner = new StringJoiner(" ");  // Use space separator for authorities
        System.out.println("Admin roles: " + user.getRoles());

        for (Role role : user.getRoles()) {
            joiner.add(role.getName());  // Make sure role names are already prefixed with "ROLE_" in the database
            // or modify this to add the prefix: joiner.add("ROLE_" + role.getName())
        }

        return joiner.toString();
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date exprityTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        return IntrospectResponse.builder()
                .valid(verified && exprityTime.after(new Date()))
                .build();
    }

    // Thêm phương thức để xác thực refresh token
    public IntrospectResponse introspectRefreshToken(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        JWSVerifier verifier = new MACVerifier(REFRESH_SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");

        var verified = signedJWT.verify(verifier) && "refresh".equals(tokenType);

        return IntrospectResponse.builder()
                .valid(verified && expiryTime.after(new Date()))
                .build();
    }

    @Override
    public boolean changePassword(String username, ChangePasswordRequest changePasswordRequest) {
        // Kiểm tra confirm password khớp với new password
        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Tìm user theo username
        User user = authRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

        // Kiểm tra current password có đúng không
        boolean currentPasswordValid = passwordEncoder.matches(
                changePasswordRequest.getCurrentPassword(),
                user.getPasswordHash()
        );

        if (!currentPasswordValid) {
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // Kiểm tra new password không giống current password
        boolean isSamePassword = passwordEncoder.matches(
                changePasswordRequest.getNewPassword(),
                user.getPasswordHash()
        );

        if (isSamePassword) {
            throw new AppException(ErrorCode.SAME_PASSWORD);
        }

        // Mã hóa password mới
        String newPasswordHash = passwordEncoder.encode(changePasswordRequest.getNewPassword());

        // Cập nhật password trong database
        user.setPasswordHash(newPasswordHash);
        authRepository.save(user);

        return true;
    }
}