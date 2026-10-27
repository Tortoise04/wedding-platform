<template>
  <div class="adm-page">
    <!-- 加载态 -->
    <div v-if="loading" class="adm-panel" aria-busy="true">
      <div class="adm-skel"><i></i><i></i><i></i><i></i></div>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error" class="adm-panel">
      <div class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">数据没能加载出来</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>
    </div>

    <template v-else>
      <!-- 指标盘：两档层级，1px 分隔 -->
      <section class="adm-panel kpi-board">
        <div class="kpi-row">
          <div v-for="k in heroCards" :key="k.label" class="kpi kpi-lg">
            <span class="kpi-dot" :class="k.tone"></span>
            <div>
              <p class="kpi-value">{{ k.value }}</p>
              <p class="kpi-label">{{ k.label }}</p>
            </div>
          </div>
        </div>
        <div class="kpi-row kpi-row-sub">
          <div v-for="k in subCards" :key="k.label" class="kpi">
            <span class="kpi-dot" :class="k.tone"></span>
            <div>
              <p class="kpi-value kpi-value-sm">{{ k.value }}</p>
              <p class="kpi-label">{{ k.label }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 环形图 + 折线图 -->
      <div class="chart-grid chart-grid-a">
        <section class="adm-panel">
          <div class="adm-panel-head">
            <h3 class="adm-panel-title">订单状态分布</h3>
            <span class="adm-sub">共 {{ count(stats.orderCount) }} 单</span>
          </div>
          <div v-if="hasOrders" class="donut-wrap">
            <div class="donut-canvas">
              <BaseChart :option="statusDonut" height="264px" />
              <div class="donut-center">
                <p class="donut-total">{{ count(stats.orderCount) }}</p>
                <p class="donut-cap">订单总数</p>
              </div>
            </div>
            <ul class="donut-legend">
              <li v-for="d in donutData" :key="d.name">
                <span class="lg-dot" :style="{ background: d.color }"></span>
                <span class="lg-name">{{ d.name }}</span>
                <span class="lg-val">{{ d.value }}</span>
              </li>
            </ul>
          </div>
          <div v-else class="adm-state">
            <span class="adm-state-ico">◇</span>
            <p class="adm-state-title">还没有订单</p>
            <p class="adm-state-desc">平台产生第一笔订单后，这里会显示状态构成。</p>
          </div>
        </section>

        <section class="adm-panel">
          <div class="adm-panel-head">
            <h3 class="adm-panel-title">近 7 天订单趋势</h3>
            <span class="adm-sub">按下单日期</span>
          </div>
          <div v-if="trend.length" class="chart-body">
            <BaseChart :option="orderTrend" height="264px" />
          </div>
          <div v-else class="adm-state">
            <span class="adm-state-ico">◇</span>
            <p class="adm-state-title">暂无趋势数据</p>
            <p class="adm-state-desc">近 7 天没有订单记录。</p>
          </div>
        </section>
      </div>

      <!-- 横向条形图 ×2 -->
      <div class="chart-grid chart-grid-b">
        <section class="adm-panel">
          <div class="adm-panel-head">
            <h3 class="adm-panel-title">热门商家 Top5</h3>
            <span class="adm-sub">按订单数</span>
          </div>
          <div v-if="stats.topMerchants?.length" class="chart-body">
            <BaseChart :option="merchantBar" height="300px" />
          </div>
          <div v-else class="adm-state">
            <span class="adm-state-ico">◇</span>
            <p class="adm-state-title">暂无商家订单</p>
            <p class="adm-state-desc">还没有商家接到订单。</p>
          </div>
        </section>

        <section class="adm-panel">
          <div class="adm-panel-head">
            <h3 class="adm-panel-title">热门服务 Top5</h3>
            <span class="adm-sub">按售出数量</span>
          </div>
          <div v-if="stats.topServices?.length" class="chart-body">
            <BaseChart :option="serviceBar" height="300px" />
          </div>
          <div v-else class="adm-state">
            <span class="adm-state-ico">◇</span>
            <p class="adm-state-title">暂无服务售出</p>
            <p class="adm-state-desc">服务成交后会出现在这里。</p>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '@/api/http'
import BaseChart from '@/components/BaseChart.vue'
import { CHART, AXIS_LINE, SPLIT_LINE, AXIS_LABEL, TOOLTIP } from '@/lib/echarts'

const STATUS = { PENDING: '待审核', CONFIRMED: '已确认', IN_PROGRESS: '履约中', COMPLETED: '已完成', CANCELLED: '已取消' }
const stats = ref({})
const loading = ref(true)
const error = ref('')

function count(v) {
  return v == null ? '—' : Number(v).toLocaleString('zh-CN')
}

const heroCards = computed(() => [
  {
    label: '成交总额',
    value: stats.value.totalAmount != null ? '¥' + Number(stats.value.totalAmount).toLocaleString('zh-CN', { minimumFractionDigits: 0 }) : '—',
    tone: 't-gold'
  },
  { label: '订单总数', value: count(stats.value.orderCount), tone: 't-rose' }
])

const subCards = computed(() => [
  { label: '注册用户', value: count(stats.value.userCount), tone: 't-rose-soft' },
  { label: '入驻商家', value: count(stats.value.merchantCount), tone: 't-gold' },
  { label: '服务总数', value: count(stats.value.serviceCount), tone: 't-rose-soft' },
  { label: '待审商家', value: count(stats.value.pendingMerchantCount), tone: 't-wait' }
])

const hasOrders = computed(() => (stats.value.orderCount || 0) > 0)
const trend = computed(() => stats.value.orderTrend || [])

// 环形图：玫瑰金系语义色，中心留空给 HTML 叠总数，四周无阴影无渐变。
const donutData = computed(() => {
  const dist = stats.value.orderStatusDistribution || {}
  return Object.entries(dist)
    .filter(([, v]) => v > 0)
    .map(([k, v]) => ({ name: STATUS[k] || k, value: v, color: CHART.statusColor[k] || CHART.inkFaint }))
})

const statusDonut = computed(() => ({
  tooltip: { ...TOOLTIP, trigger: 'item', formatter: '{b}：{c} 单（{d}%）' },
  series: [
    {
      type: 'pie',
      radius: ['58%', '82%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: CHART.surface, borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: { scale: false },
      data: donutData.value.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } }))
    }
  ]
}))

