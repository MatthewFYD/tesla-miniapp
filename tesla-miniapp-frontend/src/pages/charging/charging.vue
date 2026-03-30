<template>
  <view class="charging-page">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    
    <!-- 页面标题 -->
    <view class="page-header">
      <text class="page-title">充电记录</text>
    </view>
    
    <!-- 统计概览 -->
    <view class="stats-overview">
      <view class="stats-card">
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value">{{ formatStatValue(statistics.totalEnergyAdded || statistics.totalEnergy, 1) }}</text>
            <text class="stat-label">总充电量 (kWh)</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ statistics.totalChargingSessions || statistics.totalCount || 0 }}</text>
            <text class="stat-label">充电次数</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ formatStatValue(statistics.avgCostPerKwh || statistics.avgCostPerSession, 2) }}</text>
            <text class="stat-label">均价 (元/kWh)</text>
          </view>
        </view>
      </view>
      
      <!-- 省钱统计 -->
      <view class="savings-card">
        <view class="savings-item">
          <text class="savings-icon">💰</text>
          <view class="savings-info">
            <text class="savings-value">¥{{ formatStatValue(statistics.estimatedSavings || statistics.fuelSavings, 0) }}</text>
            <text class="savings-label">累计节省油费</text>
          </view>
        </view>
        <view class="savings-divider"></view>
        <view class="savings-item">
          <text class="savings-icon">🌱</text>
          <view class="savings-info">
            <text class="savings-value">{{ formatStatValue(statistics.carbonReduction, 1) }} kg</text>
            <text class="savings-label">减少碳排放</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 充电类型筛选 -->
    <view class="filter-tabs">
      <view 
        class="filter-tab"
        :class="{ active: activeFilter === 'all' }"
        @tap="setFilter('all')"
      >
        全部
      </view>
      <view 
        class="filter-tab"
        :class="{ active: activeFilter === 'home' }"
        @tap="setFilter('home')"
      >
        家充
      </view>
      <view 
        class="filter-tab"
        :class="{ active: activeFilter === 'super' }"
        @tap="setFilter('super')"
      >
        超充
      </view>
    </view>
    
    <!-- 充电记录列表 -->
    <view class="records-section">
      <view class="section-header">
        <text class="section-title">充电记录</text>
        <text class="section-count">共 {{ totalRecords }} 条</text>
      </view>
      
      <view class="records-list" v-if="records.length > 0">
        <view 
          class="record-item"
          v-for="record in records"
          :key="record.id"
          @tap="navigateToDetail(record)"
        >
          <view class="record-header">
            <view class="record-type" :class="getChargerTypeClass(record)">
              <text class="type-icon">{{ isSupercharger(record) ? '⚡' : '🏠' }}</text>
              <text class="type-text">{{ isSupercharger(record) ? '超级充电' : '家庭充电' }}</text>
            </view>
            <text class="record-date">{{ formatDate(record.startDate) }}</text>
          </view>
          
          <view class="record-body">
            <view class="record-location">
              <text class="location-icon">📍</text>
              <text class="location-text">{{ getAddress(record) }}</text>
            </view>
            
            <view class="record-stats">
              <view class="record-stat">
                <text class="stat-value">+{{ formatStatValue(record.chargeEnergyAdded, 1) }}</text>
                <text class="stat-unit">kWh</text>
              </view>
              <view class="record-stat">
                <text class="stat-value">{{ record.startBatteryLevel || 0 }}→{{ record.endBatteryLevel || 100 }}</text>
                <text class="stat-unit">%</text>
              </view>
              <view class="record-stat">
                <text class="stat-value">{{ formatDuration(record.durationMin) }}</text>
                <text class="stat-unit">时长</text>
              </view>
              <view class="record-stat" v-if="record.maxChargerPower">
                <text class="stat-value">{{ formatStatValue(record.maxChargerPower, 0) }}</text>
                <text class="stat-unit">kW峰值</text>
              </view>
            </view>
          </view>
          
          <view class="record-footer" v-if="record.cost">
            <text class="cost-label">费用</text>
            <text class="cost-value">¥{{ formatStatValue(record.cost, 2) }}</text>
          </view>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-else-if="!isLoading">
        <text class="empty-icon">🔌</text>
        <text class="empty-text">暂无充电记录</text>
      </view>
      
      <!-- 加载更多 -->
      <view class="load-more" v-if="hasMore" @tap="loadMore">
        <text v-if="isLoadingMore">加载中...</text>
        <text v-else>加载更多</text>
      </view>
    </view>
    
    <!-- 底部安全区域 -->
    <view class="safe-bottom"></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getChargingRecords, getChargingStatistics } from '@/api/charging'
