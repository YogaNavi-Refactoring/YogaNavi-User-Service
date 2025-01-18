package com.yoganavi.user.controller;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.dto.edit.UpdateDto;
import com.yoganavi.user.service.edit.EditService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/edit")
public class EditController {

    private final EditService editService;

    // 인증번호 전송
    @PostMapping("/credential/email")
    public ResponseEntity<Map<String, Object>> sendAuthNumber(
        @RequestBody UpdateDto updateDto) {
        log.info("회원가입 이메일 인증 요청: 이메일 {}", updateDto.getEmail());

        try {
            String result = editService.sendCredentialToken(updateDto.getEmail());

            if (result.equals("존재하지 않는 회원입니다.")) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 500);
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            // 인증번호 전송 실패한 경우
            if (result.contains("실패")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 500);
                errorResponse.put("message", "내부 서버 에러가 발생했습니다.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            // 성공 응답
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("code", 200);
            successResponse.put("data", null);
            return ResponseEntity.ok(successResponse);

        } catch (IllegalArgumentException e) { // 이메일 검증 추가 필요
            // 이메일 형식이 유효하지 않은 경우
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 604);
            errorResponse.put("message", "이메일 형식이 유효하지 않습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            // 기타 서버 에러
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "내부 서버 에러가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 토큰 확인
    @PostMapping("/credential/token")
    public ResponseEntity<Map<String, Object>> checkAuthNumber(
        @RequestBody UpdateDto updateDto) {

        try {
            boolean isValid = editService.validateToken(
                updateDto.getEmail(),
                String.valueOf(updateDto.getAuthnumber())
            );

            if (isValid) {
                // 성공
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("code", 200);
                successResponse.put("data", null);
                return ResponseEntity.ok(successResponse);
            } else {
                // 인증번호 불일치
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("code", 603);
                errorResponse.put("message", "인증 번호가 일치하지 않습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            // 서버 에러
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "내부 서버 에러가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    //비밀번호 재설정
    @PostMapping("/credential")
    public ResponseEntity<Map<String, Object>> setNewCredential(
        @RequestBody UpdateDto updateDto) {
        log.info("비밀번호 재설정 요청 이메일: {}", updateDto.getEmail());

        try {
            Users savedUser = editService.setNewCredential(updateDto);
            log.info("비밀번호 재설정 성공");

            // 성공 응답
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("code", 200);
            successResponse.put("data", null);
            return ResponseEntity.ok(successResponse);

        } catch (IllegalStateException e) {
            // 보안 문제로 인해 비밀번호 재설정 불가
            log.info("재설정 불가: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();

            errorResponse.put("code", 702);
            errorResponse.put("message", "잘못된 요청입니다");

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // 기타 서버 에러
            log.error("비밀번호 재설정 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "내부 서버 에러가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    //비밀번호 확인
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkCredential(
        @RequestBody UpdateDto updateDto, @RequestHeader("X-User-Id") Long userId) {
        log.info("비밀번호 확인 id: {}", userId);

        Map<String, Object> response = new HashMap<>();
        try {
            boolean result = editService.checkCredential(userId,
                updateDto.getPassword());

            if (result) {
                response.put("message", "success");
                response.put("data", true);
            } else {
                response.put("message", "fail");
                response.put("data", false);
            }

        } catch (Exception e) {
            // 기타 서버 에러
            log.error("비밀번호 확인 실패", e);
            response.put("code", 500);
            response.put("message", "내부 서버 에러가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> updateInfo(@RequestBody UpdateDto updateDto,
        @RequestHeader("X-User-Id") Long userId) {
        log.info("회원 정보 수정 id: {}", userId);

        Map<String, Object> response = new HashMap<>();
        try {
            Users user = editService.updateUser(updateDto, userId);

            log.info("사용자 정보 수정 요청 :  사용자 {}", userId);
            if (user != null) {
                UpdateDto responseDto = new UpdateDto();
                boolean isTeacher = user.getRole().equals("TEACHER");
                responseDto.setImageUrl(user.getProfileImageUrl());
                responseDto.setImageUrlSmall(user.getProfileImageUrlSmall());
                responseDto.setNickname(user.getNickname());
                responseDto.setTeacher(isTeacher);

                if (isTeacher) {
                    Set<String> myTags = editService.getUserHashtags(userId);
                    List<String> tags = new ArrayList<>(myTags);
                    responseDto.setHashTags(tags);
                    responseDto.setContent(user.getContent());
                }

                response.put("message", "수정 완료");
                response.put("data", responseDto);
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "사용자 찾을 수 없음");
                response.put("data", new Object[]{});
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (IllegalStateException e) {
            // 보안 문제로 인해 정보 수정 불가
            log.info("회원 정보 수정 불가: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();

            errorResponse.put("code", 702);
            errorResponse.put("message", "잘못된 요청입니다");

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            response.put("message", "수정 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
