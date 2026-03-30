<template>
  <scroll-view 
    class="dashboard-page" 
    scroll-y 
    refresher-enabled 
    :refresher-triggered="isRefreshing"
    @refresherrefresh="onPullDownRefresh"
  >
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    
    <!-- 头部状态区域 -->
    <view class="dashboard-header">
      <view class="header-bg"></view>
      <view class="header-content" :style="{ paddingRight: capsuleWidth + 'px' }">
        <!-- 顶部信息 -->
        <view class="top-bar">
          <view class="car-info">
            <text class="car-name">{{ vehicleStore.vehicleInfo?.name || 'My Tesla' }}</text>
            <view class="car-status" :class="statusClass">
              <view class="status-dot"></view>
              <text class="status-text">{{ vehicleStore.stateText }}</text>
            </view>
          </view>
        </view>
        
        <!-- 电池主卡片 -->
        <view class="battery-card">
          <view class="battery-visual">
            <!-- 电池图标 -->
            <view class="battery-icon">
              <view class="battery-body">
                <view 
                  class="battery-level" 
                  :style="{ width: batteryLevelPercent + '%' }"
                  :class="batteryLevelClass"
                ></view>
              </view>
              <view class="battery-cap"></view>
            </view>
            
            <!-- 电量数值 -->
            <view class="battery-info">
              <text class="battery-percent">{{ vehicleStore.batteryLevel }}</text>
              <text class="battery-unit">%</text>
            </view>
          </view>
          
          <!-- 续航里程 -->
          <view class="range-info">
            <view class="range-item">
              <text class="range-value">{{ formatRange(vehicleStore.idealRange) }}</text>
              <text class="range-label">理想续航</text>
            </view>
            <view class="range-divider"></view>
            <view class="range-item">
              <text class="range-value">{{ formatRange(vehicleStore.estRange) }}</text>
              <text class="range-label">预估续航</text>
            </view>
          </view>
          
          <!-- 充电状态 -->
          <view class="charging-status" v-if="vehicleStore.isCharging">
            <view class="charging-icon">⚡</view>
            <view class="charging-info">
              <text class="charging-power">{{ vehicleStore.chargerPower }} kW 充电中</text>
              <text class="charging-time" v-if="vehicleStore.vehicleStatus?.timeToFullCharge">
                预计 {{ formatChargeTime(vehicleStore.vehicleStatus.timeToFullCharge) }} 充满
              </text>
            </view>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 快捷信息卡片 -->
    <view class="info-cards">
      <!-- 温度卡片 -->
      <view class="info-card">
        <view class="info-card__header">
          <text class="info-card__icon">🌡️</text>
          <text class="info-card__title">温度</text>
        </view>
        <view class="info-card__body">
          <view class="temp-item">
            <text class="temp-value">{{ vehicleStore.insideTemp || '--' }}°</text>
            <text class="temp-label">车内</text>
          </view>
          <view class="temp-item">
            <text class="temp-value">{{ vehicleStore.outsideTemp || '--' }}°</text>
            <text class="temp-label">车外</text>
          </view>
        </view>
      </view>
      
      <!-- 里程卡片 -->
      <view class="info-card">
        <view class="info-card__header">
          <text class="info-card__icon">🛣️</text>
          <text class="info-card__title">总里程</text>
        </view>
        <view class="info-card__body">
          <text class="odometer-value">{{ formatOdometer(vehicleStore.vehicleStatus?.odometer) }}</text>
          <text class="odometer-unit">km</text>
        </view>
      </view>
    </view>
    
    <!-- 最近记录 -->
    <view class="recent-section">
      <view class="section-header">
        <text class="section-title">最近记录</text>
        <text class="section-more" @tap="navigateToCharging">更多 ›</text>
      </view>
      
      <view class="recent-list">
        <!-- 最近充电 -->
        <view class="recent-item" v-if="lastCharging" @tap="navigateToCharging">
          <view class="recent-item__icon charging">⚡</view>
          <view class="recent-item__content">
            <text class="recent-item__title">充电完成</text>
            <text class="recent-item__desc">{{ lastCharging.address || '未知位置' }}</text>
          </view>
          <view class="recent-item__data">
            <text class="recent-item__value">+{{ lastCharging.chargeEnergyAdded?.toFixed(1) }} kWh</text>
            <text class="recent-item__time">{{ formatTime(lastCharging.endDate) }}</text>
          </view>
        </view>
        
        <!-- 最近行程 -->
        <view class="recent-item" v-if="lastTrip" @tap="navigateToTrips">
          <view class="recent-item__icon trip">🚗</view>
          <view class="recent-item__content">
            <text class="recent-item__title">行程结束</text>
            <text class="recent-item__desc">{{ lastTrip.startAddress || '未知' }} → {{ lastTrip.endAddress || '未知' }}</text>
          </view>
          <view class="recent-item__data">
            <text class="recent-item__value">{{ lastTrip.distance?.toFixed(1) || '0' }} km</text>
            <text class="recent-item__time">{{ formatTime(lastTrip.endDate) }}</text>
          </view>
        </view>
        
        <!-- 空状态 -->
        <view class="empty-tip" v-if="!lastCharging && !lastTrip && !isLoading">
          <text>暂无记录</text>
        </view>
      </view>
    </view>
    
    <!-- 数据概览 -->
    <view class="stats-section">
      <view class="section-header">
        <text class="section-title">本月概览</text>
      </view>
      
      <view class="stats-grid">
        <view class="stat-item">
          <text class="stat-value">{{ monthlyStats.trips }}</text>
          <text class="stat-label">行程次数</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ monthlyStats.distance }}</text>
          <text class="stat-label">行驶里程(km)</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ monthlyStats.charging }}</text>
          <text class="stat-label">充电次数</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ monthlyStats.energy }}</text>
          <text class="stat-label">充电量(kWh)</text>
        </view>
      </view>
    </view>
    
    <!-- 底部安全区域 -->
    <view class="safe-bottom"></view>
  </scroll-view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { useVehicleStore } from '@/store/vehicle'
