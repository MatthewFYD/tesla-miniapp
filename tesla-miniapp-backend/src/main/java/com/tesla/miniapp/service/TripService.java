package com.tesla.miniapp.service;

import com.tesla.miniapp.dto.TripRecordDto;
import com.tesla.miniapp.dto.TripStatsDto;
import com.tesla.miniapp.dto.TripTraceDto;
import com.tesla.miniapp.entity.Drive;
import com.tesla.miniapp.entity.Position;
import com.tesla.miniapp.repository.DriveRepository;
import com.tesla.miniapp.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 行程服务
 * 优先从 drives 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {
    
    private final DriveRepository driveRepository;
    private final PositionRepository positionRepository;
    private final GrafanaStatsService grafanaStatsService;
    
    // 轨迹点最大数量，超过此数量将进行采样
    private static final int MAX_TRACE_POINTS = 500;
    
    // 时区
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    
    /**
     * 将中国时区时间转换为UTC
     */
    private LocalDateTime convertChinaToUtc(LocalDateTime chinaTime) {
        if (chinaTime == null) return null;
        ZonedDateTime chinaZoned = chinaTime.atZone(CHINA_ZONE);
        return chinaZoned.withZoneSameInstant(UTC_ZONE).toLocalDateTime();
    }
    
    /**
     * 将UTC时间转换为中国时区
     */
    private LocalDateTime convertUtcToChina(LocalDateTime utcTime) {
        if (utcTime == null) return null;
        ZonedDateTime utcZoned = utcTime.atZone(UTC_ZONE);
        return utcZoned.withZoneSameInstant(CHINA_ZONE).toLocalDateTime();
    }
    
    /**
     * 获取行程记录（分页）
     * 优先从 drives 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
     */
    public Page<TripRecordDto> getTripRecords(Long carId, Pageable pageable) {
        Page<Drive> drives = driveRepository.findByCarIdOrderByStartDateDesc(carId, pageable);
        
        // 如果 drives 表为空，使用 Grafana 风格从 positions 计算
        if (drives.isEmpty()) {
            log.info("No drives found for car {}, using GrafanaStatsService to get trip records from positions", carId);
            return grafanaStatsService.getTripRecords(carId, pageable, 30);
        }
        
        return drives.map(this::convertToDto);
    }
    
    /**
     * 获取指定时间范围的行程记录
     */
    public List<TripRecordDto> getTripRecordsByDateRange(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Drive> drives = driveRepository.findByCarIdAndStartDateBetweenOrderByStartDateDesc(carId, startDate, endDate);
        return drives.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    /**
     * 获取最近的行程记录
     * 优先从 drives 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
     */
    public List<TripRecordDto> getRecentTrips(Long carId, int limit) {
        List<Drive> drives = driveRepository.findTop10ByCarIdOrderByStartDateDesc(carId);
        
        // 如果 drives 表为空，使用 Grafana 风格从 positions 计算
        if (drives.isEmpty()) {
            log.info("No drives found for car {}, using GrafanaStatsService to get recent trips from positions", carId);
            Page<TripRecordDto> page = grafanaStatsService.getTripRecords(carId, PageRequest.of(0, limit), 30);
            return page.getContent();
        }
        
        return drives.stream()
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最近一条完成的行程
     * 优先从 drives 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
     */
    public Optional<TripRecordDto> getLastCompletedTrip(Long carId) {
        Optional<TripRecordDto> fromDrives = driveRepository.findFirstByCarIdAndEndDateIsNotNullOrderByEndDateDesc(carId)
                .map(this::convertToDto);
        
        if (fromDrives.isPresent()) {
            return fromDrives;
        }
        
        // Fallback 到 positions
        log.info("No drives found for car {}, using GrafanaStatsService to get last trip from positions", carId);
        Page<TripRecordDto> page = grafanaStatsService.getTripRecords(carId, PageRequest.of(0, 1), 30);
        return page.getContent().isEmpty() ? Optional.empty() : Optional.of(page.getContent().get(0));
    }
    
    /**
     * 获取行程统计
     * 优先从 drives 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
     */
    public TripStatsDto getTripStats(Long carId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        // 先检查 drives 表是否有数据
        long totalCount = driveRepository.countByCarIdAndStartDateAfter(carId, startDate);
        
        // 如果 drives 表为空，使用 Grafana 风格从 positions 计算
        if (totalCount == 0) {
            log.info("No drives found for car {}, using GrafanaStatsService to calculate from positions", carId);
            return grafanaStatsService.getTripStats(carId, days);
        }
        
        // 从 drives 表正常查询
        Double totalDistanceKm = driveRepository.sumDistanceByCarIdAndStartDateAfter(carId, startDate);
        Integer totalDurationMin = driveRepository.sumDurationByCarIdAndStartDateAfter(carId, startDate);
        Integer maxSpeed = driveRepository.findMaxSpeedByCarIdAndStartDateAfter(carId, startDate);
        Double maxDistance = driveRepository.findMaxDistanceByCarIdAndStartDateAfter(carId, startDate);
        Double avgEfficiency = driveRepository.findAvgEfficiencyByCarIdAndStartDateAfter(carId, startDate);
        
        double totalDistance = totalDistanceKm != null ? totalDistanceKm : 0.0;
        long totalDuration = totalDurationMin != null ? totalDurationMin * 60L : 0L; // 转换为秒
        
        // 估算能耗: 能耗(Wh/km) * 距离(km) / 1000 = kWh
        double totalEnergyUsed = avgEfficiency != null ? avgEfficiency * totalDistance / 1000.0 : 0.0;
        
        return TripStatsDto.builder()
                .totalCount(totalCount)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .totalEnergyUsed(totalEnergyUsed)
                .avgDistance(totalCount > 0 ? totalDistance / totalCount : 0)
                .avgDuration(totalCount > 0 ? totalDuration / totalCount : 0)
                .avgConsumption(avgEfficiency != null ? avgEfficiency : 0)
                .maxDistance(maxDistance != null ? maxDistance : 0)
                .maxSpeed(maxSpeed != null ? maxSpeed : 0)
                .build();
    }
    
    /**
     * 获取行程轨迹
     * tripId 格式可以是:
     * 1. 标准的 drives 表 ID (较小的数字)
     * 2. 起始时间戳毫秒数 (用于从 positions 表计算的行程，较大的数字如 1774688792549)
     */
    public TripTraceDto getTripTrace(Long tripId) {
        // 如果是时间戳格式（大于2020年1月1日的毫秒数：1577836800000）
        if (tripId > 1577836800000L) {
            return getTripTraceByTimestamp(tripId);
        }
        
        // 否则尝试从 drives 表查询
        Optional<Drive> driveOpt = driveRepository.findById(tripId);
        if (driveOpt.isEmpty()) {
            throw new RuntimeException("Trip not found: " + tripId);
        }
        
        Drive drive = driveOpt.get();
        if (drive.getStartDate() == null || drive.getEndDate() == null) {
            throw new RuntimeException("Trip is not completed or missing date information");
        }
        
        // 获取轨迹点
        List<Position> positions = positionRepository.findTracePoints(
                drive.getCarId(), drive.getStartDate(), drive.getEndDate());
        
        // 如果轨迹点过多，进行采样
        List<Position> sampledPositions;
        if (positions.size() > MAX_TRACE_POINTS) {
            int sampleRate = (int) Math.ceil((double) positions.size() / MAX_TRACE_POINTS);
            sampledPositions = positionRepository.findSampledTracePoints(
                    drive.getCarId(), drive.getStartDate(), drive.getEndDate(), sampleRate);
        } else {
            sampledPositions = positions;
        }
        
        // 转换为轨迹点DTO
        List<TripTraceDto.TracePoint> tracePoints = sampledPositions.stream()
                .map(this::convertToTracePoint)
                .collect(Collectors.toList());
        
        return TripTraceDto.builder()
                .tripId(tripId)
                .carId(drive.getCarId())
                .startTime(drive.getStartDate())
                .endTime(drive.getEndDate())
                .distance(drive.getDistance())
                .pointCount(tracePoints.size())
                .points(tracePoints)
                .build();
    }
    
    /**
     * 根据起始时间戳获取行程轨迹 (从 positions 表计算)
     * @param timestampMs 行程起始时间的UTC毫秒时间戳
     */
    private TripTraceDto getTripTraceByTimestamp(Long timestampMs) {
        // 将毫秒时间戳转换为 LocalDateTime (UTC)
        LocalDateTime startTimeUtc = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMs), UTC_ZONE);
        
        log.info("Getting trace for trip starting at UTC: {}", startTimeUtc);
        
        // 查找从这个时间点开始的行程（允许1秒误差）
        // 首先找到这次行程的结束时间（下一个停车超过10分钟的点）
        LocalDateTime searchStart = startTimeUtc.minusSeconds(1);
        LocalDateTime searchEnd = startTimeUtc.plusHours(12); // 最多搜索12小时
        
        List<Position> positions = positionRepository.findByCarIdAndDateBetweenAndSpeedGreaterThan(
                1L, searchStart, searchEnd, 0);
        
        if (positions.isEmpty()) {
            log.warn("No trace points found for trip starting at {}", startTimeUtc);
            return TripTraceDto.builder()
                    .tripId(timestampMs)
                    .carId(1L)
                    .startTime(convertUtcToChina(startTimeUtc))
                    .endTime(convertUtcToChina(startTimeUtc))
                    .distance(BigDecimal.ZERO)
                    .pointCount(0)
                    .points(List.of())
                    .build();
        }
        
        // 找到行程的实际边界（停车超过10分钟即为行程结束）
        List<Position> tripPositions = new java.util.ArrayList<>();
        LocalDateTime lastTime = null;
        boolean tripStarted = false;
        
        for (Position pos : positions) {
            if (lastTime != null) {
                long gapMinutes = java.time.Duration.between(lastTime, pos.getDate()).toMinutes();
                if (gapMinutes > 10) {
                    // 停车超过10分钟，检查是否已经开始了这次行程
                    if (tripStarted) {
                        break; // 行程结束
                    }
                }
            }
            
            // 检查是否到达行程起始点
            if (!tripStarted && !pos.getDate().isBefore(startTimeUtc.minusSeconds(5))) {
                tripStarted = true;
            }
            
            if (tripStarted) {
                tripPositions.add(pos);
            }
            
            lastTime = pos.getDate();
        }
        
        if (tripPositions.isEmpty()) {
            log.warn("No positions found in trip boundary for {}", startTimeUtc);
            return TripTraceDto.builder()
                    .tripId(timestampMs)
                    .carId(1L)
                    .startTime(convertUtcToChina(startTimeUtc))
                    .endTime(convertUtcToChina(startTimeUtc))
                    .distance(BigDecimal.ZERO)
                    .pointCount(0)
                    .points(List.of())
                    .build();
        }
        
        // 采样
        List<Position> sampledPositions;
        if (tripPositions.size() > MAX_TRACE_POINTS) {
            int step = tripPositions.size() / MAX_TRACE_POINTS;
            sampledPositions = new java.util.ArrayList<>();
            for (int i = 0; i < tripPositions.size(); i += step) {
                sampledPositions.add(tripPositions.get(i));
            }
            // 确保包含最后一个点
            Position lastPos = tripPositions.get(tripPositions.size() - 1);
            if (!sampledPositions.contains(lastPos)) {
                sampledPositions.add(lastPos);
            }
        } else {
            sampledPositions = tripPositions;
        }
        
        // 计算距离
        double minOdometer = tripPositions.stream()
                .filter(p -> p.getOdometer() != null)
                .mapToDouble(p -> p.getOdometer().doubleValue())
                .min().orElse(0);
        double maxOdometer = tripPositions.stream()
                .filter(p -> p.getOdometer() != null)
                .mapToDouble(p -> p.getOdometer().doubleValue())
                .max().orElse(0);
        BigDecimal distance = BigDecimal.valueOf(maxOdometer - minOdometer);
        
        // 获取时间范围 (转换为中国时区)
        LocalDateTime firstTime = convertUtcToChina(tripPositions.get(0).getDate());
        LocalDateTime endTime = convertUtcToChina(tripPositions.get(tripPositions.size() - 1).getDate());
        
        // 转换为轨迹点DTO
        List<TripTraceDto.TracePoint> tracePoints = sampledPositions.stream()
                .map(this::convertToTracePoint)
                .collect(Collectors.toList());
        
        log.info("Found {} trace points for trip (sampled to {}), distance: {}km", 
                tripPositions.size(), tracePoints.size(), distance);
        
        return TripTraceDto.builder()
                .tripId(timestampMs)
                .carId(tripPositions.get(0).getCarId())
                .startTime(firstTime)
                .endTime(endTime)
                .distance(distance)
                .pointCount(tracePoints.size())
                .points(tracePoints)
                .build();
    }
    
    /**
     * 获取实时位置历史（最近N分钟）
     */
    public List<TripTraceDto.TracePoint> getRecentPositions(Long carId, int minutes) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(minutes);
        LocalDateTime endTime = LocalDateTime.now();
        
        List<Position> positions = positionRepository.findTracePoints(carId, startTime, endTime);
        
        return positions.stream()
                .map(this::convertToTracePoint)
                .collect(Collectors.toList());
    }
    
    /**
     * 转换Position为TracePoint
     */
    private TripTraceDto.TracePoint convertToTracePoint(Position position) {
        return TripTraceDto.TracePoint.builder()
                .latitude(position.getLatitude())
                .longitude(position.getLongitude())
                .time(position.getDate())
                .speed(position.getSpeed())
                .power(position.getPower())
                .batteryLevel(position.getBatteryLevel())
                .elevation(null) // TeslaMate positions表可能没有elevation
                .odometer(position.getOdometer())
                .build();
    }
    
    /**
     * 将Drive实体转换为DTO
     */
    private TripRecordDto convertToDto(Drive drive) {
        TripRecordDto.TripRecordDtoBuilder builder = TripRecordDto.builder()
                .id(drive.getId())
                .carId(drive.getCarId())
                .startDate(drive.getStartDate())
                .endDate(drive.getEndDate())
                .distance(drive.getDistance())
                .durationMin(drive.getDurationMin())
                .speedMax(drive.getSpeedMax())
                .powerMax(drive.getPowerMax())
                .powerMin(drive.getPowerMin())
                .startBatteryLevel(drive.getStartBatteryLevel())
                .endBatteryLevel(drive.getEndBatteryLevel())
                .startIdealRangeKm(drive.getStartIdealRangeKm())
                .endIdealRangeKm(drive.getEndIdealRangeKm())
                .outsideTempAvg(drive.getOutsideTempAvg())
                .efficiency(drive.getEfficiency());
        
        // 获取起点和终点位置信息
        if (drive.getStartPositionId() != null) {
            positionRepository.findById(drive.getStartPositionId()).ifPresent(pos -> {
                builder.startLatitude(pos.getLatitude() != null ? pos.getLatitude().doubleValue() : null);
                builder.startLongitude(pos.getLongitude() != null ? pos.getLongitude().doubleValue() : null);
                // 这里可以通过地理编码获取地址，暂时留空
                builder.startAddress("起点位置");
            });
        }
        
        if (drive.getEndPositionId() != null) {
            positionRepository.findById(drive.getEndPositionId()).ifPresent(pos -> {
                builder.endLatitude(pos.getLatitude() != null ? pos.getLatitude().doubleValue() : null);
                builder.endLongitude(pos.getLongitude() != null ? pos.getLongitude().doubleValue() : null);
                builder.endAddress("终点位置");
            });
        }
        
        // 计算平均速度
        if (drive.getDistance() != null && drive.getDurationMin() != null && drive.getDurationMin() > 0) {
            double avgSpeed = drive.getDistance().doubleValue() / (drive.getDurationMin() / 60.0);
            builder.speedAvg(avgSpeed);
        }
        
        return builder.build();
    }
}
