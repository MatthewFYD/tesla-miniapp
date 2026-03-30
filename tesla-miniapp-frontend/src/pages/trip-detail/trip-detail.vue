<template>
  <view class="trip-detail-page">
    <!-- 地图区域 -->
    <view class="map-container">
      <map
        id="tripMap"
        class="trip-map"
        :latitude="mapCenter.latitude"
        :longitude="mapCenter.longitude"
        :scale="mapScale"
        :polyline="polyline"
        :markers="markers"
        :show-location="false"
        @regionchange="onRegionChange"
      />
      
      <!-- 地图控制按钮 -->
      <view class="map-controls">
        <view class="control-btn" @tap="zoomIn">
          <text>+</text>
        </view>
        <view class="control-btn" @tap="zoomOut">
          <text>-</text>
        </view>
        <view class="control-btn" @tap="fitRoute">
          <text>⟲</text>
        </view>
      </view>
    </view>
    
    <!-- 行程信息卡片 -->
    <view class="info-card">
      <!-- 基本信息 -->
      <view class="info-header">
        <view class="date-info">
          <text class="date">{{ formatDate(tripInfo.startDate) }}</text>
          <text class="time">{{ formatTimeRange(tripInfo.startDate, tripInfo.endDate) }}</text>
        </view>
        <view class="distance-badge">
          <text class="distance-value">{{ tripInfo.distance?.toFixed(1) || 0 }}</text>
          <text class="distance-unit">km</text>
        </view>
      </view>
      
      <!-- 起止点 -->
      <view class="route-info">
        <view class="route-point">
          <view class="point-icon start">起</view>
          <view class="point-content">
            <text class="point-address">{{ startAddress || '加载中...' }}</text>
            <text class="point-time">{{ formatTime(tripInfo.startDate) }}</text>
          </view>
        </view>
        <view class="route-line-vertical"></view>
        <view class="route-point">
          <view class="point-icon end">终</view>
          <view class="point-content">
            <text class="point-address">{{ endAddress || '加载中...' }}</text>
            <text class="point-time">{{ formatTime(tripInfo.endDate) }}</text>
          </view>
        </view>
      </view>
      
      <!-- 统计数据 -->
      <view class="stats-grid">
        <view class="stat-item">
          <text class="stat-value">{{ formatDuration(tripInfo.durationMin) }}</text>
          <text class="stat-label">时长</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ tripInfo.speedAvg?.toFixed(0) || 0 }}</text>
          <text class="stat-label">均速(km/h)</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ tripInfo.speedMax?.toFixed(0) || 0 }}</text>
          <text class="stat-label">最高(km/h)</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ tripInfo.efficiency?.toFixed(0) || 150 }}</text>
          <text class="stat-label">能耗(Wh/km)</text>
        </view>
      </view>
    </view>
    
    <!-- 返回按钮 -->
    <view class="back-btn" @tap="goBack">
      <text>← 返回</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getTripTrace } from '@/api/trips'
import type { TripRecord, TripTrace, TracePoint } from '@/api/trips'

// 行程信息
const tripInfo = ref<Partial<TripRecord>>({})
const tracePoints = ref<TracePoint[]>([])

// 地址
const startAddress = ref('')
const endAddress = ref('')

// 地图状态
const mapScale = ref(14)
const mapCenter = reactive({
  latitude: 31.2,
  longitude: 121.4
})

// 轨迹线
const polyline = computed(() => {
  if (tracePoints.value.length < 2) return []
  
  return [{
    points: tracePoints.value.map(p => ({
      latitude: p.latitude,
      longitude: p.longitude
    })),
    color: '#3b82f6',
    width: 6,
    arrowLine: true
  }]
})

