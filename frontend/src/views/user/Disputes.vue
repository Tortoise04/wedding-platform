<template>
  <div class="page">
    <header class="masthead">
      <h1 class="mast-title">争议仲裁</h1>
      <p class="mast-sub">对不满意的已完成订单可申请平台介入，举证与裁决全程留痕。</p>
    </header>

    <!-- ===== 发起平台介入 ===== -->
    <section class="block">
      <h2 class="block-title">发起平台介入</h2>
      <p class="block-note">填写诉求后由管理员受理仲裁；受理进入举证环节后，可在争议详情里补充材料。</p>

      <div v-if="loadingOrders" class="sk-form">
        <div class="sk-line sk-line--lg"></div>
        <div class="sk-line"></div>
        <div class="sk-line sk-line--btn"></div>
      </div>

      <template v-else-if="completedOrders.length">
        <div class="field">
          <span class="field-label">选择订单</span>
          <el-select v-model="orderId" placeholder="选择要投诉的已完成订单" class="field-control">
            <el-option v-for="o in completedOrders" :key="o.id" :label="orderLabel(o)" :value="o.id" />
          </el-select>
        </div>
        <div class="field">
          <span class="field-label">诉求说明</span>
          <el-input v-model="reason" type="textarea" :rows="4" placeholder="描述你遇到的问题与诉求" />
        </div>
        <div class="field">
          <span class="field-label">现场图片（可选，1 张）</span>
          <div v-if="uploadedUrl" class="preview">
            <img :src="uploadedUrl" alt="现场图片预览" class="preview-img" />
            <el-button link type="danger" @click="uploadedUrl = ''">移除</el-button>
          </div>
          <el-upload
            v-else
            :show-file-list="false"
            :http-request="uploadImage"
            :before-upload="checkImage"
            accept=".jpg,.jpeg,.png,.webp,.gif"
          >
            <el-button :loading="uploading">上传图片</el-button>
          </el-upload>
          <span class="field-hint">方便管理员判断现场情况：支持 jpg / png / webp / gif，单张不超过 5MB</span>
        </div>
        <div class="block-foot">
          <el-button type="primary" :loading="submitting" @click="submit">提交争议</el-button>
        </div>
      </template>

      <div v-else class="note-inline">暂无已完成订单可申请介入。</div>
    </section>

    <!-- ===== 我的争议 ===== -->
    <section class="block">
      <div class="block-head">
        <h2 class="block-title">我的争议</h2>
        <span class="block-count">共 <em>{{ disputes.length }}</em> 条</span>
      </div>

      <div v-if="loadingDisputes" class="list">
        <div v-for="i in 2" :key="i" class="sk-row">
          <div class="sk-line sk-line--lg"></div>
          <div class="sk-line"></div>
        </div>
      </div>

      <div v-else-if="error" class="state">
        <p class="state-ico">!</p>
        <p class="state-title">争议列表没有加载出来</p>
        <p class="state-desc">网络或服务端出了点问题，稍后再试一次。</p>
        <button class="act" @click="loadDisputes">重新加载</button>
      </div>

      <div v-else-if="!disputes.length" class="state">
        <p class="state-ico">◇</p>
        <p class="state-title">还没有发起过争议</p>
        <p class="state-desc">订单顺利完成就不需要走到这一步；真遇到问题，从上面的表单开始。</p>
      </div>

      <div v-else class="list">
        <article v-for="d in disputes" :key="d.id" class="card" @click="openDetail(d)">
          <div class="card-main">
            <div class="card-line">
              <h3 class="card-no">{{ d.disputeNo }}</h3>
              <span class="pill" :class="'pill--' + toneOf(d.status)">{{ statusLabel(d.status) }}</span>
            </div>
            <p class="card-reason">{{ d.reason || '发起人未填写诉求' }}</p>
            <p class="card-meta">关联订单 {{ d.orderNo || ('#' + d.orderId) }}<span class="dot">·</span>发起于 {{ fmt(d.createdAt) }}</p>
          </div>
          <span class="act act--ghost">查看举证</span>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

// 状态标签色：绿=已裁决、玫瑰金=进行中、黄=待受理、灰=已关闭
const STATUS = {
  PENDING: { label: '待受理', tone: 'warn' },
  PROCESSING: { label: '举证中', tone: 'rose' },
  ARBITRATING: { label: '仲裁中', tone: 'rose' },
  FINISHED: { label: '已裁决', tone: 'green' },
  CLOSED: { label: '已关闭', tone: 'mute' }
}

const completedOrders = ref([])
const disputes = ref([])
const svcMap = ref({})
const orderId = ref(null)
const reason = ref('')
const uploadedUrl = ref('')
const uploading = ref(false)
const submitting = ref(false)
const loadingOrders = ref(false)
const loadingDisputes = ref(false)
const error = ref(false)
const router = useRouter()

function statusLabel(s) { return STATUS[s]?.label || s || '状态未知' }
function toneOf(s) { return STATUS[s]?.tone || 'mute' }
function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}
function orderLabel(o) {
  const name = svcMap.value[o.serviceId]
  return name ? `${name} · ${o.orderNo} · ¥${o.amount}` : `${o.orderNo} · ¥${o.amount}`
}

async function loadNames() {
  try {
    const data = await http.get('/services/search', { params: { page: 0, size: 100 } })
    const map = {}
    for (const s of data?.list || []) map[s.id] = s.name
    svcMap.value = map
  } catch (e) { /* 名称补不上时退回订单号 */ }
}

