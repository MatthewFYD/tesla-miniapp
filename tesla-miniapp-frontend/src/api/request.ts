/**
 * API 基础配置和请求封装
 */

// API 基础地址 - 从环境变量读取，支持不同环境切换
// 开发环境: http://localhost:8080/api
// 生产环境: http://101.133.153.116:8080/api
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// 请求超时时间
const TIMEOUT = 30000

// 响应类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 请求选项
interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
  showError?: boolean
}

// 获取存储的 token
function getToken(): string {
  return uni.getStorageSync('token') || ''
}

// 设置 token
export function setToken(token: string): void {
  uni.setStorageSync('token', token)
}

// 移除 token
export function removeToken(): void {
  uni.removeStorageSync('token')
}

// 请求拦截器
function requestInterceptor(options: RequestOptions): UniApp.RequestOptions {
  const token = getToken()
  
  console.log('Request to:', options.url)
  console.log('Token from storage:', token ? `${token.substring(0, 20)}...` : 'empty')
  
  const header: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.header
  }
  
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }
  
  return {
    url: `${BASE_URL}${options.url}`,
    method: options.method || 'GET',
    data: options.data,
    header,
    timeout: TIMEOUT
  }
}

// 响应拦截器
function responseInterceptor<T>(response: UniApp.RequestSuccessCallbackResult): ApiResponse<T> {
  const data = response.data as ApiResponse<T>
  
  if (response.statusCode === 401) {
    // token 过期，跳转登录
    removeToken()
    uni.reLaunch({ url: '/pages/login/login' })
    throw new Error('登录已过期，请重新登录')
  }
  
  if (response.statusCode !== 200) {
    throw new Error(data.message || '请求失败')
  }
  
  if (data.code !== 200 && data.code !== 0) {
    throw new Error(data.message || '请求失败')
  }
  
  return data
}

// 统一请求方法
export function request<T = any>(options: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    // 显示加载提示
    if (options.showLoading !== false) {
      uni.showLoading({ title: '加载中...', mask: true })
    }
    
    const requestOptions = requestInterceptor(options)
    
    uni.request({
      ...requestOptions,
      success: (res) => {
        try {
          const data = responseInterceptor<T>(res)
          resolve(data.data)
        } catch (error) {
          if (options.showError !== false) {
            uni.showToast({
              title: (error as Error).message || '请求失败',
              icon: 'none'
            })
          }
          reject(error)
        }
      },
      fail: (err) => {
        if (options.showError !== false) {
          uni.showToast({
            title: err.errMsg || '网络错误',
            icon: 'none'
          })
        }
        reject(new Error(err.errMsg))
      },
      complete: () => {
        if (options.showLoading !== false) {
          uni.hideLoading()
        }
      }
    })
  })
}

// GET 请求
export function get<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'GET', data, ...options })
}

// POST 请求
export function post<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'POST', data, ...options })
}

// PUT 请求
export function put<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'PUT', data, ...options })
}

// DELETE 请求
export function del<T = any>(url: string, data?: any, options?: Partial<RequestOptions>): Promise<T> {
  return request<T>({ url, method: 'DELETE', data, ...options })
}

export default {
  get,
  post,
  put,
  del,
  setToken,
  removeToken
}
