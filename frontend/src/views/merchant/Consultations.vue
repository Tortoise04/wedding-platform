<template>
  <div class="mch-page">
    <header class="mch-head">
      <p class="mch-sub">
        共 <em>{{ sessions.length }}</em> 个会话 · 未读 <em>{{ unread }}</em> 条
      </p>
    </header>

    <section v-if="err" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">会话没有加载出来</p>
        <p class="mch-state-desc">{{ err }}</p>
        <el-button size="small" @click="boot">重试</el-button>
      </div>
    </section>

    <section v-else-if="loading" class="mch-panel">
      <div class="chat chat-sk">
        <div class="sk-col">
          <span class="sk-line" v-for="i in 4" :key="i" />
        </div>
        <div class="sk-col">
          <span class="sk-line" v-for="i in 6" :key="i" />
        </div>
      </div>
    </section>

    <section v-else-if="!sessions.length" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">◇</p>
        <p class="mch-state-title">还没有买家咨询</p>
        <p class="mch-state-desc">买家在服务详情页发起咨询后，会在这里生成会话，你可以直接回复。</p>
        <router-link to="/merchant/services"><el-button size="small">查看我的服务</el-button></router-link>
      </div>
    </section>

    <section v-else class="mch-panel chat-panel">
      <div class="chat">
        <aside class="sessions">
          <button
            v-for="s in sessions"
            :key="s.id"
            type="button"
            class="session"
            :class="{ active: s.id === curId }"
            @click="openSession(s)"
          >
            <span class="s-name">
              {{ customerName(s) }}
              <span v-if="s.merchantUnread" class="s-badge">{{ s.merchantUnread }}</span>
            </span>
            <span class="s-last">{{ s.lastMessage || '暂无消息' }}</span>
            <span class="s-time">{{ fmt(s.lastTime) }}</span>
          </button>
        </aside>

        <div class="messages">
          <template v-if="curId">
            <!-- 商家端原先没有会话头，加了它才有地方钉「这单 / 这个商品」的来源卡 -->
            <header class="chat-head">
              <div class="chat-who">
                <p class="chat-name">{{ customerName(curChat) }}</p>
                <p class="chat-meta">开始于 {{ fmt(curChat && curChat.createdAt) }}</p>
              </div>
              <router-link v-if="source" :to="source.to" class="src">
                <div class="src-cover">
                  <img
                    v-if="source.cover && coverBroken !== source.cover"
                    :src="source.cover"
                    alt=""
                    @error="coverBroken = source.cover"
                  />
                  <span v-else class="src-cover-ph" aria-hidden="true">◇</span>
                </div>
                <div class="src-main">
                  <p class="src-tag">{{ source.kind === 'order' ? '咨询订单' : '咨询商品' }}</p>
                  <p class="src-name">{{ source.name }}</p>
                  <p v-if="source.kind === 'order'" class="src-meta">
                    订单号 {{ source.orderNo || '—' }} · {{ source.status || '—' }}
                  </p>
                </div>
                <div class="src-side">
                  <p class="src-amount">{{ source.amount }}</p>
                  <span class="src-go">查看</span>
                </div>
              </router-link>
            </header>
            <div class="msg-list" ref="listEl">
              <div v-for="m in messages" :key="m.id" class="msg" :class="m.senderRole === 'MERCHANT' ? 'mine' : 'other'">
                <div class="bubble">{{ m.content }}</div>
                <div class="time">{{ fmt(m.createdAt) }}</div>
              </div>
              <div v-if="!messages.length" class="msg-empty">还没有消息，发送第一条回复吧。</div>
            </div>
            <div class="input-bar">
              <el-input v-model="draft" placeholder="输入回复" @keyup.enter="send" />
              <el-button type="primary" @click="send" :loading="sending">发送</el-button>
            </div>
          </template>
          <div v-else class="mch-state">
            <p class="mch-state-ico">◇</p>
            <p class="mch-state-title">选择一个会话</p>
            <p class="mch-state-desc">从左侧点开买家的咨询，右侧会显示完整对话。</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import http from '@/api/http'
import { connect, onMessage } from '@/api/chatSocket'

const sessions = ref([])
const curId = ref(null)
const messages = ref([])
const draft = ref('')
const listEl = ref(null)
const loading = ref(false)
const err = ref('')

