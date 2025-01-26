package com.yoganavi.user.user.controller;

import com.yoganavi.user.common.constants.SecurityConstants;
import com.yoganavi.user.common.util.JwtUtil;
import com.yoganavi.user.user.dto.login.LoginRequestDto;
import com.yoganavi.user.user.dto.login.LoginResponseDto;
import com.yoganavi.user.user.service.login.LoginService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class LoginController {

    private final JwtUtil jwtUtil;
    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDto request,
        HttpServletResponse response) {
        try {
            LoginResponseDto loginResult = loginService.login(request);

            // 리프레시 토큰 쿠키에
            ResponseCookie refreshTokenCookie = ResponseCookie.from(
                    SecurityConstants.REFRESH_TOKEN_COOKIE,
                    loginResult.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            // 성공 응답
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("code", 200);
            successResponse.put("data", null);

            // 액세스 토큰 헤더에
            return ResponseEntity.ok()
                .header(SecurityConstants.JWT_HEADER, "Bearer " + loginResult.getAccessToken())
                .body(successResponse);

        } catch (Exception e) {
            // 서버 에러
            log.error("로그인 중 예기치 않은 에러 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "내부 서버 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
        @RequestHeader(SecurityConstants.JWT_HEADER) String token) {
        log.info("로그아웃 요청 : 사용자 {}", jwtUtil.getUserIdFromToken(token));

        Map<String, Object> response = new HashMap<>();
        try {
            boolean logoutSuccess = jwtUtil.logout(token);
            if (logoutSuccess) {
                log.info("로그아웃 성공");
                response.put("message", "로그아웃 성공");
                response.put("data", new Object[]{});
                return ResponseEntity.ok(response);
            } else {
                log.warn("로그아웃 실패");
                response.put("message", "로그아웃 실패");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            log.error("로그아웃중 에러 발생: " + e);
            response.put("message", "로그아웃 처리 중 오류 발생");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}