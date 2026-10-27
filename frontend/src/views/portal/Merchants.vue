<template>
  <div class="mrc-page">
    <!-- ==================== 列表态 ==================== -->
    <template v-if="!cur">
      <header class="masthead">
        <h1 class="masthead-title">找商家</h1>
        <p class="masthead-sub">入驻前已通过资质审核，只展示过审信息 —— 场地、套餐、司仪、摄影、婚纱、婚车，按店铺一次看全</p>
      </header>

      <section class="toolbar" aria-label="商家筛选">
        <div class="field field--keyword">
          <label class="field-label" for="mrc-keyword">关键字</label>
          <el-input id="mrc-keyword" v-model="keyword" placeholder="商家名称 / 经营范围" clearable>
            <template #prefix><el-icon class="field-ico"><Search /></el-icon></template>
          </el-input>
        </div>
        <p class="result-count">
          <span class="result-label">共</span><em>{{ filtered.length }}</em><span class="result-label">家</span>
        </p>
      </section>

      <!-- 加载中：灰块骨架 -->
      <div v-if="loading" class="grid" aria-hidden="true">
        <div v-for="n in 6" :key="n" class="sk">
          <div class="sk-cover"></div>
          <div class="sk-body">
            <div class="sk-line sk-line--lg"></div>
            <div class="sk-line sk-line--pill"></div>
            <div class="sk-line"></div>
            <div class="sk-line sk-line--short"></div>
          </div>
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="state" role="alert">
        <el-icon class="state-icon"><WarningFilled /></el-icon>
        <p class="state-title">商家列表加载失败</p>
        <p class="state-note">网络或服务暂时不可用，稍后重试一次就好。</p>
        <el-button type="primary" @click="load">重新加载</el-button>
      </div>

      <!-- 空态：无入驻商家 -->
      <div v-else-if="!merchants.length" class="state">
        <el-icon class="state-icon"><Shop /></el-icon>
        <p class="state-title">暂无入驻商家</p>
        <p class="state-note">婚礼团队正在陆续完成资质审核，晚些时候再来看看。</p>
        <el-button @click="$router.push('/services')">先逛全部服务</el-button>
      </div>

      <!-- 空态：关键字无匹配 -->
      <div v-else-if="!filtered.length" class="state">
        <el-icon class="state-icon"><Search /></el-icon>
        <p class="state-title">没有匹配的商家</p>
        <p class="state-note">换个关键字再试试，比如「摄影」「婚车」「场地」。</p>
        <el-button @click="keyword = ''">清空关键字</el-button>
      </div>

      <!-- 正常态 -->
      <div v-else class="grid">
        <article
          v-for="m in filtered"
          :key="m.id"
          class="mrc"
          tabindex="0"
          role="link"
          :aria-label="'查看商家：' + (m.shopName || '未命名商家')"
          @click="openDetail(m)"
          @keydown.enter="openDetail(m)"
        >
          <div class="mrc-cover">
            <img
              v-if="!broken.has(m.id)"
              class="mrc-img"
              :src="coverOf(m)"
              :alt="(m.businessScope || '婚礼服务') + '商家实拍：' + (m.shopName || '')"
              loading="lazy"
              decoding="async"
              @error="broken.add(m.id)"
            />
          </div>
          <div class="mrc-body">
            <h2 class="mrc-name" :title="m.shopName">{{ m.shopName || '未命名商家' }}</h2>
            <span class="pill">{{ m.businessScope || '经营范围未填写' }}</span>
            <p class="mrc-desc">{{ m.description || '暂无简介' }}</p>
          </div>
        </article>
      </div>
    </template>

    <!-- ==================== 详情态（页内切换，不新增路由） ==================== -->
    <div v-else class="detail">
      <button type="button" class="back" @click="backToList">
        <el-icon class="back-ico"><ArrowLeft /></el-icon>
        <span>返回商家列表</span>
      </button>

      <div class="banner">
        <img
          v-if="!curBroken"
          class="banner-img"
          :src="coverOf(cur)"
          :alt="(cur.businessScope || '婚礼服务') + '商家实拍：' + (cur.shopName || '')"
          decoding="async"
          @error="curBroken = true"
        />
        <div class="banner-veil" aria-hidden="true"></div>
      </div>

      <header class="detail-head">
        <h1 class="detail-title">{{ cur.shopName || '未命名商家' }}</h1>
        <div class="detail-meta">
          <span class="pill">{{ cur.businessScope || '经营范围未填写' }}</span>
          <span class="meta-item">
            <el-icon class="meta-ico"><Phone /></el-icon>{{ cur.contactPhone || '未填写' }}
          </span>
          <span class="meta-item">
            <el-icon class="meta-ico meta-ico--gold"><Medal /></el-icon>已通过资质审核
          </span>
          <span class="meta-item">
            <el-icon class="meta-ico"><Clock /></el-icon>入驻于 {{ dateText(cur.createdAt) }}
          </span>
        </div>
      </header>

      <p class="detail-desc">{{ cur.description || '该商家暂未填写简介。' }}</p>

      <section class="svc-block">
        <div class="block-head">
          <h2 class="block-title">该商家的服务</h2>
          <span class="block-note">共 {{ curServices.length }} 项</span>
        </div>

        <div v-if="svcLoading" class="grid" aria-hidden="true">
          <div v-for="n in 3" :key="n" class="sk">
            <div class="sk-cover"></div>
            <div class="sk-body">
              <div class="sk-line sk-line--lg"></div>
              <div class="sk-line"></div>
              <div class="sk-line sk-line--short"></div>
            </div>
          </div>
        </div>

        <div v-else-if="svcError" class="state state--inline" role="alert">
          <el-icon class="state-icon"><WarningFilled /></el-icon>
          <p class="state-title">服务加载失败</p>
          <p class="state-note">这家商家的服务暂时取不到，稍后重试一次。</p>
          <el-button type="primary" @click="loadServices(cur)">重新加载</el-button>
        </div>

        <div v-else-if="!curServices.length" class="svc-empty">
          <el-icon class="svc-empty-ico"><Document /></el-icon>
          <p class="svc-empty-text">该商家暂未上架服务，可先联系商家咨询</p>
          <p class="svc-empty-note">{{ cur.contactPhone || '电话未填写' }}</p>
        </div>

        <div v-else class="grid">
          <article
            v-for="s in curServices"
            :key="s.id"
            class="svc"
            tabindex="0"
            role="link"
            @click="goService(s)"
            @keydown.enter="goService(s)"
          >
            <ServiceCover :service="s" ratio="4 / 3" />
            <div class="svc-body">
              <h3 class="svc-name" :title="s.name">{{ s.name }}</h3>
              <p class="svc-desc">{{ s.description || '暂无简介' }}</p>
              <div class="svc-meta">
                <span class="svc-price">
                  <em>¥</em>{{ priceText(s.price) }}<span v-if="s.unit" class="svc-unit">/{{ s.unit }}</span>
                </span>
                <span class="svc-stat">
                  <el-icon class="svc-star"><StarFilled /></el-icon>{{ scoreText(s.score) }} · {{ s.salesCount || 0 }} 成交
                </span>
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowLeft, Search, Shop, WarningFilled, Document, Phone, Medal, Clock, StarFilled
} from '@element-plus/icons-vue'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'
import { coverFor } from '@/lib/imagery'

