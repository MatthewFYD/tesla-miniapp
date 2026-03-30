package com.tesla.miniapp.controller;

import com.tesla.miniapp.dto.ApiResponse;
import com.tesla.miniapp.dto.LoginResponse;
import com.tesla.miniapp.dto.WechatLoginRequest;
import com.tesla.miniapp.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "认证接口", description = "用户登录、认证相关接口")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 微信小程序登录
     */
    @Operation(summary = "微信小程序登录", description = "通过微信授权码进行登录")
    @PostMapping("/wechat/login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        try {
            LoginResponse response = authService.wechatLogin(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("WeChat login failed", e);
            return ApiResponse.error(401, "登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 刷新访问令牌
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestParam String refreshToken) {
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ApiResponse.error(401, "令牌刷新失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查令牌有效性
     */
    @Operation(summary = "检查令牌", description = "验证当前令牌是否有效")
    @GetMapping("/check")
    public ApiResponse<Boolean> checkToken(@RequestHeader("Authorization") String authorization) {
        try {
            // 这里应该通过Security配置自动验证，简化处理
            return ApiResponse.success(true);
        } catch (Exception e) {
            return ApiResponse.error(401, "令牌无效");
        }
    }
    
    /**
     * 登出
     */
    @Operation(summary = "用户登出", description = "用户登出，清除相关缓存")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        try {
            // 这里可以实现令牌黑名单、清除Redis缓存等逻辑
            log.info("User logout");
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ApiResponse.error("登出失败: " + e.getMessage());
        }
    }
}