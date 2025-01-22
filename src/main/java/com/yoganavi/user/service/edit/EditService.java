package com.yoganavi.user.service.edit;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.dto.edit.UpdateDto;
import java.util.Set;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface EditService {

    String sendCredentialToken(String email);

    boolean validateToken(String email, String s);

    Users setNewCredential(UpdateDto registerDto);

    boolean checkCredential(Long userId, String password);

    Users updateUser(UpdateDto updateDto, Long userId);

    UpdateDto createUpdateResponse(Users updatedUser);

    Set<String> getUserHashtags(Long userId);
}
