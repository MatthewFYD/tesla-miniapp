<template>
  <view class="login-page">
    <!-- 状态栏占位 -->
    <view class="status-bar" :style="{ height: statusBarHeight + 'px' }"></view>
    
    <!-- 背景装饰 -->
    <view class="login-bg">
      <view class="login-bg__gradient"></view>
    </view>
    
    <!-- 主内容 -->
    <view class="login-content">
      <!-- Logo 和标题 -->
      <view class="login-header">
        <view class="login-logo">
          <text class="login-logo__icon">⚡</text>
        </view>
        <text class="login-title">电车助理</text>
        <text class="login-subtitle">Tesla 数据管理与分析</text>
      </view>
      
      <!-- 功能介绍 -->
      <view class="login-features">
        <view class="feature-item">
          <view class="feature-icon">🔋</view>
          <view class="feature-text">
            <text class="feature-title">实时监控</text>
            <text class="feature-desc">电量、里程、温度实时查看</text>
          </view>
        </view>
        <view class="feature-item">
          <view class="feature-icon">📊</view>
          <view class="feature-text">
            <text class="feature-title">数据分析</text>
            <text class="feature-desc">充电记录、行程统计一目了然</text>
          </view>
        </view>
        <view class="feature-item">
          <view class="feature-icon">💰</view>
          <view class="feature-text">
            <text class="feature-title">省钱计算</text>
            <text class="feature-desc">油费节省、碳减排自动计算</text>
          </view>
        </view>
      </view>
      
      <!-- 登录按钮区域 -->
      <view class="login-actions">
        <button 
          class="btn-wechat"
          :loading="isLoading"
          :disabled="isLoading"
          @tap="handleLogin"
        >
          <text class="btn-wechat__icon" v-if="!isLoading">🔐</text>
          <text class="btn-wechat__text">{{ isLoading ? '登录中...' : '微信授权登录' }}</text>
        </button>
        
        <view class="login-agreement">
          <view 
            class="agreement-checkbox"
            :class="{ 'is-checked': isAgreed }"
            @tap="isAgreed = !isAgreed"
          >
            <text v-if="isAgreed">✓</text>
          </view>
          <text class="agreement-text">
            登录即表示同意
            <text class="agreement-link" @tap="showAgreement('user')">《用户协议》</text>
            和
            <text class="agreement-link" @tap="showAgreement('privacy')">《隐私政策》</text>
          </text>
        </view>
      </view>
      
      <!-- 底部信息 -->
      <view class="login-footer">
        <text class="login-footer__text">数据由 TeslaMate 提供支持</text>
        <text class="login-footer__version">v1.0.0</text>
      </view>
    </view>
    
    <!-- 车辆选择弹窗 -->
    <view class="car-select-modal" v-if="showCarSelect" @tap.self="showCarSelect = false">
      <view class="car-select-content">
        <view class="car-select-header">
          <text class="car-select-title">选择您的车辆</text>
          <text class="car-select-subtitle">请选择要绑定的 Tesla 车辆</text>
        </view>
        
        <view class="car-list">
          <view 
            class="car-item"
            v-for="car in availableCars"
            :key="car.id"
            :class="{ 'is-selected': selectedCarId === car.id }"
            @tap="selectedCarId = car.id"
          >
            <view class="car-item__icon">🚗</view>
            <view class="car-item__info">
              <text class="car-item__name">{{ car.name || car.model }}</text>
              <text class="car-item__vin">{{ maskVin(car.vin) }}</text>
            </view>
            <view class="car-item__check" v-if="selectedCarId === car.id">✓</view>
          </view>
        </view>
        
        <view class="car-select-actions">
          <button 
            class="btn-confirm"
            :disabled="!selectedCarId"
            @tap="handleBindCar"
          >
            确认绑定
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

// 状态栏高度
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 44)

// 状态
const isLoading = ref(false)
const isAgreed = ref(false)
const showCarSelect = ref(false)
const selectedCarId = ref<number | null>(null)

// 计算属性
const availableCars = computed(() => userStore.availableCars)

// 生命周期
onMounted(async () => {
  // 检查是否已登录
  const isLoggedIn = await userStore.checkLoginStatus()
  
  if (isLoggedIn) {
    if (userStore.hasBoundCar) {
      // 已绑定车辆，跳转首页
      navigateToHome()
    } else {
      // 已登录但未绑定车辆，显示车辆选择
      showCarSelect.value = true
    }
  }
})

// 处理登录
async function handleLogin() {
  if (!isAgreed.value) {
    uni.showToast({
      title: '请先同意用户协议和隐私政策',
      icon: 'none'
    })
    return
  }
  
  isLoading.value = true
  
  try {
    const success = await userStore.loginWithWechat()
    
    if (success) {
      if (userStore.hasBoundCar) {
        navigateToHome()
      } else {
        // 显示车辆选择
        showCarSelect.value = true
      }
    }
  } finally {
    isLoading.value = false
  }
}

// 处理车辆绑定
async function handleBindCar() {
  if (!selectedCarId.value) return
  
  isLoading.value = true
  
  try {
    const success = await userStore.bindCarToUser(selectedCarId.value)
    
    if (success) {
      showCarSelect.value = false
      navigateToHome()
    }
  } finally {
    isLoading.value = false
  }
}

// 跳转首页
function navigateToHome() {
  uni.switchTab({ url: '/pages/dashboard/dashboard' })
}

// 脱敏 VIN
function maskVin(vin: string): string {
  if (!vin || vin.length < 8) return vin
  return vin.slice(0, 4) + '****' + vin.slice(-4)
}

