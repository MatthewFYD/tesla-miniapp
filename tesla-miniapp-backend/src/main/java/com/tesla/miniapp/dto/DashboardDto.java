package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆仪表盘数据DTO
 * 一站式返回所有车辆相关数据，对应Grafana仪表盘的所有字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {
    
    /**
     * 车辆基础信息
     */
    private BasicInfo basic;
    
    /**
     * 电池信息
     */
    private BatteryInfo battery;
    
    /**
     * 温度信息
     */
    private TemperatureInfo temperature;
    
    /**
     * 胎压信息
     */
    private TirePressureInfo tirePressure;
    
    /**
     * 车辆状态信息
     */
    private StatusInfo status;
    
    /**
     * 里程表读数（公里）
     */
    private BigDecimal odometer;
    
    /**
     * 固件版本
     */
    private String firmwareVersion;
    
    /**
     * 位置信息
     */
    private LocationInfo location;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
    
    // ========== 内部类 ==========
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BasicInfo {
        private Long carId;
        private String carName;
        private String model;
        private String vin;
        private String exteriorColor;
        private String wheelType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatteryInfo {
        /** 电池电量（百分比） */
        private Integer level;
        /** 可用电池电量（百分比） */
        private Integer usableLevel;
        /** 预估续航里程（公里） */
        private BigDecimal estRangeKm;
        /** 额定续航里程（公里） */
        private BigDecimal ratedRangeKm;
        /** 理想续航里程（公里） */
        private BigDecimal idealRangeKm;
        /** 是否正在充电 */
        private Boolean isCharging;
        /** 充电功率（千瓦） */
        private Integer chargePower;
        /** 充电状态 */
        private String chargingState;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemperatureInfo {
        /** 车内温度（摄氏度） */
        private BigDecimal inside;
        /** 车外温度（摄氏度） */
        private BigDecimal outside;
        /** 空调目标温度 */
        private BigDecimal climateTarget;
        /** 是否开启空调 */
        private Boolean isClimateOn;
        /** 是否开启前除霜 */
        private Boolean isFrontDefrosterOn;
        /** 是否开启后除霜 */
        private Boolean isRearDefrosterOn;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TirePressureInfo {
        /** 左前轮胎压 (bar) */
        private BigDecimal frontLeft;
        /** 右前轮胎压 (bar) */
        private BigDecimal frontRight;
        /** 左后轮胎压 (bar) */
        private BigDecimal rearLeft;
        /** 右后轮胎压 (bar) */
        private BigDecimal rearRight;
        /** 最后更新时间 */
        private LocalDateTime lastUpdated;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusInfo {
        /** 当前状态（online/offline/asleep/driving/charging） */
        private String state;
        /** 状态显示名称（在线/离线/休眠/行驶中/充电中） */
        private String stateDisplay;
        /** 状态开始时间 */
        private LocalDateTime since;
        /** 在线时间百分比 */
        private BigDecimal onlinePercent;
        /** 离线时间百分比 */
        private BigDecimal offlinePercent;
        /** 在线时间（格式化） */
        private String onlineTime;
        /** 离线时间（格式化） */
        private String offlineTime;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        /** 纬度 */
        private BigDecimal latitude;
        /** 经度 */
        private BigDecimal longitude;
        /** 速度（km/h） */
        private Integer speed;
        /** 方向角度 */
        private Integer heading;
        /** 海拔高度（米） */
        private Integer elevation;
        /** 地理围栏名称 */
        private String geofence;
    }
}
