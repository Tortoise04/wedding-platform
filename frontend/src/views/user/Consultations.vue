<template>
  <div class="page">
    <!-- ===== 会话列表 ===== -->
    <template v-if="!curId">
      <header class="masthead">
        <div class="mast-row">
          <div>
            <h1 class="mast-title">我的咨询</h1>
            <p class="mast-sub">和商家的一来一往都留在这里，从哪里停下就从哪里继续。</p>
          </div>
          <el-button type="primary" round @click="openNew">发起咨询</el-button>
        </div>
      </header>

      <p class="count-line">共 <em>{{ sessions.length }}</em> 个会话</p>

      <div v-if="loading" class="list">
        <div v-for="i in 3" :key="i" class="sk-row">
          <div class="sk-line sk-line--lg"></div>
          <div class="sk-line"></div>
          <div class="sk-line sk-line--short"></div>
        </div>
      </div>

      <div v-else-if="error" class="state">
        <p class="state-ico">!</p>
        <p class="state-title">会话没有加载出来</p>
        <p class="state-desc">网络或服务端出了点问题，稍后再试一次。</p>
        <button class="act" @click="loadSessions">重新加载</button>
      </div>

      <div v-else-if="!sessions.length" class="state">
        <p class="state-ico">◇</p>
        <p class="state-title">还没有咨询过</p>
        <p class="state-desc">想先问问档期、预算或流程，直接给商家发一条消息就好。</p>
        <button class="act" @click="openNew">发起咨询</button>
      </div>

      <div v-else class="list">
        <article v-for="s in sessions" :key="s.id" class="card" @click="openSession(s)">
          <div class="card-main">
            <div class="card-line">
              <h2 class="card-name">{{ nameMap[s.merchantId] || `商家 #${s.merchantId}` }}</h2>
              <span class="pill" :class="s.userUnread ? 'pill--rose' : 'pill--mute'">
                {{ s.userUnread ? `${s.userUnread} 条未读` : '已读' }}
              </span>
            </div>
            <p class="card-svc">{{ svcName(s.serviceId) }}</p>
            <p class="card-last">{{ s.lastMessage || '还没有消息，点开打个招呼' }}</p>
          </div>
          <div class="card-side">
            <p class="card-time">{{ fmt(s.lastTime || s.createdAt) }}</p>
            <span class="act act--ghost">进入会话</span>
          </div>
        </article>
      </div>
    </template>

    <!-- ===== 会话详情：页内全屏视图 ===== -->
    <section v-else class="chat">
      <button class="back" @click="backToList">
        <el-icon class="back-ico"><ArrowLeft /></el-icon> 返回会话列表
      </button>

      <header class="chat-head">
        <h1 class="chat-title">{{ nameMap[curChat?.merchantId] || `商家 #${curChat?.merchantId}` }}</h1>
        <p class="chat-meta">
          <span>{{ svcName(curChat?.serviceId) }}</span>
          <span class="dot">·</span>
          <span>开始于 {{ fmt(curChat?.createdAt) }}</span>
        </p>
      </header>

      <div class="msg-list" ref="listEl">
        <div v-for="m in messages" :key="m.id" class="msg" :class="m.senderRole === 'USER' ? 'mine' : 'other'">
          <div class="bubble">{{ m.content }}</div>
          <div class="msg-time">{{ fmtShort(m.createdAt) }}</div>
        </div>
        <p v-if="!messages.length" class="msg-empty">还没有消息，先发一句问候吧。</p>
      </div>

      <div class="input-bar">
        <el-input v-model="draft" placeholder="输入消息" @keyup.enter="send" />
        <el-button type="primary" @click="send" :loading="sending">发送</el-button>
      </div>
    </section>

    <!-- ===== 短表单弹窗 ===== -->
    <el-dialog v-model="newVisible" title="发起咨询" width="420px">
      <el-form label-width="70px">
        <el-form-item label="商家">
          <el-select v-model="newForm.merchantId" placeholder="选择商家" style="width:100%">
            <el-option v-for="m in merchants" :key="m.id" :label="m.shopName" :value="m.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newVisible = false">取消</el-button>
        <el-button type="primary" @click="createSession">开始咨询</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import http from '@/api/http'

