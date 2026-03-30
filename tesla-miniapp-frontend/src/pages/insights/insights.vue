<template>
  <view class="insights-page">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    
    <!-- 数据概览 -->
    <view class="overview-section">
      <view class="overview-header">
        <text class="overview-title">数据洞察</text>
        <view class="period-selector" @tap="showPeriodPicker">
          <text class="period-text">{{ periodText }}</text>
          <text class="period-icon">▼</text>
        </view>
      </view>
      
      <!-- 核心指标卡片 -->
      <view class="metrics-grid">
        <view class="metric-card primary">
          <view class="metric-icon">💰</view>
          <view class="metric-content">
            <text class="metric-value">¥{{ totalSavings }}</text>
            <text class="metric-label">节省油费</text>
          </view>
        </view>
        <view class="metric-card">
          <view class="metric-icon">🌱</view>
          <view class="metric-content">
            <text class="metric-value">{{ carbonReduction }} kg</text>
            <text class="metric-label">减少碳排放</text>
          </view>
        </view>
        <view class="metric-card">
          <view class="metric-icon">⚡</view>
          <view class="metric-content">
            <text class="metric-value">{{ avgConsumption }}</text>
            <text class="metric-label">平均能耗 (Wh/km)</text>
          </view>
        </view>
        <view class="metric-card">
          <view class="metric-icon">🛣️</view>
          <view class="metric-content">
            <text class="metric-value">{{ totalDistance }}</text>
            <text class="metric-label">总行驶里程 (km)</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 充电分析 -->
    <view class="analysis-section">
      <view class="section-header">
        <text class="section-title">充电分析</text>
      </view>
      
      <view class="analysis-card">
        <view class="analysis-row">
          <view class="analysis-item">
            <text class="analysis-label">充电次数</text>
            <text class="analysis-value">{{ totalCount }}</text>
          </view>
          <view class="analysis-item">
            <text class="analysis-label">总充电量</text>
            <text class="analysis-value">{{ formatNumber(totalEnergy, 1) }} kWh</text>
          </view>
        </view>
        
        <view class="analysis-breakdown">
          <view class="breakdown-header">
            <text class="breakdown-title">充电构成</text>
          </view>
          <view class="breakdown-bars">
            <view class="breakdown-item">
              <view class="breakdown-label">
                <text class="label-icon">🏠</text>
                <text class="label-text">家庭充电</text>
              </view>
              <view class="breakdown-bar-wrap">
                <view 
                  class="breakdown-bar home" 
                  :style="{ width: homeChargingPercent + '%' }"
                ></view>
              </view>
              <text class="breakdown-value">{{ formatNumber(homeEnergy, 1) }} kWh</text>
            </view>
            <view class="breakdown-item">
              <view class="breakdown-label">
                <text class="label-icon">⚡</text>
                <text class="label-text">超级充电</text>
              </view>
              <view class="breakdown-bar-wrap">
                <view 
                  class="breakdown-bar super" 
                  :style="{ width: superChargingPercent + '%' }"
                ></view>
              </view>
              <text class="breakdown-value">{{ formatNumber(superEnergy, 1) }} kWh</text>
            </view>
          </view>
        </view>
        
        <view class="analysis-row">
          <view class="analysis-item">
            <text class="analysis-label">平均每次充电</text>
            <text class="analysis-value">{{ formatNumber(chargingStats.avgEnergyPerSession, 1) }} kWh</text>
          </view>
          <view class="analysis-item">
            <text class="analysis-label">平均电费</text>
            <text class="analysis-value">¥{{ formatNumber(chargingStats.avgCostPerSession, 2) }}/次</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 行程分析 -->
    <view class="analysis-section">
      <view class="section-header">
        <text class="section-title">行程分析</text>
      </view>
      
      <view class="analysis-card">
        <view class="analysis-row">
          <view class="analysis-item">
            <text class="analysis-label">行程次数</text>
            <text class="analysis-value">{{ tripStats.totalCount || 0 }}</text>
          </view>
          <view class="analysis-item">
            <text class="analysis-label">总行驶时长</text>
            <text class="analysis-value">{{ formatTotalDuration(tripStats.totalDuration) }}</text>
          </view>
        </view>
        <view class="analysis-row">
          <view class="analysis-item">
            <text class="analysis-label">平均行程距离</text>
            <text class="analysis-value">{{ formatNumber(tripStats.avgDistance, 1) }} km</text>
          </view>
          <view class="analysis-item">
            <text class="analysis-label">最长单次行程</text>
            <text class="analysis-value">{{ formatNumber(tripStats.maxDistance, 1) }} km</text>
          </view>
        </view>
        <view class="analysis-row">
          <view class="analysis-item">
            <text class="analysis-label">最高速度</text>
            <text class="analysis-value">{{ formatNumber(tripStats.maxSpeed, 0) }} km/h</text>
          </view>
          <view class="analysis-item">
            <text class="analysis-label">总能耗</text>
            <text class="analysis-value">{{ formatNumber(tripStats.totalEnergyUsed, 1) }} kWh</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 效率对比 -->
    <view class="analysis-section">
      <view class="section-header">
        <text class="section-title">vs 燃油车对比</text>
      </view>
      
      <view class="comparison-card">
        <view class="comparison-item">
          <view class="comparison-icon tesla">⚡</view>
          <view class="comparison-info">
            <text class="comparison-label">Tesla 充电</text>
            <text class="comparison-value">¥{{ electricCost }}</text>
          </view>
        </view>
        <view class="comparison-vs">VS</view>
        <view class="comparison-item">
          <view class="comparison-icon gas">⛽</view>
          <view class="comparison-info">
            <text class="comparison-label">同等里程油费</text>
            <text class="comparison-value">¥{{ gasCost }}</text>
          </view>
        </view>
      </view>
      
      <view class="savings-summary">
        <text class="savings-text">相比燃油车，您已节省</text>
        <text class="savings-amount">¥{{ totalSavings }}</text>
        <text class="savings-percent">(节省 {{ savingsPercent }}%)</text>
      </view>
    </view>
    
    <!-- 底部安全区域 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getChargingStatistics } from '@/api/charging'
