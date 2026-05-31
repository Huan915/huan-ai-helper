package com.huan.aihelper.model.request.user;

import lombok.Data;

@Data
public class UserChangePasswordRequest {

    private String oldPassword;

    private String newPassword;
}
