package com.yoganavi.user.user.service.delete;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeleteServiceImpl implements DeleteService {

    private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Duration DELETE_DELAY = Duration.ofDays(7);

    private final UserRepository userRepository;

    @Override
    public void requestDeleteUser(Long userId) {
        log.info("사용자 삭제 요청: 사용자 ID {}", userId);
        Users user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자 없음"));

        ZonedDateTime nowKorea = ZonedDateTime.now(KOREA_ZONE_ID);
        ZonedDateTime deletionTimeKorea = nowKorea.plus(DELETE_DELAY);

        user.setDeletedAt(deletionTimeKorea.toInstant());
        userRepository.save(user);
        log.info("사용자 {} 삭제 예정: {}", userId, deletionTimeKorea);
    }

    @Override
    @Transactional
    public void processDeletedUsers() {
        log.info("삭제 예정 사용자 처리 시작");
        ZonedDateTime nowKorea = ZonedDateTime.now(KOREA_ZONE_ID);

        List<Users> usersToDelete = userRepository.findByDeletedAtBeforeAndIsDeletedFalse(
            nowKorea.toInstant());
        for (Users user : usersToDelete) {
            try {
                processDeletedUser(user);
            } catch (Exception e) {
                log.error("사용자 {} 삭제중 에러 발생: {}", user.getUserId(), e.getMessage());
            }
        }
        log.info("삭제 예정 사용자 처리 완료: 처리된 사용자 수 {}", usersToDelete.size());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processDeletedUser(Users user) {
        log.info("사용자 삭제 처리 시작: 사용자 ID {}", user.getUserId());
        user.setIsDeleted(true);
        userRepository.save(anonymizeUserData(user));
        log.info("사용자 삭제 처리 완료: 사용자 ID {}", user.getUserId());
    }

    private Users anonymizeUserData(Users user) {
        log.debug("사용자 데이터 익명화 시작: 사용자 ID {}", user.getUserId());
        user.setEmail("deleted_" + user.getUserId() + "@yoganavi.com");
        user.setNickname("삭제된 사용자" + user.getUserId());
        user.setProfileImageUrl(null);
        user.setProfileImageUrlSmall(null);
        user.setContent(null);
        user.setFcmToken(null);
        log.info("사용자 {} 익명화 완료", user.getUserId());
        return user;
    }

}
