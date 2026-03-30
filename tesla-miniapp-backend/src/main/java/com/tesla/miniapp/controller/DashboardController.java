package com.tesla.miniapp.controller;

import com.tesla.miniapp.dto.ApiResponse;
import com.tesla.miniapp.dto.DashboardDto;
import com.tesla.miniapp.entity.State;
import com.tesla.miniapp.entity.WechatUser;
import com.tesla.miniapp.service.AuthService;
import com.tesla.miniapp.service.DashboardService;
import com.tesla.miniapp.service.StateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘控制器
 * 提供一站式车辆数据接口，整合所有Grafana仪表盘数据
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "仪表盘接口", description = "一站式获取车辆所有数据，对应Grafana仪表盘")
public class DashboardController {
    
    private final DashboardService dashboardService;
    private final StateService stateService;
    private final AuthService authService;
    
    /**
     * 获取车辆仪表盘全部数据
     */
    @Operation(summary = "获取仪表盘数据", description = "一站式获取车辆所有状态数据，包括电量、温度、状态、位置等")
    @GetMapping("/{carId}")
    public ApiResponse<DashboardDto> getDashboard(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            // 验证用户是否有权限访问该车辆
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            DashboardDto dashboard = dashboardService.getDashboard(carId);
            return ApiResponse.success(dashboard);
        } catch (Exception e) {
            log.error("Failed to get dashboard for car: {}", carId, e);
            return ApiResponse.error("获取仪表盘数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取车辆当前状态
     */
    @Operation(summary = "获取车辆状态", description = "获取车辆当前在线/离线/休眠等状态")
    @GetMapping("/{carId}/state")
    public ApiResponse<Map<String, Object>> getVehicleState(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            String stateName = stateService.getCurrentStateName(carId);
            String stateDisplay = stateService.getStateDisplayName(stateName);
            var lastChange = stateService.getLastStateChangeTime(carId);
            
            Map<String, Object> result = Map.of(
                    "state", stateName,
                    "stateDisplay", stateDisplay,
                    "since", lastChange.orElse(null)
            );
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Failed to get state for car: {}", carId, e);
            return ApiResponse.error("获取车辆状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取车辆状态历史
     */
    @Operation(summary = "获取状态历史", description = "获取车辆最近N天的状态变化历史")
    @GetMapping("/{carId}/states")
    public ApiResponse<List<State>> getStateHistory(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            List<State> states = stateService.getRecentStates(carId, days);
            return ApiResponse.success(states);
        } catch (Exception e) {
            log.error("Failed to get state history for car: {}", carId, e);
            return ApiResponse.error("获取状态历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取在线时间统计
     */
    @Operation(summary = "获取在线统计", description = "获取车辆在线/离线时间统计")
    @GetMapping("/{carId}/online-stats")
    public ApiResponse<Map<String, Object>> getOnlineStats(
            @Parameter(description = "车辆ID") @PathVariable Long carId,
            @Parameter(description = "天数") @RequestParam(defaultValue = "1") int days,
            Authentication authentication) {
        try {
            WechatUser user = getCurrentUser(authentication);
            
            if (!hasVehicleAccess(user.getId(), carId)) {
                return ApiResponse.error(403, "无权限访问该车辆");
            }
            
            Map<String, Object> stats = stateService.getOnlineStatistics(carId, days);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get online stats for car: {}", carId, e);
            return ApiResponse.error("获取在线统计失败: " + e.getMessage());
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
        // TODO: 实现用户车辆绑定检查
        return true;
    }
}
