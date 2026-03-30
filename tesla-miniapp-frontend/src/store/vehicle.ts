/**
 * 车辆状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getVehicleStatus, getVehicleInfo, getVehiclePosition } from '@/api/vehicle'
import type { VehicleStatus, VehicleInfo, VehiclePosition } from '@/api/vehicle'
import { useUserStore } from './user'

export const useVehicleStore = defineStore('vehicle', () => {
  // 状态
  const vehicleInfo = ref<VehicleInfo | null>(null)
  const vehicleStatus = ref<VehicleStatus | null>(null)
  const vehiclePosition = ref<VehiclePosition | null>(null)
  const isLoading = ref(false)
  const lastUpdateTime = ref<Date | null>(null)
  const refreshTimer = ref<number | null>(null)

  // 计算属性
  const batteryLevel = computed(() => vehicleStatus.value?.batteryLevel || 0)
  const idealRange = computed(() => {
    const status = vehicleStatus.value
    // 支持多种字段名：idealRangeKm (后端) / idealBatteryRange (旧)
    return status?.idealRangeKm || status?.idealBatteryRange || 0
  })
  const estRange = computed(() => {
    const status = vehicleStatus.value
    // 支持多种字段名：estRangeKm (后端) / estBatteryRange (旧)
    return status?.estRangeKm || status?.estBatteryRange || 0
  })
  const isCharging = computed(() => vehicleStatus.value?.isCharging || false)
  const chargerPower = computed(() => vehicleStatus.value?.chargerPower || 0)
  const vehicleState = computed(() => vehicleStatus.value?.state || 'unknown')
  const insideTemp = computed(() => vehicleStatus.value?.insideTemp || 0)
  const outsideTemp = computed(() => vehicleStatus.value?.outsideTemp || 0)

  // 格式化里程
  const formattedRange = computed(() => {
    const range = idealRange.value
    return range > 0 ? `${Math.round(range)} km` : '--'
  })

  // 格式化电量百分比
  const formattedBattery = computed(() => {
    return `${batteryLevel.value}%`
  })

  // 状态文案
  const stateText = computed(() => {
    const state = vehicleState.value
    const stateMap: Record<string, string> = {
      'online': '在线',
      'offline': '离线',
      'asleep': '休眠中',
      'charging': '充电中',
      'driving': '行驶中',
      'updating': '更新中',
      'suspended': '已暂停',
      'unknown': '未知'
    }
    return stateMap[state] || '未知'
  })

  // 获取车辆基本信息
  async function fetchVehicleInfo(): Promise<void> {
    const userStore = useUserStore()
    const carId = userStore.carId
    
    if (!carId) return
    
    try {
      vehicleInfo.value = await getVehicleInfo(carId)
    } catch (error) {
      console.error('获取车辆信息失败:', error)
    }
  }

  // 获取车辆状态
  async function fetchVehicleStatus(showLoading = true): Promise<void> {
    const userStore = useUserStore()
    const carId = userStore.carId
    
    if (!carId) return
    
    if (showLoading) {
      isLoading.value = true
    }
    
    try {
      vehicleStatus.value = await getVehicleStatus(carId)
      lastUpdateTime.value = new Date()
    } catch (error) {
      console.error('获取车辆状态失败:', error)
    } finally {
      isLoading.value = false
    }
  }

  // 获取车辆位置
  async function fetchVehiclePosition(): Promise<void> {
    const userStore = useUserStore()
    const carId = userStore.carId
    
    if (!carId) return
    
    try {
      vehiclePosition.value = await getVehiclePosition(carId)
    } catch (error) {
      console.error('获取车辆位置失败:', error)
    }
  }

  // 获取所有数据
  async function fetchAllData(): Promise<void> {
    isLoading.value = true
    
    try {
      await Promise.all([
        fetchVehicleInfo(),
        fetchVehicleStatus(false),
        fetchVehiclePosition()
      ])
    } finally {
      isLoading.value = false
    }
  }

  // 开始自动刷新
  function startAutoRefresh(intervalMs = 30000): void {
    stopAutoRefresh()
    
    refreshTimer.value = setInterval(() => {
      fetchVehicleStatus(false)
    }, intervalMs) as unknown as number
  }

  // 停止自动刷新
  function stopAutoRefresh(): void {
    if (refreshTimer.value) {
      clearInterval(refreshTimer.value)
      refreshTimer.value = null
    }
  }

  // 清除数据
  function clearData(): void {
    vehicleInfo.value = null
    vehicleStatus.value = null
    vehiclePosition.value = null
    lastUpdateTime.value = null
    stopAutoRefresh()
  }

  return {
    // 状态
    vehicleInfo,
    vehicleStatus,
    vehiclePosition,
    isLoading,
    lastUpdateTime,
    // 计算属性
    batteryLevel,
    idealRange,
    estRange,
    isCharging,
    chargerPower,
    vehicleState,
    insideTemp,
    outsideTemp,
    formattedRange,
    formattedBattery,
    stateText,
    // 方法
    fetchVehicleInfo,
    fetchVehicleStatus,
    fetchVehiclePosition,
    fetchAllData,
    startAutoRefresh,
    stopAutoRefresh,
    clearData
  }
})
