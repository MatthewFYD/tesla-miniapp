package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.WechatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 微信用户数据访问层
 */
@Repository
public interface WechatUserRepository extends JpaRepository<WechatUser, Long> {
    
    /**
     * 根据OpenID查找用户
     */
    Optional<WechatUser> findByOpenid(String openid);
    
    /**
     * 根据UnionID查找用户
     */
    Optional<WechatUser> findByUnionId(String unionId);
    
    /**
     * 检查用户是否存在且活跃
     */
    boolean existsByOpenidAndIsActiveTrue(String openid);
    
    /**
     * 更新用户最后登录时间
     */
    default void updateLastLoginTime(String openid, LocalDateTime loginTime) {
        findByOpenid(openid).ifPresent(user -> {
            user.setLastLoginTime(loginTime);
            save(user);
        });
    }
}