const unread = computed(() => sessions.value.reduce((n, s) => n + (s.merchantUnread || 0), 0))
const curChat = computed(() => sessions.value.find(s => s.id === curId.value) || null)
// 封面加载失败记地址而不是 boolean：换会话时天然复位，不必再写清理
const coverBroken = ref('')

// 来源卡：买家到底是在问哪一单 / 哪个商品。字段由后端会话对象直接带来，不再回源查订单，
// 所以金额取的是建会话那一刻的值——够用，也省得为一张卡多打两次接口。
const source = computed(() => {
  const s = curChat.value
  if (!s) return null
  if (s.orderId) {
    return {
      kind: 'order',
      // 商家端订单页不认 ?open，直接落到订单列表
      to: '/merchant/orders',
      name: s.sourceServiceName || '未命名服务',
      orderNo: s.sourceOrderNo,
      status: s.sourceOrderStatus,
      cover: s.sourceServiceCover || '',
      amount: money2(s.sourceOrderAmount)
    }
  }
  // 没有订单号才退回商品卡：从服务详情发起的咨询本来就只是「问这个商品」
  if (s.serviceId) {
    return {
      kind: 'service',
      to: `/services/${s.serviceId}`,
      name: s.sourceServiceName || `服务 #${s.serviceId}`,
      cover: s.sourceServiceCover || '',
      amount: money2(s.sourceServicePrice)
    }
  }
  return null
})

// 金额一律两位小数：和用户端同一口径，两张卡并排时位数得齐
function money2(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return Number.isFinite(n) ? '¥' + n.toFixed(2) : '—'
}

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }
// 会话列表要显示"和谁在聊"：后端已在列表里带上 userName，取不到才退回「用户 #id」占位
function customerName(s) { return (s && s.userName) || `用户 #${s?.userId}` }

async function loadSessions() {
  const data = await http.get('/merchant/consultations')
  sessions.value = Array.isArray(data) ? data : []
}

async function openSession(s) {
  curId.value = s.id
  coverBroken.value = ''
  const data = await http.get(`/merchant/consultations/${s.id}/messages`)
  messages.value = Array.isArray(data) ? data : []
  nextTick(() => { listEl.value?.scrollTo(0, listEl.value.scrollHeight) })
}

// 只拉消息不滚屏：轮询兜底时不能把商家正在往上翻的位置抢回底部
async function pullMessages() {
  const id = curId.value
  if (!id) return
  try {
    const data = await http.get(`/merchant/consultations/${id}/messages`)
    if (curId.value === id) messages.value = Array.isArray(data) ? data : []
  } catch (e) { /* 兜底失败就等下一轮，不打扰商家 */ }
}

// 后台刷新：失败就安静地等下一轮。列表已经渲染出来的内容不能因为一次抖动被清空
async function refreshQuiet() {
  try { await loadSessions() } catch (e) { /* 同上 */ }
}

const sending = ref(false)

async function send() {
  if (!draft.value.trim() || sending.value) return
  sending.value = true
  try {
    await http.post(`/merchant/consultations/${curId.value}/messages`, { content: draft.value })
    draft.value = ''
    await openSession({ id: curId.value })
    loadSessions()
  } finally {
    sending.value = false
  }
}

