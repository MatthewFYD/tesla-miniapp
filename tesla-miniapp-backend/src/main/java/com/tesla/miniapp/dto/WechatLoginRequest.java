package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 微信登录请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatLoginRequest {
    
    /**
     * 微信小程序授权码
     */
    private String code;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 性别 (0-未知, 1-男, 2-女)
     */
    private Integer gender;
    
    /**
     * 国家
     */
    private String country;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 语言
     */
    private String language;
}