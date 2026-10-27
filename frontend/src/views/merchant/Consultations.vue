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
import { ref, computed, onMounted, nextTick } from 'vue'
import http from '@/api/http'

const sessions = ref([])
const curId = ref(null)
const messages = ref([])
const draft = ref('')
const listEl = ref(null)
const loading = ref(false)
const err = ref('')

const unread = computed(() => sessions.value.reduce((n, s) => n + (s.merchantUnread || 0), 0))

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }
// 会话列表要显示"和谁在聊"：后端已在列表里带上 userName，取不到才退回「用户 #id」占位
function customerName(s) { return (s && s.userName) || `用户 #${s?.userId}` }

async function loadSessions() {
  const data = await http.get('/merchant/consultations')
  sessions.value = Array.isArray(data) ? data : []
}

async function openSession(s) {
  curId.value = s.id
  const data = await http.get(`/merchant/consultations/${s.id}/messages`)
  messages.value = Array.isArray(data) ? data : []
  nextTick(() => { listEl.value?.scrollTo(0, listEl.value.scrollHeight) })
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

onMounted(boot)
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
