package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 车辆数据访问层
 */
@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    
    /**
     * 根据VIN查找车辆
     */
    Optional<Car> findByVin(String vin);
    
    /**
     * 根据车辆名称查找
     */
    List<Car> findByNameContainingIgnoreCase(String name);
    
    /**
     * 根据车型查找
     */
    List<Car> findByModel(String model);
    
    /**
     * 查找所有车辆（按ID排序）
     */
    @Query("SELECT c FROM Car c ORDER BY c.id ASC")
    List<Car> findAllOrderById();
}