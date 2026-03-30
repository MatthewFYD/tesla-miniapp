package com.tesla.miniapp.service;

import com.tesla.miniapp.dto.VehicleStatusDto;
import com.tesla.miniapp.entity.Car;
import com.tesla.miniapp.entity.Position;
import com.tesla.miniapp.entity.UserCarBinding;
import com.tesla.miniapp.repository.CarRepository;
import com.tesla.miniapp.repository.PositionRepository;
import com.tesla.miniapp.repository.UserCarBindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 车辆服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    
    private final CarRepository carRepository;
    private final PositionRepository positionRepository;
    private final UserCarBindingRepository userCarBindingRepository;
    
    /**
     * 获取用户的车辆列表
     */
    @Transactional(readOnly = true)
    public List<Car> getUserCars(Long userId) {
        List<UserCarBinding> bindings = userCarBindingRepository.findByUserIdWithCarDetails(userId);
        return bindings.stream()
                .map(UserCarBinding::getCar)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有车辆 (TeslaMate 中的车辆)
     */
    @Transactional(readOnly = true)
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }
    
    /**
     * 获取用户的主要车辆
     */
    @Transactional(readOnly = true)
    public Optional<Car> getUserPrimaryCar(Long userId) {
        return userCarBindingRepository.findPrimaryCarByUserId(userId)
                .map(UserCarBinding::getCar);
    }
    
    /**
     * 获取车辆当前状态
     */
    @Transactional(readOnly = true)
    public VehicleStatusDto getVehicleStatus(Long carId) {
        // 获取车辆基础信息
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + carId));
        
        // 获取最新位置信息
        Position latestPosition = positionRepository.findLatestByCarId(carId)
                .orElse(null);
        
        if (latestPosition == null) {
            log.warn("No position data found for car: {}", carId);
            return buildBasicVehicleStatus(car);
        }
        
        return buildVehicleStatus(car, latestPosition);
    }
    
    /**
     * 检查车辆是否正在充电
     */
    @Transactional(readOnly = true)
    public boolean isVehicleCharging(Long carId) {
        Optional<Position> latestPosition = positionRepository.findLatestByCarId(carId);
        return latestPosition.map(position -> position.getPower() != null && position.getPower() > 0)
                .orElse(false);
    }
    
    /**
     * 绑定车辆到用户
     */
    @Transactional
    public void bindVehicleToUser(Long userId, Long carId, String aliasName, boolean isPrimary) {
        // 检查车辆是否存在
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + carId));
        
        // 检查是否已经绑定
        if (userCarBindingRepository.existsByUserIdAndCarIdAndIsActiveTrue(userId, carId)) {
            throw new RuntimeException("Vehicle already bound to user");
        }
        
        // 如果设置为主要车辆，先取消其他车辆的主要状态
        if (isPrimary) {
            userCarBindingRepository.findPrimaryCarByUserId(userId)
                    .ifPresent(binding -> {
                        binding.setIsPrimary(false);
                        userCarBindingRepository.save(binding);
                    });
        }
        
        // 创建绑定关系
        UserCarBinding binding = new UserCarBinding();
        binding.setUserId(userId);
        binding.setCarId(carId);
        binding.setAliasName(aliasName != null ? aliasName : car.getName());
        binding.setIsPrimary(isPrimary);
        binding.setIsActive(true);
        
        userCarBindingRepository.save(binding);
        log.info("Vehicle {} bound to user {}", carId, userId);
    }
    
    /**
     * 解绑车辆
     */
    @Transactional
    public void unbindVehicleFromUser(Long userId, Long carId) {
        UserCarBinding binding = userCarBindingRepository.findByUserIdAndCarIdAndIsActiveTrue(userId, carId)
                .orElseThrow(() -> new RuntimeException("Vehicle binding not found"));
        
        binding.setIsActive(false);
        userCarBindingRepository.save(binding);
        
        log.info("Vehicle {} unbound from user {}", carId, userId);
    }
    
    /**
     * 设置主要车辆
     */
    @Transactional
    public void setPrimaryVehicle(Long userId, Long carId) {
        // 取消当前主要车辆
        userCarBindingRepository.findPrimaryCarByUserId(userId)
                .ifPresent(binding -> {
                    binding.setIsPrimary(false);
                    userCarBindingRepository.save(binding);
                });
        
        // 设置新的主要车辆
        UserCarBinding binding = userCarBindingRepository.findByUserIdAndCarIdAndIsActiveTrue(userId, carId)
                .orElseThrow(() -> new RuntimeException("Vehicle binding not found"));
        
        binding.setIsPrimary(true);
        userCarBindingRepository.save(binding);
        
        log.info("Vehicle {} set as primary for user {}", carId, userId);
    }
    
    /**
     * 构建车辆状态（含位置信息）
     */
    private VehicleStatusDto buildVehicleStatus(Car car, Position position) {
        return VehicleStatusDto.builder()
                .carId(car.getId())
                .carName(car.getName())
                .model(car.getModel())
                .vin(car.getVin())
                .batteryLevel(position.getBatteryLevel())
                .usableBatteryLevel(position.getUsableBatteryLevel())
                .estRangeKm(position.getEstBatteryRangeKm())
                .ratedRangeKm(position.getRatedBatteryRangeKm())
                .idealRangeKm(position.getIdealBatteryRangeKm())
                .outsideTemp(position.getOutsideTemp())
                .insideTemp(position.getInsideTemp())
                .isClimateOn(position.getIsClimateOn())
                .odometer(position.getOdometer())
                .chargingState(determineChargingState(position))
                .isCharging(position.getPower() != null && position.getPower() > 0)
                .chargePower(position.getPower())
                .latitude(position.getLatitude())
                .longitude(position.getLongitude())
                .lastUpdated(position.getDate())
                .build();
    }
    
    /**
     * 构建基础车辆状态（无位置信息）
     */
    private VehicleStatusDto buildBasicVehicleStatus(Car car) {
        return VehicleStatusDto.builder()
                .carId(car.getId())
                .carName(car.getName())
                .model(car.getModel())
                .vin(car.getVin())
                .chargingState("Unknown")
                .isCharging(false)
                .build();
    }
    
    /**
     * 判断充电状态
     */
    private String determineChargingState(Position position) {
        if (position.getPower() == null) {
            return "Not Charging";
        }
        
        if (position.getPower() > 0) {
            return "Charging";
        } else if (position.getPower() == 0 && position.getBatteryLevel() != null && position.getBatteryLevel() >= 100) {
            return "Complete";
        } else {
            return "Not Charging";
        }
    }
}