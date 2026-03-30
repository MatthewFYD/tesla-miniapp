package com.tesla.miniapp.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行程记录DTO
 */
@Data
@Builder
public class TripRecordDto {
    private Long id;
    private Long carId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String startAddress;
    private String endAddress;
    private Double startLatitude;
    private Double startLongitude;
    private Double endLatitude;
    private Double endLongitude;
    private BigDecimal distance;  // km
    private Integer durationMin;
    private Integer speedMax;
    private Double speedAvg;
    private Integer powerMax;
    private Integer powerMin;
    private Integer startBatteryLevel;
    private Integer endBatteryLevel;
    private BigDecimal startIdealRangeKm;
    private BigDecimal endIdealRangeKm;
    private BigDecimal outsideTempAvg;
    private BigDecimal efficiency;  // Wh/km
}
