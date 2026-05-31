package com.huan.aihelper.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huan.aihelper.mapper.UserMapper;
import com.huan.aihelper.model.request.user.UserChangePasswordRequest;
import com.huan.aihelper.model.request.user.UserLoginRequest;
import com.huan.aihelper.model.request.user.UserRegisterRequest;
import com.huan.aihelper.model.request.user.UserUpdateRequest;
import com.huan.aihelper.model.entity.User;
import com.huan.aihelper.service.UserService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User register(UserRegisterRequest request) {
        Long count = lambdaQuery()
                .eq(User::getUserName, request.getUserName())
                .count();
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUserName(request.getUserName());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(0);
        save(user);
        user.setPassword(null);

        return user;
    }

    @Override
    public User login(UserLoginRequest request) {
        User user = lambdaQuery()
                .eq(User::getUserName, request.getUserName())
                .one();
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getStatus() != 0) {
            throw new RuntimeException("用户已被禁用");
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        user.setLastLoginTime(java.time.LocalDateTime.now());
        updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public User updateUser(Long userId, UserUpdateRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (request.getUserName() != null && !request.getUserName().isEmpty()) {
            Long count = lambdaQuery()
                    .eq(User::getUserName, request.getUserName())
                    .ne(User::getId, userId)
                    .count();
            if (count > 0) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUserName(request.getUserName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public boolean changePassword(Long userId, UserChangePasswordRequest request) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!BCrypt.checkpw(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(BCrypt.hashpw(request.getNewPassword()));
        return updateById(user);
    }
}