const router = useRouter()

const merchants = ref([])
const loading = ref(true)
const error = ref(false)
const keyword = ref('')
const broken = reactive(new Set())

const cur = ref(null)
const curBroken = ref(false)
const curServices = ref([])
const svcLoading = ref(false)
const svcError = ref(false)
let savedScroll = 0

const filtered = computed(() => {
  const k = keyword.value.trim()
  if (!k) return merchants.value
  return merchants.value.filter(
    m => String(m.shopName || '').includes(k) || String(m.businessScope || '').includes(k)
  )
})

// 商家没有 logo，按经营范围关键词推断分类，取对应真实实拍图
const SCOPE_RULES = [
  ['场地', 1], ['套餐', 2], ['司仪', 3], ['主持', 3],
  ['摄影', 4], ['跟拍', 4], ['婚纱', 5], ['礼服', 5],
  ['婚车', 6], ['车队', 6]
]
function categoryOf(m) {
  const scope = String(m?.businessScope || '')
  for (const [kw, cid] of SCOPE_RULES) if (scope.includes(kw)) return cid
  return (Math.abs(Number(m?.id) || 0) % 6) + 1
}
const coverOf = m => coverFor({ id: m?.id, categoryId: categoryOf(m) })

const priceText = v => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '—'
}
const scoreText = v => {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '—'
}
// 后端给的是 2026-10-18T17:27:01 这类 ISO 串，只取日期部分，不直出 ISO
function dateText(v) {
  const m = String(v || '').match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? m[1] + '-' + m[2] + '-' + m[3] : '—'
}

