<template>
  <view class="charging-detail-page">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    
    <!-- 导航栏 -->
    <view class="nav-bar">
      <view class="nav-back" @tap="goBack">
        <text class="back-icon">‹</text>
      </view>
      <text class="nav-title">充电详情</text>
      <view class="nav-placeholder"></view>
    </view>
    
    <!-- 充电概览卡片 -->
    <view class="overview-card">
      <view class="overview-header">
        <view class="charge-type" :class="isSupercharger ? 'super' : 'home'">
          <text class="type-icon">{{ isSupercharger ? '⚡' : '🏠' }}</text>
          <text class="type-text">{{ isSupercharger ? '超级充电' : '家庭充电' }}</text>
        </view>
        <text class="charge-date">{{ formatDate(record?.startDate) }}</text>
      </view>
      
      <view class="overview-main">
        <view class="energy-display">
          <text class="energy-value">+{{ formatNumber(record?.chargeEnergyAdded, 1) }}</text>
          <text class="energy-unit">kWh</text>
        </view>
        <view class="battery-change">
          <view class="battery-bar">
            <view class="battery-start" :style="{ width: (record?.startBatteryLevel || 0) + '%' }"></view>
            <view class="battery-added" :style="{ 
              left: (record?.startBatteryLevel || 0) + '%',
              width: ((record?.endBatteryLevel || 0) - (record?.startBatteryLevel || 0)) + '%' 
            }"></view>
          </view>
          <view class="battery-labels">
            <text class="label-start">{{ record?.startBatteryLevel || 0 }}%</text>
            <text class="label-end">{{ record?.endBatteryLevel || 100 }}%</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 详细信息 -->
    <view class="detail-section">
      <view class="section-title">充电信息</view>
      <view class="detail-card">
        <view class="detail-row">
          <text class="detail-label">📍 充电地点</text>
          <text class="detail-value">{{ getAddress() }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">🕐 开始时间</text>
          <text class="detail-value">{{ formatDateTime(record?.startDate) }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">🕐 结束时间</text>
          <text class="detail-value">{{ formatDateTime(record?.endDate) }}</text>
        </view>
        <view class="detail-row">
          <text class="detail-label">⏱️ 充电时长</text>
          <text class="detail-value">{{ formatDuration(record?.durationMin) }}</text>
        </view>
        <view class="detail-row" v-if="record?.maxChargerPower">
          <text class="detail-label">⚡ 峰值功率</text>
          <text class="detail-value">{{ formatNumber(record.maxChargerPower, 0) }} kW</text>
        </view>
        <view class="detail-row" v-if="record?.cost">
          <text class="detail-label">💰 充电费用</text>
          <text class="detail-value highlight">¥{{ formatNumber(record.cost, 2) }}</text>
        </view>
        <view class="detail-row" v-if="record?.outsideTempAvg">
          <text class="detail-label">🌡️ 平均温度</text>
          <text class="detail-value">{{ formatNumber(record.outsideTempAvg, 1) }}°C</text>
        </view>
      </view>
    </view>
    
    <!-- 充电曲线（预留） -->
    <view class="chart-section">
      <view class="section-title">充电曲线</view>
      <view class="chart-placeholder">
        <text class="placeholder-icon">📈</text>
        <text class="placeholder-text">充电功率曲线</text>
        <text class="placeholder-desc">暂无详细数据</text>
      </view>
    </view>
    
    <!-- 节省统计 -->
    <view class="savings-section">
      <view class="section-title">本次充电节省</view>
      <view class="savings-card">
        <view class="savings-item">
          <text class="savings-icon">💰</text>
          <view class="savings-info">
            <text class="savings-value">¥{{ calculateSavings() }}</text>
            <text class="savings-label">相比油费节省</text>
          </view>
        </view>
        <view class="savings-item">
          <text class="savings-icon">🌱</text>
          <view class="savings-info">
            <text class="savings-value">{{ calculateCO2Reduction() }} kg</text>
            <text class="savings-label">减少碳排放</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 底部安全区域 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import type { ChargingRecord } from '@/api/charging'

// 状态栏高度
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 44)

// 充电记录
const record = ref<ChargingRecord | null>(null)

// 页面加载
onLoad((options: any) => {
  if (options.record) {
    try {
      record.value = JSON.parse(decodeURIComponent(options.record))
    } catch (e) {
      console.error('解析充电记录失败:', e)
    }
  }
})

// 计算属性
const isSupercharger = computed(() => {
  if (!record.value) return false
  if (record.value.isSupercharger !== undefined) return record.value.isSupercharger
  if (record.value.chargeType) {
    return record.value.chargeType.includes('超级') || record.value.chargeType.toLowerCase().includes('super')
  }
  return false
})

// 返回上一页
function goBack() {
  uni.navigateBack()
}

// 获取地址
function getAddress(): string {
  if (!record.value) return '未知位置'
  return record.value.address || record.value.locationName || record.value.location || '未知位置'
}

// 格式化数字
function formatNumber(value: number | null | undefined, decimals: number): string {
  if (value === null || value === undefined || isNaN(value)) return '0'
  return decimals === 0 ? Math.round(value).toString() : value.toFixed(decimals)
}

// 格式化日期
function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

// 格式化日期时间
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 格式化时长
function formatDuration(minutes?: number): string {
  if (!minutes || minutes <= 0) return '--'
  
  if (minutes > 2880) {
    const hours = Math.floor(minutes / 60)
    if (hours >= 24) {
      const days = Math.floor(hours / 24)
      return `~${days}天`
    }
  }
  
  const hours = Math.floor(minutes / 60)
  const mins = Math.round(minutes % 60)
  
  if (hours > 0) {
    return `${hours}小时${mins}分钟`
  }
  return `${mins}分钟`
}

// 计算节省费用（相比燃油车）
function calculateSavings(): string {
  if (!record.value?.chargeEnergyAdded) return '0'
  // 1kWh约等于3.3km，燃油车8L/100km，油价7.5元/L
  const km = record.value.chargeEnergyAdded * 3.3
  const fuelCost = km * 0.085 * 7.5
  const electricCost = record.value.cost || record.value.chargeEnergyAdded * 0.6
  const savings = Math.max(0, fuelCost - electricCost)
  return savings.toFixed(0)
}

// 计算碳减排
function calculateCO2Reduction(): string {
  if (!record.value?.chargeEnergyAdded) return '0'
  // 每kWh减少约0.7kg CO2
  const reduction = record.value.chargeEnergyAdded * 0.7
  return reduction.toFixed(1)
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.charging-detail-page {
  min-height: 100vh;
  background-color: $color-background;
}

.status-bar {
  width: 100%;
  background-color: $color-primary;
}

// 导航栏
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 $spacing-md;
  background-color: $color-primary;
}

.nav-back {
  width: 60rpx;
  height: 60rpx;
  @include flex-center;
}

.back-icon {
  font-size: 48rpx;
  color: $color-text-inverse;
  font-weight: bold;
}

.nav-title {
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: $color-text-inverse;
}

.nav-placeholder {
  width: 60rpx;
}

// 概览卡片
.overview-card {
  margin: $spacing-lg;
  background-color: $color-surface;
  border-radius: $radius-xl;
  padding: $spacing-xl;
  box-shadow: $shadow-card;
}

.overview-header {
  @include flex-between;
  margin-bottom: $spacing-lg;
}

.charge-type {
  display: flex;
  align-items: center;
  padding: $spacing-xs $spacing-md;
  border-radius: $radius-full;
  
  &.super {
    background-color: rgba($color-error, 0.1);
    .type-text { color: $color-error; }
  }
  
  &.home {
    background-color: rgba($color-success, 0.1);
    .type-text { color: $color-success; }
  }
}

.type-icon {
  font-size: $font-size-md;
  margin-right: $spacing-xs;
}

.type-text {
  font-size: $font-size-sm;
  font-weight: $font-weight-medium;
}

.charge-date {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.overview-main {
  @include flex-col-center;
}

.energy-display {
  display: flex;
  align-items: baseline;
  margin-bottom: $spacing-lg;
}

.energy-value {
  font-size: 72rpx;
  font-weight: $font-weight-bold;
  color: $color-success;
}

.energy-unit {
  font-size: $font-size-lg;
  color: $color-text-secondary;
  margin-left: $spacing-xs;
}

.battery-change {
  width: 100%;
}

.battery-bar {
  width: 100%;
  height: 24rpx;
  background-color: $color-surface-secondary;
  border-radius: $radius-full;
  position: relative;
  overflow: hidden;
}

.battery-start {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background-color: $color-text-tertiary;
  border-radius: $radius-full 0 0 $radius-full;
}

.battery-added {
  position: absolute;
  top: 0;
  height: 100%;
  background-color: $color-success;
  border-radius: 0 $radius-full $radius-full 0;
}

.battery-labels {
  @include flex-between;
  margin-top: $spacing-sm;
}

.label-start, .label-end {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

// 详细信息
.detail-section, .chart-section, .savings-section {
  margin: 0 $spacing-lg $spacing-lg;
}

.section-title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  margin-bottom: $spacing-md;
}

.detail-card {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-md;
  box-shadow: $shadow-card;
}

.detail-row {
  @include flex-between;
  padding: $spacing-md 0;
  border-bottom: 1rpx solid $color-border-light;
  
  &:last-child {
    border-bottom: none;
  }
}

.detail-label {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.detail-value {
  font-size: $font-size-sm;
  color: $color-text-primary;
  font-weight: $font-weight-medium;
  
  &.highlight {
    color: $color-primary;
    font-size: $font-size-md;
  }
}

// 图表占位
.chart-placeholder {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-3xl;
  box-shadow: $shadow-card;
  @include flex-col-center;
}

.placeholder-icon {
  font-size: 64rpx;
  margin-bottom: $spacing-md;
}

.placeholder-text {
  font-size: $font-size-md;
  color: $color-text-secondary;
  margin-bottom: $spacing-xs;
}

.placeholder-desc {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

// 节省统计
.savings-card {
  display: flex;
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}

.savings-item {
  flex: 1;
  display: flex;
  align-items: center;
  
  &:first-child {
    border-right: 1rpx solid $color-border-light;
    padding-right: $spacing-lg;
  }
  
  &:last-child {
    padding-left: $spacing-lg;
  }
}

.savings-icon {
  font-size: 48rpx;
  margin-right: $spacing-md;
}

.savings-info {
  display: flex;
  flex-direction: column;
}

.savings-value {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.savings-label {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-top: 4rpx;
}

.safe-bottom {
  height: 32rpx;
  @include safe-area-bottom;
}
</style>
