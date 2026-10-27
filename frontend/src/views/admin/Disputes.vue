<template>
  <!-- 页内全屏阅读视图：举证材料，返回时恢复 scrollY -->
  <div v-if="reading" class="adm-page">
    <div class="adm-head">
      <div class="read-id">
        <button type="button" class="back" @click="backToList">← 返回争议列表</button>
        <p class="adm-sub">
          <span class="adm-mono">{{ curRow.disputeNo }}</span> · 订单 {{ curRow.orderNo || ('#' + curRow.orderId) }} · 共 <em>{{ sortedEvidences.length }}</em> 条材料
        </p>
      </div>
      <div class="adm-actions">
        <span class="adm-tag" :class="STATUS[curStatus]?.tag">{{ STATUS[curStatus]?.label }}</span>
        <el-button v-if="curStatus === 'PROCESSING'" size="small" type="primary" @click="finishEvidence">结束举证</el-button>
      </div>
    </div>

    <section class="adm-panel">
      <div class="adm-panel-head">
        <span class="adm-panel-title">举证材料</span>
        <span class="adm-sub">按时间升序 · 用户靠右，商家靠左</span>
      </div>

      <div v-if="evidLoading" class="adm-skel"><i></i><i></i><i></i></div>

      <div v-else-if="evidError" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">材料没能加载</p>
        <p class="adm-state-desc">{{ evidError }}</p>
        <el-button size="small" @click="reloadEvidences">重新加载</el-button>
      </div>

      <div v-else-if="!sortedEvidences.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">暂无举证材料</p>
        <p class="adm-state-desc">双方都还没有提交文字或图片材料。</p>
      </div>

      <div v-else class="chat">
        <div v-for="e in sortedEvidences" :key="e.id" class="row" :class="e.submitterRole === 'USER' ? 'right' : 'left'">
          <div class="bubble">
            <p v-if="isEmptyEvidence(e)" class="empty-tip">（空材料）</p>
            <template v-else>
              <p v-if="e.content" class="content">{{ e.content }}</p>
              <div v-if="e.fileUrl" class="file">
                <el-image v-if="isImageFile(e.fileUrl)" :src="e.fileUrl" :preview-src-list="imageUrls"
                  :initial-index="imageUrls.indexOf(e.fileUrl)" fit="cover" class="thumb"
                  preview-teleported hide-on-click-modal />
                <a v-else :href="e.fileUrl" target="_blank" rel="noopener" class="attach">查看附件</a>
              </div>
            </template>
            <p class="meta">{{ roleLabel(e.submitterRole) }} · {{ typeLabel(e.evidenceType) }} · {{ e.createdAt }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>

  <!-- 列表视图 -->
  <div v-else class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">共 <em>{{ list.length }}</em> 条 · 待受理 <em>{{ pendingCount }}</em> 条</p>
    </div>

    <section class="adm-panel">
      <div class="adm-toolbar">
        <div class="adm-field">
          <span class="adm-label">争议状态</span>
          <el-select v-model="status" size="small" class="sel" placeholder="全部" @change="load">
            <el-option label="全部" value="" />
            <el-option v-for="(s, k) in STATUS" :key="k" :label="s.label" :value="k" />
          </el-select>
        </div>
      </div>

      <div v-if="loading && !list.length" class="adm-skel"><i></i><i></i><i></i><i></i><i></i></div>

      <div v-else-if="error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">争议列表没能加载</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>

      <div v-else-if="!list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">没有匹配的争议单</p>
        <p class="adm-state-desc">换个状态看看，或等待用户发起新的争议。</p>
        <el-button size="small" @click="status = ''; load()">查看全部</el-button>
      </div>

      <el-table v-else v-loading="loading" :data="list">
        <el-table-column label="争议单号" width="200">
          <template #default="s"><span class="adm-mono">{{ s.row.disputeNo }}</span></template>
        </el-table-column>
        <el-table-column label="订单" width="100">
          <template #default="s">{{ s.row.orderNo || ('#' + s.row.orderId) }}</template>
        </el-table-column>
        <el-table-column label="诉求" min-width="280">
          <template #default="s"><div class="adm-two-line">{{ s.row.reason }}</div></template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="s">
            <span class="adm-tag" :class="STATUS[s.row.status]?.tag">{{ STATUS[s.row.status]?.label }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="right">
          <template #default="s">
            <div class="ops">
              <el-button size="small" @click="openEvidences(s.row)">材料</el-button>
              <el-button v-if="s.row.status === 'PENDING'" size="small" type="primary" @click="openAccept(s.row)">受理</el-button>
              <el-button v-if="s.row.status === 'ARBITRATING'" size="small" type="primary" plain @click="openArbitrate(s.row)">裁决</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>

  <!-- 受理判断（短表单，保留弹窗） -->
  <el-dialog v-model="acceptVisible" title="受理判断" width="440px">
    <el-form label-width="80px">
      <el-form-item label="结论">
        <el-radio-group v-model="acceptForm.approved">
          <el-radio :value="true">受理</el-radio>
          <el-radio :value="false">不予受理</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="acceptForm.approved === false" label="原因">
        <el-input v-model="acceptForm.rejectReason" type="textarea" :rows="3" placeholder="写清不予受理的依据" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="acceptVisible = false">取消</el-button>
      <el-button type="primary" @click="doAccept">提交</el-button>
    </template>
  </el-dialog>

  <!-- 裁决（短表单，保留弹窗） -->
  <el-dialog v-model="arbitrateVisible" title="作出裁决" width="480px">
    <el-form label-width="96px">
      <el-form-item label="裁决结论">
        <el-select v-model="arbForm.resultType" class="full">
          <el-option label="退款" value="REFUND" />
          <el-option label="关闭订单" value="CLOSE_ORDER" />
          <el-option label="驳回诉求" value="REJECT" />
          <el-option label="处罚商家" value="PENALTY" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="arbForm.resultType === 'REFUND'" label="退款金额">
        <el-input-number v-model="arbForm.refundAmount" :min="0" :precision="2" class="full" />
      </el-form-item>
      <el-form-item v-if="arbForm.resultType === 'PENALTY'" label="处罚类型">
        <el-select v-model="arbForm.penaltyType" class="full">
          <el-option label="警告" value="WARNING" />
          <el-option label="服务下架" value="SERVICE_OFF" />
          <el-option label="封禁" value="BAN" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="arbForm.resultType === 'PENALTY' && arbForm.penaltyType === 'SERVICE_OFF'" label="目标服务 id">
        <el-input-number v-model="arbForm.targetId" :min="1" class="full" />
      </el-form-item>
      <el-form-item label="裁决说明">
        <el-input v-model="arbForm.result" type="textarea" :rows="3" placeholder="说明裁决理由与执行方式" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="arbitrateVisible = false">取消</el-button>
      <el-button type="primary" @click="doArbitrate">提交裁决</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { notifyError } from '@/api/http'

const STATUS = {
  PENDING: { label: '待受理', tag: 'is-wait' },
  PROCESSING: { label: '举证中', tag: 'is-doing' },
  ARBITRATING: { label: '仲裁中', tag: 'is-doing' },
  FINISHED: { label: '已裁决', tag: 'is-ok' },
  CLOSED: { label: '已关闭', tag: 'is-off' }
}

// 材料类型中文：管理端不许直接渲染英文枚举，未知值兜底显示原值
const EVIDENCE_TYPE = { TEXT: '文字', IMAGE: '图片', MIXED: '图文' }
const ROLE_TYPE = { USER: '用户', MERCHANT: '商家' }
// 图片后缀白名单，pdf 及其他附件走"查看附件"链接
const IMAGE_EXT = ['jpg', 'jpeg', 'png', 'webp', 'gif']

const list = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('')
const pendingCount = computed(() => list.value.filter(d => d.status === 'PENDING').length)

const reading = ref(false)
const curRow = ref({})
const evidences = ref([])
const evidLoading = ref(false)
const evidError = ref('')
const curId = ref(null)
const curStatus = ref('')
let savedScroll = 0

// 时间线按 createdAt 升序：复制一份再排，不动接口返回的原数组
const sortedEvidences = computed(() => {
  const arr = Array.isArray(evidences.value) ? evidences.value : []
  return [...arr].sort((a, b) => timeOf(a.createdAt) - timeOf(b.createdAt))
})

// 大图预览在整段材料内连续翻页
const imageUrls = computed(() => sortedEvidences.value.filter(e => isImageFile(e.fileUrl)).map(e => e.fileUrl))

function timeOf(v) {
  const t = v ? new Date(v).getTime() : NaN
  return Number.isNaN(t) ? 0 : t
}

// 去掉 url 的 query 与 hash 再看后缀
function fileExt(url) {
  const clean = String(url || '').split('?')[0].split('#')[0]
  const dot = clean.lastIndexOf('.')
  return dot > clean.lastIndexOf('/') ? clean.slice(dot + 1).toLowerCase() : ''
}

function isImageFile(url) {
  return IMAGE_EXT.includes(fileExt(url))
}

function roleLabel(role) {
  return ROLE_TYPE[role] || role || '未知'
}

function typeLabel(t) {
  return EVIDENCE_TYPE[t] || t || '未知'
}

// 历史脏数据可能 content 与 fileUrl 都为空，兜底文案避免空白气泡
function isEmptyEvidence(e) {
  return !e.content && !e.fileUrl
}

const acceptVisible = ref(false)
const acceptForm = reactive({ approved: true, rejectReason: '' })
const arbitrateVisible = ref(false)
const arbForm = reactive({ resultType: 'REFUND', refundAmount: null, penaltyType: 'WARNING', targetId: null, result: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    list.value = (await http.get('/admin/disputes', { params: status.value ? { status: status.value } : {} })) || []
  } catch (e) {
    error.value = e?.message || '接口 /admin/disputes 请求失败'
  } finally {
    loading.value = false
  }
}

async function fetchEvidences(id) {
  evidLoading.value = true
  evidError.value = ''
  try {
    evidences.value = (await http.get(`/admin/disputes/${id}/evidences`)) || []
  } catch (e) {
    evidences.value = []
    evidError.value = e?.message || '材料加载失败'
  } finally {
    evidLoading.value = false
  }
}

async function openEvidences(row) {
  curRow.value = row
  curId.value = row.id
  curStatus.value = row.status
  savedScroll = window.scrollY
  reading.value = true
  await nextTick()
  window.scrollTo(0, 0)
  fetchEvidences(row.id)
}

function reloadEvidences() {
  if (curId.value) fetchEvidences(curId.value)
}

function backToList() {
  reading.value = false
  nextTick(() => window.scrollTo(0, savedScroll))
}

async function finishEvidence() {
  try {
    await http.post(`/admin/disputes/${curId.value}/finish-evidence`)
    ElMessage.success('已结束举证')
    backToList()
    load()
  } catch (e) {
    notifyError(e, '操作失败，请重试')
  }
}

function openAccept(row) {
  curId.value = row.id
  acceptForm.approved = true
  acceptForm.rejectReason = ''
  acceptVisible.value = true
}

async function doAccept() {
  try {
    await http.post(`/admin/disputes/${curId.value}/accept`, acceptForm)
    ElMessage.success('已处理')
    acceptVisible.value = false
    load()
  } catch (e) {
    notifyError(e, '提交失败，请重试')
  }
}

function openArbitrate(row) {
  curId.value = row.id
  Object.assign(arbForm, { resultType: 'REFUND', refundAmount: null, penaltyType: 'WARNING', targetId: null, result: '' })
  arbitrateVisible.value = true
}

async function doArbitrate() {
  if (arbForm.resultType === 'PENALTY') {
    try {
      await ElMessageBox.confirm('该裁决会同时对商家执行处罚，确认提交？', '二次确认', {
        confirmButtonText: '确认提交',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
  }
  try {
    await http.post(`/admin/disputes/${curId.value}/arbitrate`, arbForm)
    ElMessage.success('已裁决')
    arbitrateVisible.value = false
    load()
  } catch (e) {
    notifyError(e, '提交失败，请重试')
  }
}

onMounted(load)
</script>

<style scoped>
.sel {
  width: 148px;
}
.full {
  width: 100%;
}
.ops {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}
.read-id {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.back {
  border: 0;
  background: none;
  padding: 0;
  font: inherit;
  font-size: var(--fs-sm);
  color: var(--rose-600);
  cursor: pointer;
}
.back:hover {
  color: var(--rose-500);
}
.chat {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-5);
}
.row {
  display: flex;
}
.row.left {
  justify-content: flex-start;
}
.row.right {
  justify-content: flex-end;
}
.bubble {
  max-width: 72%;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius);
  background: var(--bg-deep);
  border: 1px solid var(--border);
}
.row.right .bubble {
  background: var(--rose-50);
  border-color: var(--rose-100);
}
.content {
  margin: 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink);
  white-space: pre-wrap;
  word-break: break-word;
}
.file {
  margin-top: var(--space-2);
}
.thumb {
  width: 160px;
  height: 160px;
  border-radius: var(--radius-sm);
  display: block;
  cursor: zoom-in;
}
.attach {
  font-size: var(--fs-sm);
  color: var(--rose-600);
  text-decoration: none;
}
.attach:hover {
  text-decoration: underline;
}
.empty-tip {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.meta {
  margin: var(--space-2) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.row.right .meta {
  text-align: right;
}
</style>