async function loadOrders() {
  loadingOrders.value = true
  try {
    // /orders 已改为分页返回 {list,total,...}，这里取 list
    const data = await http.get('/orders', { params: { status: 'COMPLETED', page: 0, size: 100 } })
    completedOrders.value = data.list || []
  } catch (e) {
    completedOrders.value = []
  } finally {
    loadingOrders.value = false
  }
}

async function loadDisputes() {
  loadingDisputes.value = true
  error.value = false
  try {
    disputes.value = (await http.get('/disputes')) || []
  } catch (e) {
    disputes.value = []
    error.value = true
  } finally {
    loadingDisputes.value = false
  }
}

function openDetail(d) { router.push(`/user/disputes/${d.id}`) }

// 发起时就带一张现场图，管理员不必等举证环节才看得到材料
const IMAGE_EXT = /\.(jpe?g|png|webp|gif)$/i
const MAX_IMAGE_BYTES = 5 * 1024 * 1024

function checkImage(file) {
  if (!IMAGE_EXT.test(file.name)) { ElMessage.warning('只支持 jpg / png / webp / gif 图片'); return false }
  if (file.size > MAX_IMAGE_BYTES) { ElMessage.warning('图片大小不能超过 5MB'); return false }
  return true
}

// 走 http 实例而不是裸 axios，才能拿到拦截器的 token 与统一错误提示
async function uploadImage(options) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    uploadedUrl.value = data.url
  } catch {
    // 拦截器已提示失败原因；清掉地址，换一张重试
    uploadedUrl.value = ''
  } finally {
    uploading.value = false
  }
}

async function submit() {
  if (!orderId.value) { ElMessage.warning('请选择订单'); return }
  if (!reason.value.trim()) { ElMessage.warning('请填写诉求'); return }
  if (uploading.value) { ElMessage.warning('图片还在上传，请稍候'); return }
  submitting.value = true
  try {
    await http.post('/disputes', { orderId: orderId.value, reason: reason.value, fileUrl: uploadedUrl.value || null })
    ElMessage.success('争议已提交，管理员将受理')
    orderId.value = null
    reason.value = ''
    uploadedUrl.value = ''
    await Promise.all([loadOrders(), loadDisputes()])
  } finally {
    submitting.value = false
  }
}

onMounted(() => { loadNames(); loadOrders(); loadDisputes() })
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
.mast-title { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.mast-sub { max-width: 46ch; margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.block {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}
.block-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: var(--space-3); }
.block-title { font-size: var(--fs-lg); line-height: var(--lh-ui); font-weight: 600; }
.block-note { max-width: 56ch; margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.block-count { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.block-count em { font-family: var(--font-display); font-style: normal; font-size: var(--fs-lg); line-height: 1; color: var(--rose-500); }
.block-foot { display: flex; justify-content: flex-end; margin-top: var(--space-4); }

.field { display: flex; flex-direction: column; gap: var(--space-2); margin-top: var(--space-4); }
.field-label { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .04em; }
.field-control { width: 100%; }
.field-hint { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.preview { display: flex; align-items: center; gap: var(--space-3); }
.preview-img { display: block; width: 96px; height: 60px; object-fit: cover; border-radius: var(--radius-sm); background: var(--bg-deep); }
.note-inline { margin-top: var(--space-4); color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== 争议卡片 ===== */
.list { display: flex; flex-direction: column; gap: var(--space-4); margin-top: var(--space-4); }
.card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-4);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  transition: box-shadow .3s ease, border-color .3s ease, background-color .3s ease;
}
.card:hover { background: var(--surface); border-color: var(--rose-200); box-shadow: var(--shadow-sm); }
.card-main { min-width: 0; }
.card-line { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); }
.card-no { font-family: var(--font-display); font-size: var(--fs-base); line-height: var(--lh-ui); font-weight: 600; }
.card-reason {
  margin: var(--space-2) 0 0;
  color: var(--ink);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-meta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); margin: var(--space-2) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.dot { color: var(--ink-soft); }

/* ===== 标签 / 按钮 ===== */
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
.pill--green { color: var(--success); }
.pill--rose { color: var(--rose-500); }
.pill--warn { color: var(--el-color-warning-dark-2); }
.pill--mute { color: var(--ink-soft); }

.act {
  display: inline-flex;
  flex: none;
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
.state { padding: var(--space-7) var(--space-5); border: 1px dashed var(--border); border-radius: var(--radius); text-align: center; margin-top: var(--space-4); }
.state-ico { margin: 0; font-family: var(--font-display); font-size: var(--fs-xl); line-height: 1; color: var(--ink-soft); }
.state-title { margin: var(--space-4) 0 0; font-size: var(--fs-base); font-weight: 600; line-height: var(--lh-ui); }
.state-desc { max-width: 40ch; margin: var(--space-2) auto 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.sk-form { display: flex; flex-direction: column; gap: var(--space-3); margin-top: var(--space-4); }
.sk-row { display: flex; flex-direction: column; gap: var(--space-3); padding: var(--space-4); background: var(--bg); border: 1px solid var(--border); border-radius: var(--radius); }
.sk-line { height: var(--space-3); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 40%; }
.sk-line--btn { width: 96px; height: var(--space-6); }
.sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) { .sk-line { animation: none; } }

@media (max-width: 720px) {
  .card { grid-template-columns: minmax(0, 1fr); }
  .act--ghost { align-self: flex-start; }
}
</style>
