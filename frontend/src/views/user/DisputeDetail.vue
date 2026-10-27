<template>
  <div class="page">
    <button class="back" @click="goBack">
      <el-icon class="back-ico"><ArrowLeft /></el-icon> 返回争议列表
    </button>

    <div v-if="loading" class="sk-wrap">
      <div class="sk-line sk-line--title"></div>
      <div class="sk-line"></div>
      <div class="sk-line sk-line--short"></div>
    </div>

    <template v-else-if="dispute">
      <header class="head">
        <div class="head-line">
          <h1 class="head-no">{{ dispute.disputeNo }}</h1>
          <span class="pill" :class="'pill--' + statusMeta.tone">{{ statusMeta.label }}</span>
        </div>
        <p class="head-meta">
          <span>关联订单 {{ dispute.orderNo || ('#' + dispute.orderId) }}</span>
          <span class="dot">·</span>
          <span>发起于 {{ fmt(dispute.createdAt) }}</span>
        </p>
      </header>

      <section class="block">
        <h2 class="block-title">诉求</h2>
        <p class="body-text">{{ dispute.reason || '发起人未填写诉求' }}</p>
      </section>

      <section v-if="dispute.status === 'FINISHED' || (dispute.status === 'CLOSED' && dispute.result)" class="block">
        <h2 class="block-title">裁决结论</h2>
        <dl class="kv">
          <div class="kv-row">
            <dt>{{ dispute.status === 'FINISHED' ? '裁决结果' : '处理结果' }}</dt>
            <dd>
              <span class="pill" :class="dispute.status === 'FINISHED' ? 'pill--green' : 'pill--mute'">
                {{ dispute.status === 'FINISHED' ? resultTypeLabel(dispute.resultType) : '不予受理' }}
              </span>
            </dd>
          </div>
          <div v-if="dispute.result" class="kv-row"><dt>说明</dt><dd>{{ dispute.result }}</dd></div>
          <div v-if="hasRefund" class="kv-row"><dt>退款金额</dt><dd class="amount">{{ money(dispute.refundAmount) }}</dd></div>
          <div v-if="dispute.finishedAt" class="kv-row"><dt>结束时间</dt><dd>{{ fmt(dispute.finishedAt) }}</dd></div>
        </dl>
      </section>

      <section class="block">
        <h2 class="block-title">处理进度</h2>
        <ol class="timeline">
          <li class="tl-item">
            <p class="tl-title">用户发起平台介入</p>
            <p class="tl-time">{{ fmt(dispute.createdAt) }}</p>
          </li>
          <li v-if="dispute.acceptedAt" class="tl-item">
            <p class="tl-title">管理员已受理，进入举证环节</p>
            <p class="tl-time">{{ fmt(dispute.acceptedAt) }}</p>
          </li>
          <li v-if="evidenceEnded" class="tl-item">
            <p class="tl-title">管理员已结束举证</p>
          </li>
          <li v-if="dispute.status === 'FINISHED'" class="tl-item">
            <p class="tl-title">已作出裁决：{{ resultTypeLabel(dispute.resultType) }}</p>
            <p v-if="dispute.finishedAt" class="tl-time">{{ fmt(dispute.finishedAt) }}</p>
          </li>
          <li v-if="dispute.status === 'CLOSED'" class="tl-item">
            <p class="tl-title">争议已关闭</p>
            <p v-if="dispute.finishedAt" class="tl-time">{{ fmt(dispute.finishedAt) }}</p>
          </li>
        </ol>
      </section>

      <section class="block">
        <div class="block-head">
          <h2 class="block-title">举证材料</h2>
          <span class="block-hint">按时间顺序排列，用户一方在右，商家一方在左</span>
        </div>

        <div class="stream">
          <div v-for="e in evidences" :key="e.id" class="msg" :class="e.submitterRole === 'USER' ? 'mine' : 'other'">
            <div class="bubble">
              <p v-if="e.content" class="bubble-text">{{ e.content }}</p>
              <el-image
                v-if="isImageUrl(e.fileUrl)"
                :src="e.fileUrl"
                :preview-src-list="[e.fileUrl]"
                :initial-index="0"
                fit="cover"
                class="thumb"
                preview-teleported
              />
              <a v-else-if="e.fileUrl" :href="e.fileUrl" target="_blank" class="file-link">查看附件</a>
            </div>
            <p class="bubble-meta">{{ roleLabel(e.submitterRole) }} · {{ evidenceTypeLabel(e.evidenceType) }} · {{ fmtShort(e.createdAt) }}</p>
          </div>
          <p v-if="!evidences.length" class="stream-tip">双方暂未提交举证材料</p>
        </div>
      </section>

      <section class="block">
        <h2 class="block-title">提交举证</h2>
        <template v-if="dispute.status === 'PROCESSING'">
          <el-input v-model="draft" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="补充举证材料…" />
          <div class="composer-foot">
            <div class="attach">
              <div v-if="uploadedUrl" class="preview">
                <el-image v-if="isImageUrl(uploadedUrl)" :src="uploadedUrl" fit="cover" class="thumb thumb--sm" />
                <a v-else :href="uploadedUrl" target="_blank" class="file-link">查看附件</a>
                <el-button link type="danger" @click="uploadedUrl = ''">移除</el-button>
              </div>
              <el-upload v-else :show-file-list="false" :http-request="uploadFile" accept=".jpg,.jpeg,.png,.webp,.gif,.pdf">
                <el-button :loading="uploading">上传图片</el-button>
              </el-upload>
            </div>
            <el-button type="primary" :loading="sending" @click="send">发送</el-button>
          </div>
          <p class="composer-hint">支持 jpg/png/webp/gif/pdf，单个不超过 5MB；文字与附件至少填一项</p>
        </template>
        <p v-else class="composer-locked">当前不在举证环节，无法继续提交材料。</p>
      </section>
    </template>

    <div v-else class="state">
      <p class="state-ico">!</p>
      <p class="state-title">争议不存在或无权查看</p>
      <p class="state-desc">这条记录可能已被关闭，或不属于当前账号。</p>
      <button class="act" @click="goBack">返回争议列表</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import http from '@/api/http'

