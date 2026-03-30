<template>
  <view class="trips-page">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    
    <!-- 页面标题 -->
    <view class="page-header">
      <text class="page-title">行程记录</text>
    </view>
    
    <!-- 统计概览 -->
    <view class="stats-overview">
      <view class="stats-card">
        <view class="stats-row">
          <view class="stat-item">
            <text class="stat-value">{{ statistics.totalDistance?.toFixed(0) || 0 }}</text>
            <text class="stat-label">总里程 (km)</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ statistics.totalCount || 0 }}</text>
            <text class="stat-label">行程次数</text>
          </view>
          <view class="stat-item">
            <text class="stat-value">{{ statistics.avgConsumption?.toFixed(1) || 0 }}</text>
            <text class="stat-label">能耗 (Wh/km)</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 行程记录列表 -->
    <view class="records-section">
      <view class="section-header">
        <text class="section-title">行程记录</text>
        <text class="section-count">共 {{ totalRecords }} 条</text>
      </view>
      
      <view class="records-list" v-if="records.length > 0">
        <view 
          class="record-item"
          v-for="record in records"
          :key="record.id"
          @tap="showRecordDetail(record)"
        >
          <view class="record-header">
            <view class="record-date-info">
              <text class="record-date">{{ formatDate(record.startDate) }}</text>
              <text class="record-time">{{ formatTimeRange(record.startDate, record.endDate) }}</text>
            </view>
            <view class="record-distance">
              <text class="distance-value">{{ record.distance?.toFixed(1) || 0 }}</text>
              <text class="distance-unit">km</text>
            </view>
          </view>
          
          <view class="record-route">
            <view class="route-point start">
              <view class="point-dot"></view>
              <text class="point-text">{{ record.startAddress || '未知起点' }}</text>
            </view>
            <view class="route-line"></view>
            <view class="route-point end">
              <view class="point-dot"></view>
              <text class="point-text">{{ record.endAddress || '未知终点' }}</text>
            </view>
          </view>
          
          <view class="record-stats">
            <view class="record-stat">
              <text class="stat-icon">⏱️</text>
              <text class="stat-value">{{ formatDurationMin(record.durationMin) }}</text>
            </view>
            <view class="record-stat" v-if="record.startBatteryLevel && record.endBatteryLevel">
              <text class="stat-icon">🔋</text>
              <text class="stat-value">{{ record.startBatteryLevel }}→{{ record.endBatteryLevel }}%</text>
            </view>
            <view class="record-stat">
              <text class="stat-icon">⚡</text>
              <text class="stat-value">{{ record.efficiency?.toFixed(0) || 150 }} Wh/km</text>
            </view>
            <view class="record-stat">
              <text class="stat-icon">🚗</text>
              <text class="stat-value">{{ record.speedAvg?.toFixed(0) || 0 }} km/h</text>
            </view>
          </view>
        </view>
      </view>
      
      <!-- 空状态 -->
      <view class="empty-state" v-else-if="!isLoading">
        <text class="empty-icon">🚗</text>
        <text class="empty-text">暂无行程记录</text>
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
import { getTripRecords, getTripStatistics } from '@/api/trips'
import type { TripRecord, TripStatistics } from '@/api/trips'

const userStore = useUserStore()

// 状态栏高度
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 44)

// 状态
const isLoading = ref(false)
const isLoadingMore = ref(false)
const records = ref<TripRecord[]>([])
const statistics = ref<TripStatistics>({
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
const currentPage = ref(0)
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
  if (!userStore.carId) {
    console.log('No carId available')
    return
  }
  
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
    const result = await getTripStatistics(carId)
    console.log('Trip statistics:', result)
    statistics.value = result
  } catch (error) {
    console.error('加载行程统计失败:', error)
  }
}

// 逆地理编码：经纬度转地址（使用 OpenStreetMap Nominatim 开源服务）
async function reverseGeocode(lat: number, lng: number): Promise<string> {
  try {
    const res: any = await uni.request({
      url: 'https://nominatim.openstreetmap.org/reverse',
      data: {
        lat: lat,
        lon: lng,
        format: 'json',
        addressdetails: 1,
        'accept-language': 'zh-CN'  // 中文地址
      }
      // 注意：微信小程序不允许设置 User-Agent header
    })
    if (res.data?.display_name) {
      // 简化地址，取较短的部分
      const addr = res.data.display_name as string
      // 尝试提取更简洁的地址（街道、区域）
      const parts = addr.split(', ')
      if (parts.length > 2) {
        return parts.slice(0, 3).join(', ')
      }
      return addr
    }
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  } catch (e) {
    console.error('Geocode error:', e)
    return `${lat.toFixed(4)}, ${lng.toFixed(4)}`
  }
}

