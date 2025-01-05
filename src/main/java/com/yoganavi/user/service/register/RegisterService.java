package com.yoganavi.user.service.register;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.dto.register.RegisterDto;

public interface RegisterService {

    String sendEmailToken(String email);

    boolean validateEmailToken(String email, String s);

    Users registerUser(RegisterDto registerDto);
}
