package com.yoganavi.user.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class FallbackController {

    @GetMapping("/user/fallback/test")
    public String testFallback() {
        log.info("서킷 브레이커 폴백 트리거");
        return "This is a fallback response";
    }
}