<template>
  <div class="page">
    <header class="masthead">
      <h1 class="mast-title">我的收藏</h1>
      <p class="mast-sub">看过、比过之后仍然想留下的那些，都收在这里。</p>
    </header>

    <p class="count-line">共 <em>{{ rows.length }}</em> 个收藏</p>

    <div v-if="loading" class="grid">
      <div v-for="i in 3" :key="i" class="sk-card">
        <div class="sk-cover"></div>
        <div class="sk-line sk-line--lg"></div>
        <div class="sk-line"></div>
      </div>
    </div>

    <div v-else-if="error" class="state">
      <p class="state-ico">!</p>
      <p class="state-title">收藏没有加载出来</p>
      <p class="state-desc">网络或服务端出了点问题，稍后再试一次。</p>
      <button class="act" @click="load">重新加载</button>
    </div>

    <div v-else-if="!rows.length" class="state">
      <p class="state-ico">♡</p>
      <p class="state-title">还没有收藏</p>
      <p class="state-desc">在服务详情页点一次「收藏」，这里就会留下记录，方便下次回来比价。</p>
      <button class="act" @click="router.push('/services')">去逛逛服务</button>
    </div>

    <div v-else class="grid">
      <article v-for="r in rows" :key="r.id" class="fav">
        <button class="fav-cover" @click="goDetail(r.serviceId)" :aria-label="`查看 ${r.name}`">
          <ServiceCover :service="{ id: r.serviceId, categoryId: r.categoryId }" ratio="16 / 9" :seed="r.serviceId" />
        </button>
        <div class="fav-body">
          <h2 class="fav-name">{{ r.name }}</h2>
          <p v-if="r.price != null" class="fav-price">{{ money(r.price) }}</p>
          <p class="fav-meta">收藏于 {{ fmt(r.createdAt) }}</p>
          <div class="fav-acts">
            <button class="act" @click="goDetail(r.serviceId)">查看</button>
            <button class="act act--muted" @click="remove(r.serviceId)">取消收藏</button>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'

const router = useRouter()
const rows = ref([])
const loading = ref(false)
const error = ref(false)

// 名称字典：收藏只存 serviceId，一次性取服务列表建映射（逐条请求会触发浏览记录写入）
const svcMap = ref({})

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

// 金额统一千分位：整数不带小数，有角分才保留两位（与门户 Home / ServiceList 的写法一致）
function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

async function loadNames() {
  try {
    const data = await http.get('/services/search', { params: { page: 0, size: 100 } })
    const map = {}
    for (const s of data?.list || []) map[s.id] = { name: s.name, categoryId: s.categoryId, price: s.price }
    svcMap.value = map
  } catch (e) { /* 名称补不上时用占位文案，不影响列表本身 */ }
}

async function load() {
  loading.value = true
  error.value = false
  try {
    const list = await http.get('/favorites')
    rows.value = (list || []).map(f => ({
      ...f,
      name: svcMap.value[f.serviceId]?.name || `服务 #${f.serviceId}`,
      categoryId: svcMap.value[f.serviceId]?.categoryId,
      price: svcMap.value[f.serviceId]?.price
    }))
  } catch (e) {
    rows.value = []
    error.value = true
  } finally {
    loading.value = false
  }
}

async function remove(serviceId) {
  await http.delete(`/favorites/${serviceId}`)
  ElMessage.success('已取消收藏')
  load()
}

function goDetail(id) { router.push(`/services/${id}`) }

onMounted(async () => { await loadNames(); await load() })
</script>

<style scoped>
.page {
  padding-bottom: var(--space-7);
  /* Element 占位符默认 #9A868D（3.40:1），按 AA 提到 ink-soft */
  --el-text-color-placeholder: var(--ink-soft);
}

.masthead { padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.mast-title { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.mast-sub { max-width: 46ch; margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.count-line {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin: var(--space-5) 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.count-line em { font-family: var(--font-display); font-style: normal; font-size: var(--fs-xl); line-height: 1; color: var(--rose-500); }

.grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-6) var(--space-5); }
.fav {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: box-shadow .3s ease, transform .3s ease;
}
.fav:hover { box-shadow: var(--shadow); transform: translateY(-2px); }
.fav-cover {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: none;
  cursor: pointer;
  line-height: 0;
}
.fav-body { display: flex; flex-direction: column; flex: 1; padding: var(--space-5); }
.fav-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--fs-lg);
  line-height: var(--lh-ui);
  font-weight: 600;
  transition: color .2s ease;
}
.fav:hover .fav-name { color: var(--rose-500); }
.fav-price { margin: var(--space-2) 0 0; color: var(--rose-500); font-family: var(--font-display); font-size: var(--fs-base); font-weight: 600; line-height: 1; }
.fav-meta { margin: var(--space-2) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.fav-acts { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-4); padding-top: var(--space-4); border-top: 1px solid var(--border); }

.act {
  height: var(--space-6);
  padding: 0 var(--space-4);
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
.act:hover { border-color: var(--rose-500); color: var(--rose-500); }
.act--muted { color: var(--ink-soft); }

.state {
  padding: var(--space-8) var(--space-6);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  text-align: center;
}
.state-ico { margin: 0; font-family: var(--font-display); font-size: var(--fs-xl); line-height: 1; color: var(--ink-soft); }
.state-title { margin: var(--space-4) 0 0; font-size: var(--fs-base); font-weight: 600; line-height: var(--lh-ui); }
.state-desc { max-width: 40ch; margin: var(--space-2) auto 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

.sk-card { padding-bottom: var(--space-5); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; }
.sk-cover { aspect-ratio: 16 / 9; background: var(--bg-deep); }
.sk-line { height: var(--space-3); margin: var(--space-3) var(--space-5) 0; border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 70%; }
.sk-cover, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-cover, .sk-line { animation: none; }
}

@media (max-width: 1024px) { .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .grid { grid-template-columns: minmax(0, 1fr); } }
</style>
