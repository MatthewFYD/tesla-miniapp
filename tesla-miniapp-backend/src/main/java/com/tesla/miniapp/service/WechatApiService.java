package com.tesla.miniapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesla.miniapp.config.WechatProperties;
import com.tesla.miniapp.dto.WechatApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 微信API服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatApiService {
    
    private final WechatProperties wechatProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    /**
     * 通过code获取微信用户信息
     */
    public Mono<WechatApiResponse> code2Session(String code) {
        String url = String.format(
            "%s/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            wechatProperties.getApiBaseUrl(),
            wechatProperties.getAppId(),
            wechatProperties.getAppSecret(),
            code
        );
        
        log.info("Calling WeChat API: {}", url.replaceAll("secret=[^&]+", "secret=***"));
        
        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)  // 先获取为字符串
                .flatMap(responseBody -> {
                    try {
                        log.debug("WeChat API response: {}", responseBody);
                        WechatApiResponse response = objectMapper.readValue(responseBody, WechatApiResponse.class);
                        if (response.hasError()) {
                            log.error("WeChat API error: code={}, message={}", 
                                    response.getErrcode(), response.getErrmsg());
                        }
                        return Mono.just(response);
                    } catch (Exception e) {
                        log.error("Failed to parse WeChat API response: {}", responseBody, e);
                        WechatApiResponse errorResponse = new WechatApiResponse();
                        errorResponse.setErrcode(-1);
                        errorResponse.setErrmsg("Parse error: " + e.getMessage());
                        return Mono.just(errorResponse);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("Failed to call WeChat API", throwable);
                    WechatApiResponse errorResponse = new WechatApiResponse();
                    errorResponse.setErrcode(-1);
                    errorResponse.setErrmsg("Network error: " + throwable.getMessage());
                    return Mono.just(errorResponse);
                });
    }
    
    /**
     * 验证会话密钥是否有效
     */
    public Mono<Boolean> validateSessionKey(String openid, String sessionKey) {
        // 这里可以实现会话密钥验证逻辑
        // 微信暂不提供直接验证接口，可以通过其他方式验证
        return Mono.just(true);
    }
}