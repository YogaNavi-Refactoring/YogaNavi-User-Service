package com.yoganavi.user.user.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Duration TOKEN_VALIDITY_DURATION = Duration.ofMinutes(5);
    private static final Duration VERIFICATION_VALIDITY_DURATION = Duration.ofMinutes(30);

    private final JavaMailSender emailSender;
    private final RedisTemplate<String, Object> redisTemplate;

    public String sendVerificationEmail(String email, String purpose) {
        log.info("{} 인증 토큰 전송 시작: {}", purpose, email);

        try {
//            String token = generateToken();
            String token = "123456";
            String redisKey = getRedisKey(email, purpose);

            // 기존 토큰이 있다면 삭제
            redisTemplate.delete(redisKey);

            // redis에 토큰 저장
            redisTemplate.opsForValue().set(
                redisKey,
                token,
                TOKEN_VALIDITY_DURATION
            );

            String subject = String.format("모두의 음악 %s", purpose);
            String message = String.format("%s 인증번호 : %s\n이 인증번호는 5분 동안 유효합니다.", purpose, token);

            sendSimpleMessage(email, subject, message);

            log.info("{} 인증 토큰 전송 완료: {}", purpose, email);
            return "인증 번호 전송";

        } catch (Exception e) {
            log.error("{} 인증 토큰 전송 실패", purpose, e);
            return "인증 번호 전송 실패. 잠시 후 다시 시도해 주세요.";
        }
    }

    public boolean validateToken(String email, String token, String purpose) {
        log.info("{}의 입력 인증번호: {}", email, token);
        String redisKey = getRedisKey(email, purpose);
        Object storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken != null && token.equals(storedToken.toString())) {
            // 유효성 검증 성공 시 토큰 삭제
            redisTemplate.delete(redisKey);

            // 인증 완료 상태 저장
            String verifiedKey = getVerifiedKey(email, purpose);
            redisTemplate.opsForValue().set(
                verifiedKey,
                true,
                VERIFICATION_VALIDITY_DURATION
            );
            return true;
        }
        return false;
    }

    public boolean isVerified(String email, String purpose) {
        String verifiedKey = getVerifiedKey(email, purpose);
        Boolean isVerified = (Boolean) redisTemplate.opsForValue().get(verifiedKey);
        return isVerified != null && isVerified;
    }

    public void clearVerificationStatus(String email, String purpose) {
        String verifiedKey = getVerifiedKey(email, purpose);
        redisTemplate.delete(verifiedKey);
    }

    private void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    private String generateToken() {
        return Integer.toString((int) (Math.random() * 899999) + 100000);
    }

    private String getRedisKey(String email, String purpose) {
        return String.format("%s:token:%s", purpose, email);
    }

    private String getVerifiedKey(String email, String purpose) {
        return String.format("%s:verified:%s", purpose, email);
    }
}