import { getLastChargingRecord, getMonthlyChargingStats } from '@/api/charging'
import { getLastTrip, getTripStatistics } from '@/api/trips'
import type { ChargingRecord, ChargingStatistics } from '@/api/charging'
import type { TripRecord, TripStatistics } from '@/api/trips'

const userStore = useUserStore()
const vehicleStore = useVehicleStore()

// 状态栏高度
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 44)

// 胶囊按钮宽度（为右侧留出空间避免被遮挡）
const capsuleWidth = ref(0)

// 获取胶囊按钮信息
try {
  const menuButtonInfo = uni.getMenuButtonBoundingClientRect()
  // 胶囊按钮右边距 + 胶囊宽度
  capsuleWidth.value = uni.getSystemInfoSync().windowWidth - menuButtonInfo.left + 10
} catch (e) {
  capsuleWidth.value = 100 // 默认值
}

// 状态
const isLoading = ref(false)
const isRefreshing = ref(false)
const lastCharging = ref<ChargingRecord | null>(null)
const lastTrip = ref<TripRecord | null>(null)
const monthlyStats = ref({
  trips: 0,
  distance: 0,
  charging: 0,
  energy: 0
})

// 计算属性
const batteryLevelPercent = computed(() => Math.min(100, Math.max(0, vehicleStore.batteryLevel)))

const batteryLevelClass = computed(() => {
  const level = vehicleStore.batteryLevel
  if (level <= 20) return 'low'
  if (level <= 50) return 'medium'
  return 'high'
})

const statusClass = computed(() => {
  const state = vehicleStore.vehicleState
  return {
    'is-online': state === 'online',
    'is-charging': state === 'charging',
    'is-driving': state === 'driving',
    'is-asleep': state === 'asleep',
    'is-offline': state === 'offline'
  }
})

// 生命周期
onMounted(async () => {
  await loadData()
})

onShow(() => {
  // 页面显示时刷新数据（无自动刷新）
  loadData()
})

onHide(() => {
  // 页面隐藏时无需处理
})

onUnmounted(() => {
  // 清理资源
})

// 加载数据
async function loadData() {
  if (!userStore.carId) return
  
  isLoading.value = true
  
  try {
    await Promise.all([
      vehicleStore.fetchAllData(),
      loadLastRecords(),
      loadMonthlyStats()
    ])
  } finally {
    isLoading.value = false
  }
}

// 加载最近记录
async function loadLastRecords() {
  const carId = userStore.carId
  if (!carId) return
  
  try {
    const [charging, trip] = await Promise.all([
      getLastChargingRecord(carId),
      getLastTrip(carId)
    ])
    
    lastCharging.value = charging
    lastTrip.value = trip
  } catch (error) {
    console.error('加载最近记录失败:', error)
  }
}

