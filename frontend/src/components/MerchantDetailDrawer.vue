<template>
  <el-drawer v-model="visible" class="mdd" direction="rtl" size="860px" @closed="onClosed">
    <template #header>
      <div class="mdd-head">
        <span class="mdd-title">{{ (merchant && merchant.shopName) || '商家详情' }}</span>
        <span v-if="merchant" class="adm-tag" :class="TAG[merchant.auditStatus]">{{ AUDIT[merchant.auditStatus] }}</span>
      </div>
    </template>

    <div class="mdd-tabs">
      <nav class="adm-seg">
        <button
          v-for="t in TABS"
          :key="t.key"
          type="button"
          :class="{ on: tab === t.key }"
          @click="switchTab(t.key)"
        >{{ t.label }}</button>
      </nav>
    </div>

    <div v-if="!merchant" class="adm-state">
      <span class="adm-state-ico">!</span>
      <p class="adm-state-title">没有选中商家</p>
      <p class="adm-state-desc">从列表点「详情」打开这一家。</p>
    </div>

    <!-- 资料与材料：数据全部来自列表行对象，不额外请求 -->
    <div v-else-if="tab === 'profile'" class="mdd-body">
      <dl class="mdd-kv">
        <div><dt>店铺名称</dt><dd>{{ merchant.shopName || '—' }}</dd></div>
        <div><dt>经营范围</dt><dd>{{ merchant.businessScope || '—' }}</dd></div>
        <div><dt>联系方式</dt><dd>{{ merchant.contactPhone || '—' }}</dd></div>
        <div>
          <dt>审核状态</dt>
          <dd><span class="adm-tag" :class="TAG[merchant.auditStatus]">{{ AUDIT[merchant.auditStatus] }}</span></dd>
        </div>
        <div>
          <dt>营业状态</dt>
          <dd><span class="adm-tag" :class="merchant.status === 1 ? 'is-ok' : 'is-off'">{{ merchant.status === 1 ? '启用' : '停用' }}</span></dd>
        </div>
        <div v-if="merchant.rejectReason" class="is-full">
          <dt>驳回原因</dt>
          <dd class="mdd-reject">{{ merchant.rejectReason }}</dd>
        </div>
      </dl>

      <h3 class="mdd-sub-title">资质材料</h3>
      <div class="mdd-mats">
        <figure class="mdd-mat">
          <figcaption class="adm-label">店铺 Logo</figcaption>
          <el-image
            v-if="isImage(merchant.logo)"
            :src="merchant.logo"
            fit="cover"
            class="mdd-thumb"
            :preview-src-list="[merchant.logo]"
            preview-teleported
          />
          <a v-else-if="merchant.logo" :href="merchant.logo" target="_blank" rel="noopener" class="mdd-file">查看</a>
          <p v-else class="mdd-empty">未上传</p>
        </figure>

        <figure class="mdd-mat">
          <figcaption class="adm-label">营业执照</figcaption>
          <el-image
            v-if="isImage(merchant.licenseUrl)"
            :src="merchant.licenseUrl"
            fit="cover"
            class="mdd-thumb"
            :preview-src-list="[merchant.licenseUrl]"
            preview-teleported
          />
          <a v-else-if="merchant.licenseUrl" :href="merchant.licenseUrl" target="_blank" rel="noopener" class="mdd-file">查看</a>
          <p v-else class="mdd-empty">未上传</p>
        </figure>

        <figure class="mdd-mat">
          <figcaption class="adm-label">服务资质</figcaption>
          <el-image
            v-if="isImage(merchant.qualificationUrl)"
            :src="merchant.qualificationUrl"
            fit="cover"
            class="mdd-thumb"
            :preview-src-list="[merchant.qualificationUrl]"
            preview-teleported
          />
          <a v-else-if="merchant.qualificationUrl" :href="merchant.qualificationUrl" target="_blank" rel="noopener" class="mdd-file">查看</a>
          <p v-else class="mdd-empty">未上传</p>
        </figure>
      </div>
    </div>

    <!-- 商品：审核结论与上下架就地完成，其余字段只读 -->
    <div v-else-if="tab === 'services'" class="mdd-body">
      <div v-if="svc.loading && !svc.list.length" class="adm-skel"><i></i><i></i><i></i><i></i></div>
      <div v-else-if="svc.error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">商品没能加载</p>
        <p class="adm-state-desc">{{ svc.error }}</p>
        <el-button size="small" @click="loadServices(true)">重新加载</el-button>
      </div>
      <div v-else-if="!svc.list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">该商家还没有商品</p>
        <p class="adm-state-desc">商家发布服务后会出现在这里。</p>
      </div>
      <el-table v-else v-loading="svc.loading" :data="svc.list" max-height="560">
        <el-table-column prop="name" label="名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="价格" width="110">
          <template #default="s"><span class="adm-amount">¥{{ s.row.price }}</span></template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="s">
            <span class="adm-tag" :class="TAG[s.row.auditStatus]">{{ AUDIT[s.row.auditStatus] || s.row.auditStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="上下架" width="100">
          <template #default="s">
            <span class="adm-tag" :class="s.row.shelfStatus === 1 ? 'is-ok' : 'is-off'">{{ s.row.shelfStatus === 1 ? '上架' : '下架' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="hasViews" label="浏览量" width="90">
          <template #default="s"><span class="adm-mono">{{ s.row.viewCount }}</span></template>
        </el-table-column>
        <el-table-column v-if="hasSales" label="销量" width="90">
          <template #default="s"><span class="adm-mono">{{ s.row.salesCount }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="right">
          <template #default="s">
            <template v-if="s.row.auditStatus === 'PENDING'">
              <el-button size="small" type="primary" :loading="busyRow === s.row" @click="auditService(s.row, true)">通过</el-button>
              <el-button size="small" :loading="busyRow === s.row" @click="auditService(s.row, false)">驳回</el-button>
            </template>
            <template v-else-if="s.row.auditStatus === 'APPROVED'">
              <el-button
                v-if="s.row.shelfStatus === 1"
                size="small"
                plain
                :loading="busyRow === s.row"
                @click="offShelfService(s.row)"
              >下架</el-button>
              <el-button v-else size="small" type="primary" :loading="busyRow === s.row" @click="toggleShelf(s.row, 1)">上架</el-button>
            </template>
            <span v-else class="adm-cell-sub">已驳回</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 评价：长文本与图片用卡片，审核结论就地完成 -->
    <div v-else-if="tab === 'evaluations'" class="mdd-body">
      <div v-if="eva.loading && !eva.list.length" class="adm-skel"><i></i><i></i><i></i><i></i></div>
      <div v-else-if="eva.error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">评价没能加载</p>
        <p class="adm-state-desc">{{ eva.error }}</p>
        <el-button size="small" @click="loadEvaluations(true)">重新加载</el-button>
      </div>
      <div v-else-if="!eva.list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">该商家还没有评价</p>
        <p class="adm-state-desc">买家完成的订单评价会汇总到这里。</p>
      </div>
      <template v-else>
        <article v-for="e in eva.list" :key="e.id" class="mdd-card">
          <header class="mdd-card-head">
            <el-rate :model-value="e.score" disabled />
            <span class="adm-tag" :class="TAG[e.auditStatus]">{{ evalAuditText(e) }}</span>
          </header>
          <p class="mdd-card-text">{{ e.content || '（未填写文字）' }}</p>
          <div v-if="imgList(e.images).length" class="mdd-shots">
            <el-image
              v-for="(src, i) in imgList(e.images)"
              :key="src + i"
              :src="src"
              fit="cover"
              class="mdd-shot"
              :preview-src-list="imgList(e.images)"
              :initial-index="i"
              preview-teleported
            />
          </div>
          <p v-if="e.replyContent" class="mdd-reply"><span class="adm-label">商家回复</span>{{ e.replyContent }}</p>
          <div v-if="e.auditStatus === 'PENDING' || e.auditStatus === 'APPROVED' || e.auditStatus === 'REJECTED'" class="mdd-card-ops">
            <template v-if="e.auditStatus === 'PENDING'">
              <el-button size="small" type="primary" :loading="busyRow === e" @click="auditEvaluation(e, true)">通过</el-button>
              <el-button size="small" :loading="busyRow === e" @click="auditEvaluation(e, false)">驳回</el-button>
            </template>
            <el-button v-else-if="e.auditStatus === 'APPROVED'" size="small" plain :loading="busyRow === e" @click="hideEvaluation(e)">隐藏</el-button>
            <template v-else-if="e.hiddenAt">
              <el-button size="small" :loading="busyRow === e" @click="restoreEvaluation(e)">恢复</el-button>
              <p class="adm-cell-sub">隐藏原因：{{ clip(e.rejectReason) }}</p>
            </template>
            <template v-else>
              <span class="adm-cell-sub">已驳回</span>
              <p v-if="e.rejectReason" class="adm-cell-sub">驳回原因：{{ clip(e.rejectReason) }}</p>
            </template>
          </div>
        </article>
      </template>
    </div>

    <!-- 处罚历史：只读，解除入口在罚单页 -->
    <div v-else class="mdd-body">
      <div v-if="pen.loading && !pen.list.length" class="adm-skel"><i></i><i></i><i></i><i></i></div>
      <div v-else-if="pen.error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">处罚记录没能加载</p>
        <p class="adm-state-desc">{{ pen.error }}</p>
        <el-button size="small" @click="loadPenalties(true)">重新加载</el-button>
      </div>
      <div v-else-if="!pen.list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">没有处罚记录</p>
        <p class="adm-state-desc">这家店目前没有被平台处罚过。</p>
      </div>
      <el-table v-else v-loading="pen.loading" :data="pen.list" max-height="560">
        <el-table-column label="类型" width="110">
          <template #default="s"><span class="adm-tag">{{ PEN[s.row.penaltyType] || s.row.penaltyType }}</span></template>
        </el-table-column>
        <el-table-column label="原因" min-width="200">
          <template #default="s"><div class="adm-two-line">{{ s.row.reason || '—' }}</div></template>
        </el-table-column>
        <el-table-column label="生效时间" width="140">
          <template #default="s"><span class="adm-mono">{{ fmt(s.row.startTime) }}</span></template>
        </el-table-column>
        <el-table-column label="结束时间" width="140">
          <template #default="s"><span class="adm-mono">{{ s.row.endTime ? fmt(s.row.endTime) : '长期' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="s">
            <span class="adm-tag" :class="s.row.status === 1 ? 'is-doing' : 'is-off'">{{ s.row.status === 1 ? '生效中' : '已解除' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 底部固定操作条：待审核给出通过/驳回，已通过给出停用/启用 -->
    <div v-if="merchant && tab === 'profile'" class="mdd-actions">
      <template v-if="merchant.auditStatus === 'PENDING'">
        <el-button type="primary" :loading="acting" @click="approve">审核通过</el-button>
        <el-button :loading="acting" @click="reject">审核驳回</el-button>
      </template>
      <template v-else-if="merchant.auditStatus === 'APPROVED'">
        <el-button v-if="merchant.status === 1" :loading="acting" @click="toggleStatus(0)">停用商家</el-button>
        <el-button v-else type="primary" :loading="acting" @click="toggleStatus(1)">启用商家</el-button>
      </template>
      <p v-else class="adm-cell-sub">已驳回，商家整改后重新提交会再进入待审核。</p>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { notifyError } from '@/api/http'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  merchant: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'updated'])

const AUDIT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
// 评价页签的 REJECTED 可能是"驳回"也可能是"通过后人工隐藏"，两者靠 hidden_at 区分：
// hidden_at 非空 = 人工隐藏，显示「已隐藏」；为空 = 审核驳回，显示「已驳回」。
// 商家/商品的 REJECTED 仍是「已驳回」，故评价这里单独一套映射，不碰上面的 AUDIT。
const EVAL_AUDIT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const TAG = { PENDING: 'is-wait', APPROVED: 'is-ok', REJECTED: 'is-off' }
const PEN = { WARNING: '警告', SERVICE_OFF: '服务下架', BAN: '封禁' }
const TABS = [
  { key: 'profile', label: '资料与材料' },
  { key: 'services', label: '商品' },
  { key: 'evaluations', label: '评价' },
  { key: 'penalties', label: '处罚历史' }
]

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const tab = ref('profile')
const acting = ref(false)

// 三个只读 Tab 各自一套三态；loaded 用来做懒加载，切回已加载的 Tab 不再重复请求
const svc = reactive({ list: [], loading: false, error: '', loaded: false })
const eva = reactive({ list: [], loading: false, error: '', loaded: false })
const pen = reactive({ list: [], loading: false, error: '', loaded: false })

const hasViews = computed(() => svc.list.some(s => s.viewCount !== null && s.viewCount !== undefined))
const hasSales = computed(() => svc.list.some(s => s.salesCount !== null && s.salesCount !== undefined))

// 材料 URL 可能指向 PDF，只有图片才交给 el-image 出缩略图
function isImage(u) {
  return /\.(jpe?g|png|webp|gif)(\?|#|$)/i.test(String(u || ''))
}

// images 在评价实体里是逗号分隔的 url 字符串（见 Evaluation.images），不是数组
function imgList(v) {
  return String(v || '').split(',').map(s => s.trim()).filter(Boolean)
}

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

// 卡片里只交代"为什么被驳回"，超过 20 字截断；完整原因在评价管理页
function clip(t, n = 20) {
  const s = String(t || '')
  return s.length > n ? s.slice(0, n) + '…' : s
}

// 评价卡片状态文案：REJECTED 按 hidden_at 二选一，其余直接取 EVAL_AUDIT。
function evalAuditText(row) {
  if (row.auditStatus !== 'REJECTED') return EVAL_AUDIT[row.auditStatus] || row.auditStatus
  return row.hiddenAt ? '已隐藏' : '已驳回'
}

function resetTabs() {
  Object.assign(svc, { list: [], loading: false, error: '', loaded: false })
  Object.assign(eva, { list: [], loading: false, error: '', loaded: false })
  Object.assign(pen, { list: [], loading: false, error: '', loaded: false })
}

async function loadServices(force) {
  if (!props.merchant || svc.loading) return
  if (svc.loaded && !force) return
  const id = props.merchant.id
  svc.loading = true
  svc.error = ''
  try {
    const rows = await http.get('/admin/services', { params: { merchantId: id } })
    if (props.merchant?.id !== id) return // 换商家后旧响应作废
    svc.list = rows || []
    svc.loaded = true
  } catch (e) {
    svc.error = e?.message || '接口 /admin/services 请求失败'
    notifyError(e, '商品加载失败')
  } finally {
    svc.loading = false
  }
}

async function loadEvaluations(force) {
  if (!props.merchant || eva.loading) return
  if (eva.loaded && !force) return
  const id = props.merchant.id
  eva.loading = true
  eva.error = ''
  try {
    const rows = await http.get('/admin/evaluations', { params: { merchantId: id } })
    if (props.merchant?.id !== id) return // 换商家后旧响应作废
    eva.list = rows || []
    eva.loaded = true
  } catch (e) {
    eva.error = e?.message || '接口 /admin/evaluations 请求失败'
    notifyError(e, '评价加载失败')
  } finally {
    eva.loading = false
  }
}

async function loadPenalties(force) {
  if (!props.merchant || pen.loading) return
  if (pen.loaded && !force) return
  const id = props.merchant.id
  pen.loading = true
  pen.error = ''
  try {
    const rows = await http.get('/admin/penalties/merchant/' + id)
    if (props.merchant?.id !== id) return // 换商家后旧响应作废
    pen.list = rows || []
    pen.loaded = true
  } catch (e) {
    pen.error = e?.message || '接口 /admin/penalties/merchant/{id} 请求失败'
    notifyError(e, '处罚记录加载失败')
  } finally {
    pen.loading = false
  }
}

function switchTab(k) {
  tab.value = k
  if (k === 'services') loadServices()
  else if (k === 'evaluations') loadEvaluations()
  else if (k === 'penalties') loadPenalties()
}

// 抽屉关闭即清空，避免下次打开另一家时闪出上一家的数据
function onClosed() {
  tab.value = 'profile'
  resetTabs()
}

// 换商家同理：旧数据一律作废，回到资料页
watch(
  () => (props.merchant ? props.merchant.id : null),
  (id, old) => {
    if (id === old) return
    tab.value = 'profile'
    resetTabs()
  }
)

async function submitAudit(payload, okText) {
  if (acting.value) return
  acting.value = true
  try {
    await http.post('/admin/merchants/' + props.merchant.id + '/audit', payload)
    ElMessage.success(okText)
    emit('updated')
  } catch (e) {
    notifyError(e, '提交失败，请重试')
  } finally {
    acting.value = false
  }
}

async function approve() {
  const name = props.merchant.shopName
  try {
    await ElMessageBox.confirm(
      '通过后该商家进入平台展示，可以发布服务、接单并回复买家咨询。',
      '审核通过 · ' + name,
      { type: 'warning', confirmButtonText: '通过' }
    )
  } catch (e) {
    return // 用户取消
  }
  await submitAudit({ approved: true, rejectReason: '' }, '已通过审核')
}

async function reject() {
  const name = props.merchant.shopName
  let reason = ''
  try {
    const r = await ElMessageBox.prompt(
      '驳回后商家可依据原因整改再提交，请写清缺什么、错在哪：',
      '审核驳回 · ' + name,
      { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '驳回原因必填') }
    )
    reason = String(r.value).trim()
  } catch (e) {
    return // 用户取消
  }
  await submitAudit({ approved: false, rejectReason: reason }, '已驳回')
}

// 停用 / 启用商家：与"违规处罚"的封禁是同一条状态，只是入口在商家详情，不要求先有投诉。
// 停用原因必填并写进一张 BAN 罚单（管理端罚单页可见）；启用会把生效中的封禁罚单置为已解除。
async function toggleStatus(target) {
  const off = target === 0
  const row = props.merchant
  let reason = ''
  try {
    if (off) {
      const r = await ElMessageBox.prompt(
        '停用后该商家从「找商家」列表与店铺页消失，但名下服务仍可见可下单（服务是否下架请走「违规处罚 · 服务下架」）。请填写停用原因，会记入处罚记录：',
        '停用商家 · ' + row.shopName,
        { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '停用原因必填') }
      )
      reason = String(r.value).trim()
    } else {
      await ElMessageBox.confirm(
        '启用后该商家重新出现在「找商家」，同时把它生效中的封禁处罚置为已解除。',
        '启用商家 · ' + row.shopName,
        { type: 'warning' }
      )
    }
  } catch (e) {
    return // 用户取消
  }
  if (acting.value) return
  acting.value = true
  try {
    await http.post('/admin/merchants/' + row.id + '/status', { status: target, reason })
    ElMessage.success(off ? '已停用' : '已启用')
    emit('updated')
  } catch (e) {
    notifyError(e, '提交失败，请重试')
  } finally {
    acting.value = false
  }
}

// 商品与评价的就地操作：只做"审核结论"和"上下架"两件事，编辑/删除仍留在各自的管理页。
// busyRow 存整行对象而不是 id，两张表自增 id 会撞，用 id 会让另一张表的同号行一起转圈。
const busyRow = ref(null)

// 发请求期间锁行：连点会把"通过"和"驳回"两次相反结论都发出去，服务端只认最后一次
async function runRowOp(row, fn) {
  if (busyRow.value) return
  busyRow.value = row
  try {
    await fn()
  } finally {
    busyRow.value = null
  }
}

async function auditService(row, approved) {
  try {
    await ElMessageBox.confirm(
      approved
        ? '通过后该服务可被买家搜索到并下单。'
        : '驳回后该服务不会上架，商家需修改后重新提交。',
      '商品审核 · ' + row.name,
      { type: 'warning', confirmButtonText: approved ? '通过' : '驳回' }
    )
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post('/admin/services/' + row.id + '/audit', null, { params: { approved } })
      ElMessage.success(approved ? '已通过审核' : '已驳回')
      await loadServices(true)
      emit('updated')
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

// 下架走处罚:与列表页一致,不再直改 shelf_status,而是开一张 SERVICE_OFF 罚单(留痕 + 可解除)。
async function offShelfService(row) {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt(
      '下架后买家看不到该服务，会记入处罚记录。请写明下架原因：',
      '下架服务 · ' + row.name,
      { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '下架原因必填') }
    )
    reason = String(r.value).trim()
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post('/admin/penalties', { merchantId: row.merchantId, penaltyType: 'SERVICE_OFF', targetId: row.id, reason })
      ElMessage.success('已下架（已记入处罚）')
      await loadServices(true)
      emit('updated')
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

// 上架保持直改(恢复上架不属于处罚)；下架已收敛到 offShelfService 走处罚，此函数现在只服务上架。
async function toggleShelf(row, target) {
  const on = target === 1
  try {
    await ElMessageBox.confirm(
      on
        ? '上架后该服务重新出现在店铺页，买家可以下单。'
        : '下架后买家看不到该服务，已有订单不受影响。',
      (on ? '上架服务 · ' : '下架服务 · ') + row.name,
      { type: 'warning', confirmButtonText: on ? '上架' : '下架' }
    )
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post('/admin/services/' + row.id + '/shelf', null, { params: { shelfStatus: target } })
      ElMessage.success(on ? '已上架' : '已下架')
      await loadServices(true)
      emit('updated')
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

async function auditEvaluation(row, approved) {
  let rejectReason = ''
  try {
    if (approved) {
      await ElMessageBox.confirm(
        '通过后该评价在店铺页与商品页公开显示。',
        '评价审核 · 通过',
        { type: 'warning', confirmButtonText: '通过' }
      )
    } else {
      const r = await ElMessageBox.prompt(
        '驳回后该评价不对外展示，请写明原因，会记录在这条评价上：',
        '评价审核 · 驳回',
        { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '驳回原因必填') }
      )
      rejectReason = String(r.value).trim()
    }
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      const body = approved ? { approved: true } : { approved: false, rejectReason }
      await http.post('/admin/evaluations/' + row.id + '/audit', body)
      ElMessage.success(approved ? '已通过审核' : '已驳回')
      await loadEvaluations(true)
      emit('updated')
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

// 隐藏一条已通过的评价：原因必填，成功后重算服务评分并刷新本 Tab。
async function hideEvaluation(row) {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt(
      '隐藏后该评价从店铺页与商品页消失，服务评分也会重算。请写明隐藏原因：',
      '评价隐藏',
      { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '隐藏原因必填') }
    )
    reason = String(r.value).trim()
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post('/admin/evaluations/' + row.id + '/hide', { reason })
      ElMessage.success('已隐藏')
      await loadEvaluations(true)
      emit('updated')
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

// 恢复一条已隐藏的评价：二次确认后回到 APPROVED，服务评分重算。
async function restoreEvaluation(row) {
  try {
    await ElMessageBox.confirm(
      '恢复后该评价重新出现在店铺页与商品页，服务评分也会重算。',
      '评价恢复',
      { type: 'warning', confirmButtonText: '恢复' }
    )
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post('/admin/evaluations/' + row.id + '/restore')
      ElMessage.success('已恢复')
      await loadEvaluations(true)
      emit('updated')
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}
</script>

<style scoped>
.mdd :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.mdd :deep(.el-drawer__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.mdd-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.mdd-title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--ink);
}
.mdd-tabs {
  padding: var(--space-4) var(--space-5) 0;
}
.mdd-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  padding: var(--space-4) var(--space-5) var(--space-5);
}

/* 资料页：标签与值两列，超长文本换行不撑破抽屉 */
.mdd-kv {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3) var(--space-5);
  margin: 0;
}
.mdd-kv > div {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  min-width: 0;
}
.mdd-kv > div.is-full {
  grid-column: 1 / -1;
}
.mdd-kv dt {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
  line-height: var(--lh-ui);
}
.mdd-kv dd {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink);
  word-break: break-word;
}
.mdd-reject {
  color: var(--danger);
}

.mdd-sub-title {
  margin: var(--space-5) 0 var(--space-3);
  font-family: var(--font-display);
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--ink);
}

/* 材料：三张等宽卡片，缩略图统一 96px 见方 */
.mdd-mats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}
.mdd-mat {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}
.mdd-thumb {
  width: 100%;
  height: 96px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  cursor: zoom-in;
}
.mdd-file {
  font-size: var(--fs-sm);
  color: var(--rose-500);
  text-decoration: underline;
}
.mdd-empty {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-faint);
  line-height: 96px;
  text-align: center;
}

/* 操作条常驻底部：抽屉 body 若被 EP 默认样式接管（overflow:auto），sticky 兜住它不被滚走 */
.mdd-actions {
  flex: none;
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-5);
  background: var(--surface);
  border-top: 1px solid var(--border);
}

/* 评价卡片 */
.mdd-card {
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--border);
}
.mdd-card:last-child {
  border-bottom: 0;
}
.mdd-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}
.mdd-card-text {
  margin: var(--space-2) 0 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink);
  word-break: break-word;
}
.mdd-shots {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}
.mdd-shot {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  cursor: zoom-in;
}
.mdd-reply {
  margin: var(--space-2) 0 0;
  padding: var(--space-2) var(--space-3);
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  background: var(--bg-deep);
  border-radius: var(--radius-sm);
  word-break: break-word;
}
.mdd-reply .adm-label {
  margin-right: var(--space-2);
}

/* 评价卡片的就地操作区：按钮尺寸与评价管理页一致，靠右沉在卡片底部 */
.mdd-card-ops {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-3);
}
.mdd-card-ops p {
  margin: 0;
}
</style>
