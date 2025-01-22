package com.yoganavi.user.service.info;

import com.yoganavi.user.dto.edit.UpdateDto;
import java.util.Set;

public interface InfoService {

    Set<String> getUserHashtags(Long userId);

    UpdateDto getUserInfo(Long userId, String role);
}
