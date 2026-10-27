<template>
  <div class="mch-page">
    <header class="mch-head">
      <p class="mch-sub">
        共 <em>{{ list.length }}</em> 起争议 · 当前筛选 <em>{{ curLabel }}</em>
      </p>
      <div class="mch-actions">
        <div class="mch-seg">
          <button type="button" :class="{ on: status === '' }" @click="status = ''">全部</button>
          <button
            v-for="(s, k) in STATUS"
            :key="k"
            type="button"
            :class="{ on: status === k }"
            @click="status = k"
          >
            {{ s.label }}
          </button>
        </div>
      </div>
    </header>

    <section v-if="error" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">争议列表没有加载出来</p>
        <p class="mch-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重试</el-button>
      </div>
    </section>

    <section v-else-if="loading" class="mch-list">
      <div v-for="i in 2" :key="i" class="sk-row">
        <span class="sk-lines"><i /><i /></span>
      </div>
    </section>

    <section v-else-if="!shown.length" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">◇</p>
        <p class="mch-state-title">{{ status ? '该状态下没有争议' : '本店暂无争议' }}</p>
        <p class="mch-state-desc">
          {{ status ? '换一个状态看看，或回到全部争议。' : '买家对订单提交争议后会出现在这里，需要你举证答复。' }}
        </p>
        <el-button v-if="status" size="small" @click="status = ''">查看全部争议</el-button>
        <router-link v-else to="/merchant/orders"><el-button size="small">查看订单</el-button></router-link>
      </div>
    </section>

    <section v-else class="mch-list">
      <article v-for="row in shown" :key="row.id" class="mch-row row-plain">
        <div class="mch-row-main">
          <h3 class="mch-row-name">{{ row.reason || '未填写诉求' }}</h3>
          <div class="mch-row-meta">
            <span class="mch-mono">{{ row.disputeNo }}</span>
            <span class="mch-tag" :class="statusMeta(row.status).tone">{{ statusMeta(row.status).label }}</span>
            <span>关联订单 {{ row.orderNo || ('#' + row.orderId) }}</span>
          </div>
          <p class="mch-row-sub">
            发起 {{ fmt(row.createdAt) }}
            <template v-if="row.acceptedAt"> · 受理 {{ fmt(row.acceptedAt) }}</template>
            <template v-if="row.finishedAt"> · 裁决 {{ fmt(row.finishedAt) }}</template>
          </p>
        </div>
        <div class="mch-row-side">
          <span v-if="row.refundAmount != null" class="mch-amount">{{ money(row.refundAmount) }}</span>
          <div class="mch-row-acts">
            <button type="button" class="mch-act is-rose" @click="openDetail(row.id)">查看举证</button>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'

// 枚举中文与标签色:接口只给英文码,页面上不允许出现英文
const STATUS = {
  PENDING: { label: '待受理', tone: 'is-wait' },
  PROCESSING: { label: '举证中', tone: 'is-doing' },
  ARBITRATING: { label: '仲裁中', tone: 'is-doing' },
  FINISHED: { label: '已裁决', tone: 'is-ok' },
  CLOSED: { label: '已关闭', tone: 'is-off' }
}

const router = useRouter()
// 返回列表时恢复离开前的滚动位置:<script setup> 里的顶层变量随组件重建清零,所以位置存在 sessionStorage
const SCROLL_KEY = 'mch:disputes:scroll'
const list = ref([])
const loading = ref(false)
const status = ref('')
const error = ref('')

// 商家侧争议接口不接 status 参数,筛选在已拉到的列表上做,不额外请求
const shown = computed(() => {
  const arr = Array.isArray(list.value) ? list.value : []
  return status.value ? arr.filter(d => d.status === status.value) : arr
})

const curLabel = computed(() => (status.value ? statusMeta(status.value).label : '全部'))

function statusMeta(code) {
  return STATUS[code] || { label: '状态未知', tone: 'is-off' }
}

function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

// 时间串可能是 'YYYY-MM-DD HH:mm:ss' 或 ISO,统一截到分钟,不依赖 Date 解析
function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

// 业务错误(如未完成入驻)走到这里:优先用服务端 msg,非中文(网络层报错)一律换成中文兜底
function errText(e) {
  const msg = (e && e.response && e.response.data && e.response.data.msg) || (e && e.message) || ''
  return /[\u4e00-\u9fa5]/.test(msg) ? msg : '争议列表加载失败,请稍后重试'
}

// 拦截器已经弹过错误提示,这里只负责把页面降级成错误态加文案,不允许白屏或未捕获异常
async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await http.get('/merchant/disputes')
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    list.value = []
    error.value = errText(e)
  } finally {
    loading.value = false
  }
}

function openDetail(id) {
  const y = window.scrollY || document.documentElement.scrollTop || 0
  sessionStorage.setItem(SCROLL_KEY, String(y))
  router.push(`/merchant/disputes/${id}`)
}

// 列表是异步渲染的:内容还没落地时 scrollTo 会被浏览器夹到 0,所以按帧重试到目标位置
function restoreScroll() {
  const y = Number(sessionStorage.getItem(SCROLL_KEY) || 0)
  if (!y) return
  sessionStorage.removeItem(SCROLL_KEY)
  let tries = 0
  const put = () => {
    window.scrollTo(0, y)
    if (tries++ < 8 && Math.abs(window.scrollY - y) > 1) requestAnimationFrame(put)
  }
  nextTick(() => {
    put()
    requestAnimationFrame(put)
    setTimeout(put, 120)
  })
}

onMounted(async () => {
  await load()
  restoreScroll()
})
</script>

<style scoped>
.row-plain {
  grid-template-columns: minmax(0, 1fr) auto;
}
.sk-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.sk-lines {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.sk-lines i {
  display: block;
  height: var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: sk-pulse 1.4s ease-in-out infinite;
}
.sk-lines i:nth-child(2) {
  width: 60%;
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
  .sk-lines i {
    animation: none;
  }
}
</style>
