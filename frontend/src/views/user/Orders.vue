<template>
  <div class="page">
    <!-- ===== 列表态 ===== -->
    <template v-if="!detail">
      <header class="masthead">
        <h1 class="mast-title">我的订单</h1>
        <p class="mast-sub">从待审核到已完成，每一次婚礼筹备的进度都记在这里。</p>
      </header>

      <div class="seg">
        <button class="seg-btn" :class="{ 'is-on': status === '' }" @click="setStatus('')">全部</button>
        <button
          v-for="(s, k) in STATUS"
          :key="k"
          class="seg-btn"
          :class="{ 'is-on': status === k }"
          @click="setStatus(k)"
        >{{ s.label }}</button>
      </div>

      <p class="count-line">共 <em>{{ total }}</em> 笔订单</p>

      <div v-if="loading" class="list">
        <div v-for="i in 3" :key="i" class="sk-row">
          <div class="sk-thumb"></div>
          <div class="sk-body">
            <div class="sk-line sk-line--lg"></div>
            <div class="sk-line"></div>
            <div class="sk-line sk-line--short"></div>
          </div>
        </div>
      </div>

      <div v-else-if="error" class="state">
        <p class="state-ico">!</p>
        <p class="state-title">订单没有加载出来</p>
        <p class="state-desc">网络或服务端出了点问题，稍后再试一次。</p>
        <button class="act" @click="load">重新加载</button>
      </div>

      <div v-else-if="!orders.length" class="state">
        <p class="state-ico">◇</p>
        <p class="state-title">这里还没有订单</p>
        <p class="state-desc">挑一个喜欢的场地或套餐，第一笔订单就从那里开始。</p>
        <button class="act" @click="router.push('/services')">去看看服务</button>
      </div>

      <div v-else class="list">
        <article v-for="o in orders" :key="o.id" class="order">
          <div class="order-thumb">
            <ServiceCover :service="{ id: o.serviceId, categoryId: svcCategory(o.serviceId) }" ratio="1 / 1" :seed="o.serviceId" />
          </div>
          <div class="order-main">
            <div class="order-line">
              <h2 class="order-name">{{ svcName(o.serviceId) }}</h2>
              <span class="pill" :class="'pill--' + toneOf(o.status)">{{ statusLabel(o.status) }}</span>
            </div>
            <p class="order-meta">
              <span>{{ merchantName(o.merchantId) }}</span>
              <span class="dot">·</span>
              <span>婚礼日期 {{ o.weddingDate || '待定' }}</span>
              <span class="dot">·</span>
              <span>{{ o.orderNo }}</span>
            </p>
            <p class="order-sub">下单于 {{ fmt(o.createdAt) }}<template v-if="o.remark">　备注：{{ o.remark }}</template></p>
          </div>
          <div class="order-side">
            <p class="order-amount" :class="{ 'order-amount--off': o.status === 'CANCELLED' }">{{ money(o.amount) }}</p>
            <div class="order-acts">
              <button class="act" @click="openDetail(o)">详情</button>
              <button v-if="o.status === 'PENDING'" class="act act--muted" @click="openCancel(o)">取消</button>
              <button v-if="o.status === 'COMPLETED' && evaluated(o)" class="act act--done" disabled>已评价</button>
              <button v-else-if="o.status === 'COMPLETED'" class="act act--rose" @click="openEvaluate(o)">评价</button>
            </div>
          </div>
        </article>
      </div>

      <div v-if="!loading && total > size" class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="onPage"
        />
      </div>
    </template>

    <!-- ===== 详情态：页内全屏视图 ===== -->
    <article v-else class="detail">
      <button class="back" @click="closeDetail">
        <el-icon class="back-ico"><ArrowLeft /></el-icon> 返回订单列表
      </button>

      <header class="detail-head">
        <div class="detail-title-row">
          <h1 class="detail-no">{{ detail.order.orderNo }}</h1>
          <span class="pill" :class="'pill--' + toneOf(detail.order.status)">{{ statusLabel(detail.order.status) }}</span>
        </div>
        <p class="detail-meta">
          <span>{{ svcName(detail.order.serviceId) }}</span>
          <span class="dot">·</span>
          <span>{{ merchantName(detail.order.merchantId) }}</span>
          <span class="dot">·</span>
          <span>婚礼日期 {{ detail.order.weddingDate || '待定' }}</span>
        </p>
      </header>

      <section class="block">
        <h2 class="block-title">订单信息</h2>
        <dl class="kv">
          <div class="kv-row"><dt>服务</dt><dd>{{ svcName(detail.order.serviceId) }}</dd></div>
          <div class="kv-row"><dt>商家</dt><dd>{{ merchantName(detail.order.merchantId) }}</dd></div>
          <div class="kv-row"><dt>金额</dt><dd class="amount" :class="{ 'amount--off': detail.order.status === 'CANCELLED' }">{{ money(detail.order.amount) }}</dd></div>
          <div class="kv-row"><dt>数量</dt><dd>{{ detail.order.quantity || '—' }}</dd></div>
          <div class="kv-row"><dt>婚礼日期</dt><dd>{{ detail.order.weddingDate || '待定' }}</dd></div>
          <div class="kv-row"><dt>下单时间</dt><dd>{{ fmt(detail.order.createdAt) }}</dd></div>
          <div class="kv-row"><dt>备注</dt><dd>{{ detail.order.remark || '—' }}</dd></div>
          <div v-if="detail.order.afterSaleNote" class="kv-row"><dt>售后处理</dt><dd>{{ detail.order.afterSaleNote }}</dd></div>
        </dl>
      </section>

      <section class="block">
        <h2 class="block-title">状态流转</h2>
        <ol v-if="detail.logs && detail.logs.length" class="timeline">
          <li v-for="l in detail.logs" :key="l.id" class="tl-item">
            <p class="tl-title">{{ statusLabel(l.fromStatus) }} → {{ statusLabel(l.toStatus) }}</p>
            <p class="tl-text">{{ roleLabel(l.operatorRole) }} · {{ l.remark || '状态变更' }}</p>
            <p class="tl-time">{{ fmt(l.createdAt) }}</p>
          </li>
        </ol>
        <p v-else class="block-empty">暂无流转记录</p>
      </section>
    </article>

    <!-- ===== 短表单弹窗：仍用 el-dialog ===== -->
    <el-dialog v-model="cancelVisible" title="取消订单" width="420px">
      <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="取消原因(可选)" />
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" @click="doCancel" :loading="acting">确认取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="evaluateVisible" title="评价服务" width="520px">
      <el-form label-width="70px">
        <el-form-item label="评分"><el-rate v-model="evalForm.score" /></el-form-item>
        <el-form-item label="评价"><el-input v-model="evalForm.content" type="textarea" :rows="3" placeholder="说说你的体验" /></el-form-item>
        <el-form-item label="图片">
          <div class="eval-up">
            <input
              ref="imgInput"
              class="eval-input"
              type="file"
              multiple
              accept=".jpg,.jpeg,.png,.webp,.gif"
              @change="onPickImages"
            />
            <div v-if="evalImages.length || imgUploading" class="eval-grid">
              <div v-for="(u, i) in evalImages" :key="u" class="eval-cell">
                <img class="eval-thumb" :src="u" :alt="`评价图片 ${i + 1}`" />
                <button class="eval-del" type="button" :aria-label="`移除第 ${i + 1} 张图片`" @click="removeImage(i)">
                  <el-icon><Close /></el-icon>
                </button>
              </div>
              <div v-if="imgUploading" class="eval-cell eval-cell--busy">
                <span class="eval-busy">上传中</span>
              </div>
            </div>
            <el-button size="small" :loading="imgUploading" :disabled="evalImages.length >= MAX_IMAGES" @click="pickImages">
              添加图片（{{ evalImages.length }}/{{ MAX_IMAGES }}）
            </el-button>
            <p class="eval-tip">jpg / jpeg / png / webp / gif，单张不超过 5MB</p>
          </div>
        </el-form-item>
        <el-form-item label="视频">
          <div class="eval-up">
            <input ref="videoInput" class="eval-input" type="file" accept=".mp4,.webm,.mov" @change="onPickVideo" />
            <div v-if="evalVideoUrl" class="eval-file">
              <span class="eval-file-name">{{ evalVideoName || '已上传视频' }}</span>
              <button class="eval-file-del" type="button" @click="removeVideo">移除</button>
            </div>
            <el-button v-else size="small" :loading="videoUploading" @click="pickVideo">
              {{ videoUploading ? '上传中' : '添加视频' }}
            </el-button>
            <p class="eval-tip">mp4 / webm / mov，不超过 50MB</p>
          </div>
        </el-form-item>
        <el-form-item label="匿名">
          <div class="eval-anon">
            <el-switch v-model="evalForm.anonymous" />
            <span class="eval-anon-text">匿名展示（门户显示为匿名用户）</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeEvaluate">返回</el-button>
        <el-button type="primary" @click="doEvaluate" :loading="acting">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Close } from '@element-plus/icons-vue'
