<template>
  <div class="svc-detail">
    <!-- 加载中 -->
    <div v-if="loading" class="sk-detail" aria-hidden="true">
      <div class="sk-banner"></div>
      <div class="sk-lines">
        <div class="sk-line sk-line--lg"></div>
        <div class="sk-line"></div>
        <div class="sk-line"></div>
        <div class="sk-line sk-line--short"></div>
      </div>
    </div>

    <!-- 服务不存在 -->
    <div v-else-if="!service" class="state" role="alert">
      <el-icon class="state-icon"><WarningFilled /></el-icon>
      <p class="state-title">服务不存在或已下架</p>
      <p class="state-note">该服务可能已被商家移除，去看看其它婚礼服务吧。</p>
      <el-button type="primary" @click="$router.push('/services')">返回全部服务</el-button>
    </div>

    <template v-else>
      <!-- 主视觉 -->
      <section class="banner" :class="{ 'banner--empty': bannerBroken }">
        <img
          v-show="!bannerBroken"
          class="banner-img"
          :src="bannerSrc"
          :alt="`${categoryName(service.categoryId)}实拍：${service.name}`"
          @error="bannerBroken = true"
        />
        <div class="banner-veil" aria-hidden="true"></div>
        <div class="banner-body">
          <p class="banner-eyebrow">{{ categoryName(service.categoryId) }} · {{ merchant?.shopName || '缘集精选' }}</p>
          <h1 class="banner-title">{{ service.name }}</h1>
          <p class="banner-meta">
            <el-icon class="banner-star"><StarFilled /></el-icon>
            <span>{{ scoreText(service.score) }}</span>
            <span class="banner-dot" aria-hidden="true">·</span>
            <span>{{ service.salesCount || 0 }} 人已约</span>
            <span class="banner-dot" aria-hidden="true">·</span>
            <span>{{ service.viewCount || 0 }} 次浏览</span>
          </p>
        </div>
      </section>

      <div class="layout">
        <div class="main">
          <!-- 服务介绍 -->
          <section class="block">
            <h2 class="block-title">服务介绍</h2>
            <p class="block-body">{{ introText }}</p>
            <div v-if="!service.content" class="block-note">
              <span class="note-text">详细服务内容可与商家在线确认</span>
              <el-button link type="primary" @click="startConsult">咨询商家</el-button>
            </div>
          </section>

          <!-- 案例素材 + 可预约档期：同一行，矮而整 -->
          <div class="pair">
            <section class="block">
              <h2 class="block-title">案例素材</h2>
              <div v-if="cases.length" class="cases">
                <figure v-for="c in cases" :key="c.id" class="case">
                  <img :src="c.imageUrl" :alt="c.title || '案例'" class="case-img" />
                  <figcaption class="case-cap">
                    <span class="case-title">{{ c.title || '案例' }}</span>
                    <span class="case-desc">{{ c.description }}</span>
                  </figcaption>
                </figure>
              </div>
              <div v-else class="hint">
                <el-icon class="hint-icon"><Picture /></el-icon>
                <div class="hint-body">
                  <p class="hint-text">商家尚未上传案例，可先咨询档期与风格</p>
                  <el-button link type="primary" @click="startConsult">咨询商家</el-button>
                </div>
              </div>
            </section>

            <section class="block">
              <h2 class="block-title">可预约档期</h2>
              <div v-if="schedules.length" class="sched-list">
                <div class="schedules">
                  <el-tag
                    v-for="s in schedules"
                    :key="s.id"
                    :type="s.status === 1 ? 'success' : 'info'"
                    class="sched-tag"
                  >{{ s.scheduleDate }}<span v-if="s.status !== 1" class="sched-state">已约</span></el-tag>
                </div>
                <p v-if="hasBooked" class="sched-note">已被预约的日期不能重复下单</p>
              </div>
              <div v-else class="hint">
                <el-icon class="hint-icon"><Calendar /></el-icon>
                <div class="hint-body">
                  <p class="hint-text">商家尚未发布可预约档期</p>
                  <el-button link type="primary" @click="orderVisible = true">立即预约</el-button>
                </div>
              </div>
            </section>
          </div>
        </div>

        <aside class="aside">
          <!-- 价格与商家同卡：一条发丝线分隔，避免右侧两张小卡拼出断层 -->
          <div class="side-card">
          <div class="buy">
            <p class="buy-label">服务价格</p>
            <p class="buy-price">
              <em>¥</em>{{ priceText(service.price) }}<span class="buy-unit">/{{ service.unit || '场' }}</span>
            </p>
            <el-button type="primary" class="buy-btn" @click="orderVisible = true">立即预约</el-button>
            <el-button class="buy-btn" @click="toggleFavorite">{{ favorited ? '已收藏' : '收藏' }}</el-button>
            <el-button link type="primary" class="buy-consult" @click="startConsult">咨询商家</el-button>
          </div>

          <div v-if="merchant" class="shop">
            <p class="buy-label">所属商家</p>
            <p class="shop-name">{{ merchant.shopName }}</p>
            <p class="shop-scope">{{ merchant.businessScope }}</p>
            <p class="shop-desc">{{ merchant.description }}</p>
            <el-button link type="primary" @click="$router.push('/merchants')">查看商家</el-button>
          </div>
          </div>
        </aside>
      </div>
    </template>

    <el-dialog v-model="orderVisible" title="提交预约" width="480px">
      <el-form ref="orderFormRef" :model="order" :rules="orderRules" label-width="90px">
        <el-form-item label="婚礼日期" prop="weddingDate">
          <!-- 说明放在选择器上方：日历弹层向下展开时不会盖住它 -->
          <p class="date-note">灰色日期已被预约或已过期，选择其他日期</p>
          <el-date-picker v-model="order.weddingDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledDate" style="width:100%" />
        </el-form-item>
        <el-form-item label="宾客人数" prop="guestCount"><el-input-number v-model="order.guestCount" :min="1" :max="2000" style="width:100%" /></el-form-item>
        <el-form-item label="数量" prop="quantity"><el-input-number v-model="order.quantity" :min="1" :max="99" style="width:100%" /></el-form-item>
        <el-form-item label="预算下限" prop="budgetMin"><el-input-number v-model="order.budgetMin" :min="0" :max="9999999" :controls="false" style="width:100%" /></el-form-item>
        <el-form-item label="预算上限" prop="budgetMax"><el-input-number v-model="order.budgetMax" :min="0" :max="9999999" :controls="false" style="width:100%" /></el-form-item>
        <el-form-item label="备注" prop="remark"><el-input v-model="order.remark" type="textarea" :maxlength="200" show-word-limit :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitOrder">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { getToken } from '@/store/auth'
