package com.yoganavi.user.user.service.edit;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.user.dto.edit.UpdateDto;
import java.util.Set;

public interface EditService {

    String sendCredentialToken(String email);

    boolean validateToken(String email, String s);

    Users setNewCredential(UpdateDto registerDto);

    boolean checkCredential(Long userId, String password);

    Users updateUser(UpdateDto updateDto, Long userId);

    UpdateDto createUpdateResponse(Users updatedUser);

    Set<String> getUserHashtags(Long userId);
}
