package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆状态DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleStatusDto {
    
    /**
     * 车辆ID
     */
    private Long carId;
    
    /**
     * 车辆名称
     */
    private String carName;
    
    /**
     * 车辆型号
     */
    private String model;
    
    /**
     * VIN
     */
    private String vin;
    
    /**
     * 电池电量（百分比）
     */
    private Integer batteryLevel;
    
    /**
     * 可用电池电量（百分比）
     */
    private Integer usableBatteryLevel;
    
    /**
     * 预计续航里程（公里）
     */
    private BigDecimal estRangeKm;
    
    /**
     * 额定续航里程（公里）
     */
    private BigDecimal ratedRangeKm;
    
    /**
     * 理想续航里程（公里）
     */
    private BigDecimal idealRangeKm;
    
    /**
     * 外部温度（摄氏度）
     */
    private BigDecimal outsideTemp;
    
    /**
     * 内部温度（摄氏度）
     */
    private BigDecimal insideTemp;
    
    /**
     * 是否开启空调
     */
    private Boolean isClimateOn;
    
    /**
     * 里程表读数（公里）
     */
    private BigDecimal odometer;
    
    /**
     * 充电状态
     */
    private String chargingState;
    
    /**
     * 是否正在充电
     */
    private Boolean isCharging;
    
    /**
     * 充电功率（千瓦）
     */
    private Integer chargePower;
    
    /**
     * 车辆位置 - 纬度
     */
    private BigDecimal latitude;
    
    /**
     * 车辆位置 - 经度
     */
    private BigDecimal longitude;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
}