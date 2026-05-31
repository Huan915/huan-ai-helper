package com.huan.aihelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huan.aihelper.model.request.user.UserChangePasswordRequest;
import com.huan.aihelper.model.request.user.UserLoginRequest;
import com.huan.aihelper.model.request.user.UserRegisterRequest;
import com.huan.aihelper.model.request.user.UserUpdateRequest;
import com.huan.aihelper.model.entity.User;

public interface UserService extends IService<User> {

    User register(UserRegisterRequest request);

    User login(UserLoginRequest request);

    User getUserInfo(Long userId);

    User updateUser(Long userId, UserUpdateRequest request);

    boolean changePassword(Long userId, UserChangePasswordRequest request);
}