// 标记点（起点、终点）- 使用自定义label代替iconPath
const markers = computed(() => {
  const result: any[] = []
  
  if (tripInfo.value.startLatitude && tripInfo.value.startLongitude) {
    result.push({
      id: 1,
      latitude: tripInfo.value.startLatitude,
      longitude: tripInfo.value.startLongitude,
      width: 24,
      height: 24,
      label: {
        content: '起',
        color: '#ffffff',
        fontSize: 12,
        bgColor: '#22c55e',
        padding: 6,
        borderRadius: 12,
        textAlign: 'center'
      },
      callout: {
        content: startAddress.value || '起点',
        color: '#333333',
        bgColor: '#ffffff',
        padding: 8,
        borderRadius: 8,
        display: 'BYCLICK',
        fontSize: 12
      }
    })
  }
  
  if (tripInfo.value.endLatitude && tripInfo.value.endLongitude) {
    result.push({
      id: 2,
      latitude: tripInfo.value.endLatitude,
      longitude: tripInfo.value.endLongitude,
      width: 24,
      height: 24,
      label: {
        content: '终',
        color: '#ffffff',
        fontSize: 12,
        bgColor: '#ef4444',
        padding: 6,
        borderRadius: 12,
        textAlign: 'center'
      },
      callout: {
        content: endAddress.value || '终点',
        color: '#333333',
        bgColor: '#ffffff',
        padding: 8,
        borderRadius: 8,
        display: 'BYCLICK',
        fontSize: 12
      }
    })
  }
  
  return result
})

// 页面加载
onLoad((options) => {
  console.log('Trip detail options:', options)
  
  // 从参数解析行程信息
  if (options) {
    // URL参数需要解码
    const startDateStr = options.startDate ? decodeURIComponent(options.startDate) : ''
    const endDateStr = options.endDate ? decodeURIComponent(options.endDate) : ''
    
    tripInfo.value = {
      id: options.id ? Number(options.id) : undefined,
      carId: options.carId ? Number(options.carId) : undefined,
      startDate: startDateStr,
      endDate: endDateStr,
      distance: options.distance ? Number(options.distance) : undefined,
      durationMin: options.durationMin ? Number(options.durationMin) : undefined,
      speedMax: options.speedMax ? Number(options.speedMax) : undefined,
      speedAvg: options.speedAvg ? Number(options.speedAvg) : undefined,
      efficiency: options.efficiency ? Number(options.efficiency) : undefined,
      startLatitude: options.startLat ? Number(options.startLat) : undefined,
      startLongitude: options.startLng ? Number(options.startLng) : undefined,
      endLatitude: options.endLat ? Number(options.endLat) : undefined,
      endLongitude: options.endLng ? Number(options.endLng) : undefined
    }
    
    // 设置地图中心
    if (tripInfo.value.startLatitude && tripInfo.value.startLongitude) {
      mapCenter.latitude = tripInfo.value.startLatitude
      mapCenter.longitude = tripInfo.value.startLongitude
    }
    
    // 加载轨迹和地址
    loadTraceData()
    reverseGeocode()
  }
})

// 加载轨迹数据
async function loadTraceData() {
  const tripId = tripInfo.value.id
  if (!tripId) return
  
  try {
    const trace = await getTripTrace(tripId)
    if (trace && trace.points) {
      tracePoints.value = trace.points
      fitRoute()
    }
  } catch (error) {
    console.error('加载轨迹失败:', error)
    // 如果获取轨迹失败，至少显示起止点
  }
}