import http, { notifyError } from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'

// 状态中文与标签色：绿=已完成、玫瑰金=进行中、黄=待确认、灰=已取消
const STATUS = {
  PENDING: { label: '待审核', tone: 'warn' },
  CONFIRMED: { label: '已确认', tone: 'rose' },
  IN_PROGRESS: { label: '履约中', tone: 'rose' },
  COMPLETED: { label: '已完成', tone: 'green' },
  CANCELLED: { label: '已取消', tone: 'mute' }
}
const ROLE = { ADMIN: '管理员', MERCHANT: '商家', USER: '用户', SYSTEM: '系统' }

const router = useRouter()

const orders = ref([])
const loading = ref(false)
const error = ref(false)
const status = ref('')
const page = ref(0)
const size = ref(10)
const total = ref(0)

const detail = ref(null)
const savedScroll = ref(0)

const cancelVisible = ref(false)
const cancelReason = ref('')
const cancelId = ref(null)

const evaluateVisible = ref(false)
const evalForm = reactive({ score: 5, content: '', anonymous: false })
const evalId = ref(null)

// 评价附件：只存已上传成功的地址；上传中的文件不计入提交内容
const MAX_IMAGES = 9
const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const MAX_VIDEO_BYTES = 50 * 1024 * 1024
const IMAGE_EXT = /\.(jpe?g|png|webp|gif)$/i
const VIDEO_EXT = /\.(mp4|webm|mov)$/i
const imgInput = ref(null)
const videoInput = ref(null)
const evalImages = ref([])
const evalVideoUrl = ref('')
const evalVideoName = ref('')
const imgUploading = ref(false)
const videoUploading = ref(false)

