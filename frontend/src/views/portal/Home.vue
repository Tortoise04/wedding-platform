<template>
  <div class="home">
    <!-- ① Hero：全幅实拍 + 衬线大标题 -->
    <section class="hero" :class="{ 'hero--empty': heroBroken }">
      <img
        v-show="!heroBroken"
        class="hero-img"
        :src="heroImage"
        alt="婚礼现场实拍：仪式区花艺与灯光布置"
        fetchpriority="high"
        decoding="async"
        @error="heroBroken = true"
      />
      <div class="hero-veil" aria-hidden="true"></div>
      <div class="hero-body">
        <h1 class="hero-title">一生一次，值得最好的安排</h1>
        <p class="hero-sub">
          <span class="hero-sub-line">缘集 · 婚礼策划一站式服务平台</span>
          <span class="hero-sub-line">套餐、场地、司仪、摄影、婚纱、婚车 · 一站配齐</span>
        </p>
        <div class="hero-actions">
          <el-button class="btn-primary" size="large" round @click="$router.push('/services')">开始挑选</el-button>
          <el-button class="btn-ghost" size="large" round @click="$router.push('/merchants')">找商家</el-button>
        </div>
      </div>
    </section>

    <!-- ② 动态跑马灯：两端 mask 渐隐 -->
    <section v-if="tickerItems.length" class="ticker">
      <span class="ticker-label">动态</span>
      <div class="ticker-window">
        <div class="ticker-track">
          <span v-for="t in tickerLoop" :key="t.key" class="ticker-item">
            <span class="ticker-tag">{{ t.tag }}</span>
            <span class="ticker-text">{{ t.title }}</span>
          </span>
        </div>
      </div>
    </section>

    <!-- ③ 平台特色：四条文字块，靠竖 hairline 分隔 -->
    <section class="features">
      <div v-for="f in features" :key="f.title" class="feature">
        <el-icon class="f-ico" :size="28"><component :is="f.icon" /></el-icon>
        <h3 class="f-title">{{ f.title }}</h3>
        <p class="f-desc">{{ f.desc }}</p>
      </div>
    </section>

    <!-- ④ 服务流程：01—04 数字标尺 -->
    <section class="flow">
      <h2 class="section-title">从心动到礼成，只需几步</h2>
      <div class="steps">
        <div v-for="(s, i) in steps" :key="s.title" class="step">
          <div class="step-head">
            <span class="step-num">{{ pad(i + 1) }}</span>
            <span class="step-rule" aria-hidden="true"></span>
          </div>
          <h3 class="step-title">{{ s.title }}</h3>
          <p class="step-desc">{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ⑤ 精选服务：刊物式封面卡 -->
    <section class="featured">
      <div class="section-head">
        <h2 class="section-title">精选服务</h2>
        <el-link type="primary" underline="never" class="head-link" @click="$router.push('/services')">探索全部服务 →</el-link>
      </div>
      <div v-if="featured.length" class="featured-grid">
        <article
          v-for="s in featured"
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
              <span class="svc-price"><em>¥</em>{{ priceText(s.price) }}</span>
              <span class="svc-stat">
                <el-icon class="svc-star"><StarFilled /></el-icon>{{ scoreText(s.score) }} · {{ s.salesCount || 0 }} 成交
              </span>
            </div>
          </div>
        </article>
      </div>
      <div v-else class="state-box">
        <el-empty description="暂无服务" :image-size="72" />
      </div>
      <div class="more">
        <el-button round plain @click="$router.push('/services')">查看全部服务</el-button>
      </div>
    </section>

    <!-- ⑥ 婚庆资讯：3:2 缩略图 + 两行衬线标题 -->
    <section class="news">
      <div class="section-head">
        <h2 class="section-title">婚庆资讯</h2>
        <el-link type="primary" underline="never" class="head-link" @click="$router.push('/content')">更多 →</el-link>
      </div>
      <div v-if="newsList.length" class="news-grid">
        <article
          v-for="(a, i) in newsList"
          :key="a.id"
          class="news-card"
          tabindex="0"
          role="link"
          @click="openArticle(a)"
          @keydown.enter="openArticle(a)"
        >
          <div class="news-thumb">
            <img
              :src="newsImageFor(i)"
              :alt="`婚庆资讯配图：${a.title}`"
              loading="lazy"
              decoding="async"
              @error="hideBroken"
            />
          </div>
          <h3 class="news-title">{{ a.title }}</h3>
          <p class="news-meta">{{ a.author || '缘集' }} · {{ a.viewCount }} 阅读</p>
        </article>
      </div>
      <div v-else class="state-box">
        <el-empty description="暂无资讯" :image-size="72" />
      </div>
    </section>

    <!-- 资讯详情弹窗 -->
    <el-dialog v-model="articleVisible" :title="articleTitle" width="640px">
      <div class="article-body" v-html="articleBody"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'
import { heroImages, newsImageFor } from '@/lib/imagery'

const router = useRouter()

// —— 静态文案 ——
const features = [
  { icon: 'Present', title: '一站配齐', desc: '套餐、场地、司仪、摄影、婚纱、婚车六类服务，同一个平台全搞定' },
  { icon: 'CircleCheck', title: '放心商家', desc: '商家入驻资质审核 + 服务内容双重审核，只展示过审内容' },
  { icon: 'Umbrella', title: '全程保障', desc: '订单履约留痕、平台介入仲裁，纠纷有据可依、有处可诉' },
  { icon: 'ChatDotRound', title: '先聊后订', desc: '下单前与商家在线咨询，把要求说清楚再预约' }
]
const steps = [
  { title: '浏览挑选', desc: '按关键词、分类、价格区间找到心仪的服务' },
  { title: '在线咨询', desc: '和商家一对一沟通需求，问清楚再决定' },
  { title: '下单预约', desc: '填写婚礼日期、人数与预算，提交预约订单' },
  { title: '履约评价', desc: '商家接单履约，服务完成后留下真实评价' }
]

// —— 数据（接口与参数均未改动） ——
const featured = ref([])
const announcements = ref([])
const hotArticles = ref([])
const articles = ref([])

const heroImage = heroImages[0]
const heroBroken = ref(false)
const newsList = computed(() => articles.value.slice(0, 3))

const tickerItems = computed(() => [
  ...announcements.value.map(a => ({ key: 'a' + a.id, tag: '公告', title: a.title })),
  ...hotArticles.value.map(a => ({ key: 'h' + a.id, tag: '热门', title: a.title }))
])
const tickerLoop = computed(() => tickerItems.value.length
  ? [...tickerItems.value, ...tickerItems.value.map(t => ({ ...t, key: t.key + '-loop' }))]
  : [])

const articleVisible = ref(false)
const articleTitle = ref('')
const articleBody = ref('')

const pad = n => String(n).padStart(2, '0')
const priceText = v => {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN', { maximumFractionDigits: 0 }) : '—'
}
const scoreText = v => {
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n.toFixed(1) : '—'
}
const goService = s => router.push(`/services/${s.id}`)
// 图片 404 时隐去 img，暴露容器的 --bg-deep 底色，不出现破图
const hideBroken = e => { e.target.style.visibility = 'hidden' }

async function loadFeatured() {
  const data = await http.get('/services/search', { params: { sort: 'hot', page: 0, size: 6 } })
  featured.value = data.list
}

async function loadContent() {
  announcements.value = await http.get('/announcements')
  hotArticles.value = await http.get('/articles/hot')
  articles.value = await http.get('/articles')
}

async function openArticle(a) {
  const d = await http.get(`/articles/${a.id}`)
  articleTitle.value = d.title
  articleBody.value = (d.content || '').replace(/\n/g, '<br/>')
  articleVisible.value = true
}

onMounted(() => { loadFeatured(); loadContent() })
</script>

<style scoped>
/* ===== ① Hero ===== */
.hero {
  position: relative;
  width: 100vw;
  margin-left: calc(50% - 50vw);
  height: 560px;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: var(--ink);
  background-image: linear-gradient(115deg, #3C2A31 0%, var(--ink) 55%, #4A343B 100%);
}
.hero-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(.82) saturate(1.02) contrast(1.02);
}
/* 两层遮罩：斜向压左侧文字区 + 底部压暗，保证白字在亮照片上仍达标 */
.hero-veil {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(100deg, rgba(38, 24, 30, .86) 0%, rgba(38, 24, 30, .55) 38%, rgba(38, 24, 30, .12) 68%, rgba(38, 24, 30, 0) 100%),
    linear-gradient(to top, rgba(38, 24, 30, .55) 0%, rgba(38, 24, 30, 0) 45%);
}
.hero--empty .hero-veil {
  background: radial-gradient(120% 130% at 12% 15%, rgba(46, 32, 36, .30) 0%, rgba(46, 32, 36, .72) 70%);
}
.hero-body {
  position: relative;
  width: 100%;
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 calc(var(--gutter) + var(--space-5));
}
.hero-title {
  color: #fff;
  font-size: var(--fs-display);
  line-height: var(--lh-display);
  font-weight: 600;
  letter-spacing: .02em;
  margin: 0;
  text-shadow: 0 2px 28px rgba(0, 0, 0, .28);
}
.hero-sub {
  max-width: 46ch;
  margin: var(--space-5) 0 0;
  color: rgba(255, 255, 255, .92);
  font-size: var(--fs-base);
  line-height: var(--lh-body);
}
.hero-sub-line { display: block; }
.hero-actions {
  margin-top: var(--space-7);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}
.btn-primary.el-button,
.btn-ghost.el-button {
  height: var(--space-7);
  padding: 0 var(--space-6);
  font-size: var(--fs-base);
}
.btn-primary.el-button {
  --el-button-bg-color: var(--rose-500);
  --el-button-border-color: var(--rose-500);
  --el-button-hover-bg-color: var(--rose-600);
  --el-button-hover-border-color: var(--rose-600);
  --el-button-active-bg-color: var(--rose-700);
  --el-button-active-border-color: var(--rose-700);
  --el-button-text-color: #fff;
  padding-left: var(--space-6);
  padding-right: var(--space-6);
  font-size: var(--fs-base);
  box-shadow: 0 16px 32px -20px rgba(46, 32, 36, .7);
}
.btn-ghost.el-button {
  --el-button-bg-color: transparent;
  --el-button-border-color: rgba(255, 255, 255, .72);
  --el-button-text-color: #fff;
  --el-button-hover-bg-color: rgba(255, 255, 255, .14);
  --el-button-hover-border-color: #fff;
  --el-button-hover-text-color: #fff;
  --el-button-active-bg-color: rgba(255, 255, 255, .22);
  --el-button-active-border-color: #fff;
  --el-button-active-text-color: #fff;
  padding-left: var(--space-6);
  padding-right: var(--space-6);
  font-size: var(--fs-base);
}

/* ===== ② 动态跑马灯 ===== */
.ticker {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  height: calc(var(--space-7) - var(--space-1));
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  overflow: hidden;
  /* 右端渐隐放在外层：左端「动态」标签处于 #000 不透明区，不受影响 */
  -webkit-mask-image: linear-gradient(90deg, #000 0, #000 calc(100% - var(--space-8)), transparent 100%);
  mask-image: linear-gradient(90deg, #000 0, #000 calc(100% - var(--space-8)), transparent 100%);
}
.ticker-label {
  flex: none;
  color: var(--rose-500);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: .12em;
}
.ticker-window {
  flex: 1;
  overflow: hidden;
  padding-left: var(--space-6);
  /* 左端 48px 渐隐，右端由 .ticker 统一处理 */
  -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 var(--space-7), #000 100%);
  mask-image: linear-gradient(90deg, transparent 0, #000 var(--space-7), #000 100%);
}
.ticker-track {
  display: flex;
  align-items: center;
  width: max-content;
  white-space: nowrap;
  animation: ticker 40s linear infinite;
}
.ticker-track:hover { animation-play-state: paused; }
.ticker-item { display: inline-flex; align-items: center; gap: var(--space-2); }
.ticker-item::before {
  content: '·';
  margin: 0 var(--space-3);
  color: var(--ink-faint);
}
.ticker-item:first-child::before { content: none; margin: 0; }
.ticker-tag { color: var(--ink-soft); font-size: var(--fs-xs); }
.ticker-text { color: var(--ink); font-size: var(--fs-sm); }
@keyframes ticker { 0% { transform: translateX(0); } 100% { transform: translateX(-50%); } }

/* ===== ③ 平台特色 ===== */
.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: var(--section-y);
}
.feature { padding: 0 var(--space-5); }
.feature:first-child { padding-left: 0; }
.feature:last-child { padding-right: 0; }
.feature + .feature { border-left: 1px solid var(--border); }
.f-ico { display: block; color: var(--rose-500); margin-bottom: var(--space-4); }
.f-title { font-size: var(--fs-lg); font-weight: 600; margin: 0 0 var(--space-2); }
.f-desc { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); text-wrap: pretty; }

/* ===== ④ 服务流程 ===== */
.flow { margin-top: var(--section-y); }
.section-title { font-size: var(--fs-xl); font-weight: 600; margin: 0; }
.steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-5);
  margin-top: var(--space-7);
}
.step-head { display: flex; align-items: center; gap: var(--space-4); }
.step-num {
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  line-height: 1;
  color: var(--rose-500);
  letter-spacing: .02em;
}
.step-rule { flex: 1; height: 1px; background: var(--border); }
.step:last-child .step-rule { display: none; }
.step-title { font-size: var(--fs-base); font-weight: 600; margin: var(--space-4) 0 var(--space-2); }
.step-desc { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== ⑤ 精选服务 ===== */
.featured { margin-top: var(--section-y); }
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
}
.head-link { font-size: var(--fs-sm); letter-spacing: .02em; padding: var(--space-2) 0; }
.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
}
.svc {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow .3s ease, transform .3s ease;
}
.svc:hover,
.svc:focus-visible { box-shadow: var(--shadow); transform: translateY(-2px); }
.svc:hover :deep(.cover-img) { transform: scale(1.03); }
.svc:active { box-shadow: var(--shadow-sm); transform: translateY(0); }
.svc-body {
  padding: var(--space-4) var(--space-4) var(--space-5);
  display: flex;
  flex-direction: column;
  flex: 1;
}
.svc-name {
  font-size: var(--fs-lg);
  font-weight: 600;
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
.svc-price em { font-style: normal; font-size: var(--fs-xs); margin-right: 2px; vertical-align: super; }
.svc-stat {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
}
.svc-star { color: var(--gold-500); font-size: var(--fs-xs); }
.more { margin-top: var(--space-7); text-align: center; }

/* ===== ⑥ 婚庆资讯 ===== */
.news { margin-top: var(--section-y); }
.news-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6) var(--space-5);
}
.news-card { cursor: pointer; }
.news-thumb {
  aspect-ratio: 3 / 2;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--bg-deep);
  background-image: linear-gradient(150deg, var(--bg) 0%, var(--bg-deep) 55%, color-mix(in srgb, var(--bg-deep) 86%, var(--rose-300)) 100%);
  margin-bottom: var(--space-4);
}
.news-thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .5s ease;
}
.news-card:hover .news-thumb img { transform: scale(1.03); }
.news-title {
  font-size: var(--fs-base);
  font-weight: 600;
  line-height: var(--lh-ui);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
  transition: color .2s ease;
}
.news-card:hover .news-title { color: var(--rose-600); }
.news-card:active .news-thumb img { opacity: .9; transform: scale(1); }
.head-link:active { color: var(--rose-500); }
.news-meta { margin: var(--space-2) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

/* ===== ⑦ 空状态 ===== */
.state-box {
  padding: var(--space-8) var(--space-5);
  background: var(--surface);
  border-radius: var(--radius);
}

.article-body { line-height: var(--lh-body); color: var(--ink); }

/* ===== ⑨ 响应式 1240 / 1024 / 768 / 560 ===== */
@media (max-width: 1240px) {
  .hero-body { padding: 0 calc(var(--gutter) + var(--space-4)); }
}
@media (max-width: 1024px) {
  .hero-title { font-size: calc(var(--fs-display) - 12px); }
  .features { grid-template-columns: repeat(2, 1fr); row-gap: var(--space-7); }
  .feature { padding: 0; }
  .feature + .feature { border-left: 0; }
  .steps { grid-template-columns: repeat(2, 1fr); row-gap: var(--space-7); }
  .featured-grid { grid-template-columns: repeat(2, 1fr); }
  .news-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .hero { height: 420px; }
  .features, .steps { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .featured-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .features, .steps { grid-template-columns: 1fr; }
  .news-grid { grid-template-columns: 1fr; }
  .steps { gap: var(--space-6); }
}
</style>
