<template>
  <div class="mch-page">
    <!-- ========== 列表 ========== -->
    <template v-if="!detail">
      <header class="mch-head">
        <p class="mch-sub">
          共 <em>{{ total }}</em> 单 · 当前筛选 <em>{{ curLabel }}</em>
        </p>
        <div class="mch-actions">
          <div class="mch-seg">
            <button type="button" :class="{ on: status === '' }" @click="setStatus('')">全部</button>
            <button
              v-for="(s, k) in STATUS"
              :key="k"
              type="button"
              :class="{ on: status === k }"
              @click="setStatus(k)"
            >
              {{ s.label }}
            </button>
          </div>
        </div>
      </header>

      <section v-if="error" class="mch-panel">
        <div class="mch-state">
          <p class="mch-state-ico">!</p>
          <p class="mch-state-title">订单没有加载出来</p>
          <p class="mch-state-desc">{{ error }}</p>
          <el-button size="small" @click="load">重试</el-button>
        </div>
      </section>

      <section v-else-if="loading" class="mch-list">
        <div v-for="i in 3" :key="i" class="sk-row">
          <span class="sk-thumb" />
          <span class="sk-lines"><i /><i /></span>
        </div>
      </section>

      <section v-else-if="!orders.length" class="mch-panel">
        <div class="mch-state">
          <p class="mch-state-ico">◇</p>
          <p class="mch-state-title">这个状态下还没有订单</p>
          <p class="mch-state-desc">换一个状态筛选看看；买家下单后会出现在待审核里。</p>
          <el-button size="small" @click="setStatus('')">查看全部订单</el-button>
        </div>
      </section>

      <section v-else class="mch-list">
        <article v-for="row in orders" :key="row.id" class="mch-row">
          <div class="mch-thumb">
            <ServiceCover :service="coverOf(row)" ratio="1 / 1" />
          </div>
          <div class="mch-row-main">
            <h3 class="mch-row-name">{{ serviceName(row) }}</h3>
            <div class="mch-row-meta">
              <span class="mch-mono">{{ row.orderNo }}</span>
              <span class="mch-tag" :class="toneOf(row.status)">{{ labelOf(row.status) }}</span>
              <span v-if="row.afterSaleStatus" class="mch-tag" :class="row.afterSaleStatus === 1 ? 'is-wait' : 'is-off'">
                售后{{ AS_STATUS[row.afterSaleStatus] }}
              </span>
            </div>
            <p class="mch-row-sub">
              下单 {{ fmt(row.createdAt) }} · 婚礼日期 {{ row.weddingDate || '未定' }} · 数量
              {{ row.quantity || 1 }}
            </p>
          </div>
          <div class="mch-row-side">
            <span class="mch-amount" :class="{ 'is-off': row.status === 'CANCELLED' }">{{ money(row.amount) }}</span>
            <div class="mch-row-acts">
              <button type="button" class="mch-act" @click="openDetail(row)">查看</button>
              <button v-if="row.status === 'PENDING'" type="button" class="mch-act is-rose" @click="act('confirm', row)" :disabled="acting">接单</button>
              <button v-if="row.status === 'PENDING'" type="button" class="mch-act is-muted" @click="act('reject', row)" :disabled="acting">拒单</button>
              <button v-if="['CONFIRMED', 'IN_PROGRESS'].includes(row.status)" type="button" class="mch-act is-rose" @click="act('progress', row)" :disabled="acting">履约推进</button>
              <button v-if="row.status === 'COMPLETED'" type="button" class="mch-act" @click="openAfterSale(row)">售后</button>
            </div>
          </div>
        </article>
      </section>

      <div v-if="!loading && !error && total > size" class="mch-pager">
        <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size"
          :current-page="page + 1" @current-change="onPage" />
      </div>
    </template>

    <!-- ========== 页内全屏详情 ========== -->
    <template v-else>
      <button type="button" class="mch-back" @click="closeDetail">← 返回订单列表</button>

      <section class="mch-block">
        <div class="detail-head">
          <div>
            <p class="detail-no">订单 {{ detail.orderNo }}</p>
            <p class="detail-meta">
              <span class="mch-tag" :class="toneOf(detail.status)">{{ labelOf(detail.status) }}</span>
              <span>下单 {{ fmt(detail.createdAt) }}</span>
              <span v-if="detail.afterSaleStatus">售后{{ AS_STATUS[detail.afterSaleStatus] }}</span>
            </p>
          </div>
          <div class="detail-side">
            <span class="mch-amount" :class="{ 'is-off': detail.status === 'CANCELLED' }">{{ money(detail.amount) }}</span>
            <div class="mch-row-acts">
              <button v-if="detail.status === 'PENDING'" type="button" class="mch-act is-rose" @click="act('confirm', detail)">接单</button>
              <button v-if="detail.status === 'PENDING'" type="button" class="mch-act is-muted" @click="act('reject', detail)">拒单</button>
              <button v-if="['CONFIRMED', 'IN_PROGRESS'].includes(detail.status)" type="button" class="mch-act is-rose" @click="act('progress', detail)">履约推进</button>
              <button v-if="detail.status === 'COMPLETED'" type="button" class="mch-act" @click="openAfterSale(detail)">售后</button>
            </div>
          </div>
        </div>
      </section>

      <div class="detail-grid">
        <section class="mch-block">
          <h2 class="mch-block-title">服务与需求</h2>
          <div class="detail-svc">
            <div class="mch-thumb">
              <ServiceCover :service="coverOf(detail)" ratio="1 / 1" />
            </div>
            <div class="mch-row-main">
              <p class="mch-row-name">{{ serviceName(detail) }}</p>
              <p class="mch-row-sub">服务编号 {{ detail.serviceId || '—' }}</p>
            </div>
          </div>
          <dl class="mch-kv">
            <div class="mch-kv-row"><dt>婚礼日期</dt><dd>{{ detail.weddingDate || '未填写' }}</dd></div>
            <div class="mch-kv-row"><dt>宾客人数</dt><dd>{{ detail.guestCount ? detail.guestCount + ' 人' : '未填写' }}</dd></div>
            <div class="mch-kv-row"><dt>预算区间</dt><dd>{{ budgetText }}</dd></div>
            <div class="mch-kv-row"><dt>数量</dt><dd>{{ detail.quantity || 1 }}</dd></div>
            <div class="mch-kv-row"><dt>买家备注</dt><dd>{{ detail.remark || '买家没有留言' }}</dd></div>
            <div class="mch-kv-row" v-if="detail.cancelReason"><dt>取消原因</dt><dd>{{ detail.cancelReason }}</dd></div>
          </dl>
        </section>

        <section class="mch-block">
          <h2 class="mch-block-title">履约轨迹</h2>
          <p class="mch-row-sub trail-cap">按订单真实时间戳呈现，没有发生过的节点不显示。</p>
          <ol class="trail">
            <li v-for="(s, i) in trail" :key="i" class="trail-item" :class="{ 'is-last': i === trail.length - 1 }">
              <p class="trail-title">{{ s.name }}</p>
              <p class="trail-text">{{ s.text }}</p>
              <p class="trail-time">{{ s.time }}</p>
            </li>
          </ol>
        </section>
      </div>

      <section v-if="detail.afterSaleStatus" class="mch-block">
        <h2 class="mch-block-title">售后记录</h2>
        <dl class="mch-kv">
          <div class="mch-kv-row"><dt>售后状态</dt><dd>{{ AS_STATUS[detail.afterSaleStatus] }}</dd></div>
          <div class="mch-kv-row"><dt>登记时间</dt><dd>{{ fmt(detail.afterSaleAt) }}</dd></div>
          <div class="mch-kv-row"><dt>处理说明</dt><dd>{{ detail.afterSaleNote || '未填写' }}</dd></div>
        </dl>
        <div class="trail-foot">
          <button type="button" class="mch-act" @click="openAfterSale(detail)">更新售后</button>
        </div>
      </section>
    </template>

    <el-dialog v-model="afterSaleVisible" title="售后处理" width="480px">
      <el-form label-width="90px">
        <el-form-item label="订单号">{{ afterSaleForm.orderNo }}</el-form-item>
        <el-form-item label="订单状态">{{ labelOf(afterSaleForm.orderStatus) }}</el-form-item>
        <el-form-item label="售后状态">
          <el-radio-group v-model="afterSaleForm.status">
            <el-radio :value="1">处理中</el-radio>
            <el-radio :value="2">已完结</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="afterSaleForm.note" type="textarea" :rows="4" placeholder="必填,会展示给用户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="afterSaleVisible = false">取消</el-button>
        <el-button type="primary" @click="doAfterSale">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'