import { coverFor, categoryName } from '@/lib/imagery'

const route = useRoute()
const router = useRouter()
const service = ref(null)
const cases = ref([])
const schedules = ref([])
const occupiedDates = ref([])
const merchant = ref(null)
const favorited = ref(false)
const loading = ref(true)
const bannerBroken = ref(false)

const orderVisible = ref(false)
const submitting = ref(false)
const orderFormRef = ref(null)
const order = reactive({ weddingDate: null, guestCount: null, quantity: 1, budgetMin: null, budgetMax: null, remark: '' })

// 预约表单校验:日期必填且不早于今天、且未被占用;人数/数量必填有区间;预算可空,但填了就不能下限高于上限。
const beforeToday = d => {
  const t = new Date(); t.setHours(0, 0, 0, 0)
  return d.getTime() < t.getTime()
}
// 后端给的占用清单是 yyyy-MM-dd;日历回调给的是 Date,按本地时区格式化才不会差一天
const pad2 = n => String(n).padStart(2, '0')
const fmtDate = d => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
const isOccupied = dateStr => !!dateStr && occupiedDates.value.includes(String(dateStr))
// 灰掉:早于今天,或后端说这一天已被订单 / 档期占住
const disabledDate = d => beforeToday(d) || isOccupied(fmtDate(d))
const orderRules = {
  weddingDate: [
    { required: true, message: '请选择婚礼日期', trigger: 'change' },
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        const today = new Date(); today.setHours(0, 0, 0, 0)
        const picked = new Date(String(v).replace(/-/g, '/'))
        return picked.getTime() < today.getTime() ? cb(new Error('婚礼日期不能早于今天')) : cb()
      },
      trigger: 'change'
    },
    // 兜住"选中之后被占用"的情况:打开弹窗时是空的,期间别人下了单,或页面数据刷新
    {
      validator: (_r, v, cb) => (!v || !isOccupied(v)) ? cb() : cb(new Error('该日期已被预约，请选择其他日期')),
      trigger: 'change'
    }
  ],
  guestCount: [
    { required: true, message: '请填写宾客人数', trigger: 'change' },
    {
      validator: (_r, v, cb) => (v && v >= 1 && v <= 2000) ? cb() : cb(new Error('宾客人数需在 1–2000 之间')),
      trigger: 'change'
    }
  ],
  quantity: [
    { required: true, message: '请填写数量', trigger: 'change' },
    {
      validator: (_r, v, cb) => (v && v >= 1 && v <= 99) ? cb() : cb(new Error('数量需在 1–99 之间')),
      trigger: 'change'
    }
  ],
  budgetMax: [
    {
      validator: (_r, v, cb) => {
        if (v == null || order.budgetMin == null) return cb()
        return Number(v) < Number(order.budgetMin) ? cb(new Error('预算上限不能低于下限')) : cb()
      },
      trigger: 'change'
    }
  ]
}

