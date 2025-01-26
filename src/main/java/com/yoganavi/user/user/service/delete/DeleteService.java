package com.yoganavi.user.user.service.delete;

public interface DeleteService {

    void requestDeleteUser(Long userId);

    void processDeletedUsers();
}
