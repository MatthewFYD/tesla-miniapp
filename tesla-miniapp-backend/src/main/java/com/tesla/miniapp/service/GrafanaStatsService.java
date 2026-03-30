package com.tesla.miniapp.service;

import com.tesla.miniapp.dto.ChargingRecordDto;
import com.tesla.miniapp.dto.ChargingStatsDto;
import com.tesla.miniapp.dto.TripRecordDto;
import com.tesla.miniapp.dto.TripStatsDto;
import com.tesla.miniapp.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grafana 风格统计服务
 * 直接从 positions 表计算行程和充电统计数据
 * 当 TeslaMate 的 drives/charging_processes 表为空时使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrafanaStatsService {
    
    private final PositionRepository positionRepository;
    
    // 时区：上海/北京时间
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    
    // 燃油价格常量（用于节省计算）
    private static final BigDecimal FUEL_PRICE_PER_LITER = new BigDecimal("7.5");
    private static final BigDecimal FUEL_CONSUMPTION_PER_100KM = new BigDecimal("8.5");
    private static final BigDecimal CO2_REDUCTION_PER_KWH = new BigDecimal("0.7");
    
    /**
     * 将UTC时间转换为中国时区
     */
    private LocalDateTime convertUtcToChina(LocalDateTime utcTime) {
        if (utcTime == null) return null;
        ZonedDateTime utcZoned = utcTime.atZone(UTC_ZONE);
        return utcZoned.withZoneSameInstant(CHINA_ZONE).toLocalDateTime();
    }
    
    /**
     * 【Grafana风格】获取行程统计（从positions表计算）
     */
    @Transactional(readOnly = true)
    public TripStatsDto getTripStats(Long carId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        
        try {
            // 获取行驶统计
            Object[] drivingStats = positionRepository.getDrivingStats(carId, startDate);
            Long drivingSessionCount = positionRepository.countDrivingSessions(carId, startDate);
            
            // 处理JPA可能返回嵌套数组的情况
            Object[] stats = unwrapArray(drivingStats);
            
            // 安全地访问数组元素
            double totalDistance = getDouble(stats, 0);
            int maxSpeed = getInt(stats, 1);
            double avgSpeed = getDouble(stats, 2);
            int recordDays = getInt(stats, 3);
            
            long tripCount = drivingSessionCount != null ? drivingSessionCount : 0;
            
            // 估算行驶时间（假设平均速度计算）
            double totalDurationHours = avgSpeed > 0 ? totalDistance / avgSpeed : 0;
            long totalDurationSeconds = (long) (totalDurationHours * 3600);
            
            // 估算能耗（Tesla平均约 150 Wh/km）
            double totalEnergyUsed = totalDistance * 0.15; // kWh
            
            log.info("GrafanaStats trip for car {}: distance={}km, maxSpeed={}km/h, trips={}", 
                    carId, totalDistance, maxSpeed, tripCount);
            
            return TripStatsDto.builder()
                    .totalCount(tripCount)
                    .totalDistance(totalDistance)
                    .totalDuration(totalDurationSeconds)
                    .totalEnergyUsed(totalEnergyUsed)
                    .avgDistance(tripCount > 0 ? totalDistance / tripCount : 0)
                    .avgDuration(tripCount > 0 ? totalDurationSeconds / tripCount : 0)
                    .avgConsumption(totalDistance > 0 ? totalEnergyUsed * 1000 / totalDistance : 150) // Wh/km
                    .maxDistance(totalDistance) // 使用总里程作为最大单次行程估计
                    .maxSpeed(maxSpeed)
                    .build();
        } catch (Exception e) {
            log.error("Failed to get trip stats from positions for car: {}", carId, e);
            return TripStatsDto.builder()
                    .totalCount(0)
                    .totalDistance(0)
                    .totalDuration(0)
                    .totalEnergyUsed(0)
                    .avgDistance(0)
                    .avgDuration(0)
                    .avgConsumption(0)
                    .maxDistance(0)
                    .maxSpeed(0)
                    .build();
        }
    }
    
    /**
     * 【Grafana风格】获取行程记录列表（从positions表推断，基于停车间隔>10分钟分割）
     */
    @Transactional(readOnly = true)
    public Page<TripRecordDto> getTripRecords(Long carId, Pageable pageable, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        
        try {
            List<Object[]> trips = positionRepository.getTripRecords(carId, startDate);
            log.info("GrafanaStats: getTripRecords returned {} raw records for car {}", 
                    trips != null ? trips.size() : 0, carId);
            
            List<TripRecordDto> records = new ArrayList<>();
            for (Object[] trip : trips) {
                if (trip == null) {
                    log.warn("GrafanaStats: null trip record");
                    continue;
                }
                
                // SQL返回12列: start_time, end_time, distance, max_speed, avg_speed, duration_min,
                //              start_lat, start_lng, end_lat, end_lng, point_count, trip_id
                LocalDateTime startDateTimeUtc = trip[0] != null ? 
                        ((java.sql.Timestamp) trip[0]).toLocalDateTime() : null;
                LocalDateTime endDateTimeUtc = trip[1] != null ? 
                        ((java.sql.Timestamp) trip[1]).toLocalDateTime() : null;
                
                // 转换为中国时区
                LocalDateTime startDateTime = convertUtcToChina(startDateTimeUtc);
                LocalDateTime endDateTime = convertUtcToChina(endDateTimeUtc);
                
                Double distance = trip[2] != null ? ((Number) trip[2]).doubleValue() : 0;
                Integer maxSpeed = trip[3] != null ? ((Number) trip[3]).intValue() : 0;
                Double avgSpeed = trip[4] != null ? ((Number) trip[4]).doubleValue() : 0;
                Integer durationMin = trip[5] != null ? ((Number) trip[5]).intValue() : 0;
                Double startLat = trip[6] != null ? ((Number) trip[6]).doubleValue() : null;
                Double startLng = trip[7] != null ? ((Number) trip[7]).doubleValue() : null;
                Double endLat = trip[8] != null ? ((Number) trip[8]).doubleValue() : null;
                Double endLng = trip[9] != null ? ((Number) trip[9]).doubleValue() : null;
                // trip_id 是行程序号
                Long tripId = trip[11] != null ? ((Number) trip[11]).longValue() : 0L;
                
                // 生成唯一ID：使用起始时间戳（毫秒级）作为ID，这样可以唯一标识每次行程
                long uniqueId = startDateTimeUtc != null ? 
                        startDateTimeUtc.atZone(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli() : tripId;
                
                // 估算能耗 (150 Wh/km)
                double energyUsed = distance * 0.15;
                
                records.add(TripRecordDto.builder()
                        .id(uniqueId)
                        .carId(carId)
                        .startDate(startDateTime)
                        .endDate(endDateTime)
                        .distance(BigDecimal.valueOf(distance))
                        .durationMin(durationMin)
                        .speedMax(maxSpeed)
                        .speedAvg(avgSpeed)
                        .powerMax(null)
                        .powerMin(null)
                        .startAddress(null)
                        .endAddress(null)
                        .startLatitude(startLat)
                        .startLongitude(startLng)
                        .endLatitude(endLat)
                        .endLongitude(endLng)
                        .startBatteryLevel(null)
                        .endBatteryLevel(null)
                        .efficiency(distance > 0 ? BigDecimal.valueOf(energyUsed * 1000 / distance) : BigDecimal.valueOf(150))
                        .build());
            }
            
            log.info("GrafanaStats found {} trip records for car {}", records.size(), carId);
            
            int total = records.size();
            int start = (int) pageable.getOffset();
            if (start >= total) {
                return new PageImpl<>(Collections.emptyList(), pageable, total);
            }
            int end = Math.min(start + pageable.getPageSize(), total);
            return new PageImpl<>(records.subList(start, end), pageable, total);
        } catch (Exception e) {
            log.error("Failed to get trip records from positions for car: {}", carId, e);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
    
    /**
     * 【Grafana风格】获取充电统计（从positions表计算）
     */
    @Transactional(readOnly = true)
    public ChargingStatsDto getChargingStats(Long carId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        LocalDateTime endDate = LocalDateTime.now();
        return getChargingStatsByDateRange(carId, startDate, endDate);
    }
    
    /**
     * 【Grafana风格】获取指定时间范围的充电统计（从positions表计算）
     */
    @Transactional(readOnly = true)
    public ChargingStatsDto getChargingStatsByDateRange(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            // 获取所有充电会话
            List<Object[]> sessions = positionRepository.getRecentChargingSessions(carId, startDate);
            
            // 过滤在指定时间范围内的会话
            long chargeCount = 0;
            int totalChargePct = 0;
            
            for (Object[] session : sessions) {
                if (session == null || session.length < 5) continue;
                
                // 获取会话开始时间
                LocalDateTime sessionStart = session[0] != null ? 
                        ((java.sql.Timestamp) session[0]).toLocalDateTime() : null;
                
                // 检查是否在指定范围内
                if (sessionStart != null && !sessionStart.isBefore(startDate) && !sessionStart.isAfter(endDate)) {
                    chargeCount++;
                    if (session[4] != null) {
                        totalChargePct += ((Number) session[4]).intValue();
                    }
                }
            }
            
            // 估算充电量（假设100%电量对应60kWh电池）
            BigDecimal batteryCapacityKwh = new BigDecimal("60");
            BigDecimal totalEnergyAdded = batteryCapacityKwh
                    .multiply(BigDecimal.valueOf(totalChargePct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            
            // 估算充电费用（假设每度电0.6元）
            BigDecimal electricityPrice = new BigDecimal("0.6");
            BigDecimal totalCost = totalEnergyAdded.multiply(electricityPrice);
            
            // 计算燃油节省
            BigDecimal estimatedSavings = calculateFuelSavings(totalEnergyAdded);
            BigDecimal carbonReduction = totalEnergyAdded.multiply(CO2_REDUCTION_PER_KWH);
            
            log.info("GrafanaStats for car {}: chargeCount={}, totalChargePct={}%, totalEnergy={}kWh", 
                    carId, chargeCount, totalChargePct, totalEnergyAdded);
            
            return ChargingStatsDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalChargingSessions(chargeCount)
                    .totalEnergyAdded(totalEnergyAdded)
                    .totalCost(totalCost)
                    .avgEnergyPerSession(chargeCount > 0 ? 
                            totalEnergyAdded.divide(BigDecimal.valueOf(chargeCount), 2, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO)
                    .avgCostPerSession(chargeCount > 0 ? 
                            totalCost.divide(BigDecimal.valueOf(chargeCount), 2, RoundingMode.HALF_UP) : 
                            BigDecimal.ZERO)
                    .avgDurationMin(60.0) // 估算平均充电时间
                    .homeChargingSessions(chargeCount) // 假设都是家充
                    .homeEnergyAdded(totalEnergyAdded)
                    .superChargingSessions(0L)
                    .superEnergyAdded(BigDecimal.ZERO)
                    .homeChargingPercentage(BigDecimal.valueOf(100))
                    .superChargingPercentage(BigDecimal.ZERO)
                    .estimatedSavings(estimatedSavings)
                    .carbonReduction(carbonReduction)
                    .build();
        } catch (Exception e) {
            log.error("Failed to get charging stats from positions for car: {}", carId, e);
            return ChargingStatsDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalChargingSessions(0L)
                    .totalEnergyAdded(BigDecimal.ZERO)
                    .totalCost(BigDecimal.ZERO)
                    .avgEnergyPerSession(BigDecimal.ZERO)
                    .avgCostPerSession(BigDecimal.ZERO)
                    .avgDurationMin(0.0)
                    .homeChargingSessions(0L)
                    .homeEnergyAdded(BigDecimal.ZERO)
                    .superChargingSessions(0L)
                    .superEnergyAdded(BigDecimal.ZERO)
                    .homeChargingPercentage(BigDecimal.ZERO)
                    .superChargingPercentage(BigDecimal.ZERO)
                    .estimatedSavings(BigDecimal.ZERO)
                    .carbonReduction(BigDecimal.ZERO)
                    .build();
        }
    }
    
    /**
     * 【Grafana风格】获取最近的充电记录（从positions表推断）
     */
    @Transactional(readOnly = true)
    public Page<ChargingRecordDto> getChargingRecords(Long carId, Pageable pageable, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        
        try {
            List<Object[]> sessions = positionRepository.getRecentChargingSessions(carId, startDate);
            
            List<ChargingRecordDto> records = new ArrayList<>();
            long id = 1;
            for (Object[] session : sessions) {
                if (session == null || session.length < 5) continue;
                
                LocalDateTime sessionStartDateUtc = session[0] != null ? 
                        ((java.sql.Timestamp) session[0]).toLocalDateTime() : null;
                LocalDateTime sessionEndDateUtc = session[1] != null ? 
                        ((java.sql.Timestamp) session[1]).toLocalDateTime() : null;
                
                // 转换为中国时区
                LocalDateTime sessionStartDate = convertUtcToChina(sessionStartDateUtc);
                LocalDateTime sessionEndDate = convertUtcToChina(sessionEndDateUtc);
                
                Integer startLevel = session[2] != null ? ((Number) session[2]).intValue() : null;
                Integer endLevel = session[3] != null ? ((Number) session[3]).intValue() : null;
                Integer energyAddedPct = session[4] != null ? ((Number) session[4]).intValue() : 0;
                
                // 计算充电时长
                Integer durationMin = null;
                if (sessionStartDate != null && sessionEndDate != null) {
                    durationMin = (int) ChronoUnit.MINUTES.between(sessionStartDate, sessionEndDate);
                }
                
                // 估算充电量（60kWh电池）
                BigDecimal chargeEnergyAdded = BigDecimal.valueOf(60.0 * energyAddedPct / 100);
                
                records.add(ChargingRecordDto.builder()
                        .id(id++)
                        .carId(carId)
                        .startDate(sessionStartDate)
                        .endDate(sessionEndDate)
                        .durationMin(durationMin)
                        .chargeEnergyAdded(chargeEnergyAdded)
                        .chargeEnergyUsed(chargeEnergyAdded.multiply(BigDecimal.valueOf(1.1))) // 假设10%损耗
                        .startBatteryLevel(startLevel)
                        .endBatteryLevel(endLevel)
                        .startRatedRangeKm(startLevel != null ? BigDecimal.valueOf(startLevel * 4.2) : null)
                        .endRatedRangeKm(endLevel != null ? BigDecimal.valueOf(endLevel * 4.2) : null)
                        .cost(chargeEnergyAdded.multiply(BigDecimal.valueOf(0.6)))
                        .outsideTempAvg(null)
                        .geofenceId(null)
                        .chargeType("家用充电桩")
                        .status("已完成")
                        .isCompleted(true)
                        .build());
            }
            
            int total = records.size();
            int start = (int) pageable.getOffset();
            if (start >= total) {
                return new PageImpl<>(Collections.emptyList(), pageable, total);
            }
            int end = Math.min(start + pageable.getPageSize(), total);
            return new PageImpl<>(records.subList(start, end), pageable, total);
        } catch (Exception e) {
            log.error("Failed to get charging records from positions for car: {}", carId, e);
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }
    }
    
    /**
     * 计算燃油节省费用
     */
    private BigDecimal calculateFuelSavings(BigDecimal totalEnergyKwh) {
        BigDecimal totalKm = totalEnergyKwh.multiply(BigDecimal.valueOf(3.3));
        BigDecimal fuelNeeded = totalKm.multiply(FUEL_CONSUMPTION_PER_100KM)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return fuelNeeded.multiply(FUEL_PRICE_PER_LITER);
    }
    
    /**
     * 解包可能嵌套的数组（JPA native query 有时返回嵌套数组）
     */
    private Object[] unwrapArray(Object[] arr) {
        if (arr == null || arr.length == 0) {
            return new Object[0];
        }
        // 如果第一个元素本身是数组，说明被嵌套了
        if (arr[0] instanceof Object[]) {
            return (Object[]) arr[0];
        }
        return arr;
    }
    
    /**
     * 安全获取数组中的 double 值
     */
    private double getDouble(Object[] arr, int index) {
        if (arr == null || arr.length <= index || arr[index] == null) {
            return 0;
        }
        Object val = arr[index];
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0;
    }
    
    /**
     * 安全获取数组中的 int 值
     */
    private int getInt(Object[] arr, int index) {
        if (arr == null || arr.length <= index || arr[index] == null) {
            return 0;
        }
        Object val = arr[index];
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }
}