async function load() {
  loading.value = true
  error.value = false
  try {
    const data = await http.get('/merchants')
    merchants.value = Array.isArray(data) ? data : []
  } catch (e) {
    merchants.value = []
    error.value = true
  } finally {
    loading.value = false
  }
}

async function loadServices(m) {
  if (!m) return
  svcLoading.value = true
  svcError.value = false
  try {
    const data = await http.get('/services/search', { params: { merchantId: m.id, size: 20 } })
    curServices.value = data?.list || data?.records || (Array.isArray(data) ? data : [])
  } catch (e) {
    curServices.value = []
    svcError.value = true
  } finally {
    svcLoading.value = false
  }
}

async function openDetail(m) {
  savedScroll = window.scrollY || 0
  cur.value = m
  curBroken.value = false
  curServices.value = []
  await nextTick()
  window.scrollTo(0, 0)
  loadServices(m)
}

async function backToList() {
  cur.value = null
  curServices.value = []
  await nextTick()
  window.scrollTo(0, savedScroll)
}

const goService = s => router.push('/services/' + s.id)

onMounted(load)
</script>

<style scoped>
/* ===== 刊头 ===== */
.masthead { padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.masthead-title {
  font-family: var(--font-display);
  font-size: var(--fs-display);
  font-weight: 600;
  line-height: var(--lh-display);
  letter-spacing: .01em;
  margin: 0;
  color: var(--ink);
}
.masthead-sub { margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-base); line-height: var(--lh-body); }

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) 0;
  border-bottom: 1px solid var(--border);
}
.field { display: flex; flex-direction: column; gap: var(--space-1); min-width: 0; }
.field--keyword { flex: 1 1 auto; max-width: 360px; }
.field-label { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .04em; }
.field :deep(.el-input) { width: 100%; }
.field :deep(.el-input__wrapper) { padding: var(--space-1) var(--space-3); }
.field :deep(.el-input__inner) { height: var(--space-5); }
.field-ico { color: var(--ink-soft); font-size: var(--fs-sm); }

.result-count { margin: 0; display: flex; align-items: baseline; gap: var(--space-1); }
.result-label { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .04em; }
.result-count em {
  font-style: normal;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: var(--lh-display);
  color: var(--ink);
}

/* ===== 商家卡片 ===== */
.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-5); margin-top: var(--space-6); }
.mrc {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: box-shadow .3s ease, transform .3s ease;
}
.mrc:hover,
.mrc:focus-visible { box-shadow: var(--shadow); transform: translateY(-2px); }
.mrc:active { box-shadow: var(--shadow-sm); transform: translateY(0); }
.mrc:hover :deep(.mrc-img) { transform: scale(1.03); }
.mrc-cover { aspect-ratio: 16 / 9; background: var(--bg-deep); overflow: hidden; }
.mrc-img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform .5s ease; }
.mrc-body {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-4) var(--space-5);
  flex: 1;
}
.mrc-name {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
  margin: 0;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pill {
  display: inline-block;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-pill);
  background: var(--rose-50);
  color: var(--rose-600);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: .04em;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.mrc-desc {
  margin: 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 骨架 ===== */
.sk { background: var(--surface); border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); }
.sk-cover { aspect-ratio: 16 / 9; background: var(--bg-deep); }
.sk-body { padding: var(--space-4) var(--space-4) var(--space-5); display: flex; flex-direction: column; gap: var(--space-3); }
.sk-line { height: var(--space-3); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 66%; }
.sk-line--pill { height: var(--space-4); width: 48%; border-radius: var(--radius-pill); }
.sk-line--short { width: 40%; }
.sk-cover, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-cover, .sk-line { animation: none; }
}

