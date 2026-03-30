package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.UserCarBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户车辆绑定关系数据访问层
 */
@Repository
public interface UserCarBindingRepository extends JpaRepository<UserCarBinding, Long> {
    
    /**
     * 根据用户ID查找所有活跃绑定
     */
    List<UserCarBinding> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * 根据用户ID查找所有绑定的车辆（排序）
     */
    List<UserCarBinding> findByUserIdAndIsActiveTrueOrderByIsPrimaryDescCreatedAtDesc(Long userId);
    
    /**
     * 根据车辆ID查找所有绑定的用户
     */
    List<UserCarBinding> findByCarIdAndIsActiveTrue(Long carId);
    
    /**
     * 根据用户ID和车辆ID查找绑定关系
     */
    Optional<UserCarBinding> findByUserIdAndCarIdAndIsActiveTrue(Long userId, Long carId);
    
    /**
     * 查找用户的主要车辆
     */
    @Query("SELECT ucb FROM UserCarBinding ucb WHERE ucb.userId = :userId AND ucb.isPrimary = true AND ucb.isActive = true")
    Optional<UserCarBinding> findPrimaryCarByUserId(@Param("userId") Long userId);
    
    /**
     * 检查用户是否已绑定某车辆
     */
    boolean existsByUserIdAndCarIdAndIsActiveTrue(Long userId, Long carId);
    
    /**
     * 统计用户绑定的车辆数量
     */
    long countByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * 查找用户的车辆绑定关系，包含车辆详细信息
     */
    @Query("SELECT ucb FROM UserCarBinding ucb " +
           "JOIN FETCH ucb.car " +
           "WHERE ucb.userId = :userId AND ucb.isActive = true " +
           "ORDER BY ucb.isPrimary DESC, ucb.createdAt DESC")
    List<UserCarBinding> findByUserIdWithCarDetails(@Param("userId") Long userId);
}