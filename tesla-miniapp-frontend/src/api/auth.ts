/**
 * 认证相关 API
 * 对应后端: AuthController (/auth/*)
 */
import { post, get, setToken, removeToken } from './request'

// 微信登录参数
export interface WechatLoginRequest {
  code: string
}

// 后端返回的原始登录响应
interface BackendLoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userId: number
  nickname: string
  avatarUrl: string
  isNewUser: boolean
  hasBoundCar: boolean
  carId?: number
}

// 转换后的登录响应 (给前端使用)
export interface LoginResponse {
  token: string
  refreshToken: string
  expiresIn: number
  user: UserInfo
}

// 用户信息
export interface UserInfo {
  id: number
  openId: string
  nickname: string
  avatarUrl: string
  hasBoundCar: boolean
  carId?: number
  isNewUser?: boolean
}

// 车辆信息
export interface CarInfo {
  id: number
  name: string
  model: string
  vin: string
  efficiency: number
}

/**
 * 微信登录
 * POST /auth/wechat/login
 */
export async function wechatLogin(code: string): Promise<LoginResponse> {
  const result = await post<BackendLoginResponse>('/auth/wechat/login', { code })
  
  console.log('Backend login response:', JSON.stringify(result))
  
  // 转换后端响应为前端期望格式
  const response: LoginResponse = {
    token: result.accessToken,
    refreshToken: result.refreshToken,
    expiresIn: result.expiresIn,
    user: {
      id: result.userId,
      openId: '',  // 后端不返回 openId 给前端
      nickname: result.nickname || '',
      avatarUrl: result.avatarUrl || '',
      hasBoundCar: result.hasBoundCar || false,
      carId: result.carId,
      isNewUser: result.isNewUser
    }
  }
  
  console.log('Token to store:', response.token)
  
  if (response.token) {
    setToken(response.token)
    // 存储 refreshToken
    if (response.refreshToken) {
      uni.setStorageSync('refreshToken', response.refreshToken)
    }
  }
  return response
}

/**
 * 刷新令牌
 * POST /auth/refresh
 */
export async function refreshToken(): Promise<LoginResponse> {
  const storedRefreshToken = uni.getStorageSync('refreshToken')
  const result = await post<LoginResponse>('/auth/refresh', null, {
    header: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: `refreshToken=${storedRefreshToken}`
  })
  if (result.token) {
    setToken(result.token)
  }
  return result
}

/**
 * 检查令牌有效性
 * GET /auth/check
 */
export function checkToken(): Promise<boolean> {
  return get<boolean>('/auth/check')
}

/**
 * 获取可绑定的车辆列表
 * GET /vehicles/available (从 VehicleController 获取所有 TeslaMate 车辆)
 */
export function getAvailableCars(): Promise<CarInfo[]> {
  return get<CarInfo[]>('/vehicles/available')
}

/**
 * 绑定车辆
 * POST /vehicles/{carId}/bind
 */
export function bindCar(carId: number, isPrimary = true): Promise<void> {
  return post(`/vehicles/${carId}/bind?isPrimary=${isPrimary}`)
}

/**
 * 退出登录
 * POST /auth/logout
 */
export async function logout(): Promise<void> {
  try {
    await post('/auth/logout')
  } catch {
    // 忽略错误
  }
  removeToken()
  uni.removeStorageSync('refreshToken')
  uni.reLaunch({ url: '/pages/login/login' })
}

export default {
  wechatLogin,
  refreshToken,
  checkToken,
  getAvailableCars,
  bindCar,
  logout
}