/* ===== 状态块（错误 / 空态） ===== */
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  text-align: center;
  margin-top: var(--space-6);
  padding: var(--space-9) var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.state--inline { padding: var(--space-7) var(--space-5); margin-top: 0; box-shadow: none; }
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

/* ===== 详情态 ===== */
.detail { display: block; }
.back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-1) 0;
  margin: 0 0 var(--space-4);
  border: 0;
  background: none;
  cursor: pointer;
  color: var(--ink-soft);
  font-family: var(--font-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  transition: color .2s ease;
}
.back:hover { color: var(--rose-600); }
.back-ico { font-size: var(--fs-sm); }

.banner {
  position: relative;
  width: 100%;
  aspect-ratio: 21 / 9;
  max-height: calc(var(--space-9) * 3 + var(--space-6));
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--bg-deep);
}
.banner-img { display: block; width: 100%; height: 100%; object-fit: cover; object-position: center; }
/* 只压亮度，不放文字：照片亮度不可控，文字一律落在纸面上 */
.banner-veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(28, 20, 24, .45) 0%, rgba(28, 20, 24, 0) 62%);
  pointer-events: none;
}

.detail-head { margin-top: var(--space-6); }
.detail-title {
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: var(--lh-display);
  color: var(--ink);
  margin: 0;
}
.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2) var(--space-4);
  margin-top: var(--space-4);
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.meta-ico { font-size: var(--fs-sm); color: var(--ink-soft); }
.meta-ico--gold { color: var(--gold-500); }

.detail-desc {
  margin: var(--space-5) 0 0;
  max-width: 34em;
  color: var(--ink-soft);
  font-size: var(--fs-base);
  line-height: 1.8;
}

.svc-block { margin-top: var(--space-8); }
.block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--border);
}
.block-title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
  margin: 0;
}
.block-note { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .04em; }
.svc-block .grid { margin-top: var(--space-5); }

.svc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  text-align: center;
  margin-top: var(--space-5);
  padding: var(--space-6) var(--space-5);
  background: var(--bg-deep);
  border-radius: var(--radius);
}
.svc-empty-ico { font-size: var(--fs-xl); color: var(--ink-faint); }
.svc-empty-text { margin: 0; color: var(--ink); font-size: var(--fs-base); line-height: var(--lh-ui); }
.svc-empty-note { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .04em; }

/* ===== 服务卡（与全部服务页同源） ===== */
.svc {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: box-shadow .3s ease, transform .3s ease;
}
.svc:hover,
.svc:focus-visible { box-shadow: var(--shadow); transform: translateY(-2px); }
.svc:hover :deep(.cover-img) { transform: scale(1.03); }
.svc:active { box-shadow: var(--shadow-sm); transform: translateY(0); }
.svc-body { padding: var(--space-4) var(--space-4) var(--space-5); display: flex; flex-direction: column; flex: 1; }
.svc-name {
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
}
.svc-desc {
  margin: var(--space-2) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.svc-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
}
.svc-price {
  color: var(--rose-500);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  letter-spacing: .01em;
}
.svc-price em { font-style: normal; font-size: var(--fs-xs); margin-right: var(--space-1); vertical-align: super; }
.svc-unit { color: var(--ink-soft); font-size: var(--fs-xs); font-weight: 400; margin-left: var(--space-1); }
.svc-stat {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  white-space: nowrap;
}
.svc-star { color: var(--gold-500); font-size: var(--fs-xs); }

@media (max-width: 900px) {
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .banner { max-height: calc(var(--space-9) * 2 + var(--space-2)); }
}
@media (max-width: 640px) {
  .grid { grid-template-columns: minmax(0, 1fr); }
  .toolbar { flex-direction: column; align-items: stretch; }
  .result-count { justify-content: flex-end; }
}
</style>
