package com.yoganavi.user.test;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/test")
@Slf4j
public class CircuitBreakerTestController {

    @GetMapping("/delay")
    public String testDelay() throws InterruptedException {
        log.info("서킷 브레이커 테스트 - 지연 응답");
        Thread.sleep(6000);
        return "서킷 브레이커 테스트 - 지연 응답";
    }

    @GetMapping("/error")
    public String testError() {
        log.info("서킷 브레이커 테스트 - 에러");
        throw new RuntimeException("서킷 브레이커 테스트 - 에러");
    }

    @GetMapping("/normal")
    public String testNormal() {
        log.info("서킷 브레이커 테스트 - 일반 응답");
        return "서킷 브레이커 테스트 - 일반 응답";
    }
}