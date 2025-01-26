package com.yoganavi.user.user.service.register;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.user.dto.register.RegisterDto;

public interface RegisterService {

    String sendEmailToken(String email);

    boolean validateEmailToken(String email, String s);

    Users registerUser(RegisterDto registerDto);
}
