package com.yoganavi.user.controller;

import com.yoganavi.user.common.constants.SecurityConstants;
import com.yoganavi.user.service.jwt.JwtService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/token")
public class JwtController {

    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> reissueToekn(
        @RequestHeader(SecurityConstants.JWT_HEADER) String token,
        @RequestHeader(SecurityConstants.REFRESH_TOKEN_HEADER) String refreshToken) {
        log.info("member - 토큰 재발급 요청. access token => {}, refresh token => {}", token,
            refreshToken);
        Map<String, Object> responseBody = new HashMap<>();
        try {
            String accessToken = jwtService.reIssueRefreshToken(token, refreshToken);

            responseBody.put("code", "200");
            responseBody.put("data", null);
            return ResponseEntity.ok()
                .header(SecurityConstants.JWT_HEADER, "Bearer " + accessToken).body(responseBody);
        } catch (Exception e) {
            responseBody.put("code", "401");
            responseBody.put("message", "토큰 재발급 불가");
            return ResponseEntity.ok().body(responseBody);
        }
    }
}
