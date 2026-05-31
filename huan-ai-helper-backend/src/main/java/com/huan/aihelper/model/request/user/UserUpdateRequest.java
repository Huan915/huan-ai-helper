package com.huan.aihelper.model.request.user;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String userName;

    private String avatarUrl;

    private String email;

    private String phone;
}
