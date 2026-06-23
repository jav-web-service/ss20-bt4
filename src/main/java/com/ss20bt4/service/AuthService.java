package com.ss20bt4.service;

import com.ss20bt4.dto.AuthRequest;
import com.ss20bt4.dto.AuthResponse;
import com.ss20bt4.entity.Student;
import com.ss20bt4.entity.StudentToken;
import com.ss20bt4.repository.StudentRepository;
import com.ss20bt4.repository.StudentTokenRepository;
import com.ss20bt4.security.CustomUserDetails;
import com.ss20bt4.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StudentRepository studentRepository;
    private final StudentTokenRepository studentTokenRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Student student = studentRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        CustomUserDetails userDetails = new CustomUserDetails(student);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Save refresh token to DB
        StudentToken studentToken = StudentToken.builder()
                .student(student)
                .tokenString(refreshToken)
                .isExpired(false)
                .isRevoked(false)
                .build();
        StudentToken savedToken = studentTokenRepository.save(studentToken);

        // Put tokenId into Access Token extra claims so Filter can find it
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("tokenId", savedToken.getId());
        String accessToken = jwtService.generateToken(extraClaims, userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse refreshToken(String refreshTokenStr) {
        String userEmail = jwtService.extractUsername(refreshTokenStr);
        if (userEmail != null) {
            Student student = studentRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            StudentToken studentToken = studentTokenRepository.findByTokenString(refreshTokenStr)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            if (studentToken.getIsRevoked() || studentToken.getIsExpired()) {
                throw new RuntimeException("Refresh token is invalid");
            }

            CustomUserDetails userDetails = new CustomUserDetails(student);
            if (jwtService.isTokenValid(refreshTokenStr, userDetails)) {
                // Generate new access token mapped to the same refresh token ID
                Map<String, Object> extraClaims = new HashMap<>();
                extraClaims.put("tokenId", studentToken.getId());
                String newAccessToken = jwtService.generateToken(extraClaims, userDetails);

                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshTokenStr)
                        .build();
            }
        }
        throw new RuntimeException("Invalid Refresh Token");
    }

    public void logout() {
        // Lấy student_id từ Security Context
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long studentId = userDetails.getStudent().getId();

        // Tìm toàn bộ token còn hiệu lực
        List<StudentToken> validTokens = studentTokenRepository.findAllValidTokensByStudentId(studentId);

        // Sử dụng Stream API lặp qua toàn bộ các token còn hiệu lực và cập nhật is_revoked = true
        List<StudentToken> revokedTokens = validTokens.stream()
                .peek(token -> token.setIsRevoked(true))
                .collect(Collectors.toList());

        studentTokenRepository.saveAll(revokedTokens);

        // Xóa Security Context
        SecurityContextHolder.clearContext();
    }
}
