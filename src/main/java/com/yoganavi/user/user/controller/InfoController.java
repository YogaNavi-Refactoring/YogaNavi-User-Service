package com.yoganavi.user.user.controller;

import com.yoganavi.user.user.dto.edit.UpdateDto;
import com.yoganavi.user.user.service.info.InfoService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/info")
public class InfoController {

    private final InfoService infoService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInfo(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-Role") String role) {

        Map<String, Object> response = new HashMap<>();

        try {
            UpdateDto userInfo = infoService.getUserInfo(userId, role);
            response.put("message", "조회 성공");
            response.put("data", userInfo);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("message", "사용자 찾을 수 없음");
            response.put("data", new Object[]{});
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            response.put("message", "내 정보 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
