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
      <!-- 正常态也要有就近出口：错误态有「返回全部服务」，正常态不能没有 -->
      <nav class="svc-bar" aria-label="面包屑">
        <button type="button" class="cd-back" @click="backToList">
          <el-icon><ArrowLeft /></el-icon><span>全部服务</span>
        </button>
        <p class="svc-crumb">{{ categoryName(service.categoryId) }} · {{ service.name }}</p>
      </nav>

      <!-- 下架就是下架：从历史订单点进来的服务照样能看，但必须一眼看出它已不在售 -->
      <div v-if="isOffShelf" class="svc-off" role="status">
        <el-icon class="svc-off-icon"><WarningFilled /></el-icon>
        <div class="svc-off-body">
          <p class="svc-off-title">该服务已下架</p>
          <p class="svc-off-note">商家已停止售卖，页面仅供查看，暂不可预约。如需同类服务，可<router-link class="svc-off-link" to="/services">返回全部服务</router-link>继续挑选。</p>
        </div>
      </div>

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

          <!-- 案例素材：全宽单列，三张一行放下；不再和档期挤同一行抢高度 -->
          <section class="block">
            <h2 class="block-title">案例素材</h2>
            <!-- 案例卡片可点开：大图/视频 + 完整说明在页内全屏看，列表里只露标题与首段 -->
            <div v-if="cases.length" class="cases">
              <button
                v-for="c in cases"
                :key="c.id"
                type="button"
                class="case"
                :aria-label="`查看案例详情：${c.title || '案例'}`"
                @click="openCase(c)"
              >
                <span class="case-thumb">
                  <img :src="c.imageUrl" :alt="c.title || '案例'" class="case-img" />
                  <span v-if="c.videoUrl" class="case-play" aria-hidden="true">
                    <el-icon><VideoPlay /></el-icon>
                  </span>
                </span>
                <span class="case-title">{{ c.title || '案例' }}</span>
                <span class="case-desc">{{ c.description }}</span>
              </button>
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
            <!-- 概览一行 + 紧凑表：商家一键排整月时单服务可达上百条档期，平铺的 tag 会把页面撑爆 -->
            <div v-if="bookableDays.length" class="sched">
              <button
                type="button"
                class="sched-summary"
                aria-controls="sched-table"
                :aria-expanded="schedOpen ? 'true' : 'false'"
                @click="schedOpen = !schedOpen"
              >
                <el-icon class="sched-summary-icon"><Calendar /></el-icon>
                <span class="sched-summary-text">{{ schedSummary }}</span>
                <span class="sched-summary-action">{{ schedOpen ? '收起' : '展开' }}</span>
                <el-icon class="sched-caret" :class="{ 'is-open': schedOpen }"><ArrowDown /></el-icon>
              </button>

              <!-- 一天一行：时段做行内 chips，场地是同一天去重后的浅色补充文本 -->
              <div v-show="schedOpen" id="sched-table" class="sched-panel">
                <ul class="sched-rows">
                  <li v-for="d in visibleDays" :key="d.date" class="sched-row">
                    <span class="sched-date">{{ mdOf(d.date) }}</span>
                    <span class="sched-slots">
                      <span v-for="slot in d.slots" :key="slot" class="sched-chip">{{ slot }}</span>
                      <span v-if="d.venues.length" class="sched-venue">{{ d.venues.join(' · ') }}</span>
                    </span>
                  </li>
                </ul>
                <button
                  v-if="!showAllDays && bookableDays.length > SCHED_PREVIEW"
                  type="button"
                  class="sched-more"
                  @click="showAllDays = true"
                >查看全部 {{ bookableDays.length }} 天</button>
                <p class="sched-note">仅显示今天及以后的可约日期</p>
              </div>
            </div>
            <div v-else class="hint">
              <el-icon class="hint-icon"><Calendar /></el-icon>
              <div class="hint-body">
                <p class="hint-text">商家尚未发布可预约档期</p>
                <el-button link type="primary" :disabled="isOffShelf" @click="!isOffShelf && (orderVisible = true)">立即预约</el-button>
              </div>
            </div>
          </section>
        </div>

        <aside class="aside">
          <!-- 价格与商家同卡：一条发丝线分隔，避免右侧两张小卡拼出断层 -->
          <div class="side-card">
          <div class="buy">
            <p class="buy-label">服务价格</p>
            <p class="buy-price">
              <em>¥</em>{{ priceText(service.price) }}<span class="buy-unit">/{{ service.unit || '场' }}</span>
            </p>
            <el-button type="primary" class="buy-btn" :disabled="isOffShelf" @click="!isOffShelf && (orderVisible = true)">立即预约</el-button>
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

          <!-- 预约与保障：右栏取消粘性后，靠这张卡把侧栏撑到与正文相近的高度，底下不再空一片 -->
          <section class="side-card guard">
            <p class="buy-label">预约与保障</p>
            <ol class="guard-steps">
              <li><i>1</i><span>在上方「可预约档期」挑一天</span></li>
              <li><i>2</i><span>填婚礼日期、宾客人数与预算后提交</span></li>
              <li><i>3</i><span>商家确认后才进入履约</span></li>
            </ol>
            <ul class="guard-notes">
              <li>每一步状态都写入订单流水，可回溯</li>
              <li>出现分歧可发起争议举证</li>
              <li>订单完成后可公开评价</li>
            </ul>
          </section>
        </aside>

        <!-- 用户评价：独立请求、独立失败——评价挂了不影响上方正文与档期 -->
        <section class="block eval-block">
          <h2 class="block-title">用户评价</h2>

          <div v-if="evalError" class="eval-fallback" role="status">
            <span class="eval-fallback-text">{{ evalError }}</span>
            <el-button link type="primary" @click="loadEvaluations">重新加载</el-button>
          </div>

          <div v-else-if="evalLoading" class="eval-skel" aria-hidden="true">
            <i /><i /><i />
          </div>

          <div v-else-if="evalSummary.count > 0" class="eval-body">
            <div class="eval-sum">
              <p class="eval-score">
                <em>{{ evalScoreText }}</em>
                <span class="eval-score-cap">平均分</span>
              </p>
              <div class="eval-bars">
                <div v-for="row in evalBars" :key="row.star" class="eval-bar-row">
                  <span class="eval-bar-label">{{ row.star }} 星</span>
                  <span class="eval-bar-track">
                    <span class="eval-bar-fill" :style="{ width: row.pct + '%' }"></span>
                  </span>
                  <span class="eval-bar-num">{{ row.num }}</span>
                </div>
              </div>
              <p class="eval-count">共 {{ evalSummary.count }} 条评价</p>
            </div>

            <ul class="eval-list">
              <li v-for="e in evalSummary.list" :key="e.id" class="eval-item">
                <div class="eval-head">
                  <span class="eval-user">{{ e.userLabel || '匿名用户' }}</span>
                  <el-rate :model-value="Number(e.score) || 0" disabled />
                  <span class="eval-date">{{ dateOf(e.createdAt) }}</span>
                </div>
                <p v-if="e.content" class="eval-text">{{ e.content }}</p>
                <ul v-if="imgList(e.images).length" class="eval-shots">
                  <li v-for="(src, i) in imgList(e.images)" :key="src + i">
                    <el-image
                      :src="src"
                      fit="cover"
                      class="eval-shot"
                      :preview-src-list="imgList(e.images)"
                      :initial-index="i"
                      preview-teleported
                    />
                  </li>
                </ul>
                <video
                  v-if="e.videoUrl"
                  :src="e.videoUrl"
                  controls
                  preload="metadata"
                  class="eval-video"
                ></video>
                <div v-if="e.replyContent" class="eval-reply">
                  <span class="eval-reply-cap">商家回复</span>
                  <p class="eval-reply-text">{{ e.replyContent }}</p>
                </div>
              </li>
            </ul>
          </div>

          <p v-else class="eval-empty">暂无评价（完成订单后可评价）</p>
        </section>
      </div>
    </template>

    <el-dialog v-model="orderVisible" title="提交预约" width="480px">
      <el-form ref="orderFormRef" :model="order" :rules="orderRules" label-width="90px">
        <el-form-item label="婚礼日期" prop="weddingDate">
          <!-- 说明放在选择器上方：日历弹层向下展开时不会盖住它 -->
          <p class="date-note">{{ dateNote }}</p>
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

    <!-- 案例详情：页内全屏（不弹窗、不跳路由），返回即回到列表位置 -->
    <section
      v-if="caseDetail"
      class="case-detail"
      role="dialog"
      aria-modal="true"
      :aria-label="`案例详情：${caseDetail.title || '案例'}`"
    >
      <div class="cd-bar">
        <button type="button" class="cd-back" @click="closeCase">
          <el-icon><ArrowLeft /></el-icon><span>返回案例列表</span>
        </button>
        <p class="cd-crumb">{{ service?.name }} · 案例素材</p>
      </div>
      <div class="cd-body">
        <figure class="cd-media">
          <video
            v-if="caseDetail.videoUrl && !caseBroken"
            :src="caseDetail.videoUrl"
            :poster="caseDetail.imageUrl || undefined"
            controls
            preload="metadata"
            class="cd-video"
            @error="caseBroken = true"
          ></video>
          <img
            v-else-if="caseDetail.imageUrl && !caseBroken"
            :src="caseDetail.imageUrl"
            :alt="caseDetail.title || '案例'"
            class="cd-img"
            @error="caseBroken = true"
          />
          <div v-else class="cd-fallback">素材暂不可预览，可咨询商家索取</div>
        </figure>
        <div class="cd-text">
          <p class="cd-eyebrow">
            <el-tag size="small" effect="plain">{{ caseDetail.category || '其他' }}</el-tag>
            <span v-if="caseDetail.videoUrl" class="cd-suffix">影像素材</span>
          </p>
          <h2 class="cd-title">{{ caseDetail.title || '案例' }}</h2>
          <p class="cd-desc">{{ caseDetail.description || '商家尚未填写案例说明。' }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { getToken } from '@/store/auth'
import { coverFor, categoryName } from '@/lib/imagery'

const route = useRoute()
const router = useRouter()

// 退回列表：有来路就退回上一页，没有来路（直接输网址进来）兜底去全部服务
function backToList() {
  if (window.history.state && window.history.state.back) router.back()
  else router.push('/services')
}
const service = ref(null)
const cases = ref([])
const schedules = ref([])
const occupiedDates = ref([])
// 商家发布的可约日期(status=1 的档期)与"是否维护过档期":没维护过的服务按天报价,日历不限日期
const bookableDates = ref([])
const hasSchedule = ref(false)
const caseDetail = ref(null)
const caseBroken = ref(false)
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
// 这一天商家是否发布过可约档期:没维护过档期的服务不限日期,维护过就只放开 status=1 的日子
const isBookable = dateStr => !hasSchedule.value || bookableDates.value.includes(String(dateStr))
// 灰掉:早于今天,或商家没把这一天放出来,或这一天已被订单 / 全部时段占住
const disabledDate = d => beforeToday(d) || !isBookable(fmtDate(d)) || isOccupied(fmtDate(d))
const dateNote = computed(() => hasSchedule.value
  ? '只能选商家已发布的档期日期，灰色表示未开放、已被预约或已过期'
  : '灰色日期已被预约或已过期，选择其他日期')
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
    },
    // 日期不在商家发布的档期里(选中后档期被撤、或直接绕过日历改值)
    {
      validator: (_r, v, cb) => (!v || isBookable(v)) ? cb() : cb(new Error('该日期未开放预约，请选择档期中可约的日期')),
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

// ===== 档期展示：概览一行 + 紧凑表 =====
const SLOT_ORDER = ['整天', '上午', '下午', '晚间']
const SCHED_PREVIEW = 12   // 展开后默认最多显示的天数
const schedOpen = ref(false)
const showAllDays = ref(false)

const localToday = () => { const t = new Date(); t.setHours(0, 0, 0, 0); return t }
const mdOf = date => String(date).slice(5)
const slotRank = slot => { const i = SLOT_ORDER.indexOf(slot); return i < 0 ? SLOT_ORDER.length : i }

// 按天分组：只认 status===1 且不早于今天的时段。已约时段不进 chips，全天已约的日期整行不出现(与日历整日置灰同口径)
const bookableDays = computed(() => {
  const todayStr = fmtDate(localToday())
  const byDate = new Map()
  for (const s of schedules.value) {
    if (s.status !== 1) continue
    const date = String(s.scheduleDate || '').slice(0, 10)
    if (!date || date < todayStr) continue
    let day = byDate.get(date)
    if (!day) { day = { date, slots: [], venues: [] }; byDate.set(date, day) }
    const slot = s.slot || '整天'
    if (!day.slots.includes(slot)) day.slots.push(slot)
    if (s.venue && !day.venues.includes(s.venue)) day.venues.push(s.venue)
  }
  return [...byDate.values()]
    .sort((a, b) => a.date.localeCompare(b.date))
    .map(d => ({ ...d, slots: d.slots.slice().sort((a, b) => slotRank(a) - slotRank(b)) }))
})

// N 就是展开后的行数(全部未来的可约天数)，两个数永远相等；"最早"取最早的一天
const schedSummary = computed(() => {
  const first = bookableDays.value[0]
  if (!first) return ''
  return `共 ${bookableDays.value.length} 天可约 · 最早 ${mdOf(first.date)}`
})
const visibleDays = computed(() => showAllDays.value ? bookableDays.value : bookableDays.value.slice(0, SCHED_PREVIEW))
const introText = computed(() => service.value?.content || service.value?.description || '商家尚未填写服务介绍。')

const priceText = v => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '—'
}
const scoreText = v => {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '—'
}

