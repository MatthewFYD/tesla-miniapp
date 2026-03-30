package com.tesla.miniapp.service;

import com.tesla.miniapp.dto.ChargingRecordDto;
import com.tesla.miniapp.dto.ChargingStatsDto;
import com.tesla.miniapp.entity.ChargingProcess;
import com.tesla.miniapp.repository.ChargingProcessRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 充电服务
 * 优先从 charging_processes 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargingService {
    
    private final ChargingProcessRepository chargingProcessRepository;
    private final GrafanaStatsService grafanaStatsService;
    
    // 燃油价格常量（用于节省计算）
    private static final BigDecimal FUEL_PRICE_PER_LITER = new BigDecimal("7.5"); // 汽油价格每升
    private static final BigDecimal FUEL_CONSUMPTION_PER_100KM = new BigDecimal("8.5"); // 每100公里油耗
    private static final BigDecimal CO2_REDUCTION_PER_KWH = new BigDecimal("0.7"); // 每kWh减排CO2
    
    /**
     * 获取车辆充电记录
     * 优先从 charging_processes 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
     */
    @Transactional(readOnly = true)
    public Page<ChargingRecordDto> getChargingRecords(Long carId, Pageable pageable) {
        List<ChargingProcess> processes = chargingProcessRepository.findByCarIdOrderByStartDateDesc(carId);
        
        // 如果 charging_processes 表为空，使用 Grafana 风格从 positions 计算
        if (processes.isEmpty()) {
            log.info("No charging processes found for car {}, using GrafanaStatsService to calculate from positions", carId);
            return grafanaStatsService.getChargingRecords(carId, pageable, 30);
        }
        
        List<ChargingRecordDto> dtos = processes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        int total = dtos.size();
        int start = (int) pageable.getOffset();
        
        // 边界检查：如果起始位置超出列表大小，返回空页
        if (start >= total) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }
        
        int end = Math.min((start + pageable.getPageSize()), total);
        List<ChargingRecordDto> pageContent = dtos.subList(start, end);
        return new PageImpl<>(pageContent, pageable, total);
    }
    
    /**
     * 获取指定时间范围的充电记录
     */
    @Transactional(readOnly = true)
    public List<ChargingRecordDto> getChargingRecordsByDateRange(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        List<ChargingProcess> processes = chargingProcessRepository
                .findByCarIdAndStartDateBetweenOrderByStartDateDesc(carId, startDate, endDate);
        
        return processes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取正在进行的充电记录
     */
    @Transactional(readOnly = true)
    public List<ChargingRecordDto> getOngoingChargingSessions(Long carId) {
        List<ChargingProcess> processes = chargingProcessRepository.findOngoingByCarId(carId);
        
        return processes.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最近的完成充电记录
     */
    @Transactional(readOnly = true)
    public List<ChargingRecordDto> getRecentCompletedChargingSessions(Long carId, int limit) {
        List<ChargingProcess> processes = chargingProcessRepository.findRecentCompletedByCarId(carId);
        
        return processes.stream()
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取充电统计数据
     * 优先从 charging_processes 表查询，如果为空则使用 GrafanaStatsService 从 positions 计算
     */
    @Transactional(readOnly = true)
    public ChargingStatsDto getChargingStats(Long carId, int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minus(days, ChronoUnit.DAYS);
        
        // 先检查 charging_processes 表是否有数据
        Long totalSessions = chargingProcessRepository.countByCarIdAndDateRange(carId, startDate, endDate);
        
        // 如果 charging_processes 表为空，使用 Grafana 风格从 positions 计算
        if (totalSessions == 0) {
            log.info("No charging processes found for car {}, using GrafanaStatsService to calculate from positions", carId);
            return grafanaStatsService.getChargingStats(carId, days);
        }
        
        return calculateChargingStats(carId, startDate, endDate);
    }
    
    /**
     * 获取月度充电统计
     */
    @Transactional(readOnly = true)
    public ChargingStatsDto getMonthlyChargingStats(Long carId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);
        
        // 先检查是否有数据
        Long totalSessions = chargingProcessRepository.countByCarIdAndDateRange(carId, startDate, endDate);
        if (totalSessions == 0) {
            // 使用 GrafanaStatsService 按时间范围查询
            return grafanaStatsService.getChargingStatsByDateRange(carId, startDate, endDate);
        }
        
        return calculateChargingStats(carId, startDate, endDate);
    }
    
    /**
     * 计算充电统计
     */
    private ChargingStatsDto calculateChargingStats(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        // 基础统计数据
        Long totalSessions = chargingProcessRepository.countByCarIdAndDateRange(carId, startDate, endDate);
        BigDecimal totalEnergyAdded = chargingProcessRepository.sumEnergyAddedByCarIdAndDateRange(carId, startDate, endDate);
        BigDecimal totalCost = chargingProcessRepository.sumCostByCarIdAndDateRange(carId, startDate, endDate);
        
        if (totalSessions == 0) {
            return buildEmptyStats(startDate, endDate);
        }
        
        // 获取详细记录用于分类统计
        List<ChargingProcess> processes = chargingProcessRepository
                .findByCarIdAndStartDateBetweenOrderByStartDateDesc(carId, startDate, endDate);
        
        // 分类统计（这里简化处理，实际可根据地理围栏等信息判断）
        Long homeChargingSessions = processes.stream()
                .filter(p -> isHomeCharging(p))
                .count();
        
        BigDecimal homeEnergyAdded = processes.stream()
                .filter(p -> isHomeCharging(p))
                .map(p -> p.getChargeEnergyAdded() != null ? p.getChargeEnergyAdded() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Long superChargingSessions = totalSessions - homeChargingSessions;
        BigDecimal superEnergyAdded = totalEnergyAdded.subtract(homeEnergyAdded);
        
        // 计算百分比
        BigDecimal homePercentage = totalSessions > 0 ? 
                BigDecimal.valueOf(homeChargingSessions).divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
                BigDecimal.ZERO;
        BigDecimal superPercentage = BigDecimal.valueOf(100).subtract(homePercentage);
        
        // 计算平均值
        BigDecimal avgEnergyPerSession = totalEnergyAdded.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        BigDecimal avgCostPerSession = totalCost.divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
        
        Double avgDurationMin = processes.stream()
                .filter(p -> p.getDurationMin() != null)
                .mapToInt(ChargingProcess::getDurationMin)
                .average()
                .orElse(0.0);
        
        // 计算环保数据
        BigDecimal estimatedSavings = calculateFuelSavings(totalEnergyAdded);
        BigDecimal carbonReduction = totalEnergyAdded.multiply(CO2_REDUCTION_PER_KWH);
        
        return ChargingStatsDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalChargingSessions(totalSessions)
                .totalEnergyAdded(totalEnergyAdded)
                .totalCost(totalCost)
                .avgEnergyPerSession(avgEnergyPerSession)
                .avgCostPerSession(avgCostPerSession)
                .avgDurationMin(avgDurationMin)
                .homeChargingSessions(homeChargingSessions)
                .homeEnergyAdded(homeEnergyAdded)
                .superChargingSessions(superChargingSessions)
                .superEnergyAdded(superEnergyAdded)
                .homeChargingPercentage(homePercentage)
                .superChargingPercentage(superPercentage)
                .estimatedSavings(estimatedSavings)
                .carbonReduction(carbonReduction)
                .build();
    }
    
    /**
     * 判断是否为家庭充电
     */
    private boolean isHomeCharging(ChargingProcess process) {
        // 简化判断：可以根据地理围栏、充电功率、时间等判断
        // 这里暂时用功率判断（家用充电桩通常功率较低）
        if (process.getChargeEnergyAdded() != null && process.getDurationMin() != null && process.getDurationMin() > 0) {
            BigDecimal avgPower = process.getChargeEnergyAdded()
                    .multiply(BigDecimal.valueOf(60))
                    .divide(BigDecimal.valueOf(process.getDurationMin()), 2, RoundingMode.HALF_UP);
            return avgPower.compareTo(BigDecimal.valueOf(11)) <= 0; // 11kW以下认为是家用充电
        }
        return false;
    }
    
    /**
     * 计算燃油节省费用
     */
    private BigDecimal calculateFuelSavings(BigDecimal totalEnergyKwh) {
        // 1kWh约等于3.3公里（Tesla平均能耗）
        BigDecimal totalKm = totalEnergyKwh.multiply(BigDecimal.valueOf(3.3));
        BigDecimal fuelNeeded = totalKm.multiply(FUEL_CONSUMPTION_PER_100KM).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return fuelNeeded.multiply(FUEL_PRICE_PER_LITER);
    }
    
    /**
     * 构建空统计数据
     */
    private ChargingStatsDto buildEmptyStats(LocalDateTime startDate, LocalDateTime endDate) {
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
    
    /**
     * 转换为DTO
     */
    private ChargingRecordDto convertToDto(ChargingProcess process) {
        return ChargingRecordDto.builder()
                .id(process.getId())
                .carId(process.getCarId())
                .startDate(process.getStartDate())
                .endDate(process.getEndDate())
                .durationMin(process.getDurationMin())
                .chargeEnergyAdded(process.getChargeEnergyAdded())
                .chargeEnergyUsed(process.getChargeEnergyUsed())
                .startBatteryLevel(process.getStartBatteryLevel())
                .endBatteryLevel(process.getEndBatteryLevel())
                .startRatedRangeKm(process.getStartRatedRangeKm())
                .endRatedRangeKm(process.getEndRatedRangeKm())
                .cost(process.getCost())
                .outsideTempAvg(process.getOutsideTempAvg())
                .geofenceId(process.getGeofenceId())
                .chargeType(isHomeCharging(process) ? "家用充电桩" : "超级充电站")
                .status(process.getEndDate() != null ? "已完成" : "充电中")
                .isCompleted(process.getEndDate() != null)
                .build();
    }
}