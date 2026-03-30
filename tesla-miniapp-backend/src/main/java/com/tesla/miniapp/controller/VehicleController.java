package com.tesla.miniapp.controller;

import com.tesla.miniapp.dto.ApiResponse;
import com.tesla.miniapp.dto.VehicleStatusDto;
import com.tesla.miniapp.entity.Car;
import com.tesla.miniapp.entity.WechatUser;
import com.tesla.miniapp.service.AuthService;
import com.tesla.miniapp.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 车辆控制器
 */
@Slf4j
@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(name = "车辆接口", description = "车辆信息、状态相关接口")
public class VehicleController {
    
    private final VehicleService vehicleService;
    private final AuthService authService;
    
    /**
     * 获取用户的车辆列表
     */
    @Operation(summary = "获取车辆列表", description = "获取当前用户绑定的所有车辆")
    @GetMapping("/list")
    public ApiResponse<List<Car>> getUserVehicles(Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            List<Car> cars = vehicleService.getUserCars(user.getId());
            return ApiResponse.success(cars);
        } catch (Exception e) {
            log.error("Failed to get user vehicles", e);
            return ApiResponse.error("获取车辆列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有可绑定的车辆列表
     */
    @Operation(summary = "获取可绑定车辆", description = "获取所有可以绑定的车辆（TeslaMate中的车辆）")
    @GetMapping("/available")
    public ApiResponse<List<Car>> getAvailableVehicles(Authentication authentication) {
        try {
            List<Car> cars = vehicleService.getAllCars();
            return ApiResponse.success(cars);
        } catch (Exception e) {
            log.error("Failed to get available vehicles", e);
            return ApiResponse.error("获取可绑定车辆失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户主要车辆
     */
    @Operation(summary = "获取主要车辆", description = "获取用户设置的主要车辆")
    @GetMapping("/primary")
    public ApiResponse<Car> getPrimaryVehicle(Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            Optional<Car> primaryCar = vehicleService.getUserPrimaryCar(user.getId());
            
            if (primaryCar.isPresent()) {
                return ApiResponse.success(primaryCar.get());
            } else {
                return ApiResponse.error(404, "未设置主要车辆");
            }
        } catch (Exception e) {
            log.error("Failed to get primary vehicle", e);
            return ApiResponse.error("获取主要车辆失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取车辆状态
     */
    @Operation(summary = "获取车辆状态", description = "获取指定车辆的实时状态信息")
    @GetMapping("/{carId}/status")
    public ApiResponse<VehicleStatusDto> getVehicleStatus(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            // 验证用户是否有权限访问该车辆
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            VehicleStatusDto status = vehicleService.getVehicleStatus(carId);
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("Failed to get vehicle status for car: {}", carId, e);
            return ApiResponse.error("获取车辆状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 绑定车辆
     */
    @Operation(summary = "绑定车辆", description = "将车辆绑定到当前用户")
    @PostMapping("/{carId}/bind")
    public ApiResponse<Void> bindVehicle(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            @Parameter(description = "车辆别名") @RequestParam(required = false) String aliasName,
            @Parameter(description = "是否设为主要车辆") @RequestParam(defaultValue = "false") boolean isPrimary,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            vehicleService.bindVehicleToUser(user.getId(), carId, aliasName, isPrimary);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to bind vehicle {} to user {}", carId, getCurrentUserId(authentication), e);
            return ApiResponse.error("车辆绑定失败: " + e.getMessage());
        }
    }
    
    /**
     * 解绑车辆
     */
    @Operation(summary = "解绑车辆", description = "解除车辆与当前用户的绑定关系")
    @DeleteMapping("/{carId}/unbind")
    public ApiResponse<Void> unbindVehicle(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            vehicleService.unbindVehicleFromUser(user.getId(), carId);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to unbind vehicle {} from user {}", carId, getCurrentUserId(authentication), e);
            return ApiResponse.error("解绑车辆失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置主要车辆
     */
    @Operation(summary = "设置主要车辆", description = "将指定车辆设置为主要车辆")
    @PutMapping("/{carId}/primary")
    public ApiResponse<Void> setPrimaryVehicle(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            vehicleService.setPrimaryVehicle(user.getId(), carId);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Failed to set primary vehicle {} for user {}", carId, getCurrentUserId(authentication), e);
            return ApiResponse.error("设置主要车辆失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查车辆是否正在充电
     */
    @Operation(summary = "检查充电状态", description = "检查车辆是否正在充电")
    @GetMapping("/{carId}/charging-status")
    public ApiResponse<Boolean> getChargingStatus(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            boolean isCharging = vehicleService.isVehicleCharging(carId);
            return ApiResponse.success(isCharging);
        } catch (Exception e) {
            log.error("Failed to get charging status for car: {}", carId, e);
            return ApiResponse.error("获取充电状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户
     */
    private WechatUser getCurrentUser(Authentication authentication) {
        // authentication.getPrincipal() 是 openid (在 JwtAuthenticationFilter 中设置的)
        String openid = (String) authentication.getPrincipal();
        return authService.getUserByOpenid(openid);
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }
    
    /**
     * 检查用户是否有车辆访问权限
     */
    private boolean hasVehicleAccess(Long userId, Long carId) {
        // 这里应该检查用户是否绑定了该车辆
        // 简化处理，实际应该调用vehicleService的相关方法
        return true;
    }
}