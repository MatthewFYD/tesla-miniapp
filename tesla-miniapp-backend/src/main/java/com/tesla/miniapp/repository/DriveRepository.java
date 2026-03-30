package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.Drive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 行程数据仓库接口
 */
@Repository
public interface DriveRepository extends JpaRepository<Drive, Long> {
    
    /**
     * 根据车辆ID分页查询行程记录
     */
    Page<Drive> findByCarIdOrderByStartDateDesc(Long carId, Pageable pageable);
    
    /**
     * 根据车辆ID和时间范围查询行程记录
     */
    List<Drive> findByCarIdAndStartDateBetweenOrderByStartDateDesc(
            Long carId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 获取最近的行程记录
     */
    List<Drive> findTop10ByCarIdOrderByStartDateDesc(Long carId);
    
    /**
     * 获取最近一条完成的行程
     */
    Optional<Drive> findFirstByCarIdAndEndDateIsNotNullOrderByEndDateDesc(Long carId);
    
    /**
     * 统计行程次数
     */
    long countByCarIdAndStartDateAfter(Long carId, LocalDateTime startDate);
    
    /**
     * 计算指定时间后的总里程
     */
    @Query("SELECT COALESCE(SUM(d.distance), 0) FROM Drive d WHERE d.carId = :carId AND d.startDate > :startDate")
    Double sumDistanceByCarIdAndStartDateAfter(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 计算指定时间后的总时长（分钟）
     */
    @Query("SELECT COALESCE(SUM(d.durationMin), 0) FROM Drive d WHERE d.carId = :carId AND d.startDate > :startDate")
    Integer sumDurationByCarIdAndStartDateAfter(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 获取最大速度
     */
    @Query("SELECT COALESCE(MAX(d.speedMax), 0) FROM Drive d WHERE d.carId = :carId AND d.startDate > :startDate")
    Integer findMaxSpeedByCarIdAndStartDateAfter(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 获取最长单次行程距离
     */
    @Query("SELECT COALESCE(MAX(d.distance), 0) FROM Drive d WHERE d.carId = :carId AND d.startDate > :startDate")
    Double findMaxDistanceByCarIdAndStartDateAfter(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 计算平均能耗
     */
    @Query("SELECT COALESCE(AVG(d.efficiency), 0) FROM Drive d WHERE d.carId = :carId AND d.startDate > :startDate AND d.efficiency IS NOT NULL")
    Double findAvgEfficiencyByCarIdAndStartDateAfter(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
}
