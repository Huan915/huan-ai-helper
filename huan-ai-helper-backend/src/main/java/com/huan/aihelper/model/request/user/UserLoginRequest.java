package com.huan.aihelper.model.request.user;

import lombok.Data;

@Data
public class UserLoginRequest {

    private String userName;

    private String password;
}
