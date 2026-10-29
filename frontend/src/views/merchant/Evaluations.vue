<template>
  <div class="mch-page">
    <header class="mch-head">
      <p class="mch-sub">
        共 <em>{{ evals.length }}</em> 条评价 · 待回复 <em>{{ pendingCount }}</em> 条
      </p>
    </header>

    <section v-if="error" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">评价没有加载出来</p>
        <p class="mch-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重试</el-button>
      </div>
    </section>

    <section v-else-if="loading" class="mch-list">
      <div v-for="i in 2" :key="i" class="sk-row">
        <span class="sk-lines"><i /><i /></span>
      </div>
    </section>

    <section v-else-if="!evals.length" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">◇</p>
        <p class="mch-state-title">还没有收到评价</p>
        <p class="mch-state-desc">订单完成后买家可以对服务打分并留言，评价会出现在这里，你可以公开回复。</p>
        <router-link to="/merchant/orders"><el-button size="small">查看已完成订单</el-button></router-link>
      </div>
    </section>

    <section v-else class="mch-list">
      <article v-for="row in evals" :key="row.id" class="mch-row row-plain eval">
        <div class="mch-row-main">
          <div class="mch-row-meta">
            <el-rate :model-value="row.score" disabled />
            <span class="mch-tag" :class="row.replyContent ? 'is-ok' : 'is-wait'">
              {{ row.replyContent ? '已回复' : '待回复' }}
            </span>
            <span>{{ fmt(row.createdAt) }}</span>
          </div>
          <!-- 评价对象：服务 + 订单，是回复这栏的前提，不能只给星级和时间 -->
          <p class="eval-what">
            <span class="eval-svc">{{ serviceName(row) }}</span>
            <span class="eval-ord">订单 #{{ row.orderId || '—' }}</span>
            <span v-if="row.anonymous" class="eval-anon">匿名评价</span>
          </p>
          <p class="eval-content">{{ row.content || '买家没有留下文字' }}</p>
          <!-- 评价媒体：有才渲染，没有就整块不出现 -->
          <div v-if="imgList(row.images).length" class="eval-shots">
            <el-image
              v-for="(src, i) in imgList(row.images)"
              :key="src + i"
              :src="src"
              fit="cover"
              class="eval-thumb"
              :preview-src-list="imgList(row.images)"
              :initial-index="i"
              preview-teleported
            />
          </div>
          <video v-if="row.videoUrl" :src="row.videoUrl" controls preload="metadata" class="eval-video"></video>
          <div v-if="row.replyContent" class="reply">
            <span class="reply-cap">我的回复</span>
            <p class="reply-text">{{ row.replyContent }}</p>
          </div>
        </div>
        <div class="mch-row-side">
          <div class="mch-row-acts">
            <button v-if="!row.replyContent" type="button" class="mch-act is-rose" @click="openReply(row)">回复</button>
            <button v-else type="button" class="mch-act is-muted" disabled>已回复</button>
          </div>
        </div>
      </article>
    </section>

    <el-dialog v-model="replyVisible" :title="replyTitle" width="420px">
      <p v-if="replyRow" class="reply-what">
        <span class="eval-svc">{{ serviceName(replyRow) }}</span>
        <span class="eval-ord">订单 #{{ replyRow.orderId || '—' }}</span>
        <span>{{ replyRow.score }} 星 · {{ fmt(replyRow.createdAt) }}</span>
      </p>
      <p v-if="replyRow" class="reply-quote">{{ replyRow.content || '买家没有留下文字' }}</p>
      <el-input v-model="replyContent" type="textarea" :rows="3" placeholder="公开回复内容" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const evals = ref([])
const loading = ref(false)
const error = ref('')
const replyVisible = ref(false)
const replyContent = ref('')
const replyId = ref(null)
const replyRow = ref(null)
// 只读的服务名字典：评价实体只带 serviceId，名字要自己换
const serviceMap = ref({})

const pendingCount = computed(() => evals.value.filter(e => !e.replyContent).length)

function serviceName(row) {
  if (!row) return '服务'
  const s = serviceMap.value[row.serviceId]
  return (s && s.name) || '服务 #' + (row.serviceId || '—')
}
// 弹窗标题必须点明在回哪一条，否则就是对着空白回
const replyTitle = computed(() => (replyRow.value ? '回复评价 · ' + serviceName(replyRow.value) : '回复评价'))

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }
// images 是逗号分隔的 url 字符串；空串 / null 一律当没有，不渲染空壳
function imgList(v) { return String(v || '').split(',').map(s => s.trim()).filter(Boolean) }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await http.get('/merchant/evaluations')
    evals.value = Array.isArray(data) ? data : []
  } catch (e) {
    evals.value = []
    error.value = (e && e.message) || '评价加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function openReply(row) { replyId.value = row.id; replyRow.value = row; replyContent.value = ''; replyVisible.value = true }
async function submitReply() {
  await http.post(`/merchant/evaluations/${replyId.value}/reply`, { content: replyContent.value })
  ElMessage.success('已回复')
  replyVisible.value = false
  load()
}

async function loadNames() {
  try {
    const data = await http.get('/merchant/services', { params: { page: 0, size: 100 } })
    const map = {}
    ;(data.list || []).forEach(s => { map[s.id] = { name: s.name } })
    serviceMap.value = map
  } catch {
    serviceMap.value = {}
  }
}

onMounted(() => { load(); loadNames() })
</script>

<style scoped>
.row-plain {
  grid-template-columns: minmax(0, 1fr) auto;
}
.eval {
  align-items: flex-start;
}
.eval-what {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
  margin: 0 0 var(--space-1);
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.eval-svc {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--ink);
}
.eval-ord {
  font-variant-numeric: tabular-nums;
}
.eval-anon {
  padding: 0 var(--space-2);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.reply-what {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
  margin: 0 0 var(--space-2);
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.reply-quote {
  margin: 0 0 var(--space-3);
  padding: var(--space-3);
  background: var(--bg);
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink);
  white-space: pre-wrap;
}
.eval-content {
  margin: 0;
  font-size: var(--fs-base);
  line-height: var(--lh-body);
  color: var(--ink);
  /* 用户输入里的换行要保留：门户侧是 pre-wrap，商家侧同一段文字不能塌成一行 */
  white-space: pre-wrap;
}
/* 评价媒体：缩略图与视频小窗，都给预览不给整页大图 */
.eval-shots {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}
.eval-thumb {
  display: block;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  cursor: zoom-in;
}
.eval-video {
  display: block;
  width: 100%;
  max-width: 260px;
  margin-top: var(--space-3);
  border-radius: var(--radius-sm);
  background: #000;
}
.reply {
  margin-top: var(--space-1);
  padding: var(--space-3);
  background: var(--bg);
  border-radius: var(--radius-sm);
}
.reply-cap {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.reply-text {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink);
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
