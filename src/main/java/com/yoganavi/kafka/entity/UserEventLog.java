package com.yoganavi.kafka.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;


// saga 패턴 위한 이벤트 처리 추적용 엔티티
@Entity
@Getter
@Setter
@Table(name = "user_event_logs")
public class UserEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String eventType;  // CREATED, UPDATED, DELETED

    @Column(nullable = false)
    private String status;     // STARTED, COMPLETED, FAILED

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static UserEventLog start(Long userId, String eventType) {
        UserEventLog log = new UserEventLog();
        log.setUserId(userId);
        log.setEventType(eventType);
        log.setStatus("STARTED");
        return log;
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
}