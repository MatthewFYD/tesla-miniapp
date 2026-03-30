package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行程轨迹DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripTraceDto {
    
    /**
     * 行程ID
     */
    private Long tripId;
    
    /**
     * 车辆ID
     */
    private Long carId;
    
    /**
     * 行程开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 行程结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 行程距离（公里）
     */
    private BigDecimal distance;
    
    /**
     * 轨迹点数量
     */
    private Integer pointCount;
    
    /**
     * 轨迹点列表
     */
    private List<TracePoint> points;
    
    /**
     * 轨迹点
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TracePoint {
        /** 纬度 */
        private BigDecimal latitude;
        /** 经度 */
        private BigDecimal longitude;
        /** 时间 */
        private LocalDateTime time;
        /** 速度（km/h） */
        private Integer speed;
        /** 功率（kW） */
        private Integer power;
        /** 电池电量（%） */
        private Integer batteryLevel;
        /** 海拔高度（米） */
        private Integer elevation;
        /** 里程表读数 */
        private BigDecimal odometer;
    }
}
