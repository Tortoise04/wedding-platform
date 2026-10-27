<template>
  <div class="mch-page dd-page">
    <button type="button" class="mch-back" @click="goBack">← 返回争议列表</button>

    <section v-if="loading" class="mch-panel dd-skel">
      <div class="sk-lines"><i v-for="i in 5" :key="i" /></div>
    </section>

    <section v-else-if="!dispute" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">无法加载争议详情</p>
        <p class="mch-state-desc">争议可能已被删除，或本店不是该争议的当事方。</p>
        <el-button size="small" @click="loadAll">重试</el-button>
      </div>
    </section>

    <template v-else>
      <header class="dd-head">
        <div class="dd-head-line">
          <h1 class="dd-no mch-mono">{{ dispute.disputeNo }}</h1>
          <span class="mch-tag" :class="statusMeta.tone">{{ statusMeta.label }}</span>
        </div>
        <p class="mch-sub">
          关联订单 {{ dispute.orderNo || ('#' + dispute.orderId) }} · 发起于 {{ fmt(dispute.createdAt) }}
          <template v-if="dispute.acceptedAt"> · 受理于 {{ fmt(dispute.acceptedAt) }}</template>
          <template v-if="dispute.finishedAt"> · 裁决于 {{ fmt(dispute.finishedAt) }}</template>
        </p>
        <div class="dd-reason">
          <p class="dd-reason-cap">投诉诉求</p>
          <p class="dd-reason-text">{{ dispute.reason || '投诉人未填写诉求' }}</p>
        </div>
      </header>

      <section class="mch-panel">
        <div class="mch-panel-head">
          <h2 class="mch-panel-title">举证过程</h2>
          <p class="dd-hd-sub">按时间顺序排列，本店一方在右，投诉方在左</p>
        </div>
        <div class="mch-panel-body dd-stream">
          <div class="node">
            <div class="node-card">
              <p class="node-title">用户投诉本店</p>
              <p class="node-text">{{ dispute.reason || '投诉人未填写诉求' }}</p>
              <p class="node-time">{{ fmt(dispute.createdAt) }}</p>
            </div>
          </div>

          <div v-if="dispute.acceptedAt" class="node">
            <div class="node-card">
              <p class="node-title">管理员已受理，进入举证环节</p>
              <p class="node-time">{{ fmt(dispute.acceptedAt) }}</p>
            </div>
          </div>

          <div v-for="e in evidences" :key="e.id" class="msg" :class="e.submitterRole === 'MERCHANT' ? 'mine' : 'other'">
            <div class="bubble">
              <p v-if="e.content" class="bubble-text">{{ e.content }}</p>
              <el-image v-if="isImageUrl(e.fileUrl)" :src="e.fileUrl" :preview-src-list="[e.fileUrl]"
                :initial-index="0" fit="cover" class="thumb" preview-teleported />
              <a v-else-if="e.fileUrl" :href="e.fileUrl" target="_blank" rel="noopener" class="file-link">查看附件</a>
            </div>
            <p class="bubble-meta">{{ roleLabel(e.submitterRole) }} · {{ evidenceTypeLabel(e.evidenceType) }} · {{ fmtShort(e.createdAt) }}</p>
          </div>

          <p v-if="!evidences.length" class="stream-tip">双方暂未提交举证材料</p>

          <div v-if="evidenceEnded" class="node">
            <div class="node-card">
              <p class="node-title">管理员已结束举证</p>
            </div>
          </div>

          <div v-if="dispute.status === 'FINISHED'" class="node">
            <div class="node-card verdict">
              <p class="node-title">裁决结果：{{ resultTypeLabel(dispute.resultType) }}</p>
              <p v-if="dispute.result" class="node-text">{{ dispute.result }}</p>
              <p v-if="hasRefund" class="node-refund">退款金额 {{ money(dispute.refundAmount) }}</p>
              <p class="node-time">{{ fmt(dispute.finishedAt) }}</p>
            </div>
          </div>

          <div v-if="dispute.status === 'CLOSED' && dispute.result" class="node">
            <div class="node-card rejected">
              <p class="node-title">不予受理</p>
              <p class="node-text">{{ dispute.result }}</p>
            </div>
          </div>
        </div>
      </section>

      <section class="mch-panel">
        <div class="mch-panel-head">
          <h2 class="mch-panel-title">提交举证材料</h2>
        </div>
        <div class="mch-panel-body">
          <template v-if="dispute.status === 'PROCESSING'">
            <el-input v-model="draft" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写要对本次投诉说明的情况…" />
            <div class="composer-foot">
              <div class="attach">
                <div v-if="uploadedUrl" class="preview">
                  <el-image v-if="isImageUrl(uploadedUrl)" :src="uploadedUrl" fit="cover" class="thumb" />
                  <a v-else :href="uploadedUrl" target="_blank" rel="noopener" class="file-link">查看附件</a>
                  <button type="button" class="mch-act is-muted" @click="uploadedUrl = ''">移除</button>
                </div>
                <el-upload v-else :show-file-list="false" :http-request="uploadFile"
                  accept=".jpg,.jpeg,.png,.webp,.gif,.pdf">
                  <el-button :loading="uploading">上传附件</el-button>
                </el-upload>
              </div>
              <el-button type="primary" :loading="sending" @click="send">发送</el-button>
            </div>
            <p class="composer-hint">支持 jpg/png/webp/gif/pdf，单个不超过 5MB；文字与附件至少填一项。</p>
          </template>
          <p v-else class="composer-locked">当前不在举证环节，无法继续提交材料。</p>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

