package com.tesla.miniapp.repository;

import com.tesla.miniapp.entity.Update;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 固件更新数据访问层
 */
@Repository
public interface UpdateRepository extends JpaRepository<Update, Long> {
    
    /**
     * 获取车辆当前固件版本（最新的已完成更新）
     */
    @Query("SELECT u FROM Update u WHERE u.carId = :carId AND u.endDate IS NOT NULL ORDER BY u.endDate DESC")
    List<Update> findCompletedUpdates(@Param("carId") Long carId);
    
    /**
     * 获取当前固件版本
     */
    default Optional<String> findCurrentVersion(Long carId) {
        List<Update> updates = findCompletedUpdates(carId);
        if (updates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(updates.get(0).getVersion());
    }
    
    /**
     * 获取车辆所有更新历史
     */
    List<Update> findByCarIdOrderByStartDateDesc(Long carId);
    
    /**
     * 获取正在进行的更新
     */
    @Query("SELECT u FROM Update u WHERE u.carId = :carId AND u.endDate IS NULL ORDER BY u.startDate DESC")
    Optional<Update> findOngoingUpdate(@Param("carId") Long carId);
}
