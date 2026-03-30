package com.tesla.miniapp.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 行程统计DTO
 */
@Data
@Builder
public class TripStatsDto {
    private long totalCount;           // 总行程次数
    private double totalDistance;      // 总里程 (km)
    private long totalDuration;        // 总时长 (秒)
    private double totalEnergyUsed;    // 总能耗 (kWh)
    private double avgDistance;        // 平均单次里程 (km)
    private double avgDuration;        // 平均单次时长 (秒)
    private double avgConsumption;     // 平均能耗 (Wh/km)
    private double maxDistance;        // 最长单次里程 (km)
    private int maxSpeed;              // 最高速度 (km/h)
}