// 名称字典：/orders 只返回 serviceId / merchantId，一次性取列表建映射，避免逐行请求
const svcMap = ref({})
const merMap = ref({})
// 已评价标记：直接取 /orders 响应里的 evaluated（{"<orderId>": true}），不再单独拉全表比对
const evalMap = ref({})

function statusLabel(s) { return s ? (STATUS[s]?.label || s) : '创建' }
function toneOf(s) { return STATUS[s]?.tone || 'mute' }
function roleLabel(r) { return ROLE[r] || r || '系统' }
function svcName(id) { return svcMap.value[id]?.name || `服务 #${id}` }
function svcCategory(id) { return svcMap.value[id]?.categoryId }
function merchantName(id) { return merMap.value[id] || `商家 #${id}` }
function evaluated(o) { return !!evalMap.value[o.id] }

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

// 金额统一千分位：整数不带小数，有角分才保留两位（与门户 Home / ServiceList 的写法一致）
function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

async function loadNames() {
  try {
    const merchants = await http.get('/merchants')
    const map = {}
    for (const m of merchants || []) map[m.id] = m.shopName || m.name || `商家 #${m.id}`
    merMap.value = map
  } catch (e) { /* 名称补不上不影响订单本体，占位文案兜底 */ }
  try {
    const data = await http.get('/services/search', { params: { page: 0, size: 100 } })
    const map = {}
    for (const s of data?.list || []) map[s.id] = { name: s.name, categoryId: s.categoryId }
    svcMap.value = map
  } catch (e) { /* 同上 */ }
}

// 服务名兜底：/services/search 只回"在架且过审"的服务，已下架的服务取不到名字，
// 订单卡就会显示「服务 #131071」。这里按 id 回源 /services/{id} 逐个补齐，
// 补不到（服务真被删了）就继续用占位文案，不影响列表渲染。
async function fillMissingServiceNames(rows) {
  const ids = [...new Set((rows || []).map(o => o.serviceId).filter(id => id && !svcMap.value[id]))]
  if (!ids.length) return
  const map = { ...svcMap.value }
  await Promise.all(ids.map(async id => {
    try {
      const data = await http.get(`/services/${id}`)
      if (data?.service) map[id] = { name: data.service.name, categoryId: data.service.categoryId }
    } catch (e) { /* 拿不到就保留「服务 #id」 */ }
  }))
  svcMap.value = map
}

