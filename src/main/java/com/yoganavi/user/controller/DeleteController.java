package com.yoganavi.user.controller;

import com.yoganavi.user.common.constants.SecurityConstants;
import com.yoganavi.user.common.util.JwtUtil;
import com.yoganavi.user.service.delete.DeleteService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/delete")
@RequiredArgsConstructor
public class DeleteController {

    private final JwtUtil jwtUtil;
    private final DeleteService deleteService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> requestDeleteUser(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader(SecurityConstants.JWT_HEADER) String token) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("회원 탈퇴 요청 :  사용자 {}", userId);
            deleteService.requestDeleteUser(userId);
            jwtUtil.logout(token);  // 회원 탈퇴 시 로그아웃 처리
            response.put("message",
                "탈퇴 요청이 성공적으로 처리되었습니다. 7일 후에 계정이 삭제됩니다. 7일 이내에 로그인 시 자동으로 탈퇴가 취소됩니다.");
            response.put("data", new Object[]{});
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "탈퇴 요청 처리 중 오류 발생");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
