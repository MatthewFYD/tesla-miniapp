package com.tesla.miniapp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingStatsDto {
    
    /**
     * 统计时间范围开始
     */
    private LocalDateTime startDate;
    
    /**
     * 统计时间范围结束
     */
    private LocalDateTime endDate;
    
    /**
     * 总充电次数
     */
    private Long totalChargingSessions;
    
    /**
     * 总充电电量（kWh）
     */
    private BigDecimal totalEnergyAdded;
    
    /**
     * 总充电费用
     */
    private BigDecimal totalCost;
    
    /**
     * 平均单次充电电量
     */
    private BigDecimal avgEnergyPerSession;
    
    /**
     * 平均单次充电费用
     */
    private BigDecimal avgCostPerSession;
    
    /**
     * 平均充电时长（分钟）
     */
    private Double avgDurationMin;
    
    /**
     * 家庭充电次数
     */
    private Long homeChargingSessions;
    
    /**
     * 家庭充电电量
     */
    private BigDecimal homeEnergyAdded;
    
    /**
     * 超级充电次数
     */
    private Long superChargingSessions;
    
    /**
     * 超级充电电量
     */
    private BigDecimal superEnergyAdded;
    
    /**
     * 家庭充电占比
     */
    private BigDecimal homeChargingPercentage;
    
    /**
     * 超级充电占比
     */
    private BigDecimal superChargingPercentage;
    
    /**
     * 预估节省费用（相比燃油车）
     */
    private BigDecimal estimatedSavings;
    
    /**
     * 碳减排量（kg CO2）
     */
    private BigDecimal carbonReduction;
}