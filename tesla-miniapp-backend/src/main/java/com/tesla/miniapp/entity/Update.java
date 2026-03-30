package com.tesla.miniapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 固件更新实体类
 * 对应TeslaMate updates表
 * 记录车辆固件更新历史
 */
@Entity
@Table(name = "updates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Update {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "car_id", nullable = false)
    private Long carId;
    
    /**
     * 固件版本号
     */
    @Column(name = "version")
    private String version;
    
    /**
     * 更新开始时间
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    /**
     * 更新结束时间
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    // 关联车辆信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", insertable = false, updatable = false)
    private Car car;
}
