package com.tesla.miniapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 车辆状态实体类
 * 对应TeslaMate states表
 * 记录车辆的在线/离线/休眠/行驶/充电等状态
 */
@Entity
@Table(name = "states")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class State {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "car_id", nullable = false)
    private Long carId;
    
    /**
     * 车辆状态
     * online - 在线
     * offline - 离线
     * asleep - 休眠
     * driving - 行驶中
     * charging - 充电中
     * suspended - 已暂停
     */
    @Column(name = "state")
    private String state;
    
    /**
     * 状态开始时间
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    /**
     * 状态结束时间（null表示当前状态）
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    // 关联车辆信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", insertable = false, updatable = false)
    private Car car;
}
