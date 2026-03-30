package com.tesla.miniapp.service;

import com.tesla.miniapp.dto.LoginResponse;
import com.tesla.miniapp.dto.WechatApiResponse;
import com.tesla.miniapp.dto.WechatLoginRequest;
import com.tesla.miniapp.entity.UserCarBinding;
import com.tesla.miniapp.entity.WechatUser;
import com.tesla.miniapp.repository.UserCarBindingRepository;
import com.tesla.miniapp.repository.WechatUserRepository;
import com.tesla.miniapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final WechatApiService wechatApiService;
    private final WechatUserRepository wechatUserRepository;
    private final UserCarBindingRepository userCarBindingRepository;
    private final JwtUtil jwtUtil;
    
    /**
     * 微信登录
     */
    @Transactional
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        log.info("WeChat login request received, code: {}", request.getCode() != null ? "***" : "null");
        
        // 1. 通过code获取微信用户信息
        WechatApiResponse wechatResponse = wechatApiService.code2Session(request.getCode()).block();
        
        if (wechatResponse == null || wechatResponse.hasError()) {
            log.error("WeChat auth failed: {}", wechatResponse != null ? wechatResponse.getErrmsg() : "null response");
            throw new RuntimeException("微信授权失败: " + 
                    (wechatResponse != null ? wechatResponse.getErrmsg() : "未知错误"));
        }
        
        log.info("WeChat API response success, openid: {}", wechatResponse.getOpenid());
        
        // 2. 查找或创建用户
        WechatUser user = wechatUserRepository.findByOpenid(wechatResponse.getOpenid())
                .orElse(null);
        
        boolean isNewUser = false;
        if (user == null) {
            user = createNewUser(wechatResponse, request);
            isNewUser = true;
            log.info("Creating new user for openid: {}", wechatResponse.getOpenid());
        } else {
            user = updateUserInfo(user, wechatResponse, request);
            log.info("Updating existing user for openid: {}", wechatResponse.getOpenid());
        }
        
        user = wechatUserRepository.save(user);
        log.info("User saved successfully, id: {}, openid: {}", user.getId(), user.getOpenid());
        
        // 3. 检查用户是否绑定了车辆
        List<UserCarBinding> bindings = userCarBindingRepository.findByUserIdAndIsActiveTrue(user.getId());
        boolean hasBoundCar = !bindings.isEmpty();
        Long carId = bindings.stream()
                .filter(UserCarBinding::getIsPrimary)
                .findFirst()
                .or(() -> bindings.stream().findFirst())
                .map(UserCarBinding::getCarId)
                .orElse(null);
        
        log.info("User bindings check: hasBoundCar={}, carId={}", hasBoundCar, carId);
        
        // 4. 生成JWT令牌
        String accessToken = jwtUtil.generateAccessToken(user.getOpenid(), user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getOpenid(), user.getId());
        
        // 5. 构建响应
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24小时
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(isNewUser)
                .hasBoundCar(hasBoundCar)
                .carId(carId)
                .build();
    }
    
    /**
     * 创建新用户
     */
    private WechatUser createNewUser(WechatApiResponse wechatResponse, WechatLoginRequest request) {
        WechatUser user = new WechatUser();
        user.setOpenid(wechatResponse.getOpenid());
        user.setUnionId(wechatResponse.getUnionid());
        user.setSessionKey(wechatResponse.getSession_key());
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setCountry(request.getCountry());
        user.setProvince(request.getProvince());
        user.setCity(request.getCity());
        user.setLanguage(request.getLanguage());
        user.setIsActive(true);
        user.setLastLoginTime(LocalDateTime.now());
        
        log.info("Creating new WeChat user: {}", wechatResponse.getOpenid());
        return user;
    }
    
    /**
     * 更新用户信息
     */
    private WechatUser updateUserInfo(WechatUser user, WechatApiResponse wechatResponse, 
                                    WechatLoginRequest request) {
        user.setSessionKey(wechatResponse.getSession_key());
        user.setLastLoginTime(LocalDateTime.now());
        
        // 更新用户资料（如果提供）
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getProvince() != null) {
            user.setProvince(request.getProvince());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getLanguage() != null) {
            user.setLanguage(request.getLanguage());
        }
        
        log.info("Updating WeChat user: {}", user.getOpenid());
        return user;
    }
    
    /**
     * 刷新访问令牌
     */
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新令牌
        String openid = jwtUtil.getUsernameFromToken(refreshToken);
        String tokenType = jwtUtil.getTokenType(refreshToken);
        
        if (!"refresh".equals(tokenType) || !jwtUtil.validateToken(refreshToken, openid)) {
            throw new RuntimeException("Invalid refresh token");
        }
        
        // 查找用户
        WechatUser user = wechatUserRepository.findByOpenid(openid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateAccessToken(user.getOpenid(), user.getId());
        
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // 刷新令牌保持不变
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .isNewUser(false)
                .build();
    }
    
    /**
     * 根据令牌获取用户信息
     */
    public WechatUser getUserByToken(String token) {
        String openid = jwtUtil.getUsernameFromToken(token);
        return wechatUserRepository.findByOpenid(openid)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    /**
     * 根据 openid 获取用户信息
     */
    public WechatUser getUserByOpenid(String openid) {
        return wechatUserRepository.findByOpenid(openid)
                .orElseThrow(() -> new RuntimeException("User not found: " + openid));
    }
}