// /orders 现在多带一个 evaluated（{"<orderId>": true}）。老后端没有这个键时按「当页全部未评价」降级：
// 不报错、不白屏，按钮回落成「评价」，重复提交仍由后端 existsByOrderId 拦住。
function applyEvaluated(data) {
  const map = data && data.evaluated
  evalMap.value = (map && typeof map === 'object') ? map : {}
}

async function load() {
  loading.value = true
  error.value = false
  try {
    // 服务端分页:返回 {list, total, totalPages, page, size}
    const params = { page: page.value, size: size.value }
    if (status.value) params.status = status.value
    const data = await http.get('/orders', { params })
    orders.value = data.list || []
    total.value = data.total || 0
    applyEvaluated(data)
    fillMissingServiceNames(orders.value)
  } catch (e) {
    orders.value = []
    total.value = 0
    error.value = true
  } finally {
    loading.value = false
  }
}

function setStatus(k) { status.value = k; page.value = 0; load() }
function onPage(p) { page.value = p - 1; load() }

// 详情改为页内全屏视图：返回时把滚动位置放回原处
async function openDetail(row) {
  savedScroll.value = window.scrollY || 0
  detail.value = await http.get(`/orders/${row.id}`)
  fillMissingServiceNames([detail.value.order])
  window.scrollTo(0, 0)
}
function restoreScroll() {
  const y = savedScroll.value
  const put = () => window.scrollTo(0, y)
  nextTick(() => { put(); requestAnimationFrame(put); setTimeout(put, 80) })
}
function closeDetail() {
  detail.value = null
  restoreScroll()
}

function openCancel(row) { cancelId.value = row.id; cancelReason.value = ''; cancelVisible.value = true }
const acting = ref(false)

async function doCancel() {
  if (acting.value) return
  acting.value = true
  try {
    await http.post(`/orders/${cancelId.value}/cancel`, null, { params: cancelReason.value ? { reason: cancelReason.value } : {} })
    ElMessage.success('订单已取消')
    cancelVisible.value = false
    load()
  } finally {
    acting.value = false
  }
}

function resetEvalDraft() {
  evalForm.score = 5
  evalForm.content = ''
  evalForm.anonymous = false
  evalImages.value = []
  evalVideoUrl.value = ''
  evalVideoName.value = ''
}
function openEvaluate(row) { evalId.value = row.id; resetEvalDraft(); evaluateVisible.value = true }
function closeEvaluate() { evaluateVisible.value = false }
function pickImages() { imgInput.value?.click() }
function pickVideo() { videoInput.value?.click() }
function removeImage(i) { evalImages.value.splice(i, 1) }
function removeVideo() { evalVideoUrl.value = ''; evalVideoName.value = '' }

// 走 http 实例：token 由拦截器注入，失败提示也归拦截器，这里用 notifyError 只补一条兜底，避免弹两次
async function onPickImages(e) {
  const files = Array.from(e.target.files || [])
  e.target.value = ''                            // 清空才能再次选中同一张
  if (!files.length || imgUploading.value) return
  if (evalImages.value.length + files.length > MAX_IMAGES) {
    ElMessage.warning(`评价最多 ${MAX_IMAGES} 张图片`)
    return
  }
  imgUploading.value = true
  try {
    for (const file of files) {
      if (!IMAGE_EXT.test(file.name)) { ElMessage.warning('只支持 jpg / jpeg / png / webp / gif 图片'); continue }
      if (file.size > MAX_IMAGE_BYTES) { ElMessage.warning(`「${file.name}」超过 5MB，已跳过`); continue }
      const fd = new FormData()
      fd.append('file', file)
      try {
        const data = await http.post('/files/upload', fd)
        if (data?.url) evalImages.value.push(data.url)
        else ElMessage.error('图片上传失败，请重试')
      } catch (err) {
        notifyError(err, '图片上传失败，请重试')    // 失败的那张不进列表，直接重选
      }
    }
  } finally {
    imgUploading.value = false
  }
}

async function onPickVideo(e) {
  const file = (e.target.files || [])[0]
  e.target.value = ''
  if (!file || videoUploading.value) return
  if (!VIDEO_EXT.test(file.name)) { ElMessage.warning('只支持 mp4 / webm / mov 视频'); return }
  if (file.size > MAX_VIDEO_BYTES) { ElMessage.warning('视频大小不能超过 50MB'); return }
  videoUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    // 视频体积大，15s 默认超时不够（与 merchant/Services 的上传写法一致）
    const data = await http.post('/files/upload', fd, { timeout: 120000 })
    if (data?.url) {
      evalVideoUrl.value = data.url
      evalVideoName.value = file.name
    } else {
      ElMessage.error('视频上传失败，请重试')
    }
  } catch (err) {
    evalVideoUrl.value = ''
    evalVideoName.value = ''
    notifyError(err, '视频上传失败，请重试')
  } finally {
    videoUploading.value = false
  }
}

