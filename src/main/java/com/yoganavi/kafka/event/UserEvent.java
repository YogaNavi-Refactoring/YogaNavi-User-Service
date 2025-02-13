package com.yoganavi.kafka.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    private String transactionId;
    private Long userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String profileImageUrlSmall;
    private String role;
    private String content;
    private EventType eventType;
    private Boolean isDeleted;
    private EventStatus status;
    private LocalDateTime timestamp;
    private String errorMessage;


    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }

    public enum EventStatus {
        STARTED,    // 트랜잭션 시작
        PROCESSING, // 처리 중
        COMPLETED,  // 성공적으로 완료
        FAILED,     // 실패
        COMPENSATING, // 보상 트랜잭션 진행 중
        COMPENSATED   // 보상 트랜잭션 완료
    }
}