// 加载行程记录
async function loadRecords(append = false) {
  const carId = userStore.carId
  if (!carId) return
  
  try {
    const result = await getTripRecords(carId, {
      page: currentPage.value,
      size: pageSize
    })
    
    console.log('Trip records result:', result)
    
    if (append) {
      records.value = [...records.value, ...result.content]
    } else {
      records.value = result.content
    }
    
    totalRecords.value = result.totalElements
    
    // 为缺少地址的记录进行逆地理编码
    for (const record of records.value) {
      if (!record.startAddress && record.startLatitude && record.startLongitude) {
        reverseGeocode(record.startLatitude, record.startLongitude).then(addr => {
          record.startAddress = addr
        })
      }
      if (!record.endAddress && record.endLatitude && record.endLongitude) {
        reverseGeocode(record.endLatitude, record.endLongitude).then(addr => {
          record.endAddress = addr
        })
      }
    }
  } catch (error) {
    console.error('加载行程记录失败:', error)
  }
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

// 跳转到行程详情（地图轨迹页面）
function showRecordDetail(record: TripRecord) {
  // 构建URL参数（微信小程序不支持URLSearchParams）
  const params = [
    `id=${record.id || 0}`,
    `carId=${record.carId || userStore.carId || 1}`,
    `startDate=${encodeURIComponent(record.startDate || '')}`,
    `endDate=${encodeURIComponent(record.endDate || '')}`,
    `distance=${record.distance || 0}`,
    `durationMin=${record.durationMin || 0}`,
    `speedMax=${record.speedMax || 0}`,
    `speedAvg=${record.speedAvg || 0}`,
    `efficiency=${record.efficiency || 150}`,
    `startLat=${record.startLatitude || ''}`,
    `startLng=${record.startLongitude || ''}`,
    `endLat=${record.endLatitude || ''}`,
    `endLng=${record.endLongitude || ''}`
  ].join('&')
  
  uni.navigateTo({
    url: `/pages/trip-detail/trip-detail?${params}`
  })
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

// 格式化时间范围
function formatTimeRange(startStr?: string, endStr?: string): string {
  if (!startStr || !endStr) return ''
  const start = new Date(startStr)
  const end = new Date(endStr)
  return `${start.getHours().toString().padStart(2, '0')}:${start.getMinutes().toString().padStart(2, '0')} - ${end.getHours().toString().padStart(2, '0')}:${end.getMinutes().toString().padStart(2, '0')}`
}

// 格式化时长 (分钟)
function formatDurationMin(minutes?: number): string {
  if (!minutes) return '--'
  
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  
  if (hours > 0) {
    return `${hours}h${mins}m`
  }
  return `${mins}m`
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.trips-page {
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

.record-date-info {
  display: flex;
  flex-direction: column;
}

.record-date {
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.record-time {
  font-size: $font-size-xs;
  color: $color-text-tertiary;
  margin-top: 4rpx;
}

.record-distance {
  display: flex;
  align-items: baseline;
}

.distance-value {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-primary;
}

.distance-unit {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  margin-left: $spacing-xs;
}

// 路线显示
.record-route {
  margin-bottom: $spacing-md;
  padding-left: $spacing-sm;
}

.route-point {
  display: flex;
  align-items: center;
  
  .point-dot {
    width: 16rpx;
    height: 16rpx;
    border-radius: 50%;
    margin-right: $spacing-md;
  }
  
  .point-text {
    font-size: $font-size-sm;
    color: $color-text-secondary;
    @include text-ellipsis;
    flex: 1;
  }
  
  &.start .point-dot {
    background-color: $color-success;
  }
  
  &.end .point-dot {
    background-color: $color-error;
  }
}

.route-line {
  width: 2rpx;
  height: 24rpx;
  background-color: $color-border;
  margin-left: 7rpx;
  margin: $spacing-xs 0 $spacing-xs 7rpx;
}

// 行程统计
.record-stats {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-md;
  padding-top: $spacing-md;
  border-top: 2rpx solid $color-border-light;
}

.record-stat {
  display: flex;
  align-items: center;
  
  .stat-icon {
    font-size: $font-size-sm;
    margin-right: $spacing-xs;
  }
  
  .stat-value {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }
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
