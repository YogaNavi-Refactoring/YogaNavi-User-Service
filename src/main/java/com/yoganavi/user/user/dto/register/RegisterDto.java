package com.yoganavi.user.user.dto.register;

import lombok.Data;

@Data
public class RegisterDto {

    private String email;
    private String password;
    private String nickname;
    private Boolean teacher;
    private Integer authnumber;
    private String imageUrl;

    public boolean isTeacher() {
        return teacher;
    }

    public void setTeacher(boolean teacher) {
        this.teacher = teacher;
    }
}
