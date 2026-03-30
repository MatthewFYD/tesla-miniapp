package com.tesla.miniapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 充电记录实体类
 * 对应TeslaMate charging_processes表
 * 注意：TeslaMate的charging_processes表没有inserted_at/updated_at字段
 */
@Entity
@Table(name = "charging_processes")
public class ChargingProcess {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "car_id", nullable = false)
    private Long carId;
    
    @Column(name = "position_id")
    private Long positionId;
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "charge_energy_added", precision = 6, scale = 2)
    private BigDecimal chargeEnergyAdded;
    
    @Column(name = "charge_energy_used", precision = 6, scale = 2)
    private BigDecimal chargeEnergyUsed;
    
    @Column(name = "start_ideal_range_km", precision = 6, scale = 2)
    private BigDecimal startIdealRangeKm;
    
    @Column(name = "end_ideal_range_km", precision = 6, scale = 2)
    private BigDecimal endIdealRangeKm;
    
    @Column(name = "start_rated_range_km", precision = 6, scale = 2)
    private BigDecimal startRatedRangeKm;
    
    @Column(name = "end_rated_range_km", precision = 6, scale = 2)
    private BigDecimal endRatedRangeKm;
    
    @Column(name = "start_battery_level")
    private Integer startBatteryLevel;
    
    @Column(name = "end_battery_level")
    private Integer endBatteryLevel;
    
    @Column(name = "duration_min")
    private Integer durationMin;
    
    @Column(name = "outside_temp_avg", precision = 4, scale = 1)
    private BigDecimal outsideTempAvg;
    
    @Column(name = "cost", precision = 6, scale = 2)
    private BigDecimal cost;
    
    @Column(name = "geofence_id")
    private Long geofenceId;
    
    // 关联车辆信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", insertable = false, updatable = false)
    private Car car;

    // Constructors
    public ChargingProcess() {}

    public ChargingProcess(Long id, Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }

    public Long getPositionId() { return positionId; }
    public void setPositionId(Long positionId) { this.positionId = positionId; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public BigDecimal getChargeEnergyAdded() { return chargeEnergyAdded; }
    public void setChargeEnergyAdded(BigDecimal chargeEnergyAdded) { this.chargeEnergyAdded = chargeEnergyAdded; }

    public BigDecimal getChargeEnergyUsed() { return chargeEnergyUsed; }
    public void setChargeEnergyUsed(BigDecimal chargeEnergyUsed) { this.chargeEnergyUsed = chargeEnergyUsed; }

    public BigDecimal getStartIdealRangeKm() { return startIdealRangeKm; }
    public void setStartIdealRangeKm(BigDecimal startIdealRangeKm) { this.startIdealRangeKm = startIdealRangeKm; }

    public BigDecimal getEndIdealRangeKm() { return endIdealRangeKm; }
    public void setEndIdealRangeKm(BigDecimal endIdealRangeKm) { this.endIdealRangeKm = endIdealRangeKm; }

    public BigDecimal getStartRatedRangeKm() { return startRatedRangeKm; }
    public void setStartRatedRangeKm(BigDecimal startRatedRangeKm) { this.startRatedRangeKm = startRatedRangeKm; }

    public BigDecimal getEndRatedRangeKm() { return endRatedRangeKm; }
    public void setEndRatedRangeKm(BigDecimal endRatedRangeKm) { this.endRatedRangeKm = endRatedRangeKm; }

    public Integer getStartBatteryLevel() { return startBatteryLevel; }
    public void setStartBatteryLevel(Integer startBatteryLevel) { this.startBatteryLevel = startBatteryLevel; }

    public Integer getEndBatteryLevel() { return endBatteryLevel; }
    public void setEndBatteryLevel(Integer endBatteryLevel) { this.endBatteryLevel = endBatteryLevel; }

    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }

    public BigDecimal getOutsideTempAvg() { return outsideTempAvg; }
    public void setOutsideTempAvg(BigDecimal outsideTempAvg) { this.outsideTempAvg = outsideTempAvg; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Long getGeofenceId() { return geofenceId; }
    public void setGeofenceId(Long geofenceId) { this.geofenceId = geofenceId; }

    public Car getCar() { return car; }
    public void setCar(Car car) { this.car = car; }
}