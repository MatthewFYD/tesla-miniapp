/**
 * 行程相关 API
 * 对应后端: TripController (/trips/*)
 */
import { get } from './request'

// 行程记录 (对应后端 TripRecordDto)
export interface TripRecord {
  id: number
  carId: number
  startDate: string
  endDate: string
  startAddress: string
  endAddress: string
  startLatitude: number
  startLongitude: number
  endLatitude: number
  endLongitude: number
  distance: number          // km
  durationMin: number
  speedMax: number
  speedAvg: number
  powerMax: number
  powerMin: number
  startBatteryLevel: number
  endBatteryLevel: number
  startIdealRangeKm: number
  endIdealRangeKm: number
  outsideTempAvg: number
  efficiency: number        // Wh/km
}

// 行程统计 (对应后端 TripStatsDto)
export interface TripStatistics {
  totalCount: number
  totalDistance: number     // km
  totalDuration: number     // 秒
  totalEnergyUsed: number   // kWh
  avgDistance: number       // km
  avgDuration: number       // 秒
  avgConsumption: number    // Wh/km
  maxDistance: number       // km
  maxSpeed: number          // km/h
}

// 分页响应
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

// 轨迹点
export interface TracePoint {
  latitude: number
  longitude: number
  time: string
  speed?: number
  power?: number
  batteryLevel?: number
  elevation?: number
  odometer?: number
}

// 行程轨迹
export interface TripTrace {
  tripId: number
  carId: number
  startTime: string
  endTime: string
  distance: number
  pointCount: number
  points: TracePoint[]
}

/**
 * 获取行程记录列表 (分页)
 * GET /trips/records?carId=&page=&size=
 */
export function getTripRecords(
  carId: number,
  params?: {
    page?: number
    size?: number
  }
): Promise<PageResponse<TripRecord>> {
  return get('/trips/records', {
    carId,
    page: params?.page ?? 0,
    size: params?.size ?? 20
  })
}

/**
 * 获取指定时间范围的行程记录
 * GET /trips/records/range?carId=&startDate=&endDate=
 */
export function getTripRecordsByDateRange(
  carId: number,
  startDate: string,
  endDate: string
): Promise<TripRecord[]> {
  return get('/trips/records/range', { carId, startDate, endDate })
}

/**
 * 获取最近的行程记录
 * GET /trips/recent?carId=&limit=
 */
export function getRecentTrips(
  carId: number,
  limit = 10
): Promise<TripRecord[]> {
  return get('/trips/recent', { carId, limit })
}

/**
 * 获取最近一条行程
 * GET /trips/last?carId=
 */
export function getLastTrip(carId: number): Promise<TripRecord | null> {
  return get('/trips/last', { carId })
}

/**
 * 获取行程统计
 * GET /trips/stats?carId=&days=
 */
export function getTripStatistics(
  carId: number,
  days = 30
): Promise<TripStatistics> {
  return get('/trips/stats', { carId, days })
}

/**
 * 获取行程轨迹
 * GET /trips/{tripId}/trace
 */
export function getTripTrace(tripId: number): Promise<TripTrace> {
  return get(`/trips/${tripId}/trace`)
}

/**
 * 获取实时位置历史
 * GET /trips/positions/recent?carId=&minutes=
 */
export function getRecentPositions(
  carId: number,
  minutes = 30
): Promise<TracePoint[]> {
  return get('/trips/positions/recent', { carId, minutes })
}

export default {
  getTripRecords,
  getTripRecordsByDateRange,
  getRecentTrips,
  getLastTrip,
  getTripStatistics,
  getTripTrace,
  getRecentPositions
}