// 显示协议
function showAgreement(type: 'user' | 'privacy') {
  uni.showToast({
    title: type === 'user' ? '用户协议' : '隐私政策',
    icon: 'none'
  })
}
</script>

<style lang="scss">
@import '@/styles/variables.scss';
@import '@/styles/mixins.scss';

.login-page {
  min-height: 100vh;
  background-color: $color-background;
  position: relative;
  overflow: hidden;
}

// 状态栏占位
.status-bar {
  width: 100%;
  position: relative;
  z-index: 10;
}

// 背景
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 500rpx;
  
  &__gradient {
    width: 100%;
    height: 100%;
    background: $color-primary-gradient;
    border-radius: 0 0 100rpx 100rpx;
  }
}

// 主内容
.login-content {
  position: relative;
  z-index: 1;
  padding: $page-padding;
  padding-top: 120rpx;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

// Logo 和标题
.login-header {
  @include flex-col-center;
  margin-bottom: $spacing-2xl;
}

.login-logo {
  width: 160rpx;
  height: 160rpx;
  background-color: $color-surface;
  border-radius: 50%;
  @include flex-center;
  box-shadow: $shadow-lg;
  margin-bottom: $spacing-lg;
  
  &__icon {
    font-size: 80rpx;
  }
}

.login-title {
  font-size: $font-size-2xl;
  font-weight: $font-weight-bold;
  color: $color-text-inverse;
  margin-bottom: $spacing-xs;
}

.login-subtitle {
  font-size: $font-size-base;
  color: rgba(255, 255, 255, 0.8);
}

// 功能介绍
.login-features {
  background-color: $color-surface;
  border-radius: $radius-xl;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
  margin-bottom: $spacing-2xl;
}

.feature-item {
  display: flex;
  align-items: center;
  padding: $spacing-md 0;
  
  &:not(:last-child) {
    border-bottom: 2rpx solid $color-border-light;
  }
}

.feature-icon {
  font-size: 48rpx;
  margin-right: $spacing-md;
}

.feature-text {
  display: flex;
  flex-direction: column;
}

.feature-title {
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  margin-bottom: 4rpx;
}

.feature-desc {
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

// 登录按钮区域
.login-actions {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: $spacing-xl;
}

.btn-wechat {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: $button-height-lg;
  background: $color-primary-gradient;
  border-radius: $radius-md;
  border: none;
  margin-bottom: $spacing-lg;
  
  &::after {
    border: none;
  }
  
  &__icon {
    font-size: $font-size-md;
    color: $color-text-inverse;
    margin-right: $spacing-sm;
  }
  
  &__text {
    font-size: $font-size-md;
    font-weight: $font-weight-medium;
    color: $color-text-inverse;
  }
  
  &:active {
    opacity: 0.9;
    transform: scale(0.98);
  }
  
  &[disabled] {
    opacity: 0.6;
  }
}

// 用户协议
.login-agreement {
  display: flex;
  align-items: flex-start;
  justify-content: center;
}

.agreement-checkbox {
  width: 36rpx;
  height: 36rpx;
  border: 2rpx solid $color-border;
  border-radius: $radius-xs;
  margin-right: $spacing-sm;
  @include flex-center;
  flex-shrink: 0;
  margin-top: 4rpx;
  
  &.is-checked {
    background-color: $color-primary;
    border-color: $color-primary;
    
    text {
      font-size: $font-size-xs;
      color: $color-text-inverse;
    }
  }
}

.agreement-text {
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.6;
}

.agreement-link {
  color: $color-primary;
}

// 底部信息
.login-footer {
  @include flex-col-center;
  padding: $spacing-lg 0;
  @include safe-area-bottom;
  
  &__text {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
    margin-bottom: $spacing-xs;
  }
  
  &__version {
    font-size: $font-size-xs;
    color: $color-text-tertiary;
  }
}

// 车辆选择弹窗
.car-select-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: $z-index-modal;
  @include flex-center;
  padding: $page-padding;
}

.car-select-content {
  width: 100%;
  max-width: 640rpx;
  background-color: $color-surface;
  border-radius: $radius-xl;
  padding: $spacing-xl;
  animation: slideUp $transition-normal;
}

.car-select-header {
  text-align: center;
  margin-bottom: $spacing-xl;
}

.car-select-title {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  margin-bottom: $spacing-xs;
}

.car-select-subtitle {
  display: block;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.car-list {
  margin-bottom: $spacing-xl;
}

.car-item {
  display: flex;
  align-items: center;
  padding: $spacing-md;
  border: 2rpx solid $color-border;
  border-radius: $radius-md;
  margin-bottom: $spacing-sm;
  transition: all $transition-fast;
  
  &.is-selected {
    border-color: $color-primary;
    background-color: rgba($color-primary, 0.05);
  }
  
  &__icon {
    font-size: 48rpx;
    margin-right: $spacing-md;
  }
  
  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  
  &__name {
    font-size: $font-size-md;
    font-weight: $font-weight-medium;
    color: $color-text-primary;
    margin-bottom: 4rpx;
  }
  
  &__vin {
    font-size: $font-size-sm;
    color: $color-text-secondary;
  }
  
  &__check {
    width: 40rpx;
    height: 40rpx;
    background-color: $color-primary;
    color: $color-text-inverse;
    border-radius: 50%;
    @include flex-center;
    font-size: $font-size-sm;
  }
}

.car-select-actions {
  .btn-confirm {
    width: 100%;
    height: $button-height-md;
    background: $color-primary-gradient;
    border-radius: $radius-md;
    font-size: $font-size-md;
    font-weight: $font-weight-medium;
    color: $color-text-inverse;
    border: none;
    
    &::after {
      border: none;
    }
    
    &[disabled] {
      opacity: 0.5;
    }
  }
}
</style>
