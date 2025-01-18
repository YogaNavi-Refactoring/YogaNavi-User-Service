package com.yoganavi.user.controller;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.dto.register.RegisterDto;
import com.yoganavi.user.service.register.RegisterService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/register")
public class RegisterController {

    private final RegisterService registerService;

    /**
     * 회원가입을 위한 인증번호 이메일로 전송
     *
     * @param registerDto 이메일 정보를 담은 DTO
     * @return 인증번호 전송 결과
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> registerUserEmail(
        @RequestBody RegisterDto registerDto) {
        log.info("회원가입 이메일 인증 요청: 이메일 {}", registerDto.getEmail());

        try {
            String result = registerService.sendEmailToken(registerDto.getEmail());

            // 이미 존재하는 회원인 경우도 200으로 처리하고 메시지만 다르게
            if (result.equals("이미 존재하는 회원입니다.")) {
                Map<String, Object> response = new HashMap<>();
                response.put("code", 500);
                response.put("data", null);
                return ResponseEntity.ok(response);
            }

            if (result.equals("가입할 수 없습니다.")) {
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

    /**
     * 회원가입 시 전송된 인증번호 확인
     *
     * @param registerDto 인증번호를 담은 DTO
     * @return 인증번호 확인 결과
     */
    @PostMapping("/check/token")
    public ResponseEntity<Map<String, Object>> checkAuthNumber(
        @RequestBody RegisterDto registerDto) {

        try {
            boolean isValid = registerService.validateEmailToken(
                registerDto.getEmail(),
                String.valueOf(registerDto.getAuthnumber())
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

    /**
     * 회원 가입 처리
     *
     * @param registerDto 회원 가입에 필요한 정보 DTO
     * @return 회원 가입 결과
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> registerUser(@RequestBody RegisterDto registerDto) {
        log.info("회원 가입 요청: 이메일 {}", registerDto.getEmail());

        try {
            Users savedUser = registerService.registerUser(registerDto);
            log.info("회원가입 성공");

            // 성공 응답
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("code", 200);
            successResponse.put("data", null);
            return ResponseEntity.ok(successResponse);

        } catch (IllegalStateException e) {
            // 보안 문제로 인해 회원가입 불가
            log.info("회원가입 불가: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();

            errorResponse.put("code", 702);
            errorResponse.put("message", "잘못된 요청입니다");

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // 기타 서버 에러
            log.error("회원가입 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "내부 서버 에러가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

}