const STATUS = {
  PENDING: { label: '待审核', tone: 'is-wait' },
  CONFIRMED: { label: '已确认', tone: 'is-doing' },
  IN_PROGRESS: { label: '履约中', tone: 'is-doing' },
  COMPLETED: { label: '已完成', tone: 'is-ok' },
  CANCELLED: { label: '已取消', tone: 'is-off' }
}

// 售后状态:null 未登记 / 1 处理中 / 2 已完结
const AS_STATUS = { 1: '处理中', 2: '已完结' }

const orders = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('')
const page = ref(0)
const size = ref(10)
const total = ref(0)

const detail = ref(null)
const savedScroll = ref(0)

// serviceId -> { name, categoryId }:订单行只带 serviceId,服务名单独拉一次字典
const serviceMap = ref({})

const afterSaleVisible = ref(false)
const afterSaleForm = reactive({ id: null, orderNo: '', orderStatus: '', status: 1, note: '' })

function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '—' }
function labelOf(s) { return (STATUS[s] && STATUS[s].label) || '未知状态' }
function toneOf(s) { return (STATUS[s] && STATUS[s].tone) || 'is-off' }
const curLabel = computed(() => (status.value ? labelOf(status.value) : '全部'))

function serviceName(row) {
  const s = serviceMap.value[row.serviceId]
  return (s && s.name) || '服务 #' + (row.serviceId || '—')
}
function coverOf(row) {
  const s = serviceMap.value[row.serviceId] || {}
  return { id: row.serviceId, categoryId: s.categoryId, name: s.name }
}

