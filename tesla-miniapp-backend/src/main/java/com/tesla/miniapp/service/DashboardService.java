package com.tesla.miniapp.service;

import com.tesla.miniapp.dto.DashboardDto;
import com.tesla.miniapp.entity.Car;
import com.tesla.miniapp.entity.Position;
import com.tesla.miniapp.entity.State;
import com.tesla.miniapp.repository.CarRepository;
import com.tesla.miniapp.repository.PositionRepository;
import com.tesla.miniapp.repository.UpdateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * 仪表盘服务 - 聚合所有车辆数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final CarRepository carRepository;
    private final PositionRepository positionRepository;
    private final StateService stateService;
    private final UpdateRepository updateRepository;
    
    /**
     * 获取车辆仪表盘全部数据
     */
    @Transactional(readOnly = true)
    public DashboardDto getDashboard(Long carId) {
        // 获取车辆基础信息
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + carId));
        
        // 获取最新位置信息
        Position latestPosition = positionRepository.findLatestByCarId(carId).orElse(null);
        
        // 获取当前状态
        Optional<State> currentState = stateService.getCurrentState(carId);
        
        // 获取在线统计（最近24小时）
        Map<String, Object> onlineStats = stateService.getOnlineStatistics(carId, 1);
        
        // 获取固件版本
        String firmwareVersion = updateRepository.findCurrentVersion(carId).orElse(null);
        
        // 构建仪表盘数据
        return buildDashboard(car, latestPosition, currentState.orElse(null), onlineStats, firmwareVersion);
    }
    
    /**
     * 构建仪表盘数据
     */
    private DashboardDto buildDashboard(Car car, Position position, State state, 
                                         Map<String, Object> onlineStats, String firmwareVersion) {
        DashboardDto.DashboardDtoBuilder builder = DashboardDto.builder();
        
        // 基础信息
        builder.basic(DashboardDto.BasicInfo.builder()
                .carId(car.getId())
                .carName(car.getName())
                .model(car.getModel())
                .vin(car.getVin())
                .exteriorColor(car.getExteriorColor())
                .wheelType(car.getWheelType())
                .build());
        
        // 固件版本
        builder.firmwareVersion(firmwareVersion);
        
        if (position != null) {
            // 电池信息
            builder.battery(DashboardDto.BatteryInfo.builder()
                    .level(position.getBatteryLevel())
                    .usableLevel(position.getUsableBatteryLevel())
                    .estRangeKm(position.getEstBatteryRangeKm())
                    .ratedRangeKm(position.getRatedBatteryRangeKm())
                    .idealRangeKm(position.getIdealBatteryRangeKm())
                    .isCharging(position.getPower() != null && position.getPower() > 0)
                    .chargePower(position.getPower())
                    .chargingState(determineChargingState(position))
                    .build());
            
            // 温度信息
            builder.temperature(DashboardDto.TemperatureInfo.builder()
                    .inside(position.getInsideTemp())
                    .outside(position.getOutsideTemp())
                    .climateTarget(position.getDriverTempSetting())
                    .isClimateOn(position.getIsClimateOn())
                    .isFrontDefrosterOn(position.getIsFrontDefrosterOn())
                    .isRearDefrosterOn(position.getIsRearDefrosterOn())
                    .build());
            
            // 胎压信息 - TeslaMate可能没有独立的胎压表，这里预留接口
            // 实际数据可能需要通过MQTT或其他方式获取
            builder.tirePressure(DashboardDto.TirePressureInfo.builder()
                    .frontLeft(null)  // 待实现
                    .frontRight(null)
                    .rearLeft(null)
                    .rearRight(null)
                    .lastUpdated(null)
                    .build());
            
            // 里程表
            builder.odometer(position.getOdometer());
            
            // 位置信息
            builder.location(DashboardDto.LocationInfo.builder()
                    .latitude(position.getLatitude())
                    .longitude(position.getLongitude())
                    .speed(position.getSpeed())
                    .heading(null)  // positions表可能没有heading字段
                    .elevation(null) // positions表可能没有elevation字段
                    .geofence(null)  // 需要关联geofences表
                    .build());
            
            // 最后更新时间
            builder.lastUpdated(position.getDate());
        }
        
        // 状态信息
        String currentStateName = state != null ? state.getState() : "unknown";
        LocalDateTime stateSince = state != null ? state.getStartDate() : null;
        
        builder.status(DashboardDto.StatusInfo.builder()
                .state(currentStateName)
                .stateDisplay(stateService.getStateDisplayName(currentStateName))
                .since(stateSince)
                .onlinePercent((BigDecimal) onlineStats.getOrDefault("onlinePercent", BigDecimal.ZERO))
                .offlinePercent((BigDecimal) onlineStats.getOrDefault("offlinePercent", BigDecimal.valueOf(100)))
                .onlineTime((String) onlineStats.getOrDefault("onlineTime", "0:00:00"))
                .offlineTime((String) onlineStats.getOrDefault("offlineTime", "0:00:00"))
                .build());
        
        return builder.build();
    }
    
    /**
     * 判断充电状态
     */
    private String determineChargingState(Position position) {
        if (position.getPower() == null) {
            return "Not Charging";
        }
        
        if (position.getPower() > 0) {
            return "Charging";
        } else if (position.getPower() == 0 && position.getBatteryLevel() != null && position.getBatteryLevel() >= 100) {
            return "Complete";
        } else {
            return "Not Charging";
        }
    }
}
