package com.tesla.miniapp.service;

import com.tesla.miniapp.entity.State;
import com.tesla.miniapp.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 车辆状态服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateService {
    
    private final StateRepository stateRepository;
    
    /**
     * 获取车辆当前状态
     */
    @Transactional(readOnly = true)
    public Optional<State> getCurrentState(Long carId) {
        return stateRepository.findCurrentState(carId);
    }
    
    /**
     * 获取当前状态名称
     */
    @Transactional(readOnly = true)
    public String getCurrentStateName(Long carId) {
        return stateRepository.findCurrentState(carId)
                .map(State::getState)
                .orElse("unknown");
    }
    
    /**
     * 获取最近状态变更时间
     */
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> getLastStateChangeTime(Long carId) {
        return stateRepository.findLastStateChangeTime(carId);
    }
    
    /**
     * 获取最近N天的状态记录
     */
    @Transactional(readOnly = true)
    public List<State> getRecentStates(Long carId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        return stateRepository.findStatesSince(carId, startDate);
    }
    
    /**
     * 获取在线时间统计
     * @return Map包含 onlinePercent, offlinePercent, onlineSeconds, offlineSeconds
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOnlineStatistics(Long carId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Object[] ratio = stateRepository.getOnlineRatio(carId, startDate);
            if (ratio != null && ratio.length >= 2) {
                double onlineSeconds = ((Number) ratio[0]).doubleValue();
                double totalSeconds = ((Number) ratio[1]).doubleValue();
                
                if (totalSeconds > 0) {
                    BigDecimal onlinePercent = BigDecimal.valueOf(onlineSeconds / totalSeconds * 100)
                            .setScale(1, RoundingMode.HALF_UP);
                    BigDecimal offlinePercent = BigDecimal.valueOf(100).subtract(onlinePercent);
                    
                    result.put("onlinePercent", onlinePercent);
                    result.put("offlinePercent", offlinePercent);
                    result.put("onlineSeconds", (long) onlineSeconds);
                    result.put("offlineSeconds", (long) (totalSeconds - onlineSeconds));
                    result.put("totalSeconds", (long) totalSeconds);
                    
                    // 格式化时间
                    result.put("onlineTime", formatDuration((long) onlineSeconds));
                    result.put("offlineTime", formatDuration((long) (totalSeconds - onlineSeconds)));
                }
            }
        } catch (Exception e) {
            log.error("Failed to get online statistics for car: {}", carId, e);
        }
        
        // 默认值
        if (result.isEmpty()) {
            result.put("onlinePercent", BigDecimal.ZERO);
            result.put("offlinePercent", BigDecimal.valueOf(100));
            result.put("onlineSeconds", 0L);
            result.put("offlineSeconds", 0L);
            result.put("onlineTime", "0:00:00");
            result.put("offlineTime", "0:00:00");
        }
        
        return result;
    }
    
    /**
     * 获取各状态时长汇总
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStateDurationSummary(Long carId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        
        Map<String, Long> summary = new HashMap<>();
        
        try {
            List<Object[]> results = stateRepository.getStateDurationSummary(carId, startDate);
            for (Object[] row : results) {
                String state = (String) row[0];
                Long seconds = ((Number) row[1]).longValue();
                summary.put(state, seconds);
            }
        } catch (Exception e) {
            log.error("Failed to get state duration summary for car: {}", carId, e);
        }
        
        return summary;
    }
    
    /**
     * 格式化时长为 HH:MM:SS
     */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * 获取状态显示名称
     */
    public String getStateDisplayName(String state) {
        if (state == null) return "未知";
        
        return switch (state.toLowerCase()) {
            case "online" -> "在线";
            case "offline" -> "离线";
            case "asleep" -> "休眠";
            case "driving" -> "行驶中";
            case "charging" -> "充电中";
            case "suspended" -> "已暂停";
            case "updating" -> "更新中";
            default -> state;
        };
    }
}