const budgetText = computed(() => {
  const d = detail.value || {}
  if (d.budgetMin == null && d.budgetMax == null) return '未填写'
  return money(d.budgetMin) + ' — ' + money(d.budgetMax)
})

// 轨迹只由真实时间戳构成;当前状态为履约中时补一条无时间的现状说明
const trail = computed(() => {
  const d = detail.value || {}
  const list = [{ name: '买家下单', text: '订单已提交，等待商家处理', time: fmt(d.createdAt) }]
  if (d.confirmedAt) list.push({ name: '商家接单', text: '订单已确认，进入履约准备', time: fmt(d.confirmedAt) })
  if (d.status === 'IN_PROGRESS') list.push({ name: '履约中', text: '商家标记为履约中', time: '当前状态' })
  if (d.completedAt) list.push({ name: '履约完成', text: '订单已完成，可发起售后', time: fmt(d.completedAt) })
  if (d.cancelledAt) list.push({ name: '订单取消', text: d.cancelReason || '订单已取消', time: fmt(d.cancelledAt) })
  return list
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 服务端分页:返回 {list, total, totalPages, page, size}
    const params = { page: page.value, size: size.value }
    if (status.value) params.status = status.value
    const data = await http.get('/merchant/orders', { params })
    orders.value = data.list
    total.value = data.total
  } catch (e) {
    orders.value = []
    total.value = 0
    error.value = (e && e.message) || '订单加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

// 只读的服务名字典,不动订单列表自身的 page/size
async function loadNames() {
  try {
    const data = await http.get('/merchant/services', { params: { page: 0, size: 100 } })
    const map = {}
    ;(data.list || []).forEach(s => { map[s.id] = { name: s.name, categoryId: s.categoryId } })
    serviceMap.value = map
  } catch {
    serviceMap.value = {}
  }
}

function setStatus(k) { status.value = k; page.value = 0; load() }
function onPage(p) { page.value = p - 1; load() }

function openDetail(row) {
  savedScroll.value = window.scrollY || 0
  detail.value = { ...row }
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

const acting = ref(false)

async function act(type, row) {
  // 防连点:履约推进是两段流转(已确认→履约中→已完成),
  // 连点两下会一步跳到「已完成」,必须等上一次请求回来再放行。
  if (acting.value) return
  acting.value = true
  try {
    if (type === 'confirm') {
      await http.post(`/merchant/orders/${row.id}/confirm`)
      ElMessage.success('已接单')
    } else if (type === 'reject') {
      const { value } = await ElMessageBox.prompt('请输入拒单原因', '拒单', { inputType: 'textarea' })
      await http.post(`/merchant/orders/${row.id}/reject`, null, { params: { reason: value } })
      ElMessage.success('已拒单')
    } else {
      // 同一颗按钮走两段流转：已确认→履约中、履约中→已完成，提示按目标状态区分，
      // 否则"履约中推进到已完成"也提示"已推进履约"，商家看不出到底动了哪一步。
      const from = row.status
      await http.post(`/merchant/orders/${row.id}/progress`)
      ElMessage.success(from === 'IN_PROGRESS' ? '已完成履约，订单转入已完成' : '已开始履约')
    }
  } catch {
    // 取消输入或接口失败都由 ElMessageBox / 拦截器各自提示,这里只负责不刷新
    return
  } finally {
    acting.value = false
  }
  await load()
  if (detail.value) {
    const fresh = orders.value.find(o => o.id === detail.value.id)
    if (fresh) detail.value = { ...fresh }
  }
}

// 打开售后弹窗:默认回填该行已有的售后状态与说明,没有则默认"处理中"
function openAfterSale(row) {
  afterSaleForm.id = row.id
  afterSaleForm.orderNo = row.orderNo
  afterSaleForm.orderStatus = row.status
  afterSaleForm.status = row.afterSaleStatus || 1
  afterSaleForm.note = row.afterSaleNote || ''
  afterSaleVisible.value = true
}

async function doAfterSale() {
  // 本地只拦"说明为空"这一种,其余错误交给 http.js 拦截器统一提示
  if (!afterSaleForm.note.trim()) {
    ElMessage.warning('请填写售后处理说明')
    return
  }
  await http.post(`/merchant/orders/${afterSaleForm.id}/after-sale`, {
    status: afterSaleForm.status,
    note: afterSaleForm.note.trim()
  })
  ElMessage.success('售后处理已保存')
  afterSaleVisible.value = false
  await load()
  if (detail.value) {
    const fresh = orders.value.find(o => o.id === detail.value.id)
    if (fresh) detail.value = { ...fresh }
  }
}

onMounted(() => { loadNames(); load() })
</script>

<style scoped>
.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-5);
}
.detail-no {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-4);
  margin: var(--space-2) 0 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.detail-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-2);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: var(--space-5);
}
.detail-svc {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: var(--space-4);
  align-items: center;
  padding-bottom: var(--space-4);
  margin-bottom: var(--space-2);
  border-bottom: 1px solid var(--border);
}
.trail-cap {
  margin: 0 0 var(--space-4);
}
.trail {
  list-style: none;
  margin: 0;
  padding: 0;
}
.trail-item {
  position: relative;
  padding: 0 0 var(--space-4) var(--space-5);
  border-left: 1px solid var(--border);
}
.trail-item::before {
  content: '';
  position: absolute;
  left: -3px;
  top: var(--space-2);
  width: var(--space-2);
  height: var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--rose-500);
}
.trail-item.is-last {
  border-left-color: transparent;
  padding-bottom: 0;
}
.trail-title {
  margin: 0;
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--ink);
}
.trail-text {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.trail-time {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.trail-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}
.sk-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.sk-thumb {
  display: block;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: sk-pulse 1.4s ease-in-out infinite;
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
  .sk-thumb,
  .sk-lines i {
    animation: none;
  }
}
</style>
