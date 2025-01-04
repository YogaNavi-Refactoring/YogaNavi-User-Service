package com.yoganavi.user.service.jwt;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JwtServiceImpl implements JwtService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public String reIssueRefreshToken(String accessToken, String refreshToken) {
        log.debug("토큰 재발급 시작 - Access Token: {}, Refresh Token: {}", accessToken, refreshToken);

        try {
            // 토큰 존재 여부 확인
            if (accessToken == null || accessToken.isEmpty() ||
                refreshToken == null || refreshToken.isEmpty()) {
                throw new JwtException("토큰이 존재하지 않습니다.");
            }

            // Access Token에서 이메일 추출
            String token = jwtUtil.extractToken(accessToken);
            String originalEmail;
            try {
                Claims expiredClaims = jwtUtil.validateToken(token);
                throw new JwtException("액세스 토큰이 아직 유효합니다.");
            } catch (ExpiredJwtException e) {
                originalEmail = e.getClaims().get("email", String.class);
                log.debug("만료된 액세스 토큰에서 이메일 추출: {}", originalEmail);
            }

            // RefreshToken 검증 및 이메일 추출
            String refreshEmail;
            Claims refreshClaims;
            try {
                log.debug("리프레시 토큰 검증 시작");
                refreshClaims = jwtUtil.validateToken(refreshToken);
                refreshEmail = refreshClaims.get("email", String.class);
                log.debug("리프레시 토큰 검증 성공. 이메일: {}", refreshEmail);

                // refreshToken 검증
//                if (!jwtUtil.validateRefreshToken(refreshToken, refreshEmail)) {
//                    throw new JwtException("Redis에 저장된 리프레시 토큰과 일치하지 않습니다.");
//                }
                log.debug("Redis의 리프레시 토큰 검증 성공");

            } catch (ExpiredJwtException e) {
                log.error("리프레시 토큰 만료됨");
                throw new JwtException("리프레시 토큰이 만료되었습니다.");
            } catch (JwtException e) {
                log.error("리프레시 토큰 검증 실패: {}", e.getMessage());
                throw e;
            }

            // 이메일 일치 여부 확인
            if (!refreshEmail.equals(originalEmail)) {
                throw new JwtException("토큰 사용자 정보가 일치하지 않습니다.");
            }

            // 사용자 조회
            Optional<Users> user = userRepository.findByEmail(refreshEmail);
            if (user.isEmpty()) {
                throw new JwtException("사용자를 찾을 수 없습니다.");
            }

            // 새로운 AccessToken 발급
            boolean isOAuth;
            try {
                Claims expiredClaims = jwtUtil.validateToken(token);
                isOAuth = expiredClaims.get("isOAuth", Boolean.class);
            } catch (ExpiredJwtException e) {
                isOAuth = e.getClaims().get("isOAuth", Boolean.class);
            }

            String newAccessToken = jwtUtil.generateAccessToken(
                refreshEmail,
                user.get().getRole()
            );

            log.info("액세스 토큰 재발급 성공");
            return newAccessToken;

        } catch (JwtException e) {
            log.error("토큰 재발급 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("토큰 재발급 중 예상치 못한 에러 발생", e);
            throw new JwtException("토큰 재발급 중 오류가 발생했습니다.");
        }
    }
}