import { getTripStatistics } from '@/api/trips'
import type { ChargingStatistics } from '@/api/charging'
import type { TripStatistics } from '@/api/trips'

const userStore = useUserStore()

// 状态栏高度
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 44)

// 状态
const selectedPeriod = ref<'month' | 'year' | 'all'>('all')
const chargingStats = ref<ChargingStatistics>({
  totalCount: 0,
  totalEnergy: 0,
  totalCost: 0,
  totalDuration: 0,
  homeChargingCount: 0,
  homeChargingEnergy: 0,
  superchargerCount: 0,
  superchargerEnergy: 0,
  avgEnergyPerSession: 0,
  avgCostPerSession: 0,
  avgCostPerKwh: 0,
  fuelSavings: 0,
  carbonReduction: 0
})
const tripStats = ref<TripStatistics>({
  totalCount: 0,
  totalDistance: 0,
  totalDuration: 0,
  totalEnergyUsed: 0,
  avgDistance: 0,
  avgDuration: 0,
  avgConsumption: 0,
  maxDistance: 0,
  maxSpeed: 0
})

// 计算属性
const periodText = computed(() => {
  const map = { month: '本月', year: '本年', all: '全部' }
  return map[selectedPeriod.value]
})

// 兼容后端字段名
const totalSavings = computed(() => Math.round(chargingStats.value.estimatedSavings || chargingStats.value.fuelSavings || 0))
const carbonReduction = computed(() => (chargingStats.value.carbonReduction || 0).toFixed(1))
const avgConsumption = computed(() => (tripStats.value.avgConsumption || 0).toFixed(0))
const totalDistance = computed(() => Math.round(tripStats.value.totalDistance || 0))

// 获取总充电量（兼容不同字段名）
const totalEnergy = computed(() => chargingStats.value.totalEnergyAdded || chargingStats.value.totalEnergy || 0)
const homeEnergy = computed(() => chargingStats.value.homeEnergyAdded || chargingStats.value.homeChargingEnergy || 0)
const superEnergy = computed(() => chargingStats.value.superEnergyAdded || chargingStats.value.superchargerEnergy || 0)
const totalCount = computed(() => chargingStats.value.totalChargingSessions || chargingStats.value.totalCount || 0)

const homeChargingPercent = computed(() => {
  const total = totalEnergy.value
  const home = homeEnergy.value
  return total > 0 ? (home / total) * 100 : 0
})

const superChargingPercent = computed(() => {
  const total = totalEnergy.value
  const supercharger = superEnergy.value
  return total > 0 ? (supercharger / total) * 100 : 0
})

const electricCost = computed(() => Math.round(chargingStats.value.totalCost || 0))

// 假设油费 0.8元/km（燃油车平均）
const gasCost = computed(() => Math.round((tripStats.value.totalDistance || 0) * 0.8))

const savingsPercent = computed(() => {
  const gas = gasCost.value
  const electric = electricCost.value
  if (gas <= 0) return 0
  return Math.round(((gas - electric) / gas) * 100)
})

// 生命周期
onMounted(async () => {
  await loadData()
})