const sessions = ref([])
const merchants = ref([])
const nameMap = ref({})
const svcMap = ref({})
const curId = ref(null)
const messages = ref([])
const draft = ref('')
const listEl = ref(null)
const loading = ref(false)
const error = ref(false)
const savedScroll = ref(0)

const newVisible = ref(false)
const newForm = reactive({ merchantId: null })

const curChat = computed(() => sessions.value.find(s => s.id === curId.value) || null)

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}
function fmtShort(t) { return fmt(t).slice(5) }
function svcName(id) { return id ? (svcMap.value[id] || `服务 #${id}`) : '未指定服务' }

async function loadNames() {
  try {
    const list = await http.get('/merchants')
    const map = {}
    for (const m of list || []) map[m.id] = m.shopName || `商家 #${m.id}`
    nameMap.value = map
  } catch (e) { /* 名称补不上时用占位文案 */ }
  try {
    const data = await http.get('/services/search', { params: { page: 0, size: 100 } })
    const map = {}
    for (const s of data?.list || []) map[s.id] = s.name
    svcMap.value = map
  } catch (e) { /* 同上 */ }
}

async function loadSessions() {
  loading.value = true
  error.value = false
  try {
    sessions.value = await http.get('/consultations')
  } catch (e) {
    sessions.value = []
    error.value = true
  } finally {
    loading.value = false
  }
}

function scrollToBottom() {
  nextTick(() => { listEl.value?.scrollTo(0, listEl.value.scrollHeight) })
}

async function openSession(s) {
  savedScroll.value = window.scrollY || 0
  curId.value = s.id
  messages.value = await http.get(`/consultations/${s.id}/messages`)
  window.scrollTo(0, 0)
  scrollToBottom()
}

function backToList() {
  curId.value = null
  messages.value = []
  const y = savedScroll.value
  const put = () => window.scrollTo(0, y)
  nextTick(() => { put(); requestAnimationFrame(put); setTimeout(put, 80) })
}

const sending = ref(false)

async function send() {
  if (!draft.value.trim() || sending.value) return
  sending.value = true
  try {
    await http.post(`/consultations/${curId.value}/messages`, { content: draft.value })
    draft.value = ''
    await openSession({ id: curId.value })
    loadSessions()
  } finally {
    sending.value = false
  }
}

async function openNew() {
  merchants.value = await http.get('/merchants')
  newForm.merchantId = null
  newVisible.value = true
}

async function createSession() {
  if (!newForm.merchantId) { ElMessage.warning('请选择商家'); return }
  const c = await http.post('/consultations', { merchantId: newForm.merchantId })
  newVisible.value = false
  ElMessage.success('会话已创建')
  await loadSessions()
  openSession(c)
}

// 从服务详情点「咨询商家」过来时会话已经建好，这里直接进对话；找不到就落在列表，别再让用户重选一次
const OPEN_CONSULT_KEY = 'consult:openId'

async function openPendingSession() {
  const raw = sessionStorage.getItem(OPEN_CONSULT_KEY)
  if (!raw) return
  sessionStorage.removeItem(OPEN_CONSULT_KEY)
  const s = sessions.value.find(x => x.id === Number(raw))
  if (s) await openSession(s)
}

onMounted(async () => {
  await loadNames()
  await loadSessions()
  await openPendingSession()
})
</script>

<style scoped>
.page {
  padding-bottom: var(--space-7);
  /* Element 占位符默认 #9A868D（3.40:1），按 AA 提到 ink-soft */
  --el-text-color-placeholder: var(--ink-soft);
}