async function doEvaluate() {
  if (acting.value) return
  if (imgUploading.value || videoUploading.value) { ElMessage.warning('附件还在上传，请稍候再提交'); return }
  acting.value = true
  try {
    await http.post('/evaluations', {
      orderId: evalId.value,
      score: evalForm.score,
      content: evalForm.content,
      images: evalImages.value.join(','),
      videoUrl: evalVideoUrl.value || null,
      anonymous: !!evalForm.anonymous
    })
    ElMessage.success('评价已提交,等待审核')
    evaluateVisible.value = false
    resetEvalDraft()
    load()
  } finally {
    acting.value = false
  }
}

onMounted(() => { loadNames(); load() })
</script>

<style scoped>
.page {
  padding-bottom: var(--space-7);
  /* Element 占位符默认 #9A868D（3.40:1），按 AA 提到 ink-soft */
  --el-text-color-placeholder: var(--ink-soft);
}

/* ===== 页头 / 分段 / 计数 ===== */
.masthead { padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.mast-title { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.mast-sub { max-width: 46ch; margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.seg { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-5); margin-top: var(--space-5); }
.seg-btn {
  position: relative;
  height: var(--space-7);
  padding: 0;
  border: 0;
  background: none;
  font-family: var(--font-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
  cursor: pointer;
  transition: color .2s ease;
}
.seg-btn::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--rose-500);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform .2s ease;
}
.seg-btn:hover { color: var(--rose-500); }
.seg-btn.is-on { color: var(--ink); font-weight: 600; }
.seg-btn.is-on::after { transform: scaleX(1); }

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

/* ===== 订单卡片行 ===== */
.list { display: flex; flex-direction: column; gap: var(--space-4); }
.order {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: box-shadow .3s ease, border-color .3s ease;
}
.order:hover { box-shadow: var(--shadow); border-color: var(--rose-200); }
.order-thumb { border-radius: var(--radius); overflow: hidden; line-height: 0; }
.order-main { min-width: 0; }
.order-line { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); }
.order-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--fs-lg);
  line-height: var(--lh-ui);
  font-weight: 600;
}
.order-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-2) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
}
.order-sub { margin: var(--space-1) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.dot { color: var(--ink-soft); }
.order-side { display: flex; flex-direction: column; align-items: flex-end; gap: var(--space-3); }
.order-amount { margin: 0; color: var(--rose-500); font-family: var(--font-display); font-size: var(--fs-lg); font-weight: 600; line-height: 1; }
.order-amount--off { color: var(--ink-soft); }
.kv-row dd.amount { color: var(--rose-500); font-family: var(--font-display); font-size: var(--fs-base); font-weight: 600; }
.kv-row dd.amount--off { color: var(--ink-soft); }
.order-acts { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: var(--space-2); }

/* ===== 统一标签色 ===== */
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

/* ===== 按钮 ===== */
.act {
  height: var(--space-6);
  padding: 0 var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--ink);
  font-family: var(--font-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  cursor: pointer;
  transition: border-color .2s ease, color .2s ease;
}
.act:hover { border-color: var(--rose-500); color: var(--rose-500); }
.act--rose { border-color: var(--rose-200); color: var(--rose-500); }
.act--muted { color: var(--ink-soft); }
/* 已评价：只作状态标识，不构成可点动作 */
.act--done { color: var(--ink-mute, var(--ink-soft)); border-style: dashed; cursor: default; opacity: .75; }
.act--done:hover { border-color: var(--border); color: var(--ink-mute, var(--ink-soft)); }

/* ===== 空 / 错误 / 加载 ===== */
.state {
  padding: var(--space-8) var(--space-6);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  text-align: center;
}
.state-ico { margin: 0; font-family: var(--font-display); font-size: var(--fs-xl); line-height: 1; color: var(--ink-soft); }
.state-title { margin: var(--space-4) 0 0; font-size: var(--fs-base); font-weight: 600; line-height: var(--lh-ui); }
.state-desc { max-width: 40ch; margin: var(--space-2) auto 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.sk-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}
.sk-thumb { aspect-ratio: 1 / 1; border-radius: var(--radius); background: var(--bg-deep); }
.sk-body { display: flex; flex-direction: column; gap: var(--space-3); }
.sk-line { height: var(--space-3); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 46%; }
.sk-line--short { width: 30%; }
.sk-thumb, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-thumb, .sk-line { animation: none; }
}