// 加载本月统计数据
async function loadMonthlyStats() {
  const carId = userStore.carId
  if (!carId) return
  
  try {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth() + 1
    
    const [chargingStats, tripStats] = await Promise.all([
      getMonthlyChargingStats(carId, year, month).catch(() => null),
      getTripStatistics(carId, 30).catch(() => null)
    ])
    
    // 兼容后端字段名: totalChargingSessions/totalCount, totalEnergyAdded/totalEnergy
    monthlyStats.value = {
      trips: tripStats?.totalCount ?? 0,
      distance: tripStats?.totalDistance ? Math.round(tripStats.totalDistance) : 0,
      charging: chargingStats?.totalChargingSessions ?? chargingStats?.totalCount ?? 0,
      energy: chargingStats?.totalEnergyAdded ?? chargingStats?.totalEnergy 
        ? Math.round(chargingStats?.totalEnergyAdded ?? chargingStats?.totalEnergy ?? 0) 
        : 0
    }
  } catch (error) {
    console.error('加载本月统计失败:', error)
  }
}

// 刷新数据
async function refreshData() {
  try {
    await loadData()
  } catch {
    uni.showToast({ title: '刷新失败', icon: 'none' })
  }
}

// 下拉刷新
async function onPullDownRefresh() {
  isRefreshing.value = true
  try {
    await loadData()
  } finally {
    isRefreshing.value = false
  }
}

// 格式化里程
function formatRange(range?: number): string {
  if (!range) return '--'
  return Math.round(range).toString()
}

// 格式化总里程
function formatOdometer(odometer?: number): string {
  if (!odometer) return '--'
  return Math.round(odometer).toLocaleString()
}

// 格式化充电时间
function formatChargeTime(hours: number): string {
  if (hours < 1) {
    return `${Math.round(hours * 60)} 分钟`
  }
  return `${hours.toFixed(1)} 小时`
}

// 格式化时间
function formatTime(dateStr?: string): string {
  if (!dateStr) return ''
  
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 7) return `${days} 天前`
  
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 导航
function navigateToCharging() {
  uni.switchTab({ url: '/pages/charging/charging' })
}

function navigateToTrips() {
  uni.switchTab({ url: '/pages/trips/trips' })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.dashboard-page {
  min-height: 100vh;
  background-color: $color-background;
}

// 状态栏占位
.status-bar {
  width: 100%;
  background: $color-primary-gradient;
}

// 头部区域
.dashboard-header {
  position: relative;
  padding-bottom: $spacing-xl;
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400rpx;
  background: $color-primary-gradient;
  border-radius: 0 0 60rpx 60rpx;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: $page-padding;
  padding-top: $spacing-lg;
}

// 顶部栏
.top-bar {
  display: flex;
  align-items: flex-start;
  margin-bottom: $spacing-xl;
}

.car-info {
  display: flex;
  flex-direction: column;
}

.car-name {
  font-size: 40rpx;
  font-weight: 600;
  color: $color-text-inverse;
  margin-bottom: $spacing-xs;
  letter-spacing: 1rpx;
}

.car-status {
  display: flex;
  align-items: center;
  
  .status-dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background-color: rgba(255, 255, 255, 0.4);
    margin-right: $spacing-xs;
  }
  
  .status-text {
    font-size: $font-size-sm;
    color: rgba(255, 255, 255, 0.7);
    font-weight: 400;
  }
  
  &.is-online, &.is-charging, &.is-driving {
    .status-dot { background-color: $color-success; }
  }
  
  &.is-asleep {
    .status-dot { background-color: $color-warning; }
  }
  
  &.is-offline {
    .status-dot { background-color: rgba(255, 255, 255, 0.3); }
  }
}

// 电池卡片
.battery-card {
  background-color: $color-surface;
  border-radius: $radius-xl;
  padding: $spacing-xl;
  box-shadow: $shadow-lg;
}

.battery-visual {
  @include flex-between;
  margin-bottom: $spacing-xl;
}

.battery-icon {
  display: flex;
  align-items: center;
}

.battery-body {
  width: 120rpx;
  height: 48rpx;
  border: 4rpx solid $color-text-primary;
  border-radius: $radius-sm;
  padding: 4rpx;
  overflow: hidden;
  
  .battery-level {
    height: 100%;
    border-radius: 4rpx;
    transition: width $transition-slow, background-color $transition-normal;
    
    &.high { background-color: $color-success; }
    &.medium { background-color: $color-warning; }
    &.low { background-color: $color-error; }
  }
}