const bannerSrc = computed(() => coverFor(service.value))
// 后端会把已被订单占用的档期置 status=0；只要有一条不在售，就给一句不能重复下单的提示
const hasBooked = computed(() => schedules.value.some(s => s.status !== 1))
const introText = computed(() => service.value?.content || service.value?.description || '商家尚未填写服务介绍。')

const priceText = v => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '—'
}
const scoreText = v => {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '—'
}

async function load() {
  try {
    const data = await http.get(`/services/${route.params.id}`)
    service.value = data.service
    cases.value = data.cases || []
    schedules.value = data.schedules || []
    occupiedDates.value = data.occupiedDates || []
    if (service.value) {
      try {
        merchant.value = await http.get(`/merchants/${service.value.merchantId}`)
      } catch (e) {
        merchant.value = null
      }
    }
  } catch (e) {
    service.value = null
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!requireLogin()) return
  await http.post(`/favorites?serviceId=${route.params.id}`)
  favorited.value = true
  ElMessage.success('已收藏')
}

// 会话列表页读这个键，直接把刚建立的会话打开：点「咨询商家」就该进对话，不该落回列表再找一次
const OPEN_CONSULT_KEY = 'consult:openId'

async function startConsult() {
  if (!requireLogin()) return
  const c = await http.post('/consultations', { merchantId: service.value.merchantId, serviceId: service.value.id })
  if (c?.id) sessionStorage.setItem(OPEN_CONSULT_KEY, String(c.id))
  ElMessage.success('已发起咨询')
  router.push('/user/consultations')
}

async function submitOrder() {
  if (!requireLogin()) return
  try {
    await orderFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await http.post('/orders', { ...order, serviceId: service.value.id })
    ElMessage.success('预约成功,等待商家接单')
    orderVisible.value = false
    order.weddingDate = null
    order.guestCount = null
    order.quantity = 1
    order.budgetMin = null
    order.budgetMax = null
    order.remark = ''
  } finally {
    submitting.value = false
  }
}

function requireLogin() {
  if (!getToken()) { ElMessage.warning('请先登录'); router.push('/login'); return false }
  return true
}

onMounted(load)
</script>

<style scoped>
/* ===== 主视觉 ===== */
.banner {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--ink);
  aspect-ratio: 21 / 9;
}
.banner-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(.82) saturate(1.02);
}
/* 两层遮罩：左深右浅压住标题与价格，底部再压一层，保证白字在亮照片上可读 */
.banner-veil {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(100deg, rgba(38, 24, 30, .86) 0%, rgba(38, 24, 30, .55) 38%, rgba(38, 24, 30, .12) 68%, rgba(38, 24, 30, 0) 100%),
    linear-gradient(to top, rgba(38, 24, 30, .55) 0%, rgba(38, 24, 30, 0) 45%);
}
.banner-body {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: var(--space-6);
}
.banner-eyebrow {
  margin: 0 0 var(--space-2);
  color: var(--rose-200);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: .12em;
}
.banner-title {
  color: #fff;
  font-size: var(--fs-display);
  font-weight: 600;
  line-height: var(--lh-display);
  letter-spacing: .02em;
  margin: 0;
  text-shadow: 0 2px 28px rgba(0, 0, 0, .32);
}
.banner-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-3) 0 0;
  color: #fff;
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  text-shadow: 0 1px 12px rgba(0, 0, 0, .35);
}
.banner-star { color: var(--gold-400); font-size: var(--fs-sm); }
.banner-dot { color: var(--rose-200); }

/* ===== 栅格：正文 + 侧栏 ===== */
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1.75fr) minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
  margin-top: var(--space-6);
}
/* main 拉伸到行高，由 .pair 吸收剩余空间，使左右两栏底边对齐（不写死像素） */
.main { display: flex; flex-direction: column; gap: var(--space-6); min-width: 0; align-self: stretch; }
.block {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}
.block-title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
  margin: 0;
}
.block-body {
  margin: var(--space-3) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-base);
  line-height: var(--lh-body);
}
.block-note {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}
.note-text { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }

