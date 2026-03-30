package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 微信API响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WechatApiResponse {
    
    /**
     * 用户唯一标识
     */
    private String openid;
    
    /**
     * 会话密钥
     */
    private String session_key;
    
    /**
     * 用户在开放平台的唯一标识符
     */
    private String unionid;
    
    /**
     * 错误码
     */
    private Integer errcode;
    
    /**
     * 错误信息
     */
    private String errmsg;
    
    /**
     * 检查是否有错误
     */
    public boolean hasError() {
        return errcode != null && errcode != 0;
    }
}