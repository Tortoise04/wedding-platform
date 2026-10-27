// ECharts 按需引入 + 全局注册。只引入本次用到的图表与组件，减小包体。
import * as echarts from 'echarts/core'
import { PieChart, BarChart, LineChart, GaugeChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  PieChart, BarChart, LineChart, GaugeChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent,
  CanvasRenderer
])

// 图表配色：与 theme.css 的 CSS 变量同值（ECharts 走 canvas，读不到 CSS 变量，故在此固化）。
// 主色玫瑰金系，辅助色由 --gold-500 派生；灰阶只用 ink 系，避免默认饱和色。
export const CHART = {
  rose: '#C2416F',      // --rose-500
  roseDeep: '#8C284A',  // --rose-700
  roseMid: '#D46F99',   // --rose-400
  roseLight: '#E69ABB', // --rose-300
  roseWash: '#F9E1EC',  // --rose-100
  gold: '#B8863B',      // --gold-500
  goldMid: '#C9A15A',   // --gold-400
  goldDeep: '#9A6F2F',  // --gold-600
  success: '#2F7D5B',   // --success
  warning: '#A9762B',   // --warning
  danger: '#C0392B',    // --danger
  ink: '#2E2024',       // --ink
  inkSoft: '#6F5C63',   // --ink-soft
  inkFaint: '#9A868D',  // --ink-faint
  border: '#EDE0E5',    // --border
  surface: '#FFFFFF',   // --surface
  // 订单状态 → 颜色：与 .adm-tag 的语义一致（黄=待审、玫瑰金=进行中、绿=完成、灰=关闭）
  statusColor: {
    PENDING: '#B8863B',
    CONFIRMED: '#E69ABB',
    IN_PROGRESS: '#C2416F',
    COMPLETED: '#2F7D5B',
    CANCELLED: '#9A868D'
  },
  palette: ['#C2416F', '#B8863B', '#D46F99', '#C9A15A', '#8C284A', '#E69ABB']
}

// 网格线只用 --border 的 1px 实线，无阴影、无渐变。
export const AXIS_LINE = { lineStyle: { color: CHART.border, width: 1 } }
export const SPLIT_LINE = { lineStyle: { color: CHART.border, width: 1 } }
export const AXIS_LABEL = { color: CHART.inkSoft, fontSize: 12, fontFamily: 'inherit' }

// tooltip 统一：白底、1px 边、8px 圆角、无阴影。
export const TOOLTIP = {
  backgroundColor: CHART.surface,
  borderColor: CHART.border,
  borderWidth: 1,
  padding: [8, 12],
  textStyle: { color: CHART.ink, fontSize: 12 },
  extraCssText: 'border-radius:8px;box-shadow:none;'
}

export default echarts
