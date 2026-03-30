package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.ChargingProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

/**
 * 充电记录数据访问层
 */
@Repository
public interface ChargingProcessRepository extends JpaRepository<ChargingProcess, Long> {
    
    /**
     * 根据车辆ID查找充电记录
     */
    List<ChargingProcess> findByCarIdOrderByStartDateDesc(Long carId);
    
    /**
     * 根据车辆ID和时间范围查找充电记录
     */
    List<ChargingProcess> findByCarIdAndStartDateBetweenOrderByStartDateDesc(
            Long carId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 查找最近的充电记录
     */
    @Query("SELECT c FROM ChargingProcess c WHERE c.carId = :carId AND c.endDate IS NOT NULL ORDER BY c.endDate DESC")
    List<ChargingProcess> findRecentCompletedByCarId(@Param("carId") Long carId);
    
    /**
     * 查找正在进行的充电记录
     */
    @Query("SELECT c FROM ChargingProcess c WHERE c.carId = :carId AND c.endDate IS NULL ORDER BY c.startDate DESC")
    List<ChargingProcess> findOngoingByCarId(@Param("carId") Long carId);
    
    /**
     * 统计指定时间范围内的充电总电量
     */
    @Query("SELECT COALESCE(SUM(c.chargeEnergyAdded), 0) FROM ChargingProcess c " +
           "WHERE c.carId = :carId AND c.startDate BETWEEN :startDate AND :endDate")
    BigDecimal sumEnergyAddedByCarIdAndDateRange(@Param("carId") Long carId, 
                                                @Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * 统计指定时间范围内的充电总费用
     */
    @Query("SELECT COALESCE(SUM(c.cost), 0) FROM ChargingProcess c " +
           "WHERE c.carId = :carId AND c.startDate BETWEEN :startDate AND :endDate AND c.cost IS NOT NULL")
    BigDecimal sumCostByCarIdAndDateRange(@Param("carId") Long carId, 
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * 统计指定时间范围内的充电次数
     */
    @Query("SELECT COUNT(c) FROM ChargingProcess c " +
           "WHERE c.carId = :carId AND c.startDate BETWEEN :startDate AND :endDate")
    Long countByCarIdAndDateRange(@Param("carId") Long carId, 
                                 @Param("startDate") LocalDateTime startDate, 
                                 @Param("endDate") LocalDateTime endDate);
}