// 接口 /services/{id} 原样返回实体，shelfStatus（1 上架 / 0 下架）就在里面 —— 只读这个字段，不额外请求
const isOffShelf = computed(() => !!service.value && Number(service.value.shelfStatus) !== 1)

async function load() {
  try {
    const data = await http.get(`/services/${route.params.id}`)
    service.value = data.service
    cases.value = data.cases || []
    schedules.value = data.schedules || []
    occupiedDates.value = data.occupiedDates || []
    bookableDates.value = data.bookableDates || []
    hasSchedule.value = !!data.hasSchedule
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

// 案例详情：页内全屏视图，开关就是这一个 ref —— 不跳路由，返回不丢列表位置
function openCase(c) {
  caseDetail.value = c
  caseBroken.value = false
}
function closeCase() {
  caseDetail.value = null
  caseBroken.value = false
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

// ===== 用户评价：独立于详情请求，失败只降级这一段 =====
const evalLoading = ref(true)
const evalError = ref('')
const evalSummary = ref({ count: 0, average: 0, distribution: {}, list: [] })

// 平均分保留 1 位小数；没有评价时给「—」，不把 0.0 当成真实评分
const evalScoreText = computed(() => {
  const n = Number(evalSummary.value.average)
  return evalSummary.value.count > 0 && Number.isFinite(n) ? n.toFixed(1) : '—'
})

// 5→1 星分布：条数与占比都由后端给的 distribution 算，不自己再统计
const evalBars = computed(() => {
  const dist = evalSummary.value.distribution || {}
  const total = Number(evalSummary.value.count) || 0
  return [5, 4, 3, 2, 1].map(star => {
    const num = Number(dist[String(star)]) || 0
    return { star, num, pct: total > 0 ? Math.round((num / total) * 100) : 0 }
  })
})

// images 是逗号分隔的 url 字符串；空串 / null 一律当没有，不渲染空壳
const imgList = v => String(v || '').split(',').map(s => s.trim()).filter(Boolean)
// createdAt 是 yyyy-MM-ddTHH:mm:ss，取日期部分
const dateOf = t => (t ? String(t).slice(0, 10) : '')

async function loadEvaluations() {
  evalLoading.value = true
  evalError.value = ''
  try {
    const data = await http.get(`/services/${route.params.id}/evaluations`)
    evalSummary.value = {
      count: Number(data?.count) || 0,
      average: data?.average,
      distribution: data?.distribution || {},
      list: Array.isArray(data?.list) ? data.list : []
    }
  } catch (e) {
    // 降级：只在这一段给一句提示，整页照常渲染
    evalSummary.value = { count: 0, average: 0, distribution: {}, list: [] }
    evalError.value = '评价加载失败，稍后重试'
  } finally {
    evalLoading.value = false
  }
}

// 两个请求并行发出，互不等待；两边都各自 catch，不会有一个把另一个拖挂
onMounted(() => {
  load()
  loadEvaluations()
})

// 同一路由只换 :id 时 Vue 会复用组件、不重新挂载，页面会一直停在上一个服务上。
// 从订单详情或外部链接直接换 id 都会踩到，所以必须盯着参数重拉。
watch(() => route.params.id, (id, old) => {
  if (!id || id === old) return
  loading.value = true
  service.value = null
  cases.value = []
  schedules.value = []
  schedOpen.value = false
  load()
  loadEvaluations()
})
</script>

<style scoped>
/* ===== 主视觉 ===== */
.svc-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.svc-crumb { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
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
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: var(--space-6);
  align-items: start;
  margin-top: var(--space-6);
}
/* 正文单列：每块独占正文宽度，不再被两列平分后各自留白 */
.main { display: flex; flex-direction: column; gap: var(--space-6); min-width: 0; }
/* 评价横跨两栏：侧栏本就比正文短，让正文收口在整个版心宽度上，右下角不再空一块 */
.eval-block { grid-column: 1 / -1; }
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

/* 案例与档期不再并排：两块内容体量天生不对等，并排必有一侧空着 */
.hint { align-self: center; display: flex; align-items: flex-start; gap: var(--space-3); }
.hint-icon { flex: none; font-size: var(--fs-lg); color: var(--ink-faint); }
.hint-body { display: flex; flex-direction: column; align-items: flex-start; gap: var(--space-2); min-width: 0; }
.hint-text { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.cases { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: var(--space-4); margin-top: var(--space-4); }
/* 卡片本身是 button：键盘可达、点击语义明确，视觉上仍是图 + 标题 + 说明 */
.case {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin: 0;
  padding: 0;
  border: 0;
  background: none;
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.case:focus-visible { outline: 2px solid var(--rose-500); outline-offset: 3px; border-radius: var(--radius); }
.case-thumb { position: relative; display: block; }
.case-thumb::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius);
  background: rgba(38, 24, 30, 0);
  transition: background .18s ease;
}
.case:hover .case-thumb::after { background: rgba(38, 24, 30, .18); }
.case-img { display: block; width: 100%; aspect-ratio: 4 / 3; object-fit: cover; border-radius: var(--radius); background: var(--bg-deep); }
/* 有视频的案例:缩略图角标，点开就是全屏详情 */
.case-play {
  position: absolute;
  top: var(--space-2);
  right: var(--space-2);
  display: grid;
  place-items: center;
  width: var(--space-6);
  height: var(--space-6);
  border-radius: 999px;
  background: rgba(38, 24, 30, .62);
  color: #fff;
  font-size: var(--fs-sm);
}
.case-title { font-family: var(--font-display); font-size: var(--fs-sm); font-weight: 600; color: var(--ink); }
.case-desc {
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-body);
  /* 说明允许写详细(最长 500 字):详情页整段展示,不截断 */
  white-space: pre-wrap;
  word-break: break-word;
}

/* ===== 案例详情：页内全屏 ===== */
.case-detail {
  position: fixed;
  inset: 0;
  z-index: 60;
  overflow-y: auto;
  padding: var(--space-5) clamp(var(--space-5), 6vw, var(--space-8)) var(--space-7);
  background: var(--bg);
}
.cd-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  max-width: 1080px;
  margin: 0 auto var(--space-5);
}
.cd-back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface);
  color: var(--ink);
  font: inherit;
  font-size: var(--fs-sm);
  cursor: pointer;
}
.cd-back:hover { border-color: var(--rose-400); color: var(--rose-500); }
.cd-crumb { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.cd-body {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
  max-width: 1080px;
  margin: 0 auto;
}
.cd-media { margin: 0; overflow: hidden; border-radius: var(--radius-lg); background: var(--ink); }
.cd-img, .cd-video { display: block; width: 100%; max-height: 72vh; object-fit: contain; background: var(--ink); }
.cd-fallback {
  display: grid;
  place-items: center;
  min-height: 320px;
  padding: var(--space-5);
  color: var(--rose-200);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
}
.cd-text { display: flex; flex-direction: column; gap: var(--space-3); min-width: 0; }
.cd-eyebrow { display: flex; align-items: center; gap: var(--space-2); margin: 0; }
.cd-suffix { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.cd-title {
  margin: 0;
  color: var(--ink);
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: var(--lh-display);
}
.cd-desc {
  margin: 0;
  color: var(--ink-soft);
  font-size: var(--fs-base);
  line-height: var(--lh-body);
  white-space: pre-wrap;
  word-break: break-word;
}
@media (max-width: 900px) {
  .cd-body { grid-template-columns: minmax(0, 1fr); }
}

/* ===== 可预约档期：一行概览 + 紧凑表 ===== */
.sched { display: flex; flex-direction: column; gap: var(--space-3); margin-top: var(--space-4); }
.sched-summary {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--bg);
  color: var(--ink);
  font: inherit;
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  text-align: left;
  cursor: pointer;
}
.sched-summary:hover { border-color: var(--rose-300); background: var(--rose-50); }
.sched-summary:focus-visible { outline: 2px solid var(--rose-500); outline-offset: 2px; }
.sched-summary-icon { flex: none; color: var(--rose-500); font-size: var(--fs-sm); }
.sched-summary-text { flex: 1; min-width: 0; }
.sched-summary-action { flex: none; color: var(--ink-soft); font-size: var(--fs-xs); }
.sched-caret { flex: none; color: var(--ink-soft); font-size: var(--fs-xs); transition: transform .18s ease; }
.sched-caret.is-open { transform: rotate(180deg); }

.sched-panel { display: flex; flex-direction: column; gap: var(--space-2); }
/* 全宽之后日期横向铺开：一行放 5~7 天，24 天只占 4 行，不再拉成一根长条 */
.sched-rows { display: grid; grid-template-columns: repeat(auto-fill, minmax(136px, 1fr)); gap: var(--space-2) var(--space-3); margin: 0; padding: 0; list-style: none; }
.sched-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-1);
  padding: var(--space-2) var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface);
}
.sched-date { color: var(--ink); font-size: var(--fs-sm); line-height: var(--lh-ui); font-variant-numeric: tabular-nums; }
.sched-slots { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); min-width: 0; }
.sched-chip {
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: var(--fs-xs);
  line-height: calc(var(--lh-ui) + .4);
}
.sched-venue { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.sched-more {
  align-self: flex-start;
  padding: var(--space-1) 0;
  border: 0;
  background: none;
  color: var(--rose-500);
  font: inherit;
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  cursor: pointer;
}
.sched-more:hover { color: var(--rose-600); text-decoration: underline; }
.sched-note { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
@media (prefers-reduced-motion: reduce) { .sched-caret { transition: none; } }
/* 900px 以下行内改单列：日期一行、时段 chips 另起一行 */
@media (max-width: 900px) {
  .sched-row { grid-template-columns: minmax(0, 1fr); gap: var(--space-1); }
}
/* 日期选择器下方一行轻声说明:灰色 = 已被预约或已过期 */
.date-note { flex: 0 0 100%; margin: 0 0 var(--space-1); color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

/* ===== 侧栏 ===== */
/* align-self:stretch 是 sticky 能生效的前提：栅格默认 start 会把侧栏压成内容高度，粘性没有可移动的行程 */
/* 侧栏静态贴顶：粘性会让价格卡跟着滚动走，改为用内容把侧栏撑到与正文相近的高度 */
.aside { display: flex; flex-direction: column; gap: var(--space-4); }
.side-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: var(--space-5);
}
.buy { display: flex; flex-direction: column; align-items: stretch; }
.guard-steps { margin: var(--space-3) 0 0; padding: 0; list-style: none; display: grid; gap: var(--space-2); }
.guard-steps li {
  display: grid; grid-template-columns: var(--space-5) minmax(0, 1fr);
  column-gap: var(--space-2); align-items: start;
  color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui);
}
.guard-steps i {
  display: grid; place-items: center; width: var(--space-5); height: var(--space-5);
  border-radius: var(--radius-pill); background: var(--rose-50); color: var(--rose-500);
  font-style: normal; font-size: var(--fs-xs); font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.guard-notes {
  margin: var(--space-4) 0 0; padding: var(--space-3) 0 0;
  border-top: 1px solid var(--border); list-style: none;
  display: grid; gap: var(--space-1);
  color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui);
}
.guard-notes li { position: relative; padding-left: var(--space-3); }
.guard-notes li::before {
  content: ''; position: absolute; left: 0; top: .55em;
  width: 4px; height: 4px; border-radius: 50%; background: var(--rose-300);
}
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
.svc-off {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
  padding: var(--space-4);
  margin-bottom: var(--space-4);
  border: 1px solid var(--border);
  border-left: 3px solid var(--rose-500);
  border-radius: var(--radius-sm);
  background: var(--bg);
}
.svc-off-icon { color: var(--rose-500); font-size: 1.1rem; line-height: 1.4; }
.svc-off-body { display: grid; row-gap: var(--space-1); }
.svc-off-title { margin: 0; font-size: var(--fs-sm); font-weight: 600; color: var(--ink); }
.svc-off-note { margin: 0; font-size: var(--fs-xs); color: var(--ink-soft); line-height: var(--lh-ui); }
.svc-off-link { color: var(--rose-500); text-decoration: underline; }
.state-title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
  margin: 0;
}
.state-note { margin: 0 0 var(--space-2); color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== 用户评价 ===== */
/* 宽版：汇总占左列、列表占右列。全宽之后如果只堆成一列，星级条形会被拉成一米长的横线 */
.eval-body {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
  margin-top: var(--space-4);
}
.eval-body > .eval-sum { margin-top: 0; }
.eval-body > .eval-list { margin-top: 0; }
.eval-sum {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: var(--space-3);
  align-items: center;
  margin-top: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius);
  background: var(--bg);
}
.eval-score { display: flex; flex-direction: column; gap: var(--space-1); margin: 0; }
.eval-score em {
  font-style: normal;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: var(--lh-display);
  color: var(--rose-500);
  font-variant-numeric: tabular-nums;
}
.eval-score-cap { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.eval-bars { display: flex; flex-direction: column; gap: var(--space-1); min-width: 0; }
.eval-bar-row { display: grid; grid-template-columns: 2.5rem minmax(0, 1fr) 2rem; gap: var(--space-2); align-items: center; }
.eval-bar-label { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.eval-bar-track { height: 6px; border-radius: var(--radius-pill); background: var(--bg-deep); overflow: hidden; }
/* 宽度由 JS 给百分比，0 条的星自然是一条空轨道 */
.eval-bar-fill { display: block; height: 100%; border-radius: var(--radius-pill); background: var(--gold-400); }
.eval-bar-num { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); text-align: right; font-variant-numeric: tabular-nums; }
.eval-count { grid-column: 1 / -1; margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

.eval-list { display: flex; flex-direction: column; margin: var(--space-4) 0 0; padding: 0; list-style: none; }
.eval-item { padding: var(--space-4) 0; border-top: 1px solid var(--border); }
.eval-item:first-child { padding-top: 0; border-top: 0; }
.eval-head { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); }
.eval-user { color: var(--ink); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.eval-date {
  margin-left: auto;
  color: var(--ink-faint);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  font-variant-numeric: tabular-nums;
}
.eval-text {
  margin: var(--space-2) 0 0;
  color: var(--ink);
  font-size: var(--fs-base);
  line-height: var(--lh-body);
  white-space: pre-wrap;
  word-break: break-word;
}
.eval-shots { display: flex; flex-wrap: wrap; gap: var(--space-2); margin: var(--space-3) 0 0; padding: 0; list-style: none; }
.eval-shot { display: block; width: 96px; height: 96px; border-radius: var(--radius-sm); background: var(--bg-deep); cursor: zoom-in; }
.eval-video { display: block; width: 100%; max-width: 320px; margin-top: var(--space-3); border-radius: var(--radius-sm); background: #000; }
.eval-reply { margin-top: var(--space-3); padding: var(--space-3); border-radius: var(--radius-sm); background: var(--rose-50); }
.eval-reply-cap { color: var(--rose-600); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.eval-reply-text {
  margin: var(--space-1) 0 0;
  color: var(--ink);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  white-space: pre-wrap;
  word-break: break-word;
}
.eval-empty {
  margin: var(--space-4) 0 0;
  padding: var(--space-6) var(--space-5);
  border-radius: var(--radius);
  background: var(--bg);
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  text-align: center;
}
.eval-fallback {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius);
  background: var(--bg);
}
.eval-fallback-text { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.eval-skel { display: flex; flex-direction: column; gap: var(--space-2); margin-top: var(--space-4); }
.eval-skel i { display: block; height: var(--space-4); border-radius: var(--radius-sm); background: var(--bg-deep); animation: sk-pulse 1.6s ease-in-out infinite; }
.eval-skel i:nth-child(2) { width: 80%; }
.eval-skel i:nth-child(3) { width: 45%; }
@media (prefers-reduced-motion: reduce) { .eval-skel i { animation: none; } }
@media (max-width: 900px) {
  .eval-body { grid-template-columns: minmax(0, 1fr); gap: var(--space-4); }
  .eval-count { grid-column: auto; }
  .eval-shot { width: 80px; height: 80px; }
  .eval-date { margin-left: 0; }
}

@media (max-width: 1080px) {
  .layout { grid-template-columns: minmax(0, 1fr); }
}
@media (max-width: 768px) {
  .banner { aspect-ratio: 4 / 3; }
  .banner-body { padding: var(--space-4); }
  .banner-title { font-size: var(--fs-xl); }
}
</style>
