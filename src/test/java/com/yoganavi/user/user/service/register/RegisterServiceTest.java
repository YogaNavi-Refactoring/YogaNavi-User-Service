package com.yoganavi.user.user.service.register;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.user.dto.register.RegisterDto;
import com.yoganavi.user.user.service.email.EmailService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @InjectMocks
    private RegisterServiceImpl registerService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterDto validRegisterDto;
    private Users validUser;

    @BeforeEach
    void setUp() {
        validRegisterDto = new RegisterDto();
        validRegisterDto.setEmail("test@example.com");
        validRegisterDto.setPassword("password123");
        validRegisterDto.setNickname("테스트유저");
        validRegisterDto.setTeacher(false);

        validUser = new Users();
        validUser.setUserId(1L);
        validUser.setEmail(validRegisterDto.getEmail());
        validUser.setPwd("encodedPassword");
        validUser.setNickname(validRegisterDto.getNickname());
        validUser.setRole("STUDENT");
        validUser.setIsDeleted(false);
    }

    @Nested
    @DisplayName("이메일 인증번호 전송 테스트")
    class SendEmailTokenTest {

        @Test
        @DisplayName("신규 사용자의 이메일 인증번호 전송 성공")
        void 신규사용자_이메일인증번호_전송_성공() {
            // given
            String email = "new@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(emailService.sendVerificationEmail(email, "회원가입")).thenReturn("인증 번호 전송");

            // when
            String result = registerService.sendEmailToken(email);

            // then
            assertThat(result).isEqualTo("인증 번호 전송");
            verify(emailService).sendVerificationEmail(email, "회원가입");
        }

        @Test
        @DisplayName("이미 존재하는 이메일로 인증번호 전송 시도")
        void 기존사용자_이메일인증번호_전송_실패() {
            // given
            String email = "existing@example.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(new Users()));

            // when
            String result = registerService.sendEmailToken(email);

            // then
            assertThat(result).isEqualTo("이미 존재하는 회원입니다.");
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("삭제된 계정 이메일 형식으로 인증번호 전송 시도")
        void 삭제된계정_이메일형식_전송_실패() {
            // given
            String email = "deleted_123@yoganavi.com";

            // when
            String result = registerService.sendEmailToken(email);

            // then
            assertThat(result).isEqualTo("가입할 수 없습니다.");
            verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("이메일 인증번호 확인 테스트")
    class ValidateEmailTokenTest {

        @Test
        @DisplayName("올바른 인증번호로 검증 성공")
        void 이메일인증번호_검증_성공() {
            // given
            String email = "test@example.com";
            String token = "123456";
            when(emailService.validateToken(email, token, "회원가입")).thenReturn(true);

            // when
            boolean result = registerService.validateEmailToken(email, token);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("잘못된 인증번호로 검증 실패")
        void 이메일인증번호_검증_실패() {
            // given
            String email = "test@example.com";
            String token = "wrong123";
            when(emailService.validateToken(email, token, "회원가입")).thenReturn(false);

            // when
            boolean result = registerService.validateEmailToken(email, token);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("회원 가입 처리 테스트")
    class RegisterUserTest {

        @Test
        @DisplayName("정상적인 회원가입 성공")
        void 정상_회원가입_성공() {
            // given
            when(emailService.isVerified(validRegisterDto.getEmail(), "회원가입")).thenReturn(true);
            when(passwordEncoder.encode(validRegisterDto.getPassword())).thenReturn(
                "encodedPassword");
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            Users result = registerService.registerUser(validRegisterDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(validRegisterDto.getEmail());
            assertThat(result.getNickname()).isEqualTo(validRegisterDto.getNickname());
            assertThat(result.getRole()).isEqualTo("STUDENT");
            verify(emailService).clearVerificationStatus(validRegisterDto.getEmail(), "회원가입");
        }

        @Test
        @DisplayName("강사로 회원가입 성공")
        void 강사_회원가입_성공() {
            // given
            validRegisterDto.setTeacher(true);
            validUser.setRole("TEACHER");

            when(emailService.isVerified(validRegisterDto.getEmail(), "회원가입")).thenReturn(true);
            when(passwordEncoder.encode(validRegisterDto.getPassword())).thenReturn(
                "encodedPassword");
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            Users result = registerService.registerUser(validRegisterDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo("TEACHER");
        }

        @Test
        @DisplayName("이메일 미인증 상태로 회원가입 시도")
        void 이메일미인증_회원가입_실패() {
            // given
            when(emailService.isVerified(validRegisterDto.getEmail(), "회원가입")).thenReturn(false);

            // when, then
            assertThatThrownBy(() -> registerService.registerUser(validRegisterDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이메일 인증이 완료되지 않았습니다.");

            verify(userRepository, never()).save(any(Users.class));
        }

        @Test
        @DisplayName("중복된 이메일로 회원가입 시도")
        void 중복이메일_회원가입_실패() {
            // given
            when(emailService.isVerified(validRegisterDto.getEmail(), "회원가입")).thenReturn(true);
            when(passwordEncoder.encode(validRegisterDto.getPassword())).thenReturn(
                "encodedPassword");
            when(userRepository.save(any(Users.class))).thenThrow(
                new DataIntegrityViolationException("중복된 이메일"));

            // when, then
            assertThatThrownBy(() -> registerService.registerUser(validRegisterDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("회원 정보 저장 중 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("중복된 닉네임으로 회원가입 시도")
        void 중복닉네임_회원가입_실패() {
            // given
            when(emailService.isVerified(validRegisterDto.getEmail(), "회원가입")).thenReturn(true);
            when(passwordEncoder.encode(validRegisterDto.getPassword())).thenReturn(
                "encodedPassword");
            when(userRepository.save(any(Users.class))).thenThrow(
                new DataIntegrityViolationException("중복된 닉네임"));

            // when, then
            assertThatThrownBy(() -> registerService.registerUser(validRegisterDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("회원 정보 저장 중 오류가 발생했습니다.");
        }
    }
}