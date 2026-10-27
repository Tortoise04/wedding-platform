<template>
  <div class="svc-page">
    <header class="masthead">
      <h1 class="masthead-title">全部服务</h1>
      <p class="masthead-sub">套餐 · 场地 · 司仪 · 摄影 · 婚纱 · 婚车 —— 按你的预算与喜好，慢慢挑</p>
    </header>

    <!-- 筛选：一条行，控件同高 -->
    <section class="toolbar" aria-label="服务筛选">
      <div class="field field--keyword">
        <label class="field-label" for="svc-keyword">关键字</label>
        <el-input
          id="svc-keyword"
          v-model="query.keyword"
          placeholder="服务名称 / 简介"
          clearable
          @keyup.enter="onSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <div class="field">
        <label class="field-label" for="svc-category">分类</label>
        <el-select id="svc-category" v-model="query.categoryId" placeholder="全部分类" clearable>
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>
      <div class="field field--range">
        <span class="field-label">价格区间</span>
        <div class="range">
          <el-input-number
            v-model="query.priceMin"
            :min="0"
            :controls="false"
            placeholder="最低"
            class="range-input"
          />
          <span class="range-dash" aria-hidden="true">—</span>
          <el-input-number
            v-model="query.priceMax"
            :min="0"
            :controls="false"
            placeholder="最高"
            class="range-input"
          />
        </div>
      </div>
      <div class="field">
        <label class="field-label" for="svc-sort">排序</label>
        <el-select id="svc-sort" v-model="query.sort">
          <el-option label="最新发布" value="newest" />
          <el-option label="价格从低到高" value="priceAsc" />
          <el-option label="价格从高到低" value="priceDesc" />
          <el-option label="销量优先" value="hot" />
        </el-select>
      </div>
      <el-button type="primary" class="toolbar-submit" @click="onSearch">搜索</el-button>
    </section>

    <div class="result-line">
      <p class="result-count">
        <span class="result-label">共</span><em>{{ total }}</em><span class="result-label">项服务</span>
      </p>
    </div>

    <!-- 加载中：CSS 灰块骨架 -->
    <div v-if="loading" class="grid" aria-hidden="true">
      <div v-for="n in 6" :key="n" class="sk">
        <div class="sk-cover"></div>
        <div class="sk-body">
          <div class="sk-line sk-line--lg"></div>
          <div class="sk-line"></div>
          <div class="sk-line sk-line--short"></div>
        </div>
      </div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="state" role="alert">
      <el-icon class="state-icon"><WarningFilled /></el-icon>
      <p class="state-title">服务列表加载失败</p>
      <p class="state-note">网络或服务暂时不可用，稍后重试一次就好。</p>
      <el-button type="primary" @click="onSearch">重新加载</el-button>
    </div>

    <!-- 空结果态 -->
    <div v-else-if="!services.length" class="state">
      <el-icon class="state-icon"><Search /></el-icon>
      <p class="state-title">没有符合条件的服务</p>
      <p class="state-note">换个关键字，或把价格区间放宽一点再试试。</p>
      <el-button @click="resetFilters">清空筛选</el-button>
    </div>

    <!-- 正常态 -->
    <div v-else class="grid">
      <article
        v-for="(s, i) in services"
        :key="s.id"
        class="svc"
        tabindex="0"
        role="link"
        @click="goService(s)"
        @keydown.enter="goService(s)"
      >
        <ServiceCover :service="s" :seed="coverIds[i]" ratio="4 / 3" />
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

    <div v-if="showPager" class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page + 1"
        @current-change="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'

const router = useRouter()

const categories = ref([])
const services = ref([])
const total = ref(0)
const page = ref(0)
const size = ref(12)
const loading = ref(true)
const error = ref(false)
const query = reactive({ keyword: '', categoryId: null, priceMin: null, priceMax: null, sort: 'newest' })

const showPager = computed(() => !loading.value && !error.value && total.value > size.value)

