package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 车辆状态数据访问层
 */
@Repository
public interface StateRepository extends JpaRepository<State, Long> {
    
    /**
     * 获取车辆当前状态（endDate为null的记录）
     */
    @Query("SELECT s FROM State s WHERE s.carId = :carId AND s.endDate IS NULL ORDER BY s.startDate DESC")
    Optional<State> findCurrentState(@Param("carId") Long carId);
    
    /**
     * 获取车辆最近的状态记录
     */
    @Query("SELECT s FROM State s WHERE s.carId = :carId ORDER BY s.startDate DESC")
    List<State> findRecentStates(@Param("carId") Long carId);
    
    /**
     * 获取指定时间范围内的状态记录
     */
    @Query("SELECT s FROM State s WHERE s.carId = :carId AND s.startDate >= :startDate ORDER BY s.startDate DESC")
    List<State> findStatesSince(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 统计指定时间范围内各状态的总时长（秒）
     */
    @Query(value = "SELECT s.state, " +
           "SUM(EXTRACT(EPOCH FROM (COALESCE(s.end_date, NOW()) - s.start_date))) as total_seconds " +
           "FROM states s " +
           "WHERE s.car_id = :carId AND s.start_date >= :startDate " +
           "GROUP BY s.state", nativeQuery = true)
    List<Object[]> getStateDurationSummary(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 获取在线时间占比（最近N天）
     */
    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN s.state = 'online' THEN EXTRACT(EPOCH FROM (COALESCE(s.end_date, NOW()) - s.start_date)) ELSE 0 END), 0) as online_seconds, " +
           "COALESCE(SUM(EXTRACT(EPOCH FROM (COALESCE(s.end_date, NOW()) - s.start_date))), 1) as total_seconds " +
           "FROM states s " +
           "WHERE s.car_id = :carId AND s.start_date >= :startDate", nativeQuery = true)
    Object[] getOnlineRatio(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 获取最近一次状态变更时间
     */
    @Query("SELECT s.startDate FROM State s WHERE s.carId = :carId ORDER BY s.startDate DESC")
    Optional<LocalDateTime> findLastStateChangeTime(@Param("carId") Long carId);
}
