<template>
  <div class="content-page">
    <!-- ===================== 列表态 ===================== -->
    <template v-if="!reading">
      <header class="masthead">
        <h1 class="masthead-title">公告资讯</h1>
        <p class="masthead-sub">平台动态与备婚经验，挑一篇慢慢读完再决定，不必着急</p>
      </header>

      <div class="seg" role="tablist" aria-label="内容分类">
        <button
          v-for="t in TABS"
          :key="t.name"
          type="button"
          role="tab"
          class="seg-btn"
          :class="{ 'is-on': tab === t.name }"
          :aria-selected="tab === t.name"
          @click="tab = t.name"
        >{{ t.label }}</button>
      </div>

      <!-- ——— 载入态：灰块骨架 ——— -->
      <div v-if="loading" class="sk-wrap" aria-hidden="true">
        <div v-if="tab === 'article'" class="news-grid">
          <div v-for="n in 3" :key="n" class="sk-card">
            <div class="sk-cover"></div>
            <div class="sk-line sk-line--lg"></div>
            <div class="sk-line"></div>
            <div class="sk-line sk-line--short"></div>
          </div>
        </div>
        <div v-else class="sk-list">
          <div v-for="n in 4" :key="n" class="sk-row">
            <div class="sk-line sk-line--date"></div>
            <div class="sk-row-body">
              <div class="sk-line sk-line--lg"></div>
              <div class="sk-line"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- ——— 平台公告 ——— -->
      <template v-else-if="tab === 'announcement'">
        <div v-if="annError" class="state">
          <el-icon class="state-ico"><Warning /></el-icon>
          <p class="state-title">公告没能加载出来</p>
          <p class="state-desc">{{ annError }}</p>
          <button type="button" class="act" @click="reloadAnnouncements">重新加载</button>
        </div>

        <template v-else-if="announcements.length">
          <p class="count-line">共 <em>{{ announcements.length }}</em> 条公告</p>
          <div class="ann-list">
            <div
              v-for="n in announcements"
              :key="n.id"
              class="ann-row"
              role="button"
              tabindex="0"
              @click="openAnnouncement(n)"
              @keydown.enter.prevent="openAnnouncement(n)"
              @keydown.space.prevent="openAnnouncement(n)"
            >
              <time class="ann-date">{{ fmtDate(n.publishTime) }}</time>
              <div class="ann-body">
                <h2 class="ann-title">{{ n.title }}</h2>
                <p class="ann-sum">{{ summaryOf(n.content) }}</p>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="state">
          <el-icon class="state-ico"><Document /></el-icon>
          <p class="state-title">暂无公告</p>
          <p class="state-desc">平台规则、档期提醒和服务升级会先在这里发布，可以先去「全部服务」看看套餐。</p>
        </div>
      </template>

      <!-- ——— 婚庆资讯 ——— -->
      <template v-else>
        <div v-if="artError" class="state">
          <el-icon class="state-ico"><Warning /></el-icon>
          <p class="state-title">资讯没能加载出来</p>
          <p class="state-desc">{{ artError }}</p>
          <button type="button" class="act" @click="reloadArticles">重新加载</button>
        </div>

        <template v-else-if="articles.length">
          <p class="count-line">共 <em>{{ articles.length }}</em> 篇资讯</p>
          <div class="news-grid">
            <article
              v-for="(a, i) in articles"
              :key="a.id"
              class="news-card"
              role="button"
              tabindex="0"
              @click="openArticle(a, i)"
              @keydown.enter.prevent="openArticle(a, i)"
              @keydown.space.prevent="openArticle(a, i)"
            >
              <div class="card-cover">
                <img
                  v-show="!broken[a.id]"
                  :src="coverOf(a, i)"
                  :alt="`婚庆资讯配图：${a.title}`"
                  loading="lazy"
                  decoding="async"
                  @error="markBroken(a.id)"
                />
              </div>
              <div class="card-body">
                <h2 class="card-title">{{ a.title }}</h2>
                <p class="card-sum">{{ summaryOf(a.content) }}</p>
                <p class="card-meta">
                  <span>{{ a.author || '缘集编辑部' }}</span>
                  <span class="dot" aria-hidden="true">·</span>
                  <span>{{ a.viewCount || 0 }} 阅读</span>
                  <span class="dot" aria-hidden="true">·</span>
                  <span>{{ fmtDate(a.publishTime) }}</span>
                </p>
              </div>
            </article>
          </div>
        </template>

        <div v-else class="state">
          <el-icon class="state-ico"><Notebook /></el-icon>
          <p class="state-title">暂无资讯</p>
          <p class="state-desc">编辑部正在整理备婚清单与流程经验，发布后会出现在这里。</p>
        </div>
      </template>
    </template>

    <!-- ===================== 阅读态 ===================== -->
    <div v-else class="read">
      <button type="button" class="back" @click="backToList">
        <el-icon class="back-ico"><ArrowLeft /></el-icon>
        <span>返回公告资讯</span>
      </button>

      <!-- 详情载入中 -->
      <div v-if="detailLoading" class="read-sk" aria-hidden="true">
        <div class="sk-cover sk-cover--banner"></div>
        <div class="sk-line sk-line--kicker"></div>
        <div class="sk-line sk-line--title"></div>
        <div class="sk-line sk-line--short"></div>
        <div class="sk-line sk-line--para"></div>
        <div class="sk-line sk-line--para"></div>
      </div>

      <!-- 详情失败 -->
      <div v-else-if="detailError" class="state">
        <el-icon class="state-ico"><Warning /></el-icon>
        <p class="state-title">这篇内容暂时打不开</p>
        <p class="state-desc">{{ detailError }}</p>
        <button type="button" class="act" @click="retryDetail">重试</button>
      </div>

      <!-- 正文 -->
      <article v-else class="article">
        <div v-if="hasBanner" class="banner">
          <img
            v-show="!bannerBroken"
            :src="bannerSrc"
            :alt="`婚庆资讯配图：${current.title}`"
            decoding="async"
            @error="bannerBroken = true"
          />
        </div>

        <p class="kicker">{{ reading.kind === 'article' ? '婚庆资讯' : '平台公告' }}</p>
        <h1 class="a-title">{{ current.title }}</h1>
        <p class="a-meta">
          <template v-if="reading.kind === 'article'">
            <span>{{ current.author || '缘集编辑部' }}</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ fmtDate(current.publishTime) }}</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ current.viewCount || 0 }} 阅读</span>
          </template>
          <template v-else>
            <span>缘集平台</span>
            <span class="dot" aria-hidden="true">·</span>
            <span>{{ fmtDate(current.publishTime) }}</span>
          </template>
        </p>

        <div class="a-body">
          <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
        </div>

        <footer class="a-foot">
          <span class="a-rule" aria-hidden="true"></span>
          <p class="a-note">{{ noteText }}</p>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import http from '@/api/http'
