package com.yoganavi.user.user.service.delete;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DeleteScheduler {

    private final DeleteService deleteService;

    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시에 실행
    public void processDeletedUsers() {
        deleteService.processDeletedUsers();
    }

}
