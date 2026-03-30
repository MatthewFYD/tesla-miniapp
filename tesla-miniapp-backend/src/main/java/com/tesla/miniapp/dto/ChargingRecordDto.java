package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电记录DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingRecordDto {
    
    /**
     * 充电记录ID
     */
    private Long id;
    
    /**
     * 车辆ID
     */
    private Long carId;
    
    /**
     * 开始时间
     */
    private LocalDateTime startDate;
    
    /**
     * 结束时间
     */
    private LocalDateTime endDate;
    
    /**
     * 充电时长（分钟）
     */
    private Integer durationMin;
    
    /**
     * 充电增加的电量（kWh）
     */
    private BigDecimal chargeEnergyAdded;
    
    /**
     * 消耗的电量（kWh）
     */
    private BigDecimal chargeEnergyUsed;
    
    /**
     * 开始时电池电量（%）
     */
    private Integer startBatteryLevel;
    
    /**
     * 结束时电池电量（%）
     */
    private Integer endBatteryLevel;
    
    /**
     * 开始时续航里程（公里）
     */
    private BigDecimal startRatedRangeKm;
    
    /**
     * 结束时续航里程（公里）
     */
    private BigDecimal endRatedRangeKm;
    
    /**
     * 充电费用
     */
    private BigDecimal cost;
    
    /**
     * 平均外部温度
     */
    private BigDecimal outsideTempAvg;
    
    /**
     * 地理围栏ID
     */
    private Long geofenceId;
    
    /**
     * 充电地点名称
     */
    private String locationName;
    
    /**
     * 充电类型（家用/超充等）
     */
    private String chargeType;
    
    /**
     * 充电状态
     */
    private String status;
    
    /**
     * 是否完成
     */
    private Boolean isCompleted;
}