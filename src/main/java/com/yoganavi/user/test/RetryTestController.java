package com.yoganavi.user.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/user/test/retry")
@Slf4j
public class RetryTestController {

    private final AtomicInteger temporaryFailCounter = new AtomicInteger(0);
    private final AtomicInteger permanentFailCounter = new AtomicInteger(0);

    @GetMapping("/temporary-fail")
    public String testTemporaryFail() {
        int count = temporaryFailCounter.incrementAndGet();
        log.info("리트라이 테스트 - 일시적 실패. 시도 횟수: {}", count);

        // 처음 2번은 실패, 3번째 시도에서 성공
        if (count < 3) {
            throw new RuntimeException("일시적 오류 발생 - 재시도 필요");
        }

        temporaryFailCounter.set(0); // 카운터 리셋
        return "리트라이 테스트 - 일시적 실패 후 성공";
    }

    @GetMapping("/permanent-fail")
    public String testPermanentFail() {
        int count = permanentFailCounter.incrementAndGet();
        log.info("리트라이 테스트 - 영구적 실패. 시도 횟수: {}", count);

        // 항상 실패
        throw new RuntimeException("영구적 오류 발생 - 재시도해도 실패");
    }

    @GetMapping("/bad-gateway")
    public String testBadGateway() {
        throw new RuntimeException("Bad Gateway");
    }
}