// 拦截器已弹出错误提示，这里只把页面降级成错误态，不白屏
async function boot() {
  loading.value = true
  err.value = ''
  try {
    await loadSessions()
  } catch (e) {
    sessions.value = []
    err.value = (e && e.message) || '会话加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

// ===== 实时通道（WebSocket）+ 兜底轮询 =====
// WS 是主路径，轮询只是兜底：后端重启、代理抖一下的时候，页面不至于变成一块死掉的玻璃。
const MSG_POLL_MS = 30000
const LIST_POLL_MS = 15000
// 买家连发三条时不能一条一请求，攒 300ms 合成一次
const FRAME_DEBOUNCE_MS = 300

let offMessage = null
let msgPollTimer = null
let listPollTimer = null
let frameTimer = null
let wantMessages = false
let wantList = false

async function flushFrames() {
  const doMsg = wantMessages
  const doList = wantList
  wantMessages = false
  wantList = false
  // 先补消息再刷列表：拉消息会让后端把未读清零，顺序反了这个会话的角标就会短暂显示旧数字
  if (doMsg) {
    await pullMessages()
    nextTick(() => { listEl.value?.scrollTo(0, listEl.value.scrollHeight) })
  }
  if (doList) await refreshQuiet()
}

function scheduleFrames() {
  if (frameTimer) clearTimeout(frameTimer)
  frameTimer = setTimeout(() => { frameTimer = null; flushFrames() }, FRAME_DEBOUNCE_MS)
}

function onFrame(frame) {
  if (!frame) return
  // 只有「当前打开的这个会话」才拉消息；别的会话拉了也没处放，刷列表和角标就够
  if (curId.value && frame.conversationId === curId.value) wantMessages = true
  wantList = true
  scheduleFrames()
}

onMounted(async () => {
  await boot()

  // connect() 是幂等的，多页重复调用不会多握一条手
  connect()
  offMessage = onMessage(onFrame)
  msgPollTimer = setInterval(pullMessages, MSG_POLL_MS)
  listPollTimer = setInterval(refreshQuiet, LIST_POLL_MS)
})

// 只退订自己那份，不 close 全局连接：这条管子是全站共用的，
// 一个页面卸载就把它掐了，别处的实时会跟着断。
onUnmounted(() => {
  if (offMessage) { offMessage(); offMessage = null }
  if (msgPollTimer) { clearInterval(msgPollTimer); msgPollTimer = null }
  if (listPollTimer) { clearInterval(listPollTimer); listPollTimer = null }
  if (frameTimer) { clearTimeout(frameTimer); frameTimer = null }
})
</script>

<style scoped>
.chat-panel {
  padding: 0;
}
.chat {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: 480px;
  height: calc(100vh - var(--space-9) * 3);
  max-height: 680px;
}
.sessions {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  border-right: 1px solid var(--border);
  background: var(--bg);
  overflow-y: auto;
}
.session {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  cursor: pointer;
  font-family: inherit;
}
.session:hover {
  background: var(--surface);
}
.session.active {
  background: var(--surface);
  border-color: var(--rose-200);
}
.s-name {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--ink);
}
.s-badge {
  min-width: var(--space-4);
  padding: 0 var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--rose-500);
  color: var(--surface);
  font-size: var(--fs-xs);
  text-align: center;
}
.s-last {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.s-time {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.messages {
  display: flex;
  flex-direction: column;
  min-width: 0;
  /* 它在网格列里：不给 min-height: 0，容器就按内容长高，.msg-list 于是永不可滚，
     整页反而滚动 —— 那样 scrollTo 滚的是个不会滚的元素，新消息永远停在屏幕外。 */
  min-height: 0;
  overflow: hidden;
}
/* ---------- 会话头 + 来源卡 ---------- */
.chat-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.chat-name {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--ink);
  line-height: var(--lh-ui);
}
.chat-meta {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
  line-height: var(--lh-ui);
}
.src {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-3);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  text-decoration: none;
  color: inherit;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.src:hover {
  border-color: var(--rose-200);
  background: var(--rose-50);
}
/* 封面兜底是底色而不是空白：图挂了卡片还在，不会塌成一条缝 */
.src-cover {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: var(--space-7);
  height: var(--space-7);
  overflow: hidden;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.src-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.src-cover-ph {
  font-size: var(--fs-base);
  line-height: 1;
  color: var(--ink-soft);
}
.src-main {
  min-width: 0;
}
.src-tag {
  margin: 0;
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: 0.04em;
  color: var(--ink-soft);
}
.src-name {
  margin: var(--space-1) 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--fs-sm);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.src-meta {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
}
.src-side {
  display: flex;
  flex: none;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-1);
}
.src-amount {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-base);
  font-weight: 600;
  line-height: 1;
  color: var(--rose-500);
}
.src-go {
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
}
.src:hover .src-go {
  color: var(--rose-500);
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.msg {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.msg.mine {
  align-items: flex-end;
}
.msg.other {
  align-items: flex-start;
}
.bubble {
  max-width: 60ch;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius);
  background: var(--bg);
  border: 1px solid var(--border);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink);
}
.msg.mine .bubble {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: var(--surface);
}
.time {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.msg-empty {
  margin: auto;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.input-bar {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-4);
  border-top: 1px solid var(--border);
}
.chat-sk {
  grid-template-columns: 280px minmax(0, 1fr);
}
.sk-col {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
}
.sk-line {
  display: block;
  height: var(--space-5);
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: sk-pulse 1.4s ease-in-out infinite;
}
@keyframes sk-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sk-line {
    animation: none;
  }
}
</style>
