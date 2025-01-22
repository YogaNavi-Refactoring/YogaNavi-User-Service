package com.yoganavi.user.service.delete;

public interface DeleteService {

    void requestDeleteUser(Long userId);

    void processDeletedUsers();
}
