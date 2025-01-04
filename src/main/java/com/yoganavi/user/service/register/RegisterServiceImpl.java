package com.yoganavi.user.service.register;

import static com.yoganavi.user.common.entity.Users.Role.STUDENT;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.dto.register.RegisterDto;
import com.yoganavi.user.service.email.EmailService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private static final Duration TOKEN_VALIDITY_DURATION = Duration.ofMinutes(5);
    private static final Duration VERIFICATION_VALIDITY_DURATION = Duration.ofMinutes(30);
    private static final String EMAIL_TOKEN_PREFIX = "email:token:";
    private static final String EMAIL_VERIFIED_PREFIX = "email:verified:";

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
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

    @Override
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

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Users registerUser(RegisterDto registerDto) {
        log.info("사용자 등록 시작: 이메일 {}", registerDto.getEmail());

        try {

            // 이메일 인증 완료 여부 확인
            String verifiedKey = EMAIL_VERIFIED_PREFIX + registerDto.getEmail();
            Boolean isVerified = (Boolean) redisTemplate.opsForValue().get(verifiedKey);

            if (isVerified == null || !isVerified) {
                throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
            }

            String hashPwd = passwordEncoder.encode(registerDto.getPassword());
            Users user = new Users();
            user.setEmail(registerDto.getEmail());
            user.setPwd(hashPwd);
            user.setNickname(registerDto.getNickname());
            user.setIsDeleted(false);
            user.setRole(registerDto.isTeacher() ? "TEACHER" : "STUDENT");
            Users saveMember = userRepository.save(user);

            // 인증 완료 상태 삭제
            redisTemplate.delete(verifiedKey);

            log.info("사용자 등록 완료: 사용자 ID {}", saveMember.getUserId());
            return saveMember;

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("회원가입 유효성 검증 실패: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("데이터 무결성 위반: {}", e.getMessage());
            throw new IllegalStateException("회원 정보 저장 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("회원가입 처리 중 예기치 않은 오류 발생", e);
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.");
        }
    }

}