import { newsImageFor } from '@/lib/imagery'

const TABS = [
  { name: 'announcement', label: '平台公告' },
  { name: 'article', label: '婚庆资讯' }
]

const tab = ref('announcement')
const announcements = ref([])
const articles = ref([])
const loading = ref(true)
const annError = ref('')
const artError = ref('')

// 阅读态：{ kind: 'article' | 'announcement', item, index }
const reading = ref(null)
const detailLoading = ref(false)
const detailError = ref('')
const savedScroll = ref(0)
const broken = ref({})
const bannerBroken = ref(false)

const current = computed(() => reading.value?.item || {})
const hasBanner = computed(() => reading.value?.kind === 'article')
const bannerSrc = computed(() => current.value.coverUrl || newsImageFor(reading.value?.index ?? 0))

const paragraphs = computed(() => {
  const raw = String(current.value.content || '').replace(/\r\n?/g, '\n')
  return raw
    .split(/\n\s*\n+/)
    .map(block => block.split('\n').map(line => line.trim()).join(''))
    .filter(Boolean)
})

const noteText = computed(() => reading.value?.kind === 'article'
  ? '本文由缘集编辑部整理，不构成合同条款。'
  : '本公告由缘集平台发布，具体服务条款以站内规则为准。')

// —— 工具 ——
const pad2 = n => String(n).padStart(2, '0')
function fmtDate(v) {
  if (!v) return '—'
  const d = new Date(String(v).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(v).slice(0, 10)
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}
function summaryOf(text, max = 60) {
  const s = String(text || '').replace(/\s+/g, ' ').trim()
  return s.length > max ? s.slice(0, max) + '…' : s
}
const coverOf = (a, i) => a.coverUrl || newsImageFor(i)
const markBroken = id => { broken.value = { ...broken.value, [id]: true } }
const msgOf = (e, fallback) => e?.response?.data?.msg || e?.message || fallback

// —— 列表数据 ——
async function loadAnnouncements() {
  try {
    const d = await http.get('/announcements')
    announcements.value = Array.isArray(d) ? d : []
    annError.value = ''
  } catch (e) {
    annError.value = msgOf(e, '网络异常，请稍后重试')
  }
}
async function loadArticles() {
  try {
    const d = await http.get('/articles')
    articles.value = Array.isArray(d) ? d : []
    artError.value = ''
  } catch (e) {
    artError.value = msgOf(e, '网络异常，请稍后重试')
  }
}
async function loadLists() {
  loading.value = true
  await Promise.allSettled([loadAnnouncements(), loadArticles()])
  loading.value = false
}
async function reloadAnnouncements() {
  loading.value = true
  annError.value = ''
  await loadAnnouncements()
  loading.value = false
}
async function reloadArticles() {
  loading.value = true
  artError.value = ''
  await loadArticles()
  loading.value = false
}

// —— 进入 / 退出阅读态 ——
function rememberScroll() {
  savedScroll.value = window.scrollY || 0
}

function openAnnouncement(n) {
  rememberScroll()
  bannerBroken.value = false
  detailError.value = ''
  detailLoading.value = false
  reading.value = { kind: 'announcement', item: n, index: 0 }
  window.scrollTo(0, 0)
}

async function openArticle(a, index) {
  rememberScroll()
  bannerBroken.value = false
  detailError.value = ''
  reading.value = { kind: 'article', item: a, index }
  detailLoading.value = true
  window.scrollTo(0, 0)
  try {
    const d = await http.get(`/articles/${a.id}`)
    // 用户可能已经返回列表：只在仍停留在同一篇时写入
    if (reading.value?.item?.id === a.id) {
      const detail = d && typeof d === 'object' ? d : {}
      reading.value = { kind: 'article', item: { ...a, ...detail, content: detail.content || a.content }, index }
    }
  } catch (e) {
    detailError.value = msgOf(e, '网络异常，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

function retryDetail() {
  const cur = reading.value
  if (cur) openArticle(cur.item, cur.index)
}

function backToList() {
  reading.value = null
  detailError.value = ''
  detailLoading.value = false
  const y = savedScroll.value
  nextTick(() => window.scrollTo(0, y))
}

onMounted(loadLists)
</script>

<style scoped>
.content-page { padding-bottom: var(--space-7); }

/* ===== 页头 ===== */
.masthead {
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border);
}
.masthead-title {
  margin: 0;
  font-size: var(--fs-display);
  line-height: var(--lh-display);
  font-weight: 600;
  letter-spacing: .04em;
}
.masthead-sub {
  max-width: 46ch;
  margin: var(--space-3) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-base);
  line-height: var(--lh-body);
}

/* ===== 分段切换 ===== */
.seg {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  margin-top: var(--space-6);
}
.seg-btn {
  position: relative;
  height: var(--space-7);
  padding: 0;
  border: 0;
  background: none;
  font-family: var(--font-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
  cursor: pointer;
  transition: color .2s ease;
}
.seg-btn::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--rose-500);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform .2s ease;
}
.seg-btn:hover { color: var(--rose-600); }
.seg-btn.is-on { color: var(--ink); font-weight: 600; }
.seg-btn.is-on::after { transform: scaleX(1); }

/* ===== 条数 ===== */
.count-line {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin: var(--space-5) 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.count-line em {
  font-family: var(--font-display);
  font-style: normal;
  font-size: var(--fs-xl);
  line-height: 1;
  color: var(--rose-500);
}

/* ===== 资讯卡片 ===== */
.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6) var(--space-5);
}
.news-card {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow .3s ease, transform .3s ease;
}
.news-card:hover,
.news-card:focus-visible { box-shadow: var(--shadow); transform: translateY(-2px); }
.news-card:active { box-shadow: var(--shadow-sm); transform: translateY(0); }
.card-cover {
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: var(--bg-deep);
  background-image: linear-gradient(150deg, var(--bg) 0%, var(--bg-deep) 55%, color-mix(in srgb, var(--bg-deep) 86%, var(--rose-300)) 100%);
}
.card-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .5s ease;
}
.news-card:hover .card-cover img { transform: scale(1.03); }
.card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: var(--space-5);
}
.card-title {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color .2s ease;
}
.news-card:hover .card-title { color: var(--rose-600); }
.card-sum {
  margin: var(--space-3) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-meta {
  margin: var(--space-4) 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
}
.card-meta .dot,
.a-meta .dot { color: var(--ink-faint); }

/* ===== 公告纵向列表 ===== */
.ann-list { border-top: 1px solid var(--border); }
.ann-row {
  position: relative;
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: var(--space-5);
  padding: var(--space-5) 0;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
}
.ann-row::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  background: var(--rose-500);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform .25s ease;
}
.ann-row:hover::after,
.ann-row:focus-visible::after { transform: scaleX(1); }
.ann-date {
  padding-top: var(--space-1);
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: .02em;
}
.ann-title {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  transition: color .2s ease;
}
.ann-row:hover .ann-title { color: var(--rose-600); }
.ann-sum {
  margin: var(--space-2) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 空态 / 错误态 ===== */
.state {
  padding: var(--space-8) var(--space-6);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  text-align: center;
}
.state-ico { font-size: var(--fs-xl); color: var(--ink-faint); }
.state-title {
  margin: var(--space-4) 0 0;
  font-size: var(--fs-base);
  font-weight: 600;
  line-height: var(--lh-ui);
}
.state-desc {
  max-width: 40ch;
  margin: var(--space-2) auto 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
}
.act {
  height: 40px;
  margin-top: var(--space-5);
  padding: 0 var(--space-6);
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
.act:hover { border-color: var(--rose-500); color: var(--rose-600); }

/* ===== 骨架 ===== */
.sk-card {
  padding-bottom: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.sk-cover { aspect-ratio: 16 / 9; background: var(--bg-deep); }
.sk-cover--banner {
  aspect-ratio: 21 / 9;
  max-height: calc(var(--space-9) * 3 + var(--space-6));
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-6);
}
.sk-line {
  height: var(--space-3);
  margin: var(--space-3) var(--space-5) 0;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.sk-line--lg { height: var(--space-5); width: 70%; }
.sk-line--short { width: 40%; }
.sk-line--date { width: 72px; margin: var(--space-1) 0 0; }
.sk-line--kicker { width: 96px; margin: 0 0 var(--space-3); }
.sk-line--title { height: var(--space-6); width: 60%; margin: 0 0 var(--space-3); }
.sk-line--para { width: 100%; margin-top: var(--space-4); }
.sk-list { border-top: 1px solid var(--border); }
.sk-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: var(--space-5);
  padding: var(--space-5) 0;
  border-bottom: 1px solid var(--border);
}
.sk-row-body { display: flex; flex-direction: column; gap: var(--space-3); }
.read-sk { max-width: 36em; }
.sk-cover, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-cover, .sk-line { animation: none; }
}

/* ===== 阅读态 ===== */
.read { padding-bottom: var(--space-7); }
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
.banner {
  width: 100%;
  aspect-ratio: 21 / 9;
  max-height: calc(var(--space-9) * 3 + var(--space-6));
  margin-top: var(--space-6);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--bg-deep);
  background-image: linear-gradient(150deg, var(--bg) 0%, var(--bg-deep) 55%, color-mix(in srgb, var(--bg-deep) 86%, var(--rose-300)) 100%);
}
.banner img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}
.kicker {
  margin: var(--space-6) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: .16em;
}
.banner + .kicker { margin-top: var(--space-6); }
.a-title {
  max-width: 24em;
  margin: var(--space-3) 0 0;
  font-size: var(--fs-xl);
  line-height: var(--lh-ui);
  font-weight: 600;
}
.a-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-3) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
}
.a-body {
  max-width: 36em;
  margin: var(--space-7) 0 0;
  color: var(--ink);
  font-family: var(--font-body);
  font-size: var(--fs-base);
  line-height: 1.9;
}
.a-body p { margin: 0; }
.a-body p + p { margin-top: var(--space-4); }
.a-foot {
  max-width: 36em;
  margin-top: var(--space-7);
}
.a-rule { display: block; height: 1px; background: var(--border); }
.a-note {
  margin: var(--space-4) 0 0;
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-body);
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .news-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
  .banner,
  .sk-cover--banner { max-height: calc(var(--space-9) * 2 + var(--space-2)); }
}
@media (max-width: 768px) {
  .banner { aspect-ratio: 16 / 7; }
  .a-title { line-height: var(--lh-ui); }
}
@media (max-width: 640px) {
  .news-grid { grid-template-columns: 1fr; }
  .ann-row,
  .sk-row { grid-template-columns: 1fr; gap: var(--space-2); }
  .ann-date { padding-top: 0; }
  .count-line { margin: var(--space-4) 0; }
}
</style>