// 加载数据
async function loadData() {
  if (!userStore.carId) return
  
  try {
    const [charging, trips] = await Promise.all([
      getChargingStatistics(userStore.carId),
      getTripStatistics(userStore.carId)
    ])
    
    chargingStats.value = charging
    tripStats.value = trips
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

// 显示时间段选择器
function showPeriodPicker() {
  uni.showActionSheet({
    itemList: ['本月', '本年', '全部'],
    success: (res) => {
      const periods: Array<'month' | 'year' | 'all'> = ['month', 'year', 'all']
      selectedPeriod.value = periods[res.tapIndex]
      loadData()
    }
  })
}

// 格式化总时长
function formatTotalDuration(seconds?: number): string {
  if (!seconds) return '0h'
  
  const hours = Math.floor(seconds / 3600)
  if (hours > 0) {
    return `${hours}h`
  }
  
  const minutes = Math.floor(seconds / 60)
  return `${minutes}m`
}

// 格式化数字，处理 null/undefined 情况
function formatNumber(value: number | null | undefined, decimals: number): string {
  if (value === null || value === undefined || isNaN(value)) {
    return '0'
  }
  return value.toFixed(decimals)
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.insights-page {
  min-height: 100vh;
  background-color: $color-background;
  padding: $page-padding;
  padding-top: 0;
}

// 状态栏占位
.status-bar {
  width: 100%;
  background-color: $color-background;
}

// 概览部分
.overview-section {
  margin-bottom: $spacing-xl;
}

.overview-header {
  @include flex-between;
  margin-bottom: $spacing-lg;
}

.overview-title {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.period-selector {
  display: flex;
  align-items: center;
  padding: $spacing-xs $spacing-md;
  background-color: $color-surface;
  border-radius: $radius-full;
  box-shadow: $shadow-sm;
}

.period-text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.period-icon {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-left: $spacing-xs;
}

// 指标网格
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: $spacing-md;
}

.metric-card {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
  display: flex;
  align-items: center;
  
  &.primary {
    background: $color-primary-gradient;
    
    .metric-value, .metric-label {
      color: $color-text-inverse;
    }
  }
}

.metric-icon {
  font-size: 40rpx;
  margin-right: $spacing-md;
}

.metric-content {
  display: flex;
  flex-direction: column;
}

.metric-value {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.metric-label {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  margin-top: 4rpx;
}

// 分析部分
.analysis-section {
  margin-bottom: $spacing-xl;
}

.section-header {
  margin-bottom: $spacing-md;
}

.section-title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.analysis-card {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}

.analysis-row {
  display: flex;
  margin-bottom: $spacing-md;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.analysis-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.analysis-label {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-bottom: 4rpx;
}

.analysis-value {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

// 充电构成
.analysis-breakdown {
  padding: $spacing-md 0;
  margin: $spacing-md 0;
  border-top: 2rpx solid $color-border-light;
  border-bottom: 2rpx solid $color-border-light;
}

.breakdown-header {
  margin-bottom: $spacing-md;
}

.breakdown-title {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.breakdown-bars {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.breakdown-item {
  display: flex;
  align-items: center;
}

.breakdown-label {
  display: flex;
  align-items: center;
  width: 160rpx;
  flex-shrink: 0;
}

.label-icon {
  font-size: $font-size-sm;
  margin-right: $spacing-xs;
}

.label-text {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.breakdown-bar-wrap {
  flex: 1;
  height: 16rpx;
  background-color: $color-surface-secondary;
  border-radius: $radius-full;
  margin: 0 $spacing-md;
  overflow: hidden;
}

.breakdown-bar {
  height: 100%;
  border-radius: $radius-full;
  transition: width $transition-slow;
  
  &.home {
    background-color: $color-success;
  }
  
  &.super {
    background-color: $color-error;
  }
}

.breakdown-value {
  width: 120rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  text-align: right;
}

// 对比卡片
.comparison-card {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $spacing-md;
}

.comparison-item {
  display: flex;
  align-items: center;
}

.comparison-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-md;
  @include flex-center;
  font-size: 32rpx;
  margin-right: $spacing-md;
  
  &.tesla {
    background-color: rgba($color-success, 0.1);
  }
  
  &.gas {
    background-color: rgba($color-warning, 0.1);
  }
}

.comparison-info {
  display: flex;
  flex-direction: column;
}

.comparison-label {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-bottom: 4rpx;
}

.comparison-value {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.comparison-vs {
  font-size: $font-size-sm;
  font-weight: $font-weight-bold;
  color: $color-text-tertiary;
}

// 节省总结
.savings-summary {
  background: $color-primary-gradient;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  text-align: center;
}

.savings-text {
  display: block;
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: $spacing-xs;
}

.savings-amount {
  display: block;
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  color: $color-text-inverse;
  margin-bottom: $spacing-xs;
}

.savings-percent {
  display: block;
  font-size: $font-size-sm;
  color: rgba(255, 255, 255, 0.8);
}

// 底部安全区域
.safe-bottom {
  height: 32rpx;
  @include safe-area-bottom;
}
</style>
