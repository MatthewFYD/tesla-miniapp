package com.tesla.miniapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Tesla车辆信息实体类
 * 对应TeslaMate cars表
 * 注意：TeslaMate的cars表没有inserted_at/updated_at字段
 */
@Entity
@Table(name = "cars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "efficiency", precision = 5, scale = 2)
    private BigDecimal efficiency;
    
    @Column(name = "eid")
    private Long eid;
    
    @Column(name = "model")
    private String model;
    
    @Column(name = "vid")
    private Long vid;
    
    @Column(name = "vin")
    private String vin;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "trim_badging")
    private String trimBadging;
    
    @Column(name = "exterior_color")
    private String exteriorColor;
    
    @Column(name = "wheel_type")
    private String wheelType;
    
    @Column(name = "spoiler_type")
    private String spoilerType;
}