package com.yoganavi.user.user.service.register;

import com.yoganavi.kafka.service.KafkaProducerService;
import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.user.dto.register.RegisterDto;
import com.yoganavi.user.user.service.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegisterSyncServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private KafkaProducerService kafkaEventService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegisterServiceImpl registerService;

    private RegisterDto registerDto;

    @BeforeEach
    void setUp() {
        registerDto = new RegisterDto();
        registerDto.setEmail("test@test.com");
        registerDto.setPassword("password");
        registerDto.setNickname("testUser");
        registerDto.setTeacher(true);
    }

    @Test
    void 회원가입_성공() {
        // Given
        given(emailService.isVerified(registerDto.getEmail(), "회원가입")).willReturn(true);
        given(passwordEncoder.encode(any())).willReturn("encodedPassword");

        Users savedUser = new Users();
        savedUser.setUserId(1L);
        savedUser.setEmail(registerDto.getEmail());
        savedUser.setRole("TEACHER");
        given(userRepository.save(any())).willReturn(savedUser);

        // When
        Users result = registerService.registerUser(registerDto);

        // Then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(registerDto.getEmail());
        assertThat(result.getRole()).isEqualTo("TEACHER");

        // Kafka 이벤트 발행 확인
        verify(kafkaEventService).publishUserCreatedEvent(result);

        // 이메일 인증 상태 초기화 확인
        verify(emailService).clearVerificationStatus(registerDto.getEmail(), "회원가입");
    }

    @Test
    void 이메일_인증_미완() {
        // Given
        given(emailService.isVerified(registerDto.getEmail(), "회원가입")).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> registerService.registerUser(registerDto))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("이메일 인증이 완료되지 않았습니다.");
    }

    @Test
    void DB_저장_실패() {
        // Given
        given(emailService.isVerified(registerDto.getEmail(), "회원가입")).willReturn(true);
        given(passwordEncoder.encode(any())).willReturn("encodedPassword");
        given(userRepository.save(any())).willThrow(new RuntimeException("DB 에러"));

        // When & Then
        assertThatThrownBy(() -> registerService.registerUser(registerDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("회원가입 처리 중 오류가 발생했습니다.");
    }
}