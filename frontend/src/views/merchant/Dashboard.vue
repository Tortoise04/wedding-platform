<template>
  <div class="mch-page">
    <header class="mch-head">
      <p class="mch-sub">
        本店经营概况 · 累计 <em>{{ num(stats.orderCount) }}</em> 单，其中待接单
        <em>{{ num(stats.pendingOrderCount) }}</em>
      </p>
      <div class="mch-actions">
        <router-link to="/merchant/orders" class="mch-act link">查看订单</router-link>
      </div>
    </header>

    <div v-if="error" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">数据没有取回来</p>
        <p class="mch-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重试</el-button>
      </div>
    </div>

    <div v-else-if="loading" class="mch-panel">
      <div class="mch-panel-body">
        <div class="mch-skel"><i /><i /><i /><i /></div>
      </div>
    </div>

    <template v-else>
      <section class="mch-panel">
        <div class="kpi-row">
          <div v-for="c in topCards" :key="c.label" class="kpi">
            <span class="kpi-dot" :style="{ background: c.tone }" />
            <div>
              <p class="kpi-value">{{ c.value }}</p>
              <p class="kpi-label">{{ c.label }}</p>
            </div>
          </div>
        </div>
        <div class="kpi-row kpi-row-sub">
          <div v-for="c in subCards" :key="c.label" class="kpi">
            <div>
              <p class="kpi-value kpi-value-sm">{{ c.value }}</p>
              <p class="kpi-label">{{ c.label }}</p>
            </div>
          </div>
        </div>
      </section>

      <div class="chart-grid-a">
        <section class="mch-panel">
          <div class="mch-panel-head">
            <h2 class="mch-panel-title">订单状态分布</h2>
            <span class="mch-row-sub">全部订单</span>
          </div>
          <div class="chart-body">
            <template v-if="hasOrders">
              <div class="donut-wrap">
                <BaseChart :option="statusDonut" height="212px" />
                <div class="donut-center">
                  <span class="donut-num">{{ num(stats.orderCount) }}</span>
                  <span class="donut-cap">笔订单</span>
                </div>
              </div>
              <ul class="donut-legend">
                <li v-for="d in donutData" :key="d.name">
                  <i class="lg-dot" :style="{ background: d.color }" />
                  <span class="lg-name">{{ d.name }}</span>
                  <span class="lg-val">{{ d.value }}</span>
                </li>
              </ul>
            </template>
            <div v-else class="mch-state">
              <p class="mch-state-title">还没有订单</p>
              <p class="mch-state-desc">服务上架并被预订后，这里会显示各状态的订单分布。</p>
              <router-link to="/merchant/services"><el-button size="small">去管理服务</el-button></router-link>
            </div>
          </div>
        </section>

        <section class="mch-panel">
          <div class="mch-panel-head">
            <h2 class="mch-panel-title">近 7 天订单量 / 浏览量</h2>
            <span class="mch-row-sub">按日统计</span>
          </div>
          <div class="chart-body">
            <BaseChart :option="orderTrend" height="280px" />
          </div>
        </section>
      </div>

      <div class="chart-grid-b">
        <section class="mch-panel">
          <div class="mch-panel-head">
            <h2 class="mch-panel-title">平均评分</h2>
            <span class="mch-row-sub">满分 5 分</span>
          </div>
          <div class="chart-body">
            <BaseChart v-if="stats.evaluationCount > 0" :option="scoreGauge" height="240px" />
            <div v-else class="mch-state">
              <p class="mch-state-title">还没有评价</p>
              <p class="mch-state-desc">订单完成后买家的评分会汇总在这里。</p>
              <router-link to="/merchant/orders"><el-button size="small">查看订单</el-button></router-link>
            </div>
          </div>
        </section>

        <section class="mch-panel">
          <div class="mch-panel-head">
            <h2 class="mch-panel-title">接单与成交</h2>
            <span class="mch-row-sub">成交金额 <b class="head-amount">{{ money(stats.totalAmount) }}</b></span>
          </div>
          <div class="chart-body">
            <BaseChart :option="amountBar" height="240px" />
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

const STATUS = { PENDING: '待接单', CONFIRMED: '已确认', IN_PROGRESS: '履约中', COMPLETED: '已完成', CANCELLED: '已取消' }

const stats = ref({})
const loading = ref(true)
const error = ref('')

function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}
function num(v) {
  return v == null ? '—' : Number(v).toLocaleString('zh-CN')
}

const topCards = computed(() => [
  { label: '服务总数', value: num(stats.value.serviceCount), tone: CHART.rose },
  { label: '已上架', value: num(stats.value.onShelfCount), tone: CHART.gold },
  { label: '总浏览量', value: num(stats.value.viewCount), tone: CHART.gold },
  { label: '总订单', value: num(stats.value.orderCount), tone: CHART.rose }
])
const subCards = computed(() => [
  { label: '待接单', value: num(stats.value.pendingOrderCount) },
  { label: '已成交', value: num(stats.value.completedOrderCount) },
  { label: '成交金额', value: money(stats.value.totalAmount) },
  {
    label: '平均评分',
    value: stats.value.evaluationCount > 0 ? Number(stats.value.averageScore).toFixed(1) : '—'
  }
])

