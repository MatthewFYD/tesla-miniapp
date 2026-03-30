# API 接口对照表

## 前后端接口对应关系

### 认证接口 (AuthController)

| 前端方法 | 后端路径 | HTTP方法 | 说明 |
|---------|---------|---------|------|
| `wechatLogin(code)` | `/auth/wechat/login` | POST | 微信登录 |
| `refreshToken()` | `/auth/refresh` | POST | 刷新令牌 |
| `checkToken()` | `/auth/check` | GET | 检查令牌有效性 |
| `logout()` | `/auth/logout` | POST | 退出登录 |

### 车辆接口 (VehicleController)

| 前端方法 | 后端路径 | HTTP方法 | 说明 |
|---------|---------|---------|------|
| `getVehicleList()` | `/vehicles/list` | GET | 获取车辆列表 |
| `getPrimaryVehicle()` | `/vehicles/primary` | GET | 获取主要车辆 |
| `getVehicleStatus(carId)` | `/vehicles/{carId}/status` | GET | 获取车辆状态 |
| `bindVehicle(carId, aliasName, isPrimary)` | `/vehicles/{carId}/bind` | POST | 绑定车辆 |
| `unbindVehicle(carId)` | `/vehicles/{carId}/unbind` | DELETE | 解绑车辆 |
| `setPrimaryVehicle(carId)` | `/vehicles/{carId}/primary` | PUT | 设置主要车辆 |
| `getChargingStatus(carId)` | `/vehicles/{carId}/charging-status` | GET | 检查充电状态 |

### 仪表盘接口 (DashboardController) - 新增

| 前端方法 | 后端路径 | HTTP方法 | 说明 |
|---------|---------|---------|------|
| `getDashboard(carId)` | `/dashboard/{carId}` | GET | 获取仪表盘全部数据(一站式) |
| `getVehicleState(carId)` | `/dashboard/{carId}/state` | GET | 获取车辆在线/离线状态 |
| `getStateHistory(carId, days)` | `/dashboard/{carId}/states` | GET | 获取状态历史 |
| `getOnlineStats(carId, days)` | `/dashboard/{carId}/online-stats` | GET | 获取在线时间统计 |

### 充电接口 (ChargingController)

| 前端方法 | 后端路径 | HTTP方法 | 说明 |
|---------|---------|---------|------|
| `getChargingRecords(carId, {page, size})` | `/charging/records` | GET | 获取充电记录(分页) |
| `getChargingRecordsByDateRange(carId, start, end)` | `/charging/records/range` | GET | 按时间范围获取充电记录 |
| `getOngoingChargingSessions(carId)` | `/charging/ongoing` | GET | 获取正在进行的充电 |
| `getRecentChargingSessions(carId, limit)` | `/charging/recent` | GET | 获取最近充电记录 |
| `getChargingStatistics(carId, days)` | `/charging/stats` | GET | 获取充电统计 |
| `getMonthlyChargingStats(carId, year, month)` | `/charging/stats/monthly` | GET | 获取月度充电统计 |
| `getYearlyChargingStats(carId, year)` | `/charging/stats/yearly` | GET | 获取年度充电统计 |

### 行程接口 (TripController)

| 前端方法 | 后端路径 | HTTP方法 | 说明 |
|---------|---------|---------|------|
| `getTripRecords(carId, {page, size})` | `/trips/records` | GET | 获取行程记录(分页) |
| `getTripRecordsByDateRange(carId, start, end)` | `/trips/records/range` | GET | 按时间范围获取行程记录 |
| `getRecentTrips(carId, limit)` | `/trips/recent` | GET | 获取最近行程记录 |
| `getLastTrip(carId)` | `/trips/last` | GET | 获取最近一条行程 |
| `getTripStatistics(carId, days)` | `/trips/stats` | GET | 获取行程统计 |
| `getTripTrace(tripId)` | `/trips/{tripId}/trace` | GET | 获取行程轨迹点(新增) |
| `getRecentPositions(carId, minutes)` | `/trips/positions/recent` | GET | 获取实时位置历史(新增) |

## API 响应格式

所有后端响应遵循统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

- `code`: 状态码，200 表示成功
- `message`: 消息
- `data`: 响应数据

## 仪表盘数据结构 (新增)

`GET /dashboard/{carId}` 返回完整车辆数据：

```json
{
  "code": 200,
  "data": {
    "basic": {
      "carId": 1,
      "carName": "Tesla Model 3",
      "model": "model3",
      "vin": "xxxxx",
      "exteriorColor": "白色",
      "wheelType": "19寸"
    },
    "battery": {
      "level": 79,
      "usableLevel": 77,
      "estRangeKm": 366,
      "ratedRangeKm": 334,
      "idealRangeKm": 350,
      "isCharging": false,
      "chargePower": null,
      "chargingState": "Not Charging"
    },
    "temperature": {
      "inside": 24,
      "outside": 20,
      "climateTarget": 21,
      "isClimateOn": false,
      "isFrontDefrosterOn": false,
      "isRearDefrosterOn": false
    },
    "tirePressure": {
      "frontLeft": 2.70,
      "frontRight": 2.70,
      "rearLeft": 2.80,
      "rearRight": 2.60,
      "lastUpdated": "2026-03-28T14:14:24"
    },
    "status": {
      "state": "offline",
      "stateDisplay": "离线",
      "since": "2026-03-28T14:14:24",
      "onlinePercent": 18,
      "offlinePercent": 82,
      "onlineTime": "4:13:37",
      "offlineTime": "19:46:22"
    },
    "odometer": 52347,
    "firmwareVersion": "2025.45.32.1",
    "location": {
      "latitude": 31.2,
      "longitude": 121.5,
      "speed": null,
      "heading": null,
      "elevation": null,
      "geofence": null
    },
    "lastUpdated": "2026-03-28T14:14:24"
  }
}
```

## 行程轨迹数据结构 (新增)

`GET /trips/{tripId}/trace` 返回行程轨迹点：

```json
{
  "code": 200,
  "data": {
    "tripId": 123,
    "carId": 1,
    "startTime": "2026-03-28T10:00:00",
    "endTime": "2026-03-28T11:30:00",
    "distance": 45.5,
    "pointCount": 150,
    "points": [
      {
        "latitude": 31.2,
        "longitude": 121.5,
        "time": "2026-03-28T10:00:00",
        "speed": 60,
        "power": -15,
        "batteryLevel": 80,
        "elevation": null,
        "odometer": 52300
      }
    ]
  }
}
```

## 分页响应格式

分页接口返回 Spring Data Page 对象：

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0
}
```

## 认证

除登录接口外，所有接口需要在请求头中携带 JWT Token：

```
Authorization: Bearer <token>
```

## 后端 API 基础路径

```
http://101.133.153.116:8080/api
```

前端 `src/api/request.ts` 中配置的 `BASE_URL`。
