package com.yoganavi.user.user.service.login;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.user.dto.login.LoginRequestDto;
import com.yoganavi.user.user.dto.login.LoginResponseDto;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface LoginService {

    LoginResponseDto login(LoginRequestDto request);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    boolean recoverAccount(Users user);
}
