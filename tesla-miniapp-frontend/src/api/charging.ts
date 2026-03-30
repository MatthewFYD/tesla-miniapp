/**
 * 充电相关 API
 * 对应后端: ChargingController (/charging/*)
 */
import { get } from './request'

// 充电记录 (对应后端 ChargingRecordDto)
export interface ChargingRecord {
  id: number
  carId?: number
  startDate: string
  endDate: string
  address?: string          // 兼容旧字段
  locationName?: string     // 后端实际字段
  location?: string
  chargeEnergyAdded: number
  chargeEnergyUsed?: number
  startBatteryLevel: number
  endBatteryLevel: number
  startIdealRangeKm?: number
  endIdealRangeKm?: number
  startRatedRangeKm?: number
  endRatedRangeKm?: number
  outsideTempAvg?: number
  durationMin: number
  cost?: number
  chargerType?: string
  chargeType?: string       // 后端实际字段
  maxChargerPower?: number
  isSupercharger?: boolean
  isHomeCharging?: boolean
  isCompleted?: boolean
  status?: string
}

// 充电统计 (对应后端 ChargingStatsDto)
export interface ChargingStatistics {
  // 后端字段名
  totalChargingSessions?: number
  totalEnergyAdded?: number
  totalCost?: number
  totalDuration?: number
  homeChargingSessions?: number
  homeEnergyAdded?: number
  superChargingSessions?: number
  superEnergyAdded?: number
  avgEnergyPerSession?: number
  avgCostPerSession?: number
  avgCostPerKwh?: number
  estimatedSavings?: number
  carbonReduction?: number
  // 兼容前端旧字段名
  totalCount?: number
  totalEnergy?: number
  homeChargingCount?: number
  homeChargingEnergy?: number
  superchargerCount?: number
  superchargerEnergy?: number
  fuelSavings?: number
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

/**
 * 获取充电记录列表 (分页)
 * GET /charging/records?carId=&page=&size=&chargerType=
 */
export function getChargingRecords(
  carId: number,
  params?: {
    page?: number
    size?: number
    chargerType?: 'home' | 'supercharger'
  }
): Promise<PageResponse<ChargingRecord>> {
  const queryParams: Record<string, any> = {
    carId,
    page: params?.page ?? 0,
    size: params?.size ?? 20
  }
  
  // 只有指定了类型才传递参数
  if (params?.chargerType) {
    queryParams.chargerType = params.chargerType
  }
  
  return get('/charging/records', queryParams)
}

/**
 * 获取指定时间范围的充电记录
 * GET /charging/records/range?carId=&startDate=&endDate=
 */
export function getChargingRecordsByDateRange(
  carId: number,
  startDate: string,
  endDate: string
): Promise<ChargingRecord[]> {
  return get('/charging/records/range', { carId, startDate, endDate })
}

/**
 * 获取正在进行的充电
 * GET /charging/ongoing?carId=
 */
export function getOngoingChargingSessions(carId: number): Promise<ChargingRecord[]> {
  return get('/charging/ongoing', { carId })
}

/**
 * 获取最近的充电记录
 * GET /charging/recent?carId=&limit=
 */
export function getRecentChargingSessions(
  carId: number,
  limit = 10
): Promise<ChargingRecord[]> {
  return get('/charging/recent', { carId, limit })
}

/**
 * 获取最近一条充电记录
 */
export async function getLastChargingRecord(carId: number): Promise<ChargingRecord | null> {
  const records = await getRecentChargingSessions(carId, 1)
  return records.length > 0 ? records[0] : null
}

/**
 * 获取充电统计
 * GET /charging/stats?carId=&days=
 */
export function getChargingStatistics(
  carId: number,
  days = 30
): Promise<ChargingStatistics> {
  return get('/charging/stats', { carId, days })
}

/**
 * 获取月度充电统计
 * GET /charging/stats/monthly?carId=&year=&month=
 */
export function getMonthlyChargingStats(
  carId: number,
  year: number,
  month: number
): Promise<ChargingStatistics> {
  return get('/charging/stats/monthly', { carId, year, month })
}

/**
 * 获取年度充电统计
 * GET /charging/stats/yearly?carId=&year=
 */
export function getYearlyChargingStats(
  carId: number,
  year?: number
): Promise<ChargingStatistics> {
  return get('/charging/stats/yearly', { carId, year })
}

export default {
  getChargingRecords,
  getChargingRecordsByDateRange,
  getOngoingChargingSessions,
  getRecentChargingSessions,
  getLastChargingRecord,
  getChargingStatistics,
  getMonthlyChargingStats,
  getYearlyChargingStats
}
