<template>
  <div class="page">
    <header class="masthead">
      <h1 class="mast-title">浏览记录</h1>
      <p class="mast-sub">按时间倒序排列，回到上次看过的那一项。</p>
    </header>

    <p class="count-line">共 <em>{{ rows.length }}</em> 条记录</p>

    <div v-if="loading" class="list">
      <div v-for="i in 4" :key="i" class="sk-row">
        <div class="sk-thumb"></div>
        <div class="sk-body">
          <div class="sk-line sk-line--lg"></div>
          <div class="sk-line sk-line--short"></div>
        </div>
      </div>
    </div>

    <div v-else-if="error" class="state">
      <p class="state-ico">!</p>
      <p class="state-title">浏览记录没有加载出来</p>
      <p class="state-desc">网络或服务端出了点问题，稍后再试一次。</p>
      <button class="act" @click="load">重新加载</button>
    </div>

    <div v-else-if="!rows.length" class="state">
      <p class="state-ico">◇</p>
      <p class="state-title">还没有浏览记录</p>
      <p class="state-desc">看过场地、套餐或婚纱之后，这里会按时间留下一串脚印。</p>
      <button class="act" @click="router.push('/services')">去逛逛服务</button>
    </div>

    <div v-else class="list">
      <article v-for="r in rows" :key="r.id" class="row">
        <button class="row-cover" @click="goDetail(r.serviceId)" :aria-label="`查看 ${r.name}`">
          <ServiceCover :service="{ id: r.serviceId, categoryId: r.categoryId }" ratio="1 / 1" :seed="r.serviceId" />
        </button>
        <div class="row-main">
          <h2 class="row-name">{{ r.name }}</h2>
          <p class="row-meta">浏览于 {{ fmt(r.browseTime) }}</p>
        </div>
        <button class="act" @click="goDetail(r.serviceId)">查看</button>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'

const router = useRouter()
const rows = ref([])
const loading = ref(false)
const error = ref(false)

// 名称字典：浏览记录只存 serviceId；逐条请求 /services/{id} 会反向再写一条浏览记录
const svcMap = ref({})

function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function loadNames() {
  try {
    const data = await http.get('/services/search', { params: { page: 0, size: 100 } })
    const map = {}
    for (const s of data?.list || []) map[s.id] = { name: s.name, categoryId: s.categoryId }
    svcMap.value = map
  } catch (e) { /* 名称补不上时用占位文案 */ }
}

async function load() {
  loading.value = true
  error.value = false
  try {
    const list = await http.get('/browse-history')
    rows.value = (list || []).map(h => ({
      ...h,
      // 检索只含在架服务;查不到名字的多半已下架,给可读文案而不是裸 ID
      name: svcMap.value[h.serviceId]?.name || `已下架服务 #${h.serviceId}`,
      categoryId: svcMap.value[h.serviceId]?.categoryId
    }))
  } catch (e) {
    rows.value = []
    error.value = true
  } finally {
    loading.value = false
  }
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

.list { display: flex; flex-direction: column; gap: var(--space-3); }
.row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) auto;
  gap: var(--space-4);
  align-items: center;
  padding: var(--space-3);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: box-shadow .3s ease, border-color .3s ease;
}
.row:hover { box-shadow: var(--shadow-sm); border-color: var(--rose-200); }
.row-cover { display: block; padding: 0; border: 0; background: none; border-radius: var(--radius-sm); overflow: hidden; cursor: pointer; line-height: 0; }
.row-main { min-width: 0; }
.row-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--fs-base);
  line-height: var(--lh-ui);
  font-weight: 600;
}
.row-meta { margin: var(--space-1) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

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

.sk-row { display: grid; grid-template-columns: 64px minmax(0, 1fr); gap: var(--space-4); align-items: center; padding: var(--space-3); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius); }
.sk-thumb { aspect-ratio: 1 / 1; border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-body { display: flex; flex-direction: column; gap: var(--space-2); }
.sk-line { height: var(--space-3); border-radius: var(--radius-sm); background: var(--bg-deep); }
.sk-line--lg { height: var(--space-5); width: 50%; }
.sk-line--short { width: 28%; }
.sk-thumb, .sk-line { animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) {
  .sk-thumb, .sk-line { animation: none; }
}
</style>
