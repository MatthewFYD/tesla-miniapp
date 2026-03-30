package com.tesla.miniapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 位置信息实体类
 * 对应TeslaMate positions表
 * 注意：TeslaMate的positions表没有inserted_at字段
 */
@Entity
@Table(name = "positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "car_id", nullable = false)
    private Long carId;
    
    @Column(name = "date")
    private LocalDateTime date;
    
    @Column(name = "latitude", precision = 8, scale = 6)
    private BigDecimal latitude;
    
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;
    
    @Column(name = "speed")
    private Integer speed;
    
    @Column(name = "power")
    private Integer power;
    
    @Column(name = "odometer", precision = 9, scale = 1)
    private BigDecimal odometer;
    
    @Column(name = "ideal_battery_range_km", precision = 6, scale = 2)
    private BigDecimal idealBatteryRangeKm;
    
    @Column(name = "est_battery_range_km", precision = 6, scale = 2)
    private BigDecimal estBatteryRangeKm;
    
    @Column(name = "rated_battery_range_km", precision = 6, scale = 2)
    private BigDecimal ratedBatteryRangeKm;
    
    @Column(name = "usable_battery_level")
    private Integer usableBatteryLevel;
    
    @Column(name = "battery_level")
    private Integer batteryLevel;
    
    @Column(name = "battery_heater")
    private Boolean batteryHeater;
    
    @Column(name = "battery_heater_on")
    private Boolean batteryHeaterOn;
    
    @Column(name = "battery_heater_no_power")
    private Boolean batteryHeaterNoPower;
    
    @Column(name = "outside_temp", precision = 4, scale = 1)
    private BigDecimal outsideTemp;
    
    @Column(name = "inside_temp", precision = 4, scale = 1)
    private BigDecimal insideTemp;
    
    @Column(name = "is_climate_on")
    private Boolean isClimateOn;
    
    @Column(name = "is_rear_defroster_on")
    private Boolean isRearDefrosterOn;
    
    @Column(name = "is_front_defroster_on")
    private Boolean isFrontDefrosterOn;
    
    @Column(name = "fan_status")
    private Integer fanStatus;
    
    @Column(name = "driver_temp_setting", precision = 4, scale = 1)
    private BigDecimal driverTempSetting;
    
    @Column(name = "passenger_temp_setting", precision = 4, scale = 1)
    private BigDecimal passengerTempSetting;
    
    @Column(name = "is_auto_conditioning_on")
    private Boolean isAutoConditioningOn;
    
    // 关联车辆信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", insertable = false, updatable = false)
    private Car car;
}