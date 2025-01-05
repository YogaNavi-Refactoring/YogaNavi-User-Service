package com.yoganavi.user.controller;

import com.yoganavi.user.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/edit")
public class EditController {

    private final EmailService emailService;

//    // 인증번호 전송
//    @PostMapping("/credential/email")
//
//    // 토큰 확인
//    @PostMapping("/credential/token")
//
//    //비밀번호 재설정
//    @PostMapping("/credential")
//
//    //비밀번호 확인
//    @PostMapping("/check")
//
//    // 회원 정보 변경
//    @PostMapping
}