// 按分类内的出现序号取图：12 张卡里每类 4 张，正好用到该分类的第 1-4 张，页内不重样
const coverIds = computed(() => {
  const seen = {}
  return services.value.map(s => {
    const k = s.categoryId ?? 0
    seen[k] = (seen[k] || 0) + 1
    return seen[k] - 1
  })
})

const priceText = v => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '—'
}
const scoreText = v => {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '—'
}
const goService = s => router.push(`/services/${s.id}`)

async function loadCategories() {
  try {
    categories.value = await http.get('/categories')
  } catch (e) {
    categories.value = []
  }
}

async function load() {
  loading.value = true
  error.value = false
  const params = { page: page.value, size: size.value }
  if (query.keyword) params.keyword = query.keyword
  if (query.categoryId) params.categoryId = query.categoryId
  if (query.priceMin != null) params.priceMin = query.priceMin
  if (query.priceMax != null) params.priceMax = query.priceMax
  if (query.sort) params.sort = query.sort
  try {
    const data = await http.get('/services/search', { params })
    services.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    error.value = true
    services.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() { page.value = 0; load() }
function onPage(p) { page.value = p - 1; load() }

function resetFilters() {
  query.keyword = ''
  query.categoryId = null
  query.priceMin = null
  query.priceMax = null
  query.sort = 'newest'
  onSearch()
}

onMounted(() => { loadCategories(); load() })
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

/* ===== 筛选行 ===== */
.toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr) minmax(0, 1.6fr) minmax(0, 1fr) auto;
  align-items: end;
  gap: var(--space-4);
  padding: var(--space-5) 0;
  border-bottom: 1px solid var(--border);
}
.field { display: flex; flex-direction: column; gap: var(--space-1); min-width: 0; }
/* 12px 标签：--ink-faint 实测 3.2:1 不过 AA，用 --ink-soft 保持同级视觉重量 */
.field-label { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); letter-spacing: .04em; }
.field :deep(.el-select), .field :deep(.el-input), .field :deep(.el-input-number) { width: 100%; }
/* 控件内部节奏拉回 4px 栅格（Element Plus 默认 1/11/6 不在栅格上） */
.field :deep(.el-input__wrapper) { padding: var(--space-1) var(--space-3); }
.field :deep(.el-select__wrapper),
.field :deep(.el-select__selection),
.field :deep(.el-select__suffix) { gap: var(--space-1); }
.field :deep(.el-select__placeholder) { color: var(--ink-soft); }
.field :deep(.el-input__inner) { height: var(--space-5); }
.range { display: flex; align-items: center; gap: var(--space-2); }
.range-input { flex: 1; min-width: 0; }
.range-dash { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }

/* ===== 结果计数 ===== */
.result-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-5) 0;
}
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

/* ===== 卡片网格（与首页精选服务同源） ===== */
.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-5); }
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

/* ===== 骨架 ===== */
.sk { background: var(--surface); border-radius: var(--radius-lg); overflow: hidden; box-shadow: var(--shadow-sm); }
.sk-cover { aspect-ratio: 4 / 3; background: var(--bg-deep); }
.sk-body { padding: var(--space-4) var(--space-4) var(--space-5); display: flex; flex-direction: column; gap: var(--space-3); }
.sk-line { height: var(--space-3); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 70%; }
.sk-line--short { width: 40%; }
.sk-cover, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-cover, .sk-line { animation: none; }
}

/* ===== 状态块（错误 / 空结果） ===== */
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

.pager { margin-top: var(--space-6); display: flex; justify-content: center; }

@media (max-width: 1080px) {
  .toolbar { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: var(--space-4) var(--space-3); }
  .toolbar-submit { justify-self: start; }
}
@media (max-width: 900px) {
  .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .grid { grid-template-columns: minmax(0, 1fr); }
  .toolbar { grid-template-columns: minmax(0, 1fr); }
}
</style>
