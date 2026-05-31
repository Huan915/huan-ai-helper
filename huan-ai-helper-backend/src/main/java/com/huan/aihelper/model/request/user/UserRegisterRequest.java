package com.huan.aihelper.model.request.user;

import lombok.Data;

@Data
public class UserRegisterRequest {

    private String userName;

    private String password;

    private String email;

    private String phone;
}
