package com.tesla.miniapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatProperties {
    
    /**
     * 微信小程序AppID
     */
    private String appId;
    
    /**
     * 微信小程序AppSecret
     */
    private String appSecret;
    
    /**
     * 会话超时时间(毫秒)
     */
    private Long sessionTimeout = 7200000L; // 2小时
    
    /**
     * 微信API基础URL
     */
    private String apiBaseUrl = "https://api.weixin.qq.com";
}