package com.yoganavi.user.service.email;

import com.yoganavi.user.common.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender emailSender;

    private static final Duration TOKEN_VALIDITY_DURATION = Duration.ofMinutes(5);
    private static final Duration VERIFICATION_VALIDITY_DURATION = Duration.ofMinutes(30);
    private static final String EMAIL_TOKEN_PREFIX = "email:token:";
    private static final String EMAIL_VERIFIED_PREFIX = "email:verified:";

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public String sendEmailToken(String email) {
        log.info("이메일 인증 토큰 전송 시작: {}", email);

        int valCase = checkUser(email);
        if (valCase == 0) {
            try {
                String token = generateToken();
                String redisKey = getRedisKey(email);

                // 기존 토큰이 있다면 삭제
                redisTemplate.delete(redisKey);

                // Redis에 토큰 저장
                redisTemplate.opsForValue().set(
                    redisKey,
                    token,
                    TOKEN_VALIDITY_DURATION
                );

                emailService.sendSimpleMessage(email, "모두의 음악 회원가입 인증번호",
                    "회원가입 인증번호 : " + token + "\n이 인증번호는 5분 동안 유효합니다.");

                log.info("이메일 인증 토큰 전송 완료: {}", email);

                return "인증 번호 전송";
            } catch (Exception e) {
                log.error("이메일 인증 토큰 전송 실패", e);
                return "인증 번호 전송 실패. 잠시 후 다시 시도해 주세요.";
            }
        } else if (valCase == 1) {
            return "이미 존재하는 회원입니다.";
        } else {
            return "가입할 수 없습니다.";
        }
    }

    private String generateToken() {
        return Integer.toString((int) (Math.random() * 899999) + 100000);
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public int checkUser(String email) {
        // db에 이메일 존재 하는지
        if (userRepository.findByEmail(email).isPresent()) {
            return 1;
        }
        // 이메일이 삭제된 사용자의 패턴과 일치?
        if (email.matches("deleted_\\d+@yoganavi\\.com")) {
            return 2;
        }
        // 해당하지 않으면 사용 가능
        return 0;
    }

    public boolean validateEmailToken(String email, String token) {
        log.info("{}의 입력 인증번호: {}", email, token);
        String redisKey = getRedisKey(email);
        Object storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken != null && token.equals(storedToken.toString())) {
            // 유효성 검증 성공 시 토큰 삭제
            redisTemplate.delete(redisKey);

            // 이메일 인증 완료 상태 저장
            String verifiedKey = EMAIL_VERIFIED_PREFIX + email;
            redisTemplate.opsForValue().set(
                verifiedKey,
                true,
                VERIFICATION_VALIDITY_DURATION
            );
            return true;
        }
        return false;
    }

    private String getRedisKey(String email) {
        return EMAIL_TOKEN_PREFIX + email;
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }
}