// 折线图：2px 玫线 + 平铺淡玫填充，无阴影无渐变。
const orderTrend = computed(() => ({
  tooltip: { ...TOOLTIP, trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: CHART.roseLight } } },
  grid: { left: 0, right: 12, top: 16, bottom: 0, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map(t => t.date.slice(5)),
    axisLine: AXIS_LINE,
    axisTick: { show: false },
    axisLabel: AXIS_LABEL
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLine: { show: false },
    axisTick: { show: false },
    splitLine: SPLIT_LINE,
    axisLabel: AXIS_LABEL
  },
  series: [
    {
      name: '订单数',
      type: 'line',
      smooth: false,
      showSymbol: true,
      symbol: 'circle',
      symbolSize: 6,
      data: trend.value.map(t => t.orders),
      itemStyle: { color: CHART.rose },
      lineStyle: { width: 2, color: CHART.rose },
      areaStyle: { color: 'rgba(194, 65, 111, 0.08)' }
    }
  ]
}))

// 横向条形图：长标签用 overflow:'break' 换行，完整显示，不截断。
function barOption(list, labelOf, valueOf, color) {
  const rows = [...list].reverse()
  return {
    tooltip: { ...TOOLTIP, trigger: 'axis', axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(194,65,111,0.06)' } } },
    grid: { left: 0, right: 32, top: 8, bottom: 0, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: SPLIT_LINE,
      axisLabel: AXIS_LABEL
    },
    yAxis: {
      type: 'category',
      data: rows.map(labelOf),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { ...AXIS_LABEL, color: CHART.ink, width: 96, overflow: 'break', lineHeight: 16 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        data: rows.map(valueOf),
        itemStyle: { color, borderRadius: [0, 2, 2, 0] },
        label: { show: true, position: 'right', color: CHART.inkSoft, fontSize: 12 }
      }
    ]
  }
}

const merchantBar = computed(() =>
  barOption(stats.value.topMerchants || [], m => m.shopName || '已注销', m => m.orderCount, CHART.rose)
)
const serviceBar = computed(() =>
  barOption(stats.value.topServices || [], s => s.name, s => s.salesCount, CHART.goldMid)
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = (await http.get('/admin/stats')) || {}
  } catch (e) {
    error.value = e?.message || '接口 /admin/stats 请求失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* 指标盘 */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
}
.kpi-row-sub {
  grid-template-columns: repeat(4, 1fr);
  border-top: 1px solid var(--border);
}
.kpi {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-5);
}
.kpi + .kpi {
  border-left: 1px solid var(--border);
}
.kpi-dot {
  width: var(--space-1);
  height: var(--space-5);
  border-radius: var(--radius-pill);
}
.t-gold {
  background: var(--gold-500);
}
.t-rose {
  background: var(--rose-500);
}
.t-rose-soft {
  background: var(--rose-500);
}
.t-wait {
  background: var(--gold-500);
}
.kpi-value {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 700;
  line-height: var(--lh-ui);
  letter-spacing: 0.01em;
  color: var(--ink);
}
.kpi-value-sm {
  font-size: var(--fs-lg);
  font-weight: 600;
}
.kpi-label {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}

/* 图表区 */
.chart-grid {
  display: grid;
  gap: var(--space-5);
}
.chart-grid-a {
  grid-template-columns: 1fr 1.55fr;
}
.chart-grid-b {
  grid-template-columns: repeat(2, 1fr);
}
.chart-body {
  padding: var(--space-4) var(--space-5) var(--space-5);
}
.donut-wrap {
  display: grid;
  grid-template-columns: 1fr 128px;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5) var(--space-5);
}
.donut-canvas {
  position: relative;
}
.donut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  pointer-events: none;
}
.donut-total {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 700;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.donut-cap {
  margin: 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.donut-legend {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.donut-legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.lg-dot {
  width: var(--space-2);
  height: var(--space-2);
  border-radius: var(--radius-pill);
  flex: 0 0 auto;
}
.lg-name {
  flex: 1;
}
.lg-val {
  font-family: var(--font-display);
  font-size: var(--fs-sm);
  color: var(--ink);
}

@media (max-width: 1080px) {
  .chart-grid-a,
  .chart-grid-b {
    grid-template-columns: 1fr;
  }
  .kpi-row-sub {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