const hasOrders = computed(() => (stats.value.orderCount || 0) > 0)

const donutData = computed(() => {
  const dist = stats.value.orderStatusDistribution || {}
  return Object.entries(dist)
    .filter(([, v]) => v > 0)
    .map(([k, v]) => ({ name: STATUS[k] || k, value: v, color: CHART.statusColor[k] || CHART.inkSoft }))
})

const statusDonut = computed(() => ({
  tooltip: { ...TOOLTIP, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  series: [
    {
      type: 'pie',
      radius: ['62%', '86%'],
      center: ['50%', '50%'],
      itemStyle: { borderColor: CHART.surface, borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: { scale: false },
      data: donutData.value.map(d => ({ name: d.name, value: d.value, itemStyle: { color: d.color } }))
    }
  ]
}))

const orderTrend = computed(() => {
  const trend = stats.value.orderTrend || []
  return {
    tooltip: { ...TOOLTIP, trigger: 'axis' },
    legend: { top: 0, itemWidth: 8, itemHeight: 8, textStyle: { color: CHART.inkSoft, fontSize: 12 } },
    grid: { left: 0, right: 12, top: 32, bottom: 0, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map(t => String(t.date).slice(5)),
      axisLine: AXIS_LINE,
      axisTick: { show: false },
      axisLabel: AXIS_LABEL
    },
    yAxis: { type: 'value', minInterval: 1, splitLine: SPLIT_LINE, axisLabel: AXIS_LABEL },
    series: [
      {
        name: '订单量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: trend.map(t => t.orders),
        itemStyle: { color: CHART.rose },
        lineStyle: { width: 2, color: CHART.rose },
        areaStyle: { color: 'rgba(194, 65, 111, 0.08)' }
      },
      {
        name: '浏览量',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: trend.map(t => t.views),
        itemStyle: { color: CHART.gold },
        lineStyle: { width: 2, color: CHART.gold }
      }
    ]
  }
})

const scoreGauge = computed(() => ({
  series: [
    {
      type: 'gauge',
      min: 0,
      max: 5,
      startAngle: 210,
      endAngle: -30,
      progress: { show: true, width: 14, itemStyle: { color: CHART.gold } },
      axisLine: { lineStyle: { width: 14, color: [[1, CHART.border]] } },
      axisTick: { show: false },
      splitLine: { length: 10, lineStyle: { color: CHART.border } },
      axisLabel: { color: CHART.inkSoft, distance: 16, fontSize: 12 },
      pointer: { show: false },
      anchor: { show: false },
      detail: {
        valueAnimation: true,
        fontSize: 28,
        fontFamily: 'Noto Serif SC',
        fontWeight: 700,
        color: CHART.ink,
        offsetCenter: [0, '70%'],
        formatter: '{value} 分'
      },
      data: [{ value: Number(stats.value.averageScore) || 0 }]
    }
  ]
}))

const amountBar = computed(() => {
  const rows = [
    { name: '待接单', value: stats.value.pendingOrderCount || 0, color: CHART.gold },
    { name: '已成交', value: stats.value.completedOrderCount || 0, color: CHART.rose }
  ]
  return {
    tooltip: {
      ...TOOLTIP,
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(194, 65, 111, 0.06)' } }
    },
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
      data: rows.map(r => r.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { ...AXIS_LABEL, color: CHART.ink, width: 96, overflow: 'break', lineHeight: 16 }
    },
    series: [
      {
        type: 'bar',
        barWidth: 12,
        data: rows.map(r => ({ value: r.value, itemStyle: { color: r.color, borderRadius: [0, 2, 2, 0] } })),
        label: { show: true, position: 'right', color: CHART.inkSoft, fontSize: 12 }
      }
    ]
  }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    stats.value = await http.get('/merchant/stats')
  } catch (e) {
    error.value = (e && e.message) || '经营数据加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.link {
  display: inline-flex;
  align-items: center;
  text-decoration: none;
}
.head-amount {
  font-family: var(--font-display);
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--rose-600);
  padding-left: var(--space-1);
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.kpi-row-sub {
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
  display: block;
  width: var(--space-1);
  height: var(--space-5);
  border-radius: var(--radius-pill);
  margin-top: var(--space-1);
}
.kpi-value {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 700;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.kpi-value-sm {
  font-size: var(--fs-lg);
}
.kpi-label {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}

.chart-grid-a {
  display: grid;
  grid-template-columns: 1fr 1.55fr;
  gap: var(--space-5);
}
.chart-grid-b {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
}
.chart-body {
  padding: var(--space-4) var(--space-5) var(--space-5);
}
.donut-wrap {
  position: relative;
}
.donut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
}
.donut-num {
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 700;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.donut-cap {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.donut-legend {
  list-style: none;
  margin: var(--space-3) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
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
}
.lg-name {
  flex: 1;
}
.lg-val {
  font-family: var(--font-display);
  font-size: var(--fs-sm);
  color: var(--ink);
}
</style>
