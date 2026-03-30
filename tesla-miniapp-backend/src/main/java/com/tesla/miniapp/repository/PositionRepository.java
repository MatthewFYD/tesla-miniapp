package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 位置信息数据访问层
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    /**
     * 获取车辆最新位置信息
     */
    @Query("SELECT p FROM Position p WHERE p.carId = :carId ORDER BY p.date DESC LIMIT 1")
    Optional<Position> findLatestByCarId(@Param("carId") Long carId);
    
    /**
     * 根据车辆ID和时间范围查找位置记录
     */
    List<Position> findByCarIdAndDateBetweenOrderByDateDesc(
            Long carId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 根据车辆ID、时间范围和速度过滤查找位置记录（用于轨迹）
     */
    @Query("SELECT p FROM Position p WHERE p.carId = :carId AND p.date BETWEEN :startDate AND :endDate AND p.speed > :minSpeed ORDER BY p.date ASC")
    List<Position> findByCarIdAndDateBetweenAndSpeedGreaterThan(
            @Param("carId") Long carId, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate,
            @Param("minSpeed") int minSpeed);
    
    /**
     * 获取车辆最近N条位置记录
     */
    @Query("SELECT p FROM Position p WHERE p.carId = :carId ORDER BY p.date DESC LIMIT :limit")
    List<Position> findRecentByCarId(@Param("carId") Long carId, @Param("limit") int limit);
    
    /**
     * 获取车辆当前电量信息
     */
    @Query("SELECT p FROM Position p WHERE p.carId = :carId AND p.batteryLevel IS NOT NULL ORDER BY p.date DESC LIMIT 1")
    Optional<Position> findLatestBatteryInfoByCarId(@Param("carId") Long carId);
    
    /**
     * 获取车辆温度信息
     */
    @Query("SELECT p FROM Position p WHERE p.carId = :carId AND (p.outsideTemp IS NOT NULL OR p.insideTemp IS NOT NULL) ORDER BY p.date DESC LIMIT 1")
    Optional<Position> findLatestTemperatureByCarId(@Param("carId") Long carId);
    
    /**
     * 根据行程时间范围获取轨迹点（用于行程轨迹）
     */
    @Query("SELECT p FROM Position p WHERE p.carId = :carId AND p.date BETWEEN :startDate AND :endDate ORDER BY p.date ASC")
    List<Position> findTracePoints(@Param("carId") Long carId, 
                                   @Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * 获取行程轨迹点（带采样，避免数据量过大）
     * 每隔N条记录取一条，确保轨迹点数量合理
     */
    @Query(value = "SELECT * FROM (SELECT p.*, ROW_NUMBER() OVER (ORDER BY p.date) as rn " +
           "FROM positions p WHERE p.car_id = :carId AND p.date BETWEEN :startDate AND :endDate) sub " +
           "WHERE sub.rn % :sampleRate = 0 OR sub.rn = 1 ORDER BY sub.date", nativeQuery = true)
    List<Position> findSampledTracePoints(@Param("carId") Long carId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("sampleRate") int sampleRate);
    
    // ========== Grafana 风格统计查询 ==========
    
    /**
     * 【Grafana风格】获取行驶统计（从positions表计算）
     * 返回: [总里程变化, 最高速度, 平均速度, 记录天数]
     */
    @Query(value = "SELECT " +
           "COALESCE(MAX(odometer) - MIN(odometer), 0) as total_distance, " +
           "COALESCE(MAX(speed), 0) as max_speed, " +
           "COALESCE(AVG(CASE WHEN speed > 0 THEN speed END), 0) as avg_speed, " +
           "COUNT(DISTINCT DATE(date)) as days " +
           "FROM positions WHERE car_id = :carId AND date >= :startDate", nativeQuery = true)
    Object[] getDrivingStats(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 【Grafana风格】获取行驶次数（按天聚合，至少行驶1公里算一次行程）
     */
    @Query(value = 
           "WITH ordered_pos AS (" +
           "  SELECT date, speed, odometer, latitude, longitude, " +
           "    LAG(date) OVER (ORDER BY date) as prev_date, " +
           "    EXTRACT(EPOCH FROM (date - LAG(date) OVER (ORDER BY date)))/60 as gap_minutes " +
           "  FROM positions " +
           "  WHERE car_id = :carId AND date >= :startDate AND speed > 0" +
           "), " +
           "trip_boundaries AS (" +
           "  SELECT date, odometer, latitude, longitude, gap_minutes, " +
           "    SUM(CASE WHEN gap_minutes > 10 OR gap_minutes IS NULL THEN 1 ELSE 0 END) OVER (ORDER BY date) as trip_id " +
           "  FROM ordered_pos" +
           "), " +
           "trip_stats AS (" +
           "  SELECT trip_id, COUNT(*) as point_count " +
           "  FROM trip_boundaries " +
           "  GROUP BY trip_id " +
           "  HAVING MAX(odometer) - MIN(odometer) > 0.5" +
           ") " +
           "SELECT COUNT(*) FROM trip_stats", nativeQuery = true)
    Long countDrivingSessions(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 【Grafana风格】获取行程记录列表（基于停车间隔>10分钟分割真正的行程）
     * 返回: [start_time, end_time, distance, max_speed, avg_speed, duration_min, 
     *        start_lat, start_lng, end_lat, end_lng, point_count, trip_id]
     */
    @Query(value = 
           "WITH ordered_pos AS (" +
           "  SELECT date, speed, odometer, latitude, longitude, " +
           "    LAG(date) OVER (ORDER BY date) as prev_date, " +
           "    EXTRACT(EPOCH FROM (date - LAG(date) OVER (ORDER BY date)))/60 as gap_minutes " +
           "  FROM positions " +
           "  WHERE car_id = :carId AND date >= :startDate AND speed > 0" +
           "), " +
           "trip_boundaries AS (" +
           "  SELECT date, speed, odometer, latitude, longitude, gap_minutes, " +
           "    SUM(CASE WHEN gap_minutes > 10 OR gap_minutes IS NULL THEN 1 ELSE 0 END) OVER (ORDER BY date) as trip_id " +
           "  FROM ordered_pos" +
           "), " +
           "trip_stats AS (" +
           "  SELECT trip_id, " +
           "    MIN(date) as start_time, MAX(date) as end_time, " +
           "    MAX(odometer) - MIN(odometer) as distance, " +
           "    MAX(speed) as max_speed, " +
           "    AVG(speed) as avg_speed, " +
           "    EXTRACT(EPOCH FROM (MAX(date) - MIN(date)))/60 as duration_min, " +
           "    COUNT(*) as point_count " +
           "  FROM trip_boundaries " +
           "  GROUP BY trip_id " +
           "  HAVING MAX(odometer) - MIN(odometer) > 0.5" +
           "), " +
           "start_points AS (" +
           "  SELECT DISTINCT ON (trip_id) trip_id, latitude as start_lat, longitude as start_lng " +
           "  FROM trip_boundaries ORDER BY trip_id, date ASC" +
           "), " +
           "end_points AS (" +
           "  SELECT DISTINCT ON (trip_id) trip_id, latitude as end_lat, longitude as end_lng " +
           "  FROM trip_boundaries ORDER BY trip_id, date DESC" +
           ") " +
           "SELECT ts.start_time, ts.end_time, ts.distance, ts.max_speed, ts.avg_speed, " +
           "  ts.duration_min, sp.start_lat, sp.start_lng, ep.end_lat, ep.end_lng, " +
           "  ts.point_count, ts.trip_id " +
           "FROM trip_stats ts " +
           "JOIN start_points sp ON ts.trip_id = sp.trip_id " +
           "JOIN end_points ep ON ts.trip_id = ep.trip_id " +
           "ORDER BY ts.start_time DESC", nativeQuery = true)
    List<Object[]> getTripRecords(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 获取指定行程的轨迹点（基于行程起止时间）
     */
    @Query(value = "SELECT date, latitude, longitude, speed, odometer, battery_level, elevation, power " +
           "FROM positions WHERE car_id = :carId AND date BETWEEN :startTime AND :endTime AND speed > 0 " +
           "ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getTripTracePointsByTimeRange(@Param("carId") Long carId, 
           @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取指定日期的行程轨迹点
     */
    @Query(value = "SELECT date, latitude, longitude, speed, odometer, battery_level, elevation, power " +
           "FROM positions WHERE car_id = :carId AND DATE(date) = :tripDate AND speed > 0 " +
           "ORDER BY date ASC", nativeQuery = true)
    List<Object[]> getTripTracePoints(@Param("carId") Long carId, @Param("tripDate") java.sql.Date tripDate);
    
    /**
     * 【Grafana风格】获取充电统计（从电量变化计算）
     * 统计总充电量百分比变化（电量增加的记录）
     * 返回: [充电事件数, 总充电量变化百分比]
     */
    @Query(value = "WITH battery_changes AS (" +
           "  SELECT date, battery_level, " +
           "    LAG(battery_level) OVER (ORDER BY date) as prev_level " +
           "  FROM positions WHERE car_id = :carId AND date >= :startDate AND battery_level IS NOT NULL" +
           ") SELECT " +
           "  COUNT(*) FILTER (WHERE battery_level > prev_level) as charge_events, " +
           "  COALESCE(SUM(CASE WHEN battery_level > prev_level THEN battery_level - prev_level ELSE 0 END), 0) as total_charge_pct " +
           "FROM battery_changes", nativeQuery = true)
    Object[] getChargingStats(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 【Grafana风格】获取充电会话统计
     * 返回有效的充电会话数量（电量从低充到高，且间隔超过30分钟算新会话）
     */
    @Query(value = "WITH min_max_battery AS (" +
           "  SELECT MIN(battery_level) as min_level, MAX(battery_level) as max_level " +
           "  FROM positions WHERE car_id = :carId AND date >= :startDate AND battery_level IS NOT NULL" +
           ") SELECT max_level - min_level, 1 as session_count FROM min_max_battery " +
           "WHERE max_level > min_level", nativeQuery = true)
    Object[] getChargingSessionSummary(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 【Grafana风格】获取最近充电会话详情
     * 检测电量持续增长的时间段，按充电间隔（>60分钟）分割为独立会话
     */
    @Query(value = 
           "WITH battery_changes AS (" +
           "  SELECT date, battery_level, " +
           "    LAG(battery_level) OVER (ORDER BY date) as prev_level, " +
           "    LAG(date) OVER (ORDER BY date) as prev_date, " +
           "    EXTRACT(EPOCH FROM (date - LAG(date) OVER (ORDER BY date)))/60 as gap_minutes " +
           "  FROM positions WHERE car_id = :carId AND date >= :startDate AND battery_level IS NOT NULL" +
           "), charging_flags AS (" +
           "  SELECT date, battery_level, prev_level, prev_date, gap_minutes, " +
           "    CASE WHEN battery_level > prev_level THEN 1 ELSE 0 END as is_charging " +
           "  FROM battery_changes " +
           "), session_boundaries AS (" +
           "  SELECT *, " +
           "    SUM(CASE WHEN is_charging = 0 OR gap_minutes > 60 OR gap_minutes IS NULL THEN 1 ELSE 0 END) " +
           "    OVER (ORDER BY date) as session_id " +
           "  FROM charging_flags " +
           "  WHERE is_charging = 1" +
           ") SELECT " +
           "  MIN(prev_date) as start_date, " +
           "  MAX(date) as end_date, " +
           "  MIN(prev_level) as start_level, " +
           "  MAX(battery_level) as end_level, " +
           "  MAX(battery_level) - MIN(prev_level) as energy_added_pct " +
           "FROM session_boundaries " +
           "GROUP BY session_id " +
           "HAVING MAX(battery_level) - MIN(prev_level) >= 5 " +  // 至少充5%电量算一次
           "ORDER BY MIN(prev_date) DESC", nativeQuery = true)
    List<Object[]> getRecentChargingSessions(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * 【Grafana风格】获取能耗统计
     */
    @Query(value = "SELECT " +
           "  COALESCE(AVG(CASE WHEN speed > 0 THEN ABS(power) END), 0) as avg_power_consumption, " +
           "  COALESCE(MAX(ABS(power)) FILTER (WHERE power < 0), 0) as max_regen_power, " +
           "  COALESCE(MAX(power) FILTER (WHERE power > 0), 0) as max_charge_power " +
           "FROM positions WHERE car_id = :carId AND date >= :startDate", nativeQuery = true)
    Object[] getEnergyStats(@Param("carId") Long carId, @Param("startDate") LocalDateTime startDate);
}