/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { wechatLogin, checkToken, getAvailableCars, bindCar, logout } from '@/api/auth'
import type { UserInfo, CarInfo, LoginResponse } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(uni.getStorageSync('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const currentCar = ref<CarInfo | null>(null)
  const availableCars = ref<CarInfo[]>([])
  const isLoading = ref(false)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const hasBoundCar = computed(() => userInfo.value?.hasBoundCar || false)
  const carId = computed(() => currentCar.value?.id || userInfo.value?.carId)

  // 检查登录状态
  async function checkLoginStatus(): Promise<boolean> {
    if (!token.value) {
      return false
    }

    try {
      // 验证 token 有效性
      await checkToken()
      
      // Token 有效，获取车辆列表
      const cars = await getAvailableCars()
      availableCars.value = cars
      
      // 如果已有绑定的车辆，设置为当前车辆
      if (cars.length > 0) {
        currentCar.value = cars[0]
        if (userInfo.value) {
          userInfo.value.hasBoundCar = true
          userInfo.value.carId = cars[0].id
        }
      }
      
      return true
    } catch {
      // Token 无效，清除
      clearUserData()
      return false
    }
  }

  // 微信登录
  async function loginWithWechat(): Promise<boolean> {
    isLoading.value = true
    
    try {
      // 获取微信登录 code
      const loginResult = await new Promise<UniApp.LoginRes>((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: resolve,
          fail: reject
        })
      })

      if (!loginResult.code) {
        throw new Error('获取微信登录凭证失败')
      }

      // 调用后端登录接口
      const result = await wechatLogin(loginResult.code)
      
      console.log('Login result:', JSON.stringify(result))
      
      token.value = result.token
      userInfo.value = result.user
      // token 已在 wechatLogin 中通过 setToken 存储，无需重复
      
      // 获取可绑定车辆列表
      if (!result.user.hasBoundCar) {
        try {
          availableCars.value = await getAvailableCars()
        } catch (e) {
          console.error('获取车辆列表失败:', e)
          // 不影响登录流程
        }
      }
      
      return true
    } catch (error) {
      console.error('微信登录失败:', error)
      uni.showToast({
        title: '登录失败，请重试',
        icon: 'none'
      })
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 绑定车辆
  async function bindCarToUser(carId: number): Promise<boolean> {
    isLoading.value = true
    
    try {
      await bindCar(carId)
      
      // 更新用户信息
      if (userInfo.value) {
        userInfo.value.hasBoundCar = true
        userInfo.value.carId = carId
      }
      
      // 设置当前车辆
      currentCar.value = availableCars.value.find(c => c.id === carId) || null
      
      return true
    } catch (error) {
      console.error('绑定车辆失败:', error)
      uni.showToast({
        title: '绑定失败，请重试',
        icon: 'none'
      })
      return false
    } finally {
      isLoading.value = false
    }
  }

  // 退出登录
  function logoutUser(): void {
    clearUserData()
    logout()
  }

  // 清除用户数据
  function clearUserData(): void {
    token.value = ''
    userInfo.value = null
    currentCar.value = null
    availableCars.value = []
    uni.removeStorageSync('token')
  }

  return {
    // 状态
    token,
    userInfo,
    currentCar,
    availableCars,
    isLoading,
    // 计算属性
    isLoggedIn,
    hasBoundCar,
    carId,
    // 方法
    checkLoginStatus,
    loginWithWechat,
    bindCarToUser,
    logoutUser,
    clearUserData
  }
})