import type { ChargingRecord, ChargingStatistics } from '@/api/charging'

const userStore = useUserStore()

// 状态栏高度
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 44)

// 状态
const isLoading = ref(false)
const isLoadingMore = ref(false)
const activeFilter = ref<'all' | 'home' | 'super'>('all')
const records = ref<ChargingRecord[]>([])
const statistics = ref<ChargingStatistics>({
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
const currentPage = ref(1)
const totalRecords = ref(0)
const pageSize = 10

// 计算属性
const hasMore = computed(() => records.value.length < totalRecords.value)

// 生命周期
onMounted(async () => {
  await loadData()
})

// 加载数据
async function loadData() {
  if (!userStore.carId) return
  
  isLoading.value = true
  
  try {
    await Promise.all([
      loadStatistics(),
      loadRecords()
    ])
  } finally {
    isLoading.value = false
  }
}

// 加载统计数据
async function loadStatistics() {
  const carId = userStore.carId
  if (!carId) return
  
  try {
    statistics.value = await getChargingStatistics(carId)
  } catch (error) {
    console.error('加载充电统计失败:', error)
  }
}

// 加载充电记录
async function loadRecords(append = false) {
  const carId = userStore.carId
  if (!carId) return
  
  try {
    // 将筛选值转换为后端接受的格式
    const chargerType = activeFilter.value === 'all' 
      ? undefined 
      : activeFilter.value === 'home' ? 'home' : 'supercharger'
    
    const result = await getChargingRecords(carId, {
      page: currentPage.value - 1, // 后端分页从0开始
      size: pageSize,
      chargerType
    })
    
    if (append) {
      records.value = [...records.value, ...result.content]
    } else {
      records.value = result.content || []
    }
    
    totalRecords.value = result.totalElements || 0
  } catch (error) {
    console.error('加载充电记录失败:', error)
    if (!append) {
      records.value = []
      totalRecords.value = 0
    }
  }
}

// 设置筛选条件
function setFilter(filter: 'all' | 'home' | 'super') {
  if (activeFilter.value === filter) return
  
  activeFilter.value = filter
  currentPage.value = 1
  loadRecords()
}

// 加载更多
async function loadMore() {
  if (isLoadingMore.value || !hasMore.value) return
  
  isLoadingMore.value = true
  currentPage.value++
  
  try {
    await loadRecords(true)
  } finally {
    isLoadingMore.value = false
  }
}

// 格式化日期
function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// 格式化完整日期时间
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 格式化时长 - 修正：后端返回的 durationMin 可能过大需要处理
function formatDuration(minutes?: number): string {
  if (!minutes || minutes <= 0) return '--'
  
  // 如果时长超过48小时（2880分钟），认为数据异常，可能是统计周期问题
  // 此时根据充电量估算时长（假设平均充电功率7kW）
  if (minutes > 2880) {
    // 数据异常，显示估算值或简化显示
    const hours = Math.floor(minutes / 60)
    if (hours >= 24) {
      const days = Math.floor(hours / 24)
      return `~${days}天`
    }
  }
  
  const hours = Math.floor(minutes / 60)
  const mins = Math.round(minutes % 60)
  
  if (hours > 0) {
    return `${hours}h${mins}m`
  }
  return `${mins}m`
}

// 格式化统计数值
function formatStatValue(value: number | null | undefined, decimals: number): string {
  if (value === null || value === undefined || isNaN(value)) {
    return '0'
  }
  return decimals === 0 ? Math.round(value).toString() : value.toFixed(decimals)
}

// 判断是否为超级充电
function isSupercharger(record: ChargingRecord): boolean {
  // 优先使用明确的字段
  if (record.isSupercharger !== undefined) return record.isSupercharger
  // 根据 chargeType 判断
  if (record.chargeType) {
    return record.chargeType.includes('超级') || record.chargeType.toLowerCase().includes('super')
  }
  if (record.chargerType) {
    return record.chargerType.includes('超级') || record.chargerType.toLowerCase().includes('super')
  }
  return false
}

// 获取地址
function getAddress(record: ChargingRecord): string {
  return record.address || record.locationName || record.location || '未知位置'
}

// 跳转到详情页
function navigateToDetail(record: ChargingRecord) {
  // 跳转到充电详情页
  uni.navigateTo({
    url: `/pages/charging-detail/charging-detail?record=${encodeURIComponent(JSON.stringify(record))}`
  })
}

// 获取充电器类型样式类
function getChargerTypeClass(record: ChargingRecord): string {
  return isSupercharger(record) ? 'supercharger' : 'home'
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.charging-page {
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

// 页面标题
.page-header {
  padding: $spacing-md $page-padding;
  background-color: $color-background;
}

.page-title {
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

// 统计概览
.stats-overview {
  margin-bottom: $spacing-lg;
}

.stats-card {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
  margin-bottom: $spacing-md;
}

.stats-row {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  @include flex-col-center;
}

.stat-value {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-primary;
}

.stat-label {
  font-size: $font-size-xs;
  color: $color-text-secondary;
  margin-top: $spacing-xs;
}

// 省钱统计
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
  color: $color-text-secondary;
  margin-top: 4rpx;
}

.savings-divider {
  width: 2rpx;
  height: 60rpx;
  background-color: $color-border-light;
  margin: 0 $spacing-md;
}

// 筛选标签
.filter-tabs {
  display: flex;
  background-color: $color-surface;
  border-radius: $radius-md;
  padding: $spacing-xs;
  margin-bottom: $spacing-lg;
  box-shadow: $shadow-card;
}

.filter-tab {
  flex: 1;
  text-align: center;
  padding: $spacing-sm 0;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  border-radius: $radius-sm;
  transition: all $transition-fast;
  
  &.active {
    background-color: $color-primary;
    color: $color-text-inverse;
    font-weight: $font-weight-medium;
  }
}

// 记录部分
.records-section {
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

.section-count {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

// 记录列表
.records-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.record-item {
  background-color: $color-surface;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
  @include tap-highlight;
}

.record-header {
  @include flex-between;
  margin-bottom: $spacing-md;
}

.record-type {
  display: flex;
  align-items: center;
  padding: $spacing-xs $spacing-sm;
  border-radius: $radius-full;
  
  &.supercharger {
    background-color: rgba($color-error, 0.1);
    
    .type-text { color: $color-error; }
  }
  
  &.home {
    background-color: rgba($color-success, 0.1);
    
    .type-text { color: $color-success; }
  }
}

.type-icon {
  font-size: $font-size-sm;
  margin-right: 4rpx;
}

.type-text {
  font-size: $font-size-xs;
  font-weight: $font-weight-medium;
}

.record-date {
  font-size: $font-size-sm;
  color: $color-text-tertiary;
}

.record-body {
  margin-bottom: $spacing-md;
}

.record-location {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
}

.location-icon {
  font-size: $font-size-sm;
  margin-right: $spacing-xs;
}

.location-text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  @include text-ellipsis;
}

.record-stats {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-lg;
}

.record-stat {
  display: flex;
  align-items: baseline;
  
  .stat-value {
    font-size: $font-size-md;
    font-weight: $font-weight-semibold;
    color: $color-text-primary;
  }
  
  .stat-unit {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    margin-left: 4rpx;
  }
}

.record-footer {
  @include flex-between;
  padding-top: $spacing-md;
  border-top: 2rpx solid $color-border-light;
}

.cost-label {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.cost-value {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}

// 空状态
.empty-state {
  @include flex-col-center;
  padding: $spacing-3xl $spacing-xl;
  
  .empty-icon {
    font-size: 96rpx;
    margin-bottom: $spacing-lg;
  }
  
  .empty-text {
    font-size: $font-size-base;
    color: $color-text-secondary;
  }
}

// 加载更多
.load-more {
  @include flex-center;
  padding: $spacing-lg;
  font-size: $font-size-sm;
  color: $color-primary;
}

// 底部安全区域
.safe-bottom {
  height: 32rpx;
  @include safe-area-bottom;
}
</style>