/* Element 控件内部留白落到 --space 令牌，去掉 5/11/18px 这类一次性数值 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) { padding: var(--space-1) var(--space-3); }
:deep(.el-textarea__inner) { padding: var(--space-3); }
:deep(.el-select__suffix),
:deep(.el-input__suffix) { gap: var(--space-1); padding-left: var(--space-1); }
:deep(.el-form-item) { margin-bottom: var(--space-4); }
:deep(.el-select__wrapper) { gap: var(--space-1); }


.masthead { padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.mast-row { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: var(--space-4); }
.mast-title { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.mast-sub { max-width: 46ch; margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.count-line {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin: var(--space-5) 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.count-line em { font-family: var(--font-display); font-style: normal; font-size: var(--fs-xl); line-height: 1; color: var(--rose-500); }

/* ===== 会话卡片 ===== */
.list { display: flex; flex-direction: column; gap: var(--space-4); }
.card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: box-shadow .3s ease, border-color .3s ease;
}
.card:hover { box-shadow: var(--shadow); border-color: var(--rose-200); }
.card-main { min-width: 0; }
.card-line { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); }
.card-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--fs-lg); line-height: var(--lh-ui); font-weight: 600; }
.card-svc { margin: var(--space-2) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.card-last {
  margin: var(--space-2) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-side { display: flex; flex-direction: column; align-items: flex-end; gap: var(--space-2); }
.card-time { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

/* ===== 标签 ===== */
.pill {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  flex: none;
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  font-weight: 600;
  letter-spacing: .04em;
  color: var(--ink-soft);
}
.pill::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}
.pill--rose { color: var(--rose-500); }
.pill--mute { color: var(--ink-soft); }

/* ===== 按钮 ===== */
.act {
  display: inline-flex;
  align-items: center;
  height: var(--space-6);
  padding: 0 var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--ink);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  cursor: pointer;
  transition: border-color .2s ease, color .2s ease;
}
.act:hover { border-color: var(--rose-500); color: var(--rose-500); }
.act--ghost { color: var(--ink-soft); }
.card:hover .act--ghost { border-color: var(--rose-500); color: var(--rose-500); }

/* ===== 空 / 错误 / 加载 ===== */
.state { padding: var(--space-8) var(--space-6); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); text-align: center; }
.state-ico { margin: 0; font-family: var(--font-display); font-size: var(--fs-xl); line-height: 1; color: var(--ink-soft); }
.state-title { margin: var(--space-4) 0 0; font-size: var(--fs-base); font-weight: 600; line-height: var(--lh-ui); }
.state-desc { max-width: 40ch; margin: var(--space-2) auto 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.sk-row { display: flex; flex-direction: column; gap: var(--space-3); padding: var(--space-5); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); }
.sk-line { height: var(--space-3); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 34%; }
.sk-line--short { width: 24%; }
.sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) { .sk-line { animation: none; } }

/* ===== 会话详情 ===== */
.chat { padding-bottom: var(--space-7); }
.back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  height: var(--space-6);
  padding: 0;
  border: 0;
  background: none;
  color: var(--ink-soft);
  font-family: var(--font-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  cursor: pointer;
  transition: color .2s ease;
}
.back:hover { color: var(--rose-500); }
.back-ico { font-size: var(--fs-base); }
.chat-head { margin-top: var(--space-5); padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.chat-title { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.chat-meta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.dot { color: var(--ink-soft); }

.msg-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  height: 52vh;
  min-height: 280px;
  margin-top: var(--space-5);
  padding: var(--space-5);
  background: var(--bg-deep);
  border-radius: var(--radius-lg);
  overflow-y: auto;
}
.msg { display: flex; flex-direction: column; gap: var(--space-1); }
.msg.mine { align-items: flex-end; }
.msg.other { align-items: flex-start; }
.bubble {
  max-width: 62%;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--ink);
  font-size: var(--fs-base);
  line-height: var(--lh-body);
  white-space: pre-wrap;
  word-break: break-word;
}
.msg.mine .bubble { background: var(--rose-500); border-color: var(--rose-500); color: #fff; }
.msg-time { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.msg-empty { margin: auto; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.input-bar { display: flex; gap: var(--space-3); margin-top: var(--space-4); }

:deep(.el-dialog__title) { font-family: var(--font-display); font-size: var(--fs-lg); }

@media (max-width: 720px) {
  .card { grid-template-columns: minmax(0, 1fr); gap: var(--space-3); }
  .card-side { flex-direction: row; align-items: center; justify-content: space-between; }
  .bubble { max-width: 82%; }
}
</style>