// 枚举中文与标签色:接口只给英文码,页面上不允许出现英文
const STATUS = {
  PENDING: { label: '待受理', tone: 'is-wait' },
  PROCESSING: { label: '举证中', tone: 'is-doing' },
  ARBITRATING: { label: '仲裁中', tone: 'is-doing' },
  FINISHED: { label: '已裁决', tone: 'is-ok' },
  CLOSED: { label: '已关闭', tone: 'is-off' }
}
const RESULT_TYPE = { REFUND: '退款', CLOSE_ORDER: '关闭订单', PENALTY: '处罚商家', REJECT: '驳回诉求' }
const EVIDENCE_TYPE = { TEXT: '文字', IMAGE: '图片', MIXED: '图文' }
// 商家视角标注角色:自己一方标"商家(本店)",让对方一眼看出哪条是我方材料
const ROLE = { USER: '用户(投诉方)', MERCHANT: '商家(本店)' }
// 结束举证这个动作后端没有存时间戳,所以节点只给文案不给时间,也不许用别的字段顶替

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

const statusMeta = computed(() => STATUS[dispute.value?.status] || { label: '状态未知', tone: 'is-off' })
// 后端没存"结束举证"的时间,只能用状态推断这一步是否发生过
const evidenceEnded = computed(() => ['ARBITRATING', 'FINISHED', 'CLOSED'].includes(dispute.value?.status))
const hasRefund = computed(() => {
  const v = dispute.value?.refundAmount
  return v !== null && v !== undefined && v !== ''
})

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

// 气泡下方空间小,只留「月-日 时:分」
function fmtShort(t) {
  return fmt(t).slice(5)
}

// 只有这些后缀按图片预览,其余(如 pdf)一律走"查看附件"链接
function isImageUrl(url) {
  return /\.(jpe?g|png|webp|gif)$/i.test(String(url || '').split('?')[0])
}

function roleLabel(role) {
  return ROLE[role] || '未知'
}

function evidenceTypeLabel(type) {
  return EVIDENCE_TYPE[type] || '未知'
}

function resultTypeLabel(type) {
  return RESULT_TYPE[type] || '未知结果'
}

// 两个接口并发拉,单个失败只让对应区块走错误态:拦截器已弹过错误,这里不再重复提示,也不能白屏
async function loadAll() {
  loading.value = true
  try {
    const [d, list] = await Promise.all([
      http.get(`/disputes/${disputeId.value}`).catch(() => null),
      http.get(`/disputes/${disputeId.value}/evidences`).catch(() => [])
    ])
    dispute.value = d
    evidences.value = list || []
    draft.value = ''
    uploadedUrl.value = ''
  } finally {
    loading.value = false
  }
}

