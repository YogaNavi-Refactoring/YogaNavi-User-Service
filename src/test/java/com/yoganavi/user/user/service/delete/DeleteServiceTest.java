package com.yoganavi.user.user.service.delete;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteServiceTest {

    @InjectMocks
    private DeleteServiceImpl deleteService;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<Users> usersCaptor;

    private Users validUser;
    private Instant now;

    @BeforeEach
    void setUp() {
        validUser = new Users();
        validUser.setUserId(1L);
        validUser.setEmail("test@example.com");
        validUser.setNickname("테스트유저");
        validUser.setProfileImageUrl("http://example.com/image.jpg");
        validUser.setProfileImageUrlSmall("http://example.com/image_small.jpg");
        validUser.setContent("테스트 소개글");
        validUser.setFcmToken("fcm-token");
        validUser.setIsDeleted(false);

        now = Instant.now();
    }

    @Nested
    @DisplayName("회원 탈퇴 요청 테스트")
    class RequestDeleteUserTest {

        @Test
        @DisplayName("정상적인 회원 탈퇴 요청 성공")
        void 정상_회원탈퇴요청_성공() {
            // given
            when(userRepository.findById(validUser.getUserId())).thenReturn(Optional.of(validUser));
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            deleteService.requestDeleteUser(validUser.getUserId());

            // then
            verify(userRepository, times(1)).save(usersCaptor.capture());
            Users savedUser = usersCaptor.getValue();
            assertThat(savedUser.getDeletedAt()).isNotNull();
            assertThat(savedUser.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 회원 탈퇴 요청")
        void 존재하지않는회원_탈퇴요청_실패() {
            // given
            when(userRepository.findById(validUser.getUserId())).thenReturn(Optional.empty());

            // when, then
            assertThatThrownBy(() -> deleteService.requestDeleteUser(validUser.getUserId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자 없음");

            verify(userRepository, times(0)).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("회원 삭제 처리 테스트")
    class ProcessDeletedUsersTest {

        @Test
        @DisplayName("정상적인 회원 삭제 처리 성공")
        void 정상_회원삭제처리_성공() {
            // given
            validUser.setDeletedAt(now.minus(8, ChronoUnit.DAYS));
            when(userRepository.findByDeletedAtBeforeAndIsDeletedFalse(any(Instant.class)))
                .thenReturn(Arrays.asList(validUser));
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            deleteService.processDeletedUsers();

            // then
            verify(userRepository, times(1)).save(any(Users.class));
            verify(userRepository).save(usersCaptor.capture());
            Users deletedUser = usersCaptor.getValue();
            assertThat(deletedUser.getIsDeleted()).isTrue();
            assertThat(deletedUser.getEmail()).startsWith("deleted_");
            assertThat(deletedUser.getNickname()).startsWith("삭제된 사용자");
            assertThat(deletedUser.getProfileImageUrl()).isNull();
            assertThat(deletedUser.getProfileImageUrlSmall()).isNull();
            assertThat(deletedUser.getContent()).isNull();
            assertThat(deletedUser.getFcmToken()).isNull();
        }

        @Test
        @DisplayName("삭제 대상 회원이 없는 경우")
        void 삭제대상회원없음() {
            // given
            when(userRepository.findByDeletedAtBeforeAndIsDeletedFalse(any(Instant.class)))
                .thenReturn(Arrays.asList());

            // when
            deleteService.processDeletedUsers();

            // then
            verify(userRepository, times(0)).save(any(Users.class));
        }

        @Test
        @DisplayName("여러 회원 동시 삭제 처리")
        void 다중회원_삭제처리_성공() {
            // given
            Users user2 = new Users();
            user2.setUserId(2L);
            user2.setEmail("test2@example.com");
            user2.setNickname("테스트유저2");
            user2.setDeletedAt(now.minus(8, ChronoUnit.DAYS));
            user2.setIsDeleted(false);

            validUser.setDeletedAt(now.minus(8, ChronoUnit.DAYS));

            when(userRepository.findByDeletedAtBeforeAndIsDeletedFalse(any(Instant.class)))
                .thenReturn(Arrays.asList(validUser, user2));
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            deleteService.processDeletedUsers();

            // then
            verify(userRepository, times(2)).save(any(Users.class));
        }
    }

    @Nested
    @DisplayName("회원 데이터 익명화 테스트")
    class AnonymizeUserDataTest {

        @Test
        @DisplayName("회원 데이터 익명화 성공")
        void 회원데이터_익명화_성공() {
            // given
            validUser.setDeletedAt(now.minus(8, ChronoUnit.DAYS));
            when(userRepository.save(any(Users.class))).thenReturn(validUser);

            // when
            deleteService.processDeletedUser(validUser);

            // then
            verify(userRepository, times(1)).save(any(Users.class));
            verify(userRepository).save(usersCaptor.capture());
            Users anonymizedUser = usersCaptor.getValue();
            assertThat(anonymizedUser.getEmail()).isEqualTo(
                "deleted_" + validUser.getUserId() + "@yoganavi.com");
            assertThat(anonymizedUser.getNickname()).isEqualTo("삭제된 사용자" + validUser.getUserId());
            assertThat(anonymizedUser.getProfileImageUrl()).isNull();
            assertThat(anonymizedUser.getProfileImageUrlSmall()).isNull();
            assertThat(anonymizedUser.getContent()).isNull();
            assertThat(anonymizedUser.getFcmToken()).isNull();
            assertThat(anonymizedUser.getIsDeleted()).isTrue();
        }
    }
}