.battery-cap {
  width: 8rpx;
  height: 24rpx;
  background-color: $color-text-primary;
  border-radius: 0 4rpx 4rpx 0;
  margin-left: 4rpx;
}

.battery-info {
  display: flex;
  align-items: baseline;
}

.battery-percent {
  font-size: $font-size-3xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.battery-unit {
  font-size: $font-size-lg;
  color: $color-text-secondary;
  margin-left: $spacing-xs;
}

// 续航信息
.range-info {
  @include flex-center;
  padding: $spacing-lg 0;
  border-top: 2rpx solid $color-border-light;
  border-bottom: 2rpx solid $color-border-light;
  margin-bottom: $spacing-lg;
}

.range-item {
  flex: 1;
  @include flex-col-center;
}

.range-value {
  font-size: $font-size-xl;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
  
  &::after {
    content: ' km';
    font-size: $font-size-sm;
    color: $color-text-secondary;
    font-weight: $font-weight-normal;
  }
}

.range-label {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.range-divider {
  width: 2rpx;
  height: 60rpx;
  background-color: $color-border-light;
}

// 充电状态
.charging-status {
  display: flex;
  align-items: center;
  background-color: $color-charging-bg;
  border-radius: $radius-md;
  padding: $spacing-md;
}

.charging-icon {
  font-size: 40rpx;
  margin-right: $spacing-md;
  animation: pulse 1.5s infinite;
}

.charging-info {
  display: flex;
  flex-direction: column;
}

.charging-power {
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  color: $color-success;
}

.charging-time {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-top: 4rpx;
}

// 信息卡片
.info-cards {
  display: flex;
  padding: 0 $page-padding;
  gap: $spacing-md;
  margin-bottom: $spacing-xl;
}

.info-card {
  flex: 1;
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
  
  &__header {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-md;
  }
  
  &__icon {
    font-size: 32rpx;
    margin-right: $spacing-sm;
  }
  
  &__title {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }
  
  &__body {
    display: flex;
    align-items: baseline;
  }
}

.temp-item {
  flex: 1;
  @include flex-col-center;
}

.temp-value {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.temp-label {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-top: 4rpx;
}

.odometer-value {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.odometer-unit {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-left: $spacing-xs;
}

// 最近记录
.recent-section {
  padding: 0 $page-padding;
  margin-bottom: $spacing-xl;
}

.section-header {
  @include flex-between;
  margin-bottom: $spacing-md;
}

.section-title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.section-more {
  font-size: $font-size-sm;
  color: $color-primary;
}

.recent-list {
  background-color: $color-surface;
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.recent-item {
  display: flex;
  align-items: center;
  padding: $spacing-lg;
  border-bottom: 2rpx solid $color-border-light;
  @include tap-highlight;
  
  &:last-child {
    border-bottom: none;
  }
  
  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: $radius-md;
    @include flex-center;
    font-size: 32rpx;
    margin-right: $spacing-md;
    
    &.charging {
      background-color: rgba($color-success, 0.1);
    }
    
    &.trip {
      background-color: rgba($color-primary, 0.1);
    }
  }
  
  &__content {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  
  &__title {
    font-size: $font-size-base;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
    margin-bottom: 4rpx;
  }
  
  &__desc {
    font-size: $font-size-sm;
    color: $color-text-secondary;
    @include text-ellipsis;
  }
  
  &__data {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-left: $spacing-md;
  }
  
  &__value {
    font-size: $font-size-base;
    font-weight: $font-weight-medium;
    color: $color-primary;
    margin-bottom: 4rpx;
  }
  
  &__time {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
  }
}

.empty-tip {
  padding: $spacing-xl;
  text-align: center;
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

// 数据概览
.stats-section {
  padding: 0 $page-padding;
  margin-bottom: $spacing-xl;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg 0;
  box-shadow: $shadow-card;
}

.stat-item {
  @include flex-col-center;
  padding: $spacing-sm;
}

.stat-value {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  margin-bottom: 4rpx;
}

.stat-label {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  text-align: center;
}

// 底部安全区域
.safe-bottom {
  height: 32rpx;
  @include safe-area-bottom;
}
</style>