.pager { display: flex; justify-content: center; margin-top: var(--space-6); }
.pager :deep(.el-pager li),
.pager :deep(.btn-prev),
.pager :deep(.btn-next) { background: var(--bg); color: var(--ink-soft); font-size: var(--fs-sm); border-radius: var(--radius-sm); }
.pager :deep(.el-pager li.is-active) { background: var(--rose-500); color: #fff; }
.pager :deep(.el-pagination__total) { color: var(--ink-soft); font-size: var(--fs-sm); }

/* ===== 详情态 ===== */
.detail { padding-bottom: var(--space-7); }
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
.detail-head { margin-top: var(--space-5); padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.detail-title-row { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-4); }
.detail-no { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-3) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}

.block {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}
.block-title { font-size: var(--fs-lg); line-height: var(--lh-ui); font-weight: 600; }
.block-empty { margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.kv { margin: var(--space-4) 0 0; }
.kv-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: var(--space-4);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border);
}
.kv-row:last-child { border-bottom: 0; padding-bottom: 0; }
.kv-row dt { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.kv-row dd { margin: 0; color: var(--ink); font-size: var(--fs-sm); line-height: var(--lh-body); word-break: break-word; }

.timeline { margin: var(--space-4) 0 0; padding: 0; list-style: none; }
.tl-item { position: relative; padding: 0 0 var(--space-4) var(--space-5); border-left: 1px solid var(--border); }
.tl-item:last-child { padding-bottom: 0; border-left-color: transparent; }
.tl-item::before {
  content: '';
  position: absolute;
  left: -3px;
  top: var(--space-1);
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--rose-500);
  opacity: .4;
}
.tl-item:first-child::before { background: var(--rose-500); opacity: 1; }
.tl-title { margin: 0; font-size: var(--fs-sm); font-weight: 600; line-height: var(--lh-ui); }
.tl-text { margin: var(--space-1) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.tl-time { margin: var(--space-1) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

/* ===== 评价弹窗：图片 / 视频 / 匿名 ===== */
.eval-up { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); width: 100%; }
/* 原生 file input 只作触发口，视觉交给 el-button */
.eval-input { display: none; }

.eval-grid { display: flex; flex-wrap: wrap; gap: var(--space-2); width: 100%; }
.eval-cell {
  position: relative;
  width: var(--space-8);
  height: var(--space-8);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--bg-deep);
}
.eval-thumb { display: block; width: 100%; height: 100%; object-fit: cover; }
.eval-del {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--space-5);
  height: var(--space-5);
  padding: 0;
  border: 0;
  border-radius: 0 0 0 var(--radius-sm);
  background: rgba(0, 0, 0, .45);
  color: #fff;
  font-size: var(--fs-xs);
  line-height: 1;
  cursor: pointer;
  transition: background .2s ease;
}
.eval-del:hover { background: var(--rose-500); }
.eval-cell--busy {
  display: flex;
  align-items: center;
  justify-content: center;
  border-style: dashed;
}
.eval-busy { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

.eval-file { display: flex; align-items: center; gap: var(--space-3); min-width: 0; }
.eval-file-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--ink);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.eval-file-del {
  flex: none;
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
.eval-file-del:hover { color: var(--rose-500); }

.eval-tip { width: 100%; margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.eval-anon { display: flex; align-items: center; gap: var(--space-3); }
.eval-anon-text { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }

:deep(.el-dialog__title) { font-family: var(--font-display); font-size: var(--fs-lg); }

@media (max-width: 820px) {
  .order { grid-template-columns: 72px minmax(0, 1fr); align-items: start; }
  .order-side { grid-column: 1 / -1; flex-direction: row; align-items: center; justify-content: space-between; }
  .order-acts { justify-content: flex-end; }
}
@media (max-width: 560px) {
  .kv-row { grid-template-columns: minmax(0, 1fr); gap: var(--space-1); }
  .order-name { font-size: var(--fs-base); }
}
</style>
