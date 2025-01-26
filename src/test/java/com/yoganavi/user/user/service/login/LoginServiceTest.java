package com.yoganavi.user.user.service.login;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.common.util.JwtUtil;
import com.yoganavi.user.user.dto.login.LoginRequestDto;
import com.yoganavi.user.user.dto.login.LoginResponseDto;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @InjectMocks
    private LoginServiceImpl loginService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private LoginRequestDto validLoginRequest;
    private Users validUser;
    private String rawPassword = "password123";
    private String encodedPassword = "encodedPassword123";

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequestDto();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword(rawPassword);

        validUser = new Users();
        validUser.setUserId(1L);
        validUser.setEmail("test@example.com");
        validUser.setPwd(encodedPassword);
        validUser.setNickname("테스트유저");
        validUser.setRole("STUDENT");
        validUser.setIsDeleted(false);
    }

    @Nested
    @DisplayName("일반 로그인 테스트")
    class NormalLoginTest {

        @Test
        @DisplayName("정상적인 로그인 성공")
        void 정상_로그인_성공() {
            // given
            when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(validUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access.token.here");
            when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh.token.here");

            // when
            LoginResponseDto response = loginService.login(validLoginRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access.token.here");
            assertThat(response.getRefreshToken()).isEqualTo("refresh.token.here");
            assertThat(response.getMessage()).isEqualTo("로그인 성공");
            verify(jwtUtil).generateAccessToken(validUser.getEmail(), validUser.getRole());
            verify(jwtUtil).generateRefreshToken(validUser.getEmail());
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시도")
        void 존재하지않는_이메일_로그인_실패() {
            // given
            when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.empty());

            // when, then
            assertThatThrownBy(() -> loginService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("사용자가 존재하지 않습니다.");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtUtil, never()).generateAccessToken(anyString(), anyString());
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 시도")
        void 잘못된_비밀번호_로그인_실패() {
            // given
            when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(validUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

            // when, then
            assertThatThrownBy(() -> loginService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");

            verify(jwtUtil, never()).generateAccessToken(anyString(), anyString());
        }

        @Test
        @DisplayName("삭제된 계정으로 로그인 시도")
        void 삭제된_계정_로그인_실패() {
            // given
            validUser.setIsDeleted(true);
            when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(validUser));

            // when, then
            assertThatThrownBy(() -> loginService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("계정이 삭제되었습니다.");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(jwtUtil, never()).generateAccessToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("탈퇴 진행 중인 계정 로그인 테스트")
    class DeletedAccountLoginTest {

        @Test
        @DisplayName("탈퇴 진행 중인 계정 로그인 시 계정 복구 성공")
        void 탈퇴진행중_계정_복구_성공() {
            // given
            validUser.setDeletedAt(Instant.now());
            when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(validUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
            when(userRepository.findById(validUser.getUserId())).thenReturn(Optional.of(validUser));
            when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access.token.here");
            when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh.token.here");

            // when
            LoginResponseDto response = loginService.login(validLoginRequest);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotNull();
            verify(userRepository).save(any(Users.class));
        }

        @Test
        @DisplayName("탈퇴 진행 중인 계정 잘못된 비밀번호로 로그인 시도")
        void 탈퇴진행중_계정_잘못된_비밀번호_실패() {
            // given
            validUser.setDeletedAt(Instant.now());
            when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(validUser));
            when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

            // when, then
            assertThatThrownBy(() -> loginService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");

            verify(userRepository, never()).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("계정 복구 테스트")
    class AccountRecoveryTest {

        @Test
        @DisplayName("계정 복구 처리 성공")
        void 계정_복구_처리_성공() {
            // given
            validUser.setDeletedAt(Instant.now());
            when(userRepository.findById(validUser.getUserId())).thenReturn(Optional.of(validUser));
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            boolean result = loginService.recoverAccount(validUser);

            // then
            assertThat(result).isTrue();
            verify(userRepository).save(any(Users.class));
        }

        @Test
        @DisplayName("존재하지 않는 계정 복구 시도")
        void 존재하지않는_계정_복구_실패() {
            // given
            when(userRepository.findById(validUser.getUserId())).thenReturn(Optional.empty());

            // when
            boolean result = loginService.recoverAccount(validUser);

            // then
            assertThat(result).isFalse();
            verify(userRepository, never()).save(any(Users.class));
        }

        @Test
        @DisplayName("null 계정 복구 시도")
        void NULL_계정_복구_실패() {
            // when
            boolean result = loginService.recoverAccount(null);

            // then
            assertThat(result).isFalse();
            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).save(any());
        }
    }
}