// 发送后只重拉举证列表,让新气泡由服务端数据渲染,不在前端自造
async function loadEvidences() {
  evidences.value = (await http.get(`/disputes/${disputeId.value}/evidences`)) || []
}

// 走 http 实例而不是裸 axios,才能拿到拦截器的 token 与统一错误提示
async function uploadFile(options) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    uploadedUrl.value = data.url
  } catch {
    // 拦截器已提示失败原因,保留输入内容方便重试
  } finally {
    uploading.value = false
  }
}

async function send() {
  const text = draft.value.trim()
  if (!text && !uploadedUrl.value) { ElMessage.warning('请填写文字说明或上传附件'); return }
  if (uploading.value) { ElMessage.warning('附件还在上传,请稍候'); return }
  sending.value = true
  try {
    // 空项传 null:服务端按非空项推导 evidenceType
    await http.post(`/disputes/${disputeId.value}/evidence`, { content: text || null, fileUrl: uploadedUrl.value || null })
    draft.value = ''
    uploadedUrl.value = ''
    ElMessage.success('举证已提交')
    await loadEvidences()
  } catch {
    // 拦截器已提示失败原因,不清空输入,用户可改后重发
  } finally {
    sending.value = false
  }
}

function goBack() {
  router.push('/merchant/disputes')
}

// 同一路由只改 :id 时组件会被复用、不会重新挂载,必须监听参数变化重拉数据
watch(() => route.params.id, loadAll)

onMounted(loadAll)
</script>

<style scoped>
.dd-page {
  max-width: 880px;
}
.dd-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.dd-head-line {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.dd-no {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  color: var(--ink);
}
.dd-reason {
  margin-top: var(--space-1);
  padding: var(--space-3) var(--space-4);
  background: var(--bg-deep);
  border-radius: var(--radius);
}
.dd-reason-cap {
  margin: 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.dd-reason-text {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-base);
  line-height: var(--lh-body);
  color: var(--ink);
  white-space: pre-wrap;
  word-break: break-word;
}
.dd-hd-sub {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.dd-stream {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.stream-tip {
  margin: 0 auto;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.node {
  display: flex;
  justify-content: center;
}
.node-card {
  max-width: 78%;
  padding: var(--space-3) var(--space-4);
  text-align: center;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.node-title {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--ink);
}
.node-text {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink-soft);
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}
.node-time {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.node-refund {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--rose-600);
}
.node-card.verdict {
  background: var(--surface);
  border-color: var(--rose-200);
}
.node-card.rejected {
  background: var(--rose-50);
  border-color: var(--rose-100);
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
  max-width: 46ch;
  padding: var(--space-3) var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}
.msg.mine .bubble {
  background: var(--rose-500);
  border-color: var(--rose-500);
  color: var(--surface);
}
.bubble-text {
  margin: 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  white-space: pre-wrap;
  word-break: break-word;
}
.thumb {
  display: block;
  width: 132px;
  height: 132px;
  border-radius: var(--radius-sm);
  cursor: zoom-in;
}
.bubble-text + .thumb,
.bubble-text + .file-link {
  margin-top: var(--space-2);
}
.file-link {
  font-size: var(--fs-sm);
  color: var(--rose-600);
  text-decoration: underline;
}
.msg.mine .file-link {
  color: var(--surface);
}
.bubble-meta {
  margin: 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.composer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-3);
}
.preview {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.preview .thumb {
  width: 64px;
  height: 64px;
  cursor: default;
}
.composer-hint {
  margin: var(--space-2) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.composer-locked {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.dd-skel {
  padding: var(--space-5);
}
.sk-lines {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.sk-lines i {
  display: block;
  height: var(--space-6);
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
  .sk-lines i {
    animation: none;
  }
}
</style>