// 枚举中文与标签色：接口只给英文码，页面上不允许出现英文
const STATUS = {
  PENDING: { label: '待受理', tone: 'warn' },
  PROCESSING: { label: '举证中', tone: 'rose' },
  ARBITRATING: { label: '仲裁中', tone: 'rose' },
  FINISHED: { label: '已裁决', tone: 'green' },
  CLOSED: { label: '已关闭', tone: 'mute' }
}
const RESULT_TYPE = { REFUND: '退款', CLOSE_ORDER: '关闭订单', PENALTY: '处罚商家', REJECT: '驳回诉求' }
const EVIDENCE_TYPE = { TEXT: '文字', IMAGE: '图片', MIXED: '图文' }
const ROLE = { USER: '用户', MERCHANT: '商家' }
// 结束举证这个动作后端没有存时间戳，所以节点只给文案不给时间，也不许用别的字段顶替

const route = useRoute()
const router = useRouter()
const disputeId = computed(() => route.params.id)

const dispute = ref(null)
const evidences = ref([])
const loading = ref(false)
const draft = ref('')
const uploadedUrl = ref('')
const uploading = ref(false)
const sending = ref(false)

const statusMeta = computed(() => STATUS[dispute.value?.status] || { label: '状态未知', tone: 'mute' })
// 后端没存"结束举证"的时间，只能用状态推断这一步是否发生过
const evidenceEnded = computed(() => ['ARBITRATING', 'FINISHED', 'CLOSED'].includes(dispute.value?.status))
const hasRefund = computed(() => {
  const v = dispute.value?.refundAmount
  return v !== null && v !== undefined && v !== ''
})

// 时间串可能是 'YYYY-MM-DD HH:mm:ss' 或 ISO，统一截到分钟，不依赖 Date 解析
function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 16)
}

// 气泡下方空间小，只留「月-日 时:分」
function fmtShort(t) { return fmt(t).slice(5) }

// 金额统一千分位：整数不带小数，有角分才保留两位（与门户 Home / ServiceList 的写法一致）
function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

// 只有这些后缀按图片预览，其余（如 pdf）一律走「查看附件」链接
function isImageUrl(url) {
  return /\.(jpe?g|png|webp|gif)$/i.test(String(url || '').split('?')[0])
}

function roleLabel(role) { return ROLE[role] || '未知' }
function evidenceTypeLabel(type) { return EVIDENCE_TYPE[type] || '未知' }
function resultTypeLabel(type) { return RESULT_TYPE[type] || '未知结果' }

// 两个接口并发拉，单个失败只让对应区块走空态：拦截器已弹过错误，这里不再重复提示，也不能白屏
async function loadAll() {
  loading.value = true
  try {
    const [d, list] = await Promise.all([
      http.get(`/disputes/${disputeId.value}`).catch(() => null),
      http.get(`/disputes/${disputeId.value}/evidences`).catch(() => [])
    ])
    dispute.value = d
    evidences.value = list || []
  } finally {
    loading.value = false
  }
}

// 发送后只重拉举证列表，让新气泡由服务端数据渲染，不在前端自造
async function loadEvidences() {
  evidences.value = (await http.get(`/disputes/${disputeId.value}/evidences`)) || []
}

// 走 http 实例而不是裸 axios，才能拿到拦截器的 token 与统一错误提示
async function uploadFile(options) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    uploadedUrl.value = data.url
  } catch {
    // 拦截器已提示失败原因，保留输入内容方便重试
  } finally {
    uploading.value = false
  }
}

