package com.tesla.miniapp.controller;

import com.tesla.miniapp.dto.ApiResponse;
import com.tesla.miniapp.dto.TripRecordDto;
import com.tesla.miniapp.dto.TripStatsDto;
import com.tesla.miniapp.dto.TripTraceDto;
import com.tesla.miniapp.entity.WechatUser;
import com.tesla.miniapp.service.AuthService;
import com.tesla.miniapp.service.TripService;
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
import java.util.Optional;

/**
 * 行程控制器
 */
@Slf4j
@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
@Validated
@Tag(name = "行程接口", description = "行程记录、统计相关接口")
public class TripController {
    
    private final TripService tripService;
    private final AuthService authService;
    
    /**
     * 获取行程记录列表（分页）
     */
    @Operation(summary = "获取行程记录", description = "分页获取车辆的行程记录")
    @GetMapping("/records")
    public ApiResponse<Page<TripRecordDto>> getTripRecords(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TripRecordDto> records = tripService.getTripRecords(carId, pageable);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get trip records for car: {}", carId, e);
            return ApiResponse.error("获取行程记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定时间范围的行程记录
     */
    @Operation(summary = "按时间范围获取行程记录", description = "获取指定时间范围内的行程记录")
    @GetMapping("/records/range")
    public ApiResponse<List<TripRecordDto>> getTripRecordsByDateRange(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "开始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<TripRecordDto> records = tripService.getTripRecordsByDateRange(carId, startDate, endDate);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get trip records by date range for car: {}", carId, e);
            return ApiResponse.error("获取行程记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近的行程记录
     */
    @Operation(summary = "获取最近行程记录", description = "获取车辆最近的行程记录")
    @GetMapping("/recent")
    public ApiResponse<List<TripRecordDto>> getRecentTrips(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "记录数量") @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<TripRecordDto> records = tripService.getRecentTrips(carId, limit);
            return ApiResponse.success(records);
        } catch (Exception e) {
            log.error("Failed to get recent trips for car: {}", carId, e);
            return ApiResponse.error("获取最近行程记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近一条完成的行程
     */
    @Operation(summary = "获取最近一条行程", description = "获取车辆最近完成的一条行程")
    @GetMapping("/last")
    public ApiResponse<TripRecordDto> getLastTrip(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            Optional<TripRecordDto> trip = tripService.getLastCompletedTrip(carId);
            return trip.map(ApiResponse::success)
                    .orElse(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("Failed to get last trip for car: {}", carId, e);
            return ApiResponse.error("获取最近行程失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取行程统计数据
     */
    @Operation(summary = "获取行程统计", description = "获取指定天数内的行程统计数据")
    @GetMapping("/stats")
    public ApiResponse<TripStatsDto> getTripStats(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "统计天数") @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            TripStatsDto stats = tripService.getTripStats(carId, days);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get trip stats for car: {}", carId, e);
            return ApiResponse.error("获取行程统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取行程轨迹
     */
    @Operation(summary = "获取行程轨迹", description = "获取指定行程的GPS轨迹点，用于地图展示")
    @GetMapping("/{tripId}/trace")
    public ApiResponse<TripTraceDto> getTripTrace(
            @Parameter(description = "行程ID") @PathVariable Long tripId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            TripTraceDto trace = tripService.getTripTrace(tripId);
            
            // 验证用户是否有权限访问该车辆
            if (!hasVehicleAccess(user.getId(), trace.getCarId())) {
                return ApiResponse.error(403, "无权限访问该行程");
            }
            
            return ApiResponse.success(trace);
        } catch (Exception e) {
            log.error("Failed to get trip trace for trip: {}", tripId, e);
            return ApiResponse.error("获取行程轨迹失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取实时位置历史
     */
    @Operation(summary = "获取实时位置历史", description = "获取车辆最近N分钟的位置历史，用于实时追踪")
    @GetMapping("/positions/recent")
    public ApiResponse<List<TripTraceDto.TracePoint>> getRecentPositions(
            @Parameter(description = "车辆ID") @RequestParam Long carId,
            @Parameter(description = "分钟数") @RequestParam(defaultValue = "30") int minutes,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<TripTraceDto.TracePoint> points = tripService.getRecentPositions(carId, minutes);
            return ApiResponse.success(points);
        } catch (Exception e) {
            log.error("Failed to get recent positions for car: {}", carId, e);
            return ApiResponse.error("获取位置历史失败: " + e.getMessage());
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
