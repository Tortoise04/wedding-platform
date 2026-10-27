<template>
  <div class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">共 <em>{{ total }}</em> 单 · 本页 <em>{{ list.length }}</em> 条</p>
    </div>

    <section class="adm-panel">
      <div class="adm-toolbar">
        <div class="adm-field">
          <span class="adm-label">订单状态</span>
          <el-select v-model="status" size="small" class="sel" placeholder="全部" @change="onQueryChange">
            <el-option label="全部" value="" />
            <el-option v-for="(s, k) in STATUS" :key="k" :label="s.label" :value="k" />
          </el-select>
        </div>
      </div>

      <div v-if="loading && !list.length" class="adm-skel"><i></i><i></i><i></i><i></i><i></i></div>

      <div v-else-if="error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">订单列表没能加载</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>

      <div v-else-if="!list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">没有匹配的订单</p>
        <p class="adm-state-desc">换个状态看看，或等待新的订单进来。</p>
        <el-button size="small" @click="status = ''; onQueryChange()">查看全部</el-button>
      </div>

      <template v-else>
        <el-table v-loading="loading" :data="list">
          <el-table-column label="订单号" width="190">
            <template #default="s"><span class="adm-mono">{{ s.row.orderNo }}</span></template>
          </el-table-column>
          <el-table-column label="用户 / 商家" width="170">
            <template #default="s">
              <div class="adm-cell-sub">
                {{ userNameOf(s.row.userId) }}<span v-if="s.row.userId != null" class="adm-cell-id">#{{ s.row.userId }}</span>
              </div>
              <div class="adm-cell-sub">
                {{ merchantNameOf(s.row.merchantId) }}<span v-if="s.row.merchantId != null" class="adm-cell-id">#{{ s.row.merchantId }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="110">
            <template #default="s"><span class="adm-amount">¥{{ Number(s.row.amount).toLocaleString() }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="s">
              <span class="adm-tag" :class="STATUS[s.row.status]?.tag">{{ STATUS[s.row.status]?.label }}</span>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="165">
            <template #default="s"><span class="adm-mono">{{ fmt(s.row.createdAt) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" min-width="200" align="right">
            <template #default="s">
              <div class="ops">
                <el-button size="small" @click="openLogs(s.row)">履约轨迹</el-button>
                <el-button v-if="['PENDING','CONFIRMED'].includes(s.row.status)" size="small" plain @click="openCancel(s.row)">强制取消</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="adm-pager">
          <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size"
            :current-page="page + 1" @current-change="onPage" />
        </div>
      </template>
    </section>

    <el-dialog v-model="logsVisible" title="履约轨迹" width="520px">
      <el-timeline v-if="logs.length" class="logs">
        <el-timeline-item v-for="l in logs" :key="l.id" :timestamp="fmt(l.createdAt)" color="#C2416F">
          <p class="log-line">{{ roleOf(l.operatorRole) }} · {{ l.remark || '状态变更' }}</p>
          <p class="adm-cell-sub">{{ statusOf(l.fromStatus) }} → {{ statusOf(l.toStatus) }}</p>
        </el-timeline-item>
      </el-timeline>
      <div v-else class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">暂无轨迹记录</p>
        <p class="adm-state-desc">这笔订单还没有状态变更。</p>
      </div>
    </el-dialog>

    <el-dialog v-model="cancelVisible" title="强制取消订单" width="440px">
      <p class="dlg-tip">取消后订单不可恢复，用户会收到通知。请填写取消原因。</p>
      <el-input v-model="reason" type="textarea" :rows="3" placeholder="取消原因（必填）" />
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" @click="doCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http, { notifyError } from '@/api/http'

const STATUS = {
  PENDING: { label: '待审核', tag: 'is-wait' },
  CONFIRMED: { label: '已确认', tag: 'is-doing' },
  IN_PROGRESS: { label: '履约中', tag: 'is-doing' },
  COMPLETED: { label: '已完成', tag: 'is-ok' },
  CANCELLED: { label: '已取消', tag: 'is-off' }
}

// 管理端不直接渲染英文枚举
const ROLE = { USER: '用户', MERCHANT: '商家', ADMIN: '平台' }
function roleOf(r) {
  return ROLE[r] || r || '系统'
}
function statusOf(s) {
  return s ? STATUS[s]?.label || s : '创建'
}
// 后端给的是 ISO LocalDateTime,表格里别把 T 直接露出来
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

const list = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('')
const page = ref(0)
const size = ref(10)
const total = ref(0)

// 订单表只回 userId / merchantId,这里复用已有的管理端接口取名称做映射,
// 不去改后端:取不到就退回「用户 #id / 商家 #id」,不阻塞列表本身。
const userNameMap = ref({})
const merchantNameMap = ref({})

function userNameOf(id) {
  if (id == null) return '—'
  return userNameMap.value[id] || `用户 #${id}`
}
function merchantNameOf(id) {
  if (id == null) return '—'
  return merchantNameMap.value[id] || `商家 #${id}`
}

async function loadNameMaps() {
  try {
    const users = (await http.get('/users')) || []
    const um = {}
    for (const u of users) um[u.id] = u.realName || u.username || `用户 #${u.id}`
    userNameMap.value = um
  } catch (e) { /* 名称映射失败不影响列表 */ }
  try {
    const merchants = (await http.get('/admin/merchants')) || []
    const mm = {}
    for (const m of merchants) mm[m.id] = m.shopName || `商家 #${m.id}`
    merchantNameMap.value = mm
  } catch (e) { /* 名称映射失败不影响列表 */ }
}

const logsVisible = ref(false)
const logs = ref([])

const cancelVisible = ref(false)
const reason = ref('')
const cancelId = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 服务端分页：返回 {list, total, totalPages, page, size}
    const params = { page: page.value, size: size.value }
    if (status.value) params.status = status.value
    const data = await http.get('/admin/orders', { params })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e?.message || '接口 /admin/orders 请求失败'
  } finally {
    loading.value = false
  }
}

function onQueryChange() {
  page.value = 0
  load()
}
function onPage(p) {
  page.value = p - 1
  load()
}

async function openLogs(row) {
  try {
    logs.value = (await http.get(`/admin/orders/${row.id}/logs`)) || []
    logsVisible.value = true
  } catch (e) {
    notifyError(e, '轨迹加载失败')
  }
}

function openCancel(row) {
  cancelId.value = row.id
  reason.value = ''
  cancelVisible.value = true
}

async function doCancel() {
  if (!reason.value.trim()) {
    ElMessage.warning('请填写取消原因')
    return
  }
  try {
    await http.post(`/admin/orders/${cancelId.value}/force-cancel`, { reason: reason.value })
    ElMessage.success('已强制取消')
    cancelVisible.value = false
    load()
  } catch (e) {
    notifyError(e, '取消失败，请重试')
  }
}

onMounted(() => {
  load()
  loadNameMaps()
})
</script>

<style scoped>
.sel {
  width: 148px;
}
.ops {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
}
.logs {
  padding-top: var(--space-1);
}
/* 名称后面的 #id 是给管理员做核对用的,别抢名字的注意力 */
.adm-cell-id {
  margin-left: var(--space-1);
  opacity: 0.5;
  font-weight: 400;
}
.log-line {
  margin: 0;
  color: var(--ink);
}
.dlg-tip {
  margin: 0 0 var(--space-3);
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
</style>