async function send() {
  const text = draft.value.trim()
  if (!text && !uploadedUrl.value) { ElMessage.warning('请填写文字说明或上传附件'); return }
  if (uploading.value) { ElMessage.warning('附件还在上传，请稍候'); return }
  sending.value = true
  try {
    // 空项传 null：服务端按非空项推导 evidenceType
    await http.post(`/disputes/${disputeId.value}/evidence`, { content: text || null, fileUrl: uploadedUrl.value || null })
    draft.value = ''
    uploadedUrl.value = ''
    ElMessage.success('举证已提交')
    await loadEvidences()
  } catch {
    // 拦截器已提示失败原因，不清空输入，用户可改后重发
  } finally {
    sending.value = false
  }
}

function goBack() { router.push('/user/disputes') }

// 同一路由只改 :id 时组件会被复用、不会重新挂载，必须监听参数变化重拉数据
watch(() => route.params.id, loadAll)

onMounted(loadAll)
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

.head { margin-top: var(--space-5); padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.head-line { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-4); }
.head-no { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.head-meta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.dot { color: var(--ink-soft); }

.block {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}
.block-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: var(--space-2); }
.block-title { font-size: var(--fs-lg); line-height: var(--lh-ui); font-weight: 600; }
.block-hint { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

.body-text { margin: var(--space-3) 0 0; color: var(--ink); font-size: var(--fs-base); line-height: var(--lh-body); white-space: pre-wrap; word-break: break-word; }

.kv { margin: var(--space-4) 0 0; }
.kv-row { display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: var(--space-4); padding: var(--space-3) 0; border-bottom: 1px solid var(--border); }
.kv-row:last-child { border-bottom: 0; padding-bottom: 0; }
.kv-row dt { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.kv-row dd { margin: 0; color: var(--ink); font-size: var(--fs-sm); line-height: var(--lh-body); word-break: break-word; }
.kv-row dd.amount { color: var(--rose-500); font-family: var(--font-display); font-size: var(--fs-base); font-weight: 600; }

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
.tl-time { margin: var(--space-1) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

/* ===== 举证气泡 ===== */
.stream { display: flex; flex-direction: column; gap: var(--space-4); margin-top: var(--space-4); }
.msg { display: flex; flex-direction: column; gap: var(--space-1); }
.msg.mine { align-items: flex-end; }
.msg.other { align-items: flex-start; }
.bubble {
  max-width: 66%;
  padding: var(--space-3) var(--space-4);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.msg.mine .bubble { background: var(--bg-deep); border-color: var(--rose-200); }
.bubble-text { margin: 0; color: var(--ink); font-size: var(--fs-sm); line-height: var(--lh-body); white-space: pre-wrap; word-break: break-word; }
.thumb { display: block; width: 132px; height: 132px; margin-top: var(--space-2); border-radius: var(--radius-sm); cursor: zoom-in; }
.thumb--sm { width: 64px; height: 64px; margin-top: 0; cursor: default; }
.file-link { display: inline-block; margin-top: var(--space-2); color: var(--rose-500); font-size: var(--fs-sm); line-height: var(--lh-ui); text-decoration: underline; }
.bubble-meta { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.stream-tip { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== 举证表单 ===== */
.composer-foot { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--space-3); margin-top: var(--space-4); }
.attach { display: flex; align-items: center; }
.preview { display: flex; align-items: center; gap: var(--space-3); }
.composer-hint { margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.composer-locked { margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== 空 / 加载 ===== */
.state { padding: var(--space-8) var(--space-6); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); text-align: center; margin-top: var(--space-6); }
.state-ico { margin: 0; font-family: var(--font-display); font-size: var(--fs-xl); line-height: 1; color: var(--ink-soft); }
.state-title { margin: var(--space-4) 0 0; font-size: var(--fs-base); font-weight: 600; line-height: var(--lh-ui); }
.state-desc { max-width: 40ch; margin: var(--space-2) auto 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.act {
  display: inline-flex;
  align-items: center;
  height: var(--space-6);
  margin-top: var(--space-4);
  padding: 0 var(--space-5);
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

.sk-wrap { display: flex; flex-direction: column; gap: var(--space-3); margin-top: var(--space-6); padding: var(--space-5); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); }
.sk-line { height: var(--space-4); border-radius: var(--radius-sm); background: var(--bg-deep); animation: sk-pulse 1.6s ease-in-out infinite; }
.sk-line--title { height: var(--space-6); width: 40%; }
.sk-line--short { width: 28%; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) { .sk-line { animation: none; } }

@media (max-width: 640px) {
  .kv-row { grid-template-columns: minmax(0, 1fr); gap: var(--space-1); }
  .bubble { max-width: 82%; }
}
</style>
