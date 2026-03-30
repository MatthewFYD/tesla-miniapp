/**
 * 仪表盘 API
 * 一站式获取车辆所有数据
 */
import request from './request'

// ========== 类型定义 ==========

/** 车辆基础信息 */
export interface BasicInfo {
  carId: number
  carName: string
  model: string
  vin: string
  exteriorColor?: string
  wheelType?: string
}

/** 电池信息 */
export interface BatteryInfo {
  level: number
  usableLevel: number
  estRangeKm: number
  ratedRangeKm: number
  idealRangeKm: number
  isCharging: boolean
  chargePower?: number
  chargingState: string
}

/** 温度信息 */
export interface TemperatureInfo {
  inside: number
  outside: number
  climateTarget?: number
  isClimateOn: boolean
  isFrontDefrosterOn?: boolean
  isRearDefrosterOn?: boolean
}

/** 胎压信息 */
export interface TirePressureInfo {
  frontLeft?: number
  frontRight?: number
  rearLeft?: number
  rearRight?: number
  lastUpdated?: string
}

/** 车辆状态信息 */
export interface StatusInfo {
  state: string
  stateDisplay: string
  since?: string
  onlinePercent: number
  offlinePercent: number
  onlineTime: string
  offlineTime: string
}

/** 位置信息 */
export interface LocationInfo {
  latitude?: number
  longitude?: number
  speed?: number
  heading?: number
  elevation?: number
  geofence?: string
}

/** 仪表盘完整数据 */
export interface DashboardData {
  basic: BasicInfo
  battery: BatteryInfo
  temperature: TemperatureInfo
  tirePressure: TirePressureInfo
  status: StatusInfo
  odometer: number
  firmwareVersion?: string
  location: LocationInfo
  lastUpdated: string
}

/** 状态记录 */
export interface StateRecord {
  id: number
  carId: number
  state: string
  startDate: string
  endDate?: string
}

/** 在线统计数据 */
export interface OnlineStats {
  onlinePercent: number
  offlinePercent: number
  onlineSeconds: number
  offlineSeconds: number
  totalSeconds: number
  onlineTime: string
  offlineTime: string
}

// ========== API 方法 ==========

/**
 * 获取仪表盘全部数据
 */
export async function getDashboard(carId: number): Promise<DashboardData> {
  const res = await request.get<{ data: DashboardData }>(`/dashboard/${carId}`)
  return res.data
}

/**
 * 获取车辆状态
 */
export async function getVehicleState(carId: number): Promise<{ state: string; stateDisplay: string; since?: string }> {
  const res = await request.get<{ data: { state: string; stateDisplay: string; since?: string } }>(`/dashboard/${carId}/state`)
  return res.data
}

/**
 * 获取状态历史
 */
export async function getStateHistory(carId: number, days: number = 7): Promise<StateRecord[]> {
  const res = await request.get<{ data: StateRecord[] }>(`/dashboard/${carId}/states`, {
    params: { days }
  })
  return res.data
}

/**
 * 获取在线时间统计
 */
export async function getOnlineStats(carId: number, days: number = 1): Promise<OnlineStats> {
  const res = await request.get<{ data: OnlineStats }>(`/dashboard/${carId}/online-stats`, {
    params: { days }
  })
  return res.data
}
