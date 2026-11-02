import { getToken } from '@/store/auth'

// 聊天实时通道：一条长连接当水管，页面只是往管子上挂水龙头（监听回调）。
// 单例的理由很实际——商家开三个标签页也不该握三条手，一条管子谁挂谁收。
// 断线是常态（后端重启、合盖休眠、代理抽风），所以退避到 30s 之后就一直按 30s 试，不放弃：
// 一旦放弃，用户又得自己点刷新，那正是这次要消掉的东西。

const PING_MS = 25000
// 1s → 2s → 4s → 8s → 16s → 30s，之后封顶在 30s
const BACKOFF = [1000, 2000, 4000, 8000, 16000, 30000]
// 鉴权失败时别再 1s 一次地砸门，直接跳到最长的那档
const AUTH_FAILED_CLOSE = 4001
// 去重表不用长命：一屏能看完的历史也就几十条，留 500 条够翻两轮了
const DEDUPE_MAX = 500

let sock = null
let pingTimer = null
let retryTimer = null
let attempt = 0
let stopped = false // 主动 close 过就别再自己爬起来
let status = 'idle'

const messageCbs = new Set()
const statusCbs = new Set()

// 自己发的那条也会从服务端绕回来，多标签页更是各收一份；靠 message.id 只认第一次
const seen = new Set()
const seenQueue = []

function setStatus(next) {
  if (status === next) return
  status = next
  for (const cb of statusCbs) {
    try { cb(next) } catch (e) { /* 一个回调坏了不能拖累其它回调 */ }
  }
}

function wsUrl() {
  // 走 vite 的 /ws 代理：5173 下直连 ws://localhost:18888 会被代理层当成跨域握手拒掉
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  return proto + '://' + location.host + '/ws/chat?token=' + encodeURIComponent(getToken())
}

function remember(id) {
  if (id == null) return true // 没 id 就没法去重，宁可多推一条也别丢消息
  if (seen.has(id)) return false
  seen.add(id)
  seenQueue.push(id)
  if (seenQueue.length > DEDUPE_MAX) seen.delete(seenQueue.shift())
  return true
}

function clearTimers() {
  if (pingTimer) { clearInterval(pingTimer); pingTimer = null }
  if (retryTimer) { clearTimeout(retryTimer); retryTimer = null }
}

function startPing() {
  if (pingTimer) clearInterval(pingTimer)
  // 心跳既是保活也是探测：连接已经被中间设备悄悄掐死时，这条 ping 发不出去，
  // 浏览器随后会触发 close，重连逻辑才接得上
  pingTimer = setInterval(() => {
    if (sock && sock.readyState === WebSocket.OPEN) {
      try { sock.send(JSON.stringify({ type: 'ping' })) } catch (e) { /* 发不出去就等 onclose 收尾 */ }
    }
  }, PING_MS)
}

function armRetry() {
  if (stopped || retryTimer) return
  const wait = BACKOFF[Math.min(attempt, BACKOFF.length - 1)]
  attempt += 1
  setStatus('reconnecting')
  retryTimer = setTimeout(() => {
    retryTimer = null
    open()
  }, wait)
}

function dispatch(raw) {
  let frame
  try { frame = JSON.parse(raw) } catch (e) { return } // 服务端只发 JSON，不是就忽略
  if (!frame || frame.type === 'pong') return
  if (frame.type !== 'message') return // 契约里业务帧只有一种，多余的一律当没看见
  if (!remember(frame.message && frame.message.id)) return
  for (const cb of messageCbs) {
    try { cb(frame) } catch (e) {
      // 页面的回调自己出错不该把整条管子弄断，吞掉继续推下一条
      if (typeof console !== 'undefined') console.error('[chatSocket] 消息回调异常', e)
    }
  }
}

function open() {
  if (stopped) return
  if (sock && (sock.readyState === WebSocket.OPEN || sock.readyState === WebSocket.CONNECTING)) return
  if (!getToken()) { setStatus('unauthorized'); return } // 没登录，握手必被 4001，空转没意义

  let next
  try {
    next = new WebSocket(wsUrl())
  } catch (e) {
    armRetry()
    return
  }
  sock = next

  next.onopen = () => {
    if (sock !== next) return // 已经被更新的连接顶替，旧回调作废
    attempt = 0
    setStatus('open')
    startPing()
  }

  next.onmessage = ev => {
    if (sock !== next) return
    dispatch(ev.data)
  }

  next.onclose = ev => {
    if (sock !== next) return
    sock = null
    clearTimers()
    if (ev && ev.code === AUTH_FAILED_CLOSE) {
      // 令牌过期是唯一不自己爬起来的断线：手里这张票已经废了，原地重连只会一直撞门，
      // 服务端也是这么约定的（ChatWebSocketHandler.java:28「前端看到 4001 就知道该重新登录了」）。
      // 重新登录后页面会重新挂载并再调一次 connect()，那时手里是新令牌，自然接得回来。
      attempt = 0
      setStatus('unauthorized')
      return
    }
    // 其余断线（网络、后端重启、代理掐连接）一律退避重试，不放弃
    armRetry()
  }

  next.onerror = () => {
    // 握手失败的细节浏览器不给，统一交给 onclose 收尾，避免这里和 onclose 各重连一次
  }
}

export function connect() {
  stopped = false
  // 布局和页面都会调它：已经连着（或正在连）就交回原来那条，别为同一个用户再握一只手，
  // 否则同一帧要收两遍，服务端也多养一条会话。
  if (sock && (sock.readyState === WebSocket.CONNECTING || sock.readyState === WebSocket.OPEN)) return
  open()
}

export function close() {
  stopped = true
  clearTimers()
  const cur = sock
  sock = null
  if (cur) {
    try { cur.close() } catch (e) { /* 已经死了就没什么可关的 */ }
  }
  setStatus('closed')
}

// 返回退订函数：组件 onUnmounted 里直接调，省得各页面自己记引用
export function onMessage(cb) {
  messageCbs.add(cb)
  return () => messageCbs.delete(cb)
}

export function onStatus(cb) {
  statusCbs.add(cb)
  return () => statusCbs.delete(cb)
}
