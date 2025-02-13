package com.yoganavi.kafka.service;

import com.yoganavi.kafka.entity.UserEventLog;
import com.yoganavi.kafka.event.UserEvent;
import com.yoganavi.kafka.event.UserEvent.EventStatus;
import com.yoganavi.kafka.repository.UserEventLogRepository;
import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSyncResultService {

    private final UserRepository userRepository;
    private final UserEventLogRepository eventLogRepository;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @KafkaListener(topics = "${spring.kafka.topics.user-sync-result}")
    public void handleUserSyncResult(UserEvent event) {
        try {
            if (event.getStatus() == EventStatus.FAILED) {
                compensateFailedRegistration(event);
            }
        } catch (Exception e) {
            log.error("보상 트랜잭션 실패. userId: {}, transactionId: {}",
                event.getUserId(), event.getTransactionId(), e);
            handleCompensationFailure(event, e);
        }
    }

    private void handleCompensationFailure(UserEvent event, Exception e) {
        // 실패한 보상 트랜잭션 로깅
        UserEventLog eventLog = UserEventLog.start(event.getUserId(), "COMPENSATION_FAILED");
        eventLog.fail(e.getMessage());
        eventLogRepository.save(eventLog);

        throw new RegistrationRollbackException(String.format(
            "회원가입 보상 트랜잭션 처리 중 오류가 발생했습니다. userId: %d, transactionId: %s, error: %s",
            event.getUserId(),
            event.getTransactionId(),
            e.getMessage()
        ));
    }

    private void compensateFailedRegistration(UserEvent event) {
        // 멱등성 체크
        if (!userRepository.existsById(event.getUserId())) {
            log.info("보상 트랜잭션 불필요: 사용자가 이미 존재하지 않음. userId: {}", event.getUserId());
            return;
        }

        UserEventLog eventLog = UserEventLog.start(event.getUserId(), "COMPENSATION");
        try {
            // 사용자 삭제
            userRepository.deleteById(event.getUserId());

            // 보상 트랜잭션 완료 로깅
            eventLog.complete();
            eventLogRepository.save(eventLog);

            log.info("보상 트랜잭션 완료. userId: {}, transactionId: {}",
                event.getUserId(), event.getTransactionId());

        } catch (Exception e) {
            eventLog.fail(e.getMessage());
            eventLogRepository.save(eventLog);
            throw e;
        }
    }

    // DLQ 리스너
    @KafkaListener(topics = "${spring.kafka.topics.user-sync-result-dlq}")
    public void handleDlqMessage(UserEvent event) {
        log.error("DLQ로 이동된 동기화 결과 메시지 발견. userId: {}, status: {}",
            event.getUserId(), event.getStatus());
    }

    public static class RegistrationRollbackException extends RuntimeException {

        public RegistrationRollbackException(String message) {
            super(message);
        }
    }

}