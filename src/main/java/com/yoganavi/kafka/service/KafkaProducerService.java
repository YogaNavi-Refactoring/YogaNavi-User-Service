package com.yoganavi.kafka.service;

import com.yoganavi.kafka.event.UserEvent;
import com.yoganavi.kafka.event.UserEvent.EventStatus;
import com.yoganavi.user.common.entity.Users;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.user-created}")
    private String userCreatedTopic;

    @Value("${spring.kafka.topics.user-updated}")
    private String userUpdatedTopic;

    @Value("${spring.kafka.topics.user-deleted}")
    private String userDeletedTopic;

    @Transactional
    public void publishUserCreatedEvent(Users user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.CREATED);
        kafkaTemplate.executeInTransaction(operations -> {
            sendMessage(userCreatedTopic, String.valueOf(user.getUserId()), event);
            return null;
        });
    }

    @Transactional
    public void publishUserUpdatedEvent(Users user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.UPDATED);
        kafkaTemplate.executeInTransaction(operations -> {
            sendMessage(userUpdatedTopic, String.valueOf(user.getUserId()), event);
            return null;
        });
    }

    @Transactional
    public void publishUserDeletedEvent(Users user) {
        UserEvent event = createUserEvent(user, UserEvent.EventType.DELETED);
        kafkaTemplate.executeInTransaction(operations -> {
            sendMessage(userDeletedTopic, String.valueOf(user.getUserId()), event);
            return null;
        });
    }

    private UserEvent createUserEvent(Users user, UserEvent.EventType eventType) {
        return UserEvent.builder()
            .transactionId(UUID.randomUUID().toString())
            .userId(user.getUserId())
            .email(user.getEmail())
            .nickname(user.getNickname())
            .profileImageUrl(user.getProfileImageUrl())
            .profileImageUrlSmall(user.getProfileImageUrlSmall())
            .role(user.getRole())
            .content(user.getContent())
            .eventType(eventType)
            .status(EventStatus.STARTED)
            .timestamp(LocalDateTime.now())
            .build();
    }

    private void sendMessage(String topic, String key, UserEvent message) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("토픽 {}: {}으로 메시지 전송 성공",
                        topic, message);
                } else {
                    log.error("토픽 {}: {}으로 메시지 전송 실패",
                        topic, ex.getMessage());
                    throw new RuntimeException("메시지 전송 실패", ex);
                }
            });
        } catch (TimeoutException e) {
            log.error("메시지 전송 타임아웃: {}", e.getMessage());
            throw new RuntimeException("메시지 전송 타임아웃", e);
        } catch (Exception e) {
            log.error("메시지 전송 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("메시지 전송 중 오류 발생", e);
        }
    }
}