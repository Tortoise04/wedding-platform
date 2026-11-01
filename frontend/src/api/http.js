import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from '@/store/auth'

// 统一 axios 实例:baseURL=/api(走 Vite 代理到后端),自动带 token、自动解包 Result。
const http = axios.create({ baseURL: '/api', timeout: 15000 })

// 一页里可能并发好几个请求,被停用时会同时返 403 —— 只提示并踢出一次,别连弹三条。
let kicked = false

http.interceptors.request.use(config => {
  const t = getToken()
  if (t) config.headers.Authorization = 'Bearer ' + t
  return config
})

http.interceptors.response.use(
  response => {
    const res = response.data
    // 兼容两种返回:新接口包一层 Result({code,msg,data}),老接口(如 /api/categories)直接返回裸数据。
    if (res && typeof res === 'object' && 'code' in res && 'msg' in res) {
      if (res.code === 200) {
        kicked = false            // 正常拿到响应,允许下一次停用提示
        return res.data
      }
      ElMessage.error(res.msg || '请求失败')
      const err = new Error(res.msg || '请求失败')
      err.__notified = true          // 已经弹过提示,页面 catch 里不要再弹一次
      return Promise.reject(err)
    }
    return res
  },
  error => {
    const status = error.response?.status
    const msg = error.response?.data?.msg || '网络错误'
    if (status === 401) {
      clearAuth()
      ElMessage.error('登录已过期,请重新登录')
      window.location.hash = '#/login'
    } else if (status === 403 && /店铺已被平台停用/.test(msg)) {
      if (kicked) return Promise.reject(Object.assign(error, { message: msg, __notified: true }))
      kicked = true
      // 店铺被平台停用后,商家手里的旧 token 虽然还在有效期内,后端每个请求都会回库核状态并返 403。
      // 这里按"登录失效"处理:清掉本地凭证并回登录页,否则商家会在每个操作上弹一次同一句提示。
      clearAuth()
      ElMessage.error(msg)
      window.location.hash = '#/login'
    } else {
      ElMessage.error(msg)
    }
    // 把 axios 的英文 "Request failed with status code 400" 换回后端给的中文提示,
    // 并打标记,页面 catch 里走 notifyError 就不会出现"中文 + 英文"两条 toast。
    error.message = msg
    error.__notified = true
    return Promise.reject(error)
  }
)

// 页面 catch 里的统一入口:拦截器已经弹过就不重复弹,否则用本地兜底文案。
export function notifyError(e, fallback = '操作失败,请重试') {
  if (e && e.__notified) return
  ElMessage.error((e && e.message) || fallback)
}

export default http
