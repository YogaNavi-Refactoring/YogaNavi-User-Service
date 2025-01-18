package com.yoganavi.user.service.register;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.dto.register.RegisterDto;
import com.yoganavi.user.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private static final String PURPOSE = "회원가입";

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String sendEmailToken(String email) {
        int valCase = checkUser(email);
        if (valCase == 0) {
            return emailService.sendVerificationEmail(email, PURPOSE);
        } else if (valCase == 1) {
            return "이미 존재하는 회원입니다.";
        } else {
            return "가입할 수 없습니다.";
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public int checkUser(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            return 1;
        }
        if (email.matches("deleted_\\d+@yoganavi\\.com")) {
            return 2;
        }
        return 0;
    }

    @Override
    public boolean validateEmailToken(String email, String token) {
        return emailService.validateToken(email, token, PURPOSE);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Users registerUser(RegisterDto registerDto) {
        log.info("사용자 등록 시작: 이메일 {}", registerDto.getEmail());

        try {
            if (!emailService.isVerified(registerDto.getEmail(), PURPOSE)) {
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

            emailService.clearVerificationStatus(registerDto.getEmail(), PURPOSE);

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