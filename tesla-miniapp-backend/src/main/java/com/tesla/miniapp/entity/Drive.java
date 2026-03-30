package com.tesla.miniapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 行程记录实体类
 * 对应TeslaMate drives表
 * 注意：TeslaMate的drives表没有inserted_at/updated_at字段
 */
@Entity
@Table(name = "drives")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Drive {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "car_id", nullable = false)
    private Long carId;
    
    @Column(name = "start_position_id")
    private Long startPositionId;
    
    @Column(name = "end_position_id")
    private Long endPositionId;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "outside_temp_avg", precision = 4, scale = 1)
    private BigDecimal outsideTempAvg;
    
    @Column(name = "speed_max")
    private Integer speedMax;
    
    @Column(name = "power_max")
    private Integer powerMax;
    
    @Column(name = "power_min")
    private Integer powerMin;
    
    @Column(name = "start_ideal_range_km", precision = 6, scale = 2)
    private BigDecimal startIdealRangeKm;
    
    @Column(name = "end_ideal_range_km", precision = 6, scale = 2)
    private BigDecimal endIdealRangeKm;
    
    @Column(name = "start_rated_range_km", precision = 6, scale = 2)
    private BigDecimal startRatedRangeKm;
    
    @Column(name = "end_rated_range_km", precision = 6, scale = 2)
    private BigDecimal endRatedRangeKm;
    
    @Column(name = "distance", precision = 6, scale = 2)
    private BigDecimal distance;
    
    @Column(name = "duration_min")
    private Integer durationMin;
    
    @Column(name = "start_km", precision = 9, scale = 1)
    private BigDecimal startKm;
    
    @Column(name = "end_km", precision = 9, scale = 1)
    private BigDecimal endKm;
    
    @Column(name = "start_battery_level")
    private Integer startBatteryLevel;
    
    @Column(name = "end_battery_level")
    private Integer endBatteryLevel;
    
    @Column(name = "efficiency", precision = 5, scale = 2)
    private BigDecimal efficiency;
    
    @Column(name = "start_geofence_id")
    private Long startGeofenceId;
    
    @Column(name = "end_geofence_id")
    private Long endGeofenceId;
    
    // 关联车辆信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", insertable = false, updatable = false)
    private Car car;
}