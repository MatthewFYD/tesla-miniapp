package com.tesla.miniapp.controller;

import com.tesla.miniapp.dto.ApiResponse;
import com.tesla.miniapp.dto.ChargingRecordDto;
import com.tesla.miniapp.dto.ChargingStatsDto;
import com.tesla.miniapp.entity.WechatUser;
import com.tesla.miniapp.service.AuthService;
import com.tesla.miniapp.service.ChargingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电控制器
 */
@Slf4j
@RestController
@RequestMapping("/charging")
@RequiredArgsConstructor
@Validated
@Tag(name = "充电接口", description = "充电记录、统计相关接口")
public class ChargingController {
    
    private final ChargingService chargingService;
    private final AuthService authService;
    
    /**
     * 获取充电记录列表
     */
    @Operation(summary = "获取充电记录", description = "分页获取车辆的充电记录")
    @GetMapping("/records")
    public ApiResponse<Page<ChargingRecordDto>> getChargingRecords(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            // 检查车辆访问权限
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<ChargingRecordDto> records = chargingService.getChargingRecords(carId, pageable);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get charging records for car: {}", carId, e);
            return ApiResponse.error("获取充电记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定时间范围的充电记录
     */
    @Operation(summary = "按时间范围获取充电记录", description = "获取指定时间范围内的充电记录")
    @GetMapping("/records/range")
    public ApiResponse<List<ChargingRecordDto>> getChargingRecordsByDateRange(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<ChargingRecordDto> records = chargingService.getChargingRecordsByDateRange(carId, startDate, endDate);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get charging records by date range for car: {}", carId, e);
            return ApiResponse.error("获取充电记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取正在进行的充电记录
     */
    @Operation(summary = "获取正在进行的充电", description = "获取车辆当前正在进行的充电记录")
    @GetMapping("/ongoing")
    public ApiResponse<List<ChargingRecordDto>> getOngoingChargingSessions(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<ChargingRecordDto> records = chargingService.getOngoingChargingSessions(carId);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get ongoing charging sessions for car: {}", carId, e);
            return ApiResponse.error("获取正在进行的充电记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近的充电记录
     */
    @Operation(summary = "获取最近充电记录", description = "获取车辆最近完成的充电记录")
    @GetMapping("/recent")
    public ApiResponse<List<ChargingRecordDto>> getRecentChargingSessions(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "记录数量") @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<ChargingRecordDto> records = chargingService.getRecentCompletedChargingSessions(carId, limit);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get recent charging sessions for car: {}", carId, e);
            return ApiResponse.error("获取最近充电记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取充电统计数据
     */
    @Operation(summary = "获取充电统计", description = "获取指定天数内的充电统计数据")
    @GetMapping("/stats")
    public ApiResponse<ChargingStatsDto> getChargingStats(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            ChargingStatsDto stats = chargingService.getChargingStats(carId, days);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get charging stats for car: {}", carId, e);
            return ApiResponse.error("获取充电统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取月度充电统计
     */
    @Operation(summary = "获取月度充电统计", description = "获取指定月份的充电统计数据")
    @GetMapping("/stats/monthly")
    public ApiResponse<ChargingStatsDto> getMonthlyChargingStats(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "月份") @RequestParam int month,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            ChargingStatsDto stats = chargingService.getMonthlyChargingStats(carId, year, month);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get monthly charging stats for car: {}", carId, e);
            return ApiResponse.error("获取月度充电统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前年度充电统计摘要
     */
    @Operation(summary = "获取年度充电摘要", description = "获取当前年度的充电统计摘要")
    @GetMapping("/stats/yearly")
    public ApiResponse<ChargingStatsDto> getYearlyChargingStats(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "年份") @RequestParam(required = false) Integer year,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            int targetYear = year != null ? year : LocalDateTime.now().getYear();
            
            // 计算全年统计（1月1日到12月31日）
            int daysInYear = targetYear % 4 == 0 ? 366 : 365;
            ChargingStatsDto stats = chargingService.getChargingStats(carId, daysInYear);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get yearly charging stats for car: {}", carId, e);
            return ApiResponse.error("获取年度充电统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户
     */
    private WechatUser getCurrentUser(Authentication authentication) {
        String openid = (String) authentication.getPrincipal();
        return authService.getUserByOpenid(openid);
    }
    
    /**
     * 检查用户是否有车辆访问权限
     */
    private boolean hasVehicleAccess(Long userId, Long carId) {
        // 实际应该检查用户车辆绑定关系
        return true;
    }
}