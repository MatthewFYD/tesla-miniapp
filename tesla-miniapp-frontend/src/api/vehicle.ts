/**
 * 车辆相关 API
 * 对应后端: VehicleController (/vehicles/*)
 */
import { get, post, put, del } from './request'

// 车辆基本信息 (对应后端 Car entity)
export interface VehicleInfo {
  id: number
  name: string
  model: string
  vin: string
  exteriorColor: string
  wheelType: string
  spoilerType: string
  efficiency: number
}

// 车辆状态 (对应后端 VehicleStatusDto)
export interface VehicleStatus {
  id: number
  name: string
  model: string
  vin: string
  batteryLevel: number
  idealBatteryRange: number
  estBatteryRange: number
  ratedBatteryRange: number
  usableBatteryLevel: number
  isCharging: boolean
  chargerPower: number
  chargeEnergyAdded: number
  chargerVoltage: number
  chargerActualCurrent: number
  timeToFullCharge: number
  chargePortDoorOpen: boolean
  chargePortLatch: string
  insideTemp: number
  outsideTemp: number
  isClimateOn: boolean
  driverTempSetting: number
  passengerTempSetting: number
  latitude: number
  longitude: number
  speed: number
  power: number
  odometer: number
  state: string
  geofence: string
  lastUpdate: string
}

/**
 * 获取用户的车辆列表
 * GET /vehicles/list
 */
export function getVehicleList(): Promise<VehicleInfo[]> {
  return get<VehicleInfo[]>('/vehicles/list')
}

/**
 * 获取用户主要车辆
 * GET /vehicles/primary
 */
export function getPrimaryVehicle(): Promise<VehicleInfo> {
  return get<VehicleInfo>('/vehicles/primary')
}

/**
 * 获取车辆状态
 * GET /vehicles/{carId}/status
 */
export function getVehicleStatus(carId: number): Promise<VehicleStatus> {
  return get<VehicleStatus>(`/vehicles/${carId}/status`)
}

/**
 * 绑定车辆
 * POST /vehicles/{carId}/bind
 */
export function bindVehicle(
  carId: number,
  aliasName?: string,
  isPrimary = false
): Promise<void> {
  const params = new URLSearchParams()
  if (aliasName) params.append('aliasName', aliasName)
  params.append('isPrimary', String(isPrimary))
  return post(`/vehicles/${carId}/bind?${params.toString()}`)
}

/**
 * 解绑车辆
 * DELETE /vehicles/{carId}/unbind
 */
export function unbindVehicle(carId: number): Promise<void> {
  return del(`/vehicles/${carId}/unbind`)
}

/**
 * 设置主要车辆
 * PUT /vehicles/{carId}/primary
 */
export function setPrimaryVehicle(carId: number): Promise<void> {
  return put(`/vehicles/${carId}/primary`)
}

/**
 * 检查车辆充电状态
 * GET /vehicles/{carId}/charging-status
 */
export function getChargingStatus(carId: number): Promise<boolean> {
  return get<boolean>(`/vehicles/${carId}/charging-status`)
}

// 车辆位置信息
export interface VehiclePosition {
  latitude: number
  longitude: number
  speed: number
  heading: number
  timestamp: string
}

/**
 * 获取车辆详细信息 (从状态中获取)
 * GET /vehicles/{carId}/status
 */
export function getVehicleInfo(carId: number): Promise<VehicleInfo> {
  return get<VehicleStatus>(`/vehicles/${carId}/status`).then(status => ({
    id: status.id,
    name: status.name,
    model: status.model,
    vin: status.vin,
    exteriorColor: '',
    wheelType: '',
    spoilerType: '',
    efficiency: 0
  }))
}

/**
 * 获取车辆位置
 * GET /vehicles/{carId}/status (从状态中提取位置)
 */
export function getVehiclePosition(carId: number): Promise<VehiclePosition> {
  return get<VehicleStatus>(`/vehicles/${carId}/status`).then(status => ({
    latitude: status.latitude,
    longitude: status.longitude,
    speed: status.speed || 0,
    heading: 0,
    timestamp: status.lastUpdate
  }))
}

export default {
  getVehicleList,
  getPrimaryVehicle,
  getVehicleStatus,
  getVehicleInfo,
  getVehiclePosition,
  bindVehicle,
  unbindVehicle,
  setPrimaryVehicle,
  getChargingStatus
}