/* 案例 / 档期：同一行，两块都矮；flex:1 吸收侧栏更高时多出来的高度 */
.pair { flex: 1; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-6); }
/* 第二行 1fr 吃掉多出来的高度，提示块在行内居中（不用 auto margin，否则计算值会掉出 4px 栅格） */
.pair > .block { display: grid; grid-template-rows: auto 1fr; row-gap: var(--space-4); }
.hint { align-self: center; display: flex; align-items: flex-start; gap: var(--space-3); }
.hint-icon { flex: none; font-size: var(--fs-lg); color: var(--ink-faint); }
.hint-body { display: flex; flex-direction: column; align-items: flex-start; gap: var(--space-2); min-width: 0; }
.hint-text { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.cases { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-4); margin-top: var(--space-4); }
.case { margin: 0; }
.case-img { display: block; width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: var(--radius); background: var(--bg-deep); }
.case-cap { display: flex; flex-direction: column; gap: var(--space-1); margin-top: var(--space-2); }
.case-title { font-family: var(--font-display); font-size: var(--fs-sm); font-weight: 600; color: var(--ink); }
.case-desc { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-body); }

.sched-list { display: flex; flex-direction: column; gap: var(--space-3); margin-top: var(--space-4); }
.schedules { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.sched-tag { margin: 0; }
.sched-state { margin-left: var(--space-2); font-size: var(--fs-xs); line-height: var(--lh-ui); opacity: .72; }
.sched-note { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
/* 日期选择器下方一行轻声说明:灰色 = 已被预约或已过期 */
.date-note { flex: 0 0 100%; margin: 0 0 var(--space-1); color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

/* ===== 侧栏 ===== */
.aside { position: sticky; top: var(--space-5); }
.side-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}
.buy { display: flex; flex-direction: column; align-items: stretch; }
/* 12px 标签：--ink-faint 实测 3.4:1 不过 AA，用 --ink-soft 保持同级视觉重量 */
.buy-label { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .08em; }
.buy-price {
  margin: var(--space-2) 0 var(--space-4);
  color: var(--rose-500);
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: var(--lh-display);
}
.buy-price em { font-style: normal; font-size: var(--fs-sm); margin-right: var(--space-1); vertical-align: super; }
.buy-unit { color: var(--ink-soft); font-size: var(--fs-xs); font-weight: 400; margin-left: var(--space-1); }
.buy :deep(.el-button) { margin-left: 0; }
/* 文字按钮补足 24px 触控高度（默认 20px） */
:deep(.el-button.is-link) { padding: var(--space-1) 0; height: auto; }
.buy-btn { width: 100%; }
.buy :deep(.el-button + .el-button) { margin-top: var(--space-2); }
.buy-consult { align-self: center; margin-top: var(--space-3); }

.shop {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-1);
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  border-top: 1px solid var(--border);
}
.shop-name { margin: var(--space-2) 0 0; font-family: var(--font-display); font-size: var(--fs-lg); font-weight: 600; color: var(--ink); line-height: var(--lh-ui); }
.shop-scope { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.shop-desc { margin: var(--space-2) 0 var(--space-3); color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== 骨架 / 状态 ===== */
.sk-detail { display: flex; flex-direction: column; gap: var(--space-6); }
.sk-banner { aspect-ratio: 21 / 9; border-radius: var(--radius-lg); background: var(--bg-deep); }
.sk-lines { display: flex; flex-direction: column; gap: var(--space-3); }
.sk-line { height: var(--space-4); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 40%; }
.sk-line--short { width: 25%; }
.sk-banner, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-banner, .sk-line { animation: none; }
}

.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  text-align: center;
  padding: var(--space-9) var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.state-icon { font-size: var(--fs-xl); color: var(--ink-faint); }
.state-title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
  margin: 0;
}
.state-note { margin: 0 0 var(--space-2); color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

@media (max-width: 1080px) {
  .layout { grid-template-columns: minmax(0, 1fr); }
  .aside { position: static; }
}
@media (max-width: 768px) {
  .pair { grid-template-columns: minmax(0, 1fr); }
  .banner { aspect-ratio: 4 / 3; }
  .banner-body { padding: var(--space-4); }
  .banner-title { font-size: var(--fs-xl); }
}
</style>
