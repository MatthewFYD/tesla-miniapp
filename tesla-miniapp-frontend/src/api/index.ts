/**
 * API 模块统一导出
 */
export * from './request'
export * from './auth'
export * from './vehicle'
export * from './charging'
export * from './trips'
export * from './dashboard'

import auth from './auth'
import vehicle from './vehicle'
import charging from './charging'
import trips from './trips'
import * as dashboard from './dashboard'
import { get, post, put, del, setToken, removeToken } from './request'

export default {
  auth,
  vehicle,
  charging,
  trips,
  dashboard,
  get,
  post,
  put,
  del,
  setToken,
  removeToken
}