// 逆地理编码（经纬度转地址）- 使用 OpenStreetMap Nominatim 开源服务
async function reverseGeocode() {
  const startLat = tripInfo.value.startLatitude
  const startLng = tripInfo.value.startLongitude
  const endLat = tripInfo.value.endLatitude
  const endLng = tripInfo.value.endLongitude
  
  // 辅助函数：调用 Nominatim API
  const geocode = async (lat: number, lng: number): Promise<string> => {
    try {
      const res: any = await uni.request({
        url: 'https://nominatim.openstreetmap.org/reverse',
        data: {
          lat: lat,
          lon: lng,
          format: 'json',
          addressdetails: 1,
          'accept-language': 'zh-CN'
        }
        // 注意：微信小程序不允许设置 User-Agent header
      })
      if (res.data?.display_name) {
        const addr = res.data.display_name as string
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
  
  if (startLat && startLng) {
    startAddress.value = await geocode(startLat, startLng)
  }
  
  if (endLat && endLng) {
    // Nominatim 有请求频率限制，稍微延迟一下
    await new Promise(resolve => setTimeout(resolve, 1000))
    endAddress.value = await geocode(endLat, endLng)
  }
}

// 适配路线到视图
function fitRoute() {
  if (!tripInfo.value.startLatitude || !tripInfo.value.endLatitude) return
  
  // 计算中心点
  mapCenter.latitude = (tripInfo.value.startLatitude + tripInfo.value.endLatitude) / 2
  mapCenter.longitude = (tripInfo.value.startLongitude! + tripInfo.value.endLongitude!) / 2
  
  // 根据距离调整缩放级别
  const distance = tripInfo.value.distance || 10
  if (distance < 5) {
    mapScale.value = 15
  } else if (distance < 20) {
    mapScale.value = 13
  } else if (distance < 50) {
    mapScale.value = 11
  } else {
    mapScale.value = 10
  }
}

// 地图控制
function zoomIn() {
  if (mapScale.value < 20) mapScale.value++
}

function zoomOut() {
  if (mapScale.value > 5) mapScale.value--
}

function onRegionChange(e: any) {
  // 可以在这里处理地图区域变化
}

// 返回
function goBack() {
  uni.navigateBack()
}

// 格式化函数
function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return ''
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

function formatTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return ''
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

function formatTimeRange(startStr?: string, endStr?: string): string {
  if (!startStr || !endStr) return ''
  return `${formatTime(startStr)} - ${formatTime(endStr)}`
}

// 格式化时长（分钟转为时分格式）
function formatDuration(minutes?: number): string {
  if (!minutes || minutes <= 0) return '0分'
  
  const hours = Math.floor(minutes / 60)
  const mins = Math.round(minutes % 60)
  
  if (hours > 0) {
    return mins > 0 ? `${hours}时${mins}分` : `${hours}时`
  }
  return `${mins}分`
}
</script>

<style lang="scss">
.trip-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

// 地图容器
.map-container {
  position: relative;
  width: 100%;
  height: 50vh;
}

.trip-map {
  width: 100%;
  height: 100%;
}

.map-controls {
  position: absolute;
  right: 20rpx;
  bottom: 40rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.control-btn {
  width: 72rpx;
  height: 72rpx;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);
  
  text {
    font-size: 36rpx;
    color: #333;
  }
}

// 信息卡片
.info-card {
  margin: -40rpx 24rpx 24rpx;
  background: white;
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
  padding-bottom: 24rpx;
  border-bottom: 2rpx solid #f0f0f0;
}

.date-info {
  display: flex;
  flex-direction: column;
}

.date {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}

.time {
  font-size: 24rpx;
  color: #666;
  margin-top: 8rpx;
}

.distance-badge {
  display: flex;
  align-items: baseline;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  padding: 16rpx 24rpx;
  border-radius: 16rpx;
}

.distance-value {
  font-size: 40rpx;
  font-weight: 700;
  color: white;
}

.distance-unit {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-left: 8rpx;
}

// 路线信息
.route-info {
  margin-bottom: 32rpx;
}

.route-point {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 0;
}

.point-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
  
  &.start {
    background: #22c55e;
  }
  
  &.end {
    background: #ef4444;
  }
}

.point-content {
  margin-left: 20rpx;
  flex: 1;
}

.point-address {
  font-size: 28rpx;
  color: #1a1a1a;
  line-height: 1.4;
}

.point-time {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.route-line-vertical {
  width: 4rpx;
  height: 32rpx;
  background: #e0e0e0;
  margin-left: 22rpx;
}

// 统计网格
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #f0f0f0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 36rpx;
  font-weight: 600;
  color: #3b82f6;
}

.stat-label {
  font-size: 22rpx;
  color: #999;
  margin-top: 8rpx;
}

// 返回按钮
.back-btn {
  position: fixed;
  top: 80rpx;
  left: 24rpx;
  background: rgba(0, 0, 0, 0.6);
  padding: 16rpx 24rpx;
  border-radius: 32rpx;
  z-index: 100;
  
  text {
    font-size: 28rpx;
    color: white;
  }
}
</style>
