/**
 * 常用工具函数
 */

/**
 * 格式化日期
 */
export function formatDate(date: Date | string | number, format = 'YYYY-MM-DD'): string {
  const d = new Date(date)
  
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 格式化相对时间
 */
export function formatRelativeTime(date: Date | string | number): string {
  const d = new Date(date)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 7) return `${days} 天前`
  if (days < 30) return `${Math.floor(days / 7)} 周前`
  if (days < 365) return `${Math.floor(days / 30)} 个月前`
  return `${Math.floor(days / 365)} 年前`
}

/**
 * 格式化时长（分钟）
 */
export function formatDurationMinutes(minutes: number): string {
  if (!minutes || minutes < 0) return '--'
  
  const hours = Math.floor(minutes / 60)
  const mins = Math.round(minutes % 60)
  
  if (hours > 0) {
    return `${hours}h${mins}m`
  }
  return `${mins}m`
}

/**
 * 格式化时长（秒）
 */
export function formatDurationSeconds(seconds: number): string {
  if (!seconds || seconds < 0) return '--'
  
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  
  if (hours > 0) {
    return `${hours}h${minutes}m`
  }
  return `${minutes}m`
}

/**
 * 格式化数字（千分位）
 */
export function formatNumber(num: number, decimals = 0): string {
  if (num === null || num === undefined || isNaN(num)) return '--'
  
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals
  })
}

/**
 * 格式化金额
 */
export function formatMoney(amount: number, decimals = 2): string {
  if (amount === null || amount === undefined || isNaN(amount)) return '¥0.00'
  
  return `¥${formatNumber(amount, decimals)}`
}

/**
 * 格式化里程
 */
export function formatDistance(meters: number): string {
  if (!meters || meters < 0) return '--'
  
  const km = meters / 1000
  
  if (km < 1) {
    return `${Math.round(meters)} m`
  }
  return `${km.toFixed(1)} km`
}

/**
 * 格式化电量
 */
export function formatEnergy(kwh: number, decimals = 1): string {
  if (kwh === null || kwh === undefined || isNaN(kwh)) return '--'
  return `${kwh.toFixed(decimals)} kWh`
}

/**
 * 格式化功率
 */
export function formatPower(kw: number, decimals = 0): string {
  if (kw === null || kw === undefined || isNaN(kw)) return '--'
  return `${kw.toFixed(decimals)} kW`
}

/**
 * 格式化温度
 */
export function formatTemperature(temp: number): string {
  if (temp === null || temp === undefined || isNaN(temp)) return '--'
  return `${Math.round(temp)}°C`
}

/**
 * 脱敏 VIN
 */
export function maskVin(vin: string): string {
  if (!vin || vin.length < 8) return vin || ''
  return vin.slice(0, 4) + '****' + vin.slice(-4)
}

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay = 300
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  
  return function (this: ThisParameterType<T>, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay = 300
): (...args: Parameters<T>) => void {
  let lastTime = 0
  
  return function (this: ThisParameterType<T>, ...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}

/**
 * 复制到剪贴板
 */
export function copyToClipboard(text: string): Promise<void> {
  return new Promise((resolve, reject) => {
    uni.setClipboardData({
      data: text,
      success: () => {
        uni.showToast({ title: '已复制', icon: 'success' })
        resolve()
      },
      fail: reject
    })
  })
}

/**
 * 显示确认对话框
 */
export function confirm(options: {
  title?: string
  content: string
  confirmText?: string
  cancelText?: string
}): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title: options.title || '提示',
      content: options.content,
      confirmText: options.confirmText || '确定',
      cancelText: options.cancelText || '取消',
      success: (res) => {
        resolve(res.confirm)
      },
      fail: () => {
        resolve(false)
      }
    })
  })
}

/**
 * 显示 Toast
 */
export function showToast(title: string, icon: 'success' | 'error' | 'none' = 'none'): void {
  uni.showToast({
    title,
    icon,
    duration: 2000
  })
}

/**
 * 显示加载中
 */
export function showLoading(title = '加载中...'): void {
  uni.showLoading({ title, mask: true })
}

/**
 * 隐藏加载中
 */
export function hideLoading(): void {
  uni.hideLoading()
}
