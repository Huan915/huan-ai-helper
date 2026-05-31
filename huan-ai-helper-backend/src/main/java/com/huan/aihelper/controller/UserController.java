package com.huan.aihelper.controller;

import com.huan.aihelper.common.Result;
import com.huan.aihelper.manager.OssManager;
import com.huan.aihelper.model.request.user.UserChangePasswordRequest;
import com.huan.aihelper.model.request.user.UserLoginRequest;
import com.huan.aihelper.model.request.user.UserRegisterRequest;
import com.huan.aihelper.model.request.user.UserUpdateRequest;
import com.huan.aihelper.model.entity.User;
import com.huan.aihelper.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "用户中心")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private OssManager ossManager;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody UserRegisterRequest request) {
        try {
            User user = userService.register(request);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<User> login(@RequestBody UserLoginRequest request) {
        try {
            User user = userService.login(request);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/{userId}")
    public Result<User> getUserInfo(@PathVariable Long userId) {
        try {
            User user = userService.getUserInfo(userId);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{userId}")
    public Result<User> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        try {
            User user = userService.updateUser(userId, request);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "修改密码")
    @PutMapping("/{userId}/password")
    public Result<Boolean> changePassword(@PathVariable Long userId, @RequestBody UserChangePasswordRequest request) {
        try {
            boolean success = userService.changePassword(userId, request);
            return Result.success(success);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "上传头像")
    @PostMapping("/{userId}/avatar")
    public Result<String> uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = "user-avatar/" + userId + "/" + UUID.randomUUID() + extension;
            ossManager.upload(file, objectName);
            String avatarUrl = ossManager.getPublicUrl(objectName);
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setAvatarUrl(avatarUrl);
            userService.updateUser(userId, updateRequest);
            return Result.success(avatarUrl);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
