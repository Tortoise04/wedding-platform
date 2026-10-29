<template>
  <div class="mch-root">
    <aside class="side">
      <div class="brand">
        <router-link to="/merchant/dashboard" class="brand-name">缘集</router-link>
        <span class="brand-sub">商家端</span>
      </div>
      <nav class="nav">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav-item"
          :class="{ on: isOn(m) }"
        >
          <el-icon class="nav-ico"><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
          <span v-if="badgeOf(m.path)" class="nav-badge">{{ badgeOf(m.path) }}</span>
        </router-link>
      </nav>
      <div class="side-foot">商家工作台 · v1</div>
    </aside>

    <div class="body">
      <header class="topbar">
        <div class="tb-left">
          <h1 class="tb-title">{{ current.label }}</h1>
          <p class="tb-crumb">商家端<span class="tb-sep">/</span>{{ current.label }}</p>
        </div>
        <div class="tb-right">
          <span class="who">
            <span class="who-role">{{ roleText }}</span>
            <span class="who-name">{{ userId ? 'UID ' + userId : '未登录' }}</span>
          </span>
          <router-link to="/home" class="tb-link">返回首页</router-link>
          <button type="button" class="tb-out" @click="logout">退出</button>
        </div>
      </header>
      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getRole, getUserId } from '@/store/auth'
import http from '@/api/http'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/merchant/dashboard', label: '经营概览', icon: 'DataAnalysis' },
  { path: '/merchant/services', label: '服务管理', icon: 'Goods' },
  { path: '/merchant/orders', label: '订单履约', icon: 'List' },
  { path: '/merchant/disputes', label: '争议举证', icon: 'Warning' },
  { path: '/merchant/evaluations', label: '评价回复', icon: 'Star' },
  { path: '/merchant/consultations', label: '咨询消息', icon: 'ChatDotRound' },
  { path: '/merchant/profile', label: '店铺资料', icon: 'Shop' }
]

const ROLE_TEXT = { ADMIN: '管理员', MERCHANT: '商家', USER: '用户' }
const roleText = computed(() => ROLE_TEXT[getRole()] || '商家')
const userId = computed(() => getUserId())
const current = computed(
  () => menus.find(m => route.path.startsWith(m.path)) || { label: '商家工作台' }
)

function isOn(m) {
  return route.path.startsWith(m.path)
}

// 待接单角标：数字挂在导航上，商家在任何一个页面都知道有没有没接的单
const pendingCount = ref(0)
const BADGE_PATH = '/merchant/orders'
const badgeOf = path => (path === BADGE_PATH ? pendingCount.value : 0)

async function loadPending() {
  try {
    const stats = await http.get('/merchant/stats')
    pendingCount.value = Number(stats && stats.pendingOrderCount) || 0
  } catch {
    pendingCount.value = 0
  }
}

onMounted(loadPending)
// 每次进商家端换页都刷一次：刚处理完一单，角标要跟着掉
watch(() => route.path, p => { if (p.startsWith('/merchant')) loadPending() })

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.mch-root {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  font-family: var(--font-body);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
}

/* ---------- 侧栏 ---------- */
.side {
  position: sticky;
  top: 0;
  align-self: flex-start;
  height: 100vh;
  width: 232px;
  flex: 0 0 232px;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-right: 1px solid var(--border);
}
.brand {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  padding: var(--space-5) var(--space-5) var(--space-4);
  border-bottom: 1px solid var(--border);
}
.brand-name {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--rose-600);
  text-decoration: none;
}
.brand-sub {
  font-size: var(--fs-xs);
  letter-spacing: 0.16em;
  color: var(--ink-soft);
}
.nav {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: var(--space-3) 0;
  overflow-y: auto;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-5);
  color: var(--ink-soft);
  text-decoration: none;
  border-left: 2px solid transparent;
  transition: color 0.15s ease, background 0.15s ease, border-color 0.15s ease;
}
.nav-item:hover {
  color: var(--rose-600);
  background: var(--rose-50);
}
.nav-item.on {
  color: var(--rose-600);
  font-weight: 600;
  background: var(--rose-50);
  border-left-color: var(--rose-500);
}
.nav-ico {
  font-size: var(--fs-base);
}
/* 待接单角标：靠右贴边，和菜单文字同一行 */
.nav-badge {
  margin-left: auto;
  min-width: var(--space-4);
  padding: 0 var(--space-2);
  border-radius: var(--radius-pill);
  background: var(--rose-500);
  color: var(--surface);
  font-size: var(--fs-xs);
  font-weight: 600;
  line-height: var(--space-4);
  text-align: center;
  font-variant-numeric: tabular-nums;
}
.side-foot {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--border);
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}

/* ---------- 主体 ---------- */
.body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.topbar {
  position: sticky;
  top: 0;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-6);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}
.tb-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.tb-crumb {
  margin: var(--space-1) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.tb-sep {
  padding: 0 var(--space-1);
  color: var(--ink-soft);
}
.tb-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}
.who {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: var(--lh-ui);
}
.who-role {
  font-size: var(--fs-sm);
  color: var(--ink);
}
.who-name {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.tb-link {
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  text-decoration: none;
  padding-bottom: var(--space-1);
  border-bottom: 1px solid transparent;
}
.tb-link:hover {
  color: var(--rose-600);
  border-bottom-color: var(--rose-300);
}
.tb-out {
  font: inherit;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  padding: var(--space-1) var(--space-3);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease;
}
.tb-out:hover {
  color: var(--rose-600);
  border-color: var(--rose-300);
}
.main {
  flex: 1;
  padding: var(--space-6) var(--space-6) var(--space-9);
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
}
@media (max-width: 900px) {
  .side {
    display: none;
  }
}
</style>

<style>
/* ==========================================================================
   商家端整端共用样式（与 .adm-* 同一套语言）：页头 / 面板 / 卡片行 / 状态标签 /
   三态 / 分页 / 表单控件栅格。全部挂在 .mch-root 之下，离开商家端不外泄。
   ========================================================================== */
/* 输入框 / 下拉的占位文本默认是 3.4:1 的浅灰（--ink-faint），拉回 --ink-soft 才过 AA */
.mch-root {
  --el-text-color-placeholder: var(--ink-soft);
}
.mch-root .el-select__placeholder {
  color: var(--ink-soft);
}

/* 分页按钮底色拉回令牌，避免 Element Plus 的 #f0f2f5 成为第 N 个色 */
.mch-root .el-pagination.is-background .btn-prev,
.mch-root .el-pagination.is-background .btn-next,
.mch-root .el-pagination.is-background .el-pager li {
  background: var(--bg-deep);
  color: var(--ink-soft);
}
.mch-root .el-pagination.is-background .el-pager li.is-active {
  background: var(--rose-500);
  color: var(--surface);
}

.mch-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* 页头行：左侧计数摘要，右侧主操作（页标题由顶部条承担，不重复） */
.mch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  min-height: var(--space-6);
}
.mch-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.mch-sub {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.mch-sub em {
  font-family: var(--font-display);
  font-style: normal;
  font-size: var(--fs-lg);
  color: var(--rose-600);
  padding: 0 var(--space-1);
}
.mch-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* 面板：图表与表格的唯一容器 */
.mch-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}
.mch-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.mch-panel-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--ink);
}
.mch-panel-body {
  padding: var(--space-5);
}

/* 筛选行 */
.mch-toolbar {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.mch-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.mch-field.is-grow {
  flex: 1;
  min-width: 200px;
}
.mch-label {
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
}
.mch-toolbar .mch-actions {
  margin-left: auto;
}

/* 卡片行：服务封面 + 主信息 + 状态/金额 + 操作 */
.mch-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.mch-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) auto;
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.mch-row:hover {
  border-color: var(--rose-200);
  box-shadow: var(--shadow-sm);
}
.mch-thumb {
  width: 88px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  line-height: 0;
}
.mch-row-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.mch-row-name {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-base);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.mch-row-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-1) var(--space-3);
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.mch-row-sub {
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
}
.mch-row-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-2);
}
.mch-row-acts {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}
.mch-amount {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--rose-600);
}
.mch-amount.is-off {
  color: var(--ink-soft);
}
.mch-act {
  height: var(--space-6);
  padding: 0 var(--space-4);
  font: inherit;
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  color: var(--ink);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: color 0.15s ease, border-color 0.15s ease;
}
.mch-act:hover {
  color: var(--rose-600);
  border-color: var(--rose-300);
}
.mch-act.is-rose {
  border-color: var(--rose-200);
  color: var(--rose-600);
}
.mch-act.is-muted {
  color: var(--ink-soft);
}

/* 键值清单（详情页） */
.mch-kv {
  margin: 0;
  display: flex;
  flex-direction: column;
}
.mch-kv-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: var(--space-4);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border);
  font-size: var(--fs-sm);
}
.mch-kv-row:last-child {
  border-bottom: 0;
}
.mch-kv-row dt {
  margin: 0;
  color: var(--ink-soft);
}
.mch-kv-row dd {
  margin: 0;
  color: var(--ink);
  word-break: break-word;
}
.mch-block {
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.mch-block-title {
  margin: 0 0 var(--space-3);
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.mch-back {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  align-self: flex-start;
  height: var(--space-6);
  padding: 0;
  font: inherit;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  background: transparent;
  border: 0;
  cursor: pointer;
}
.mch-back:hover {
  color: var(--rose-600);
}
.mch-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.mch-two-line {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

/* 状态标签：绿=完成/通过、玫瑰=进行中、黄=待处理、灰=关闭/取消 */
.mch-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  white-space: nowrap;
  color: var(--ink);
  background: var(--bg-deep);
}
.mch-tag::before {
  content: '';
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--ink-faint);
}
.mch-tag.is-ok {
  background: color-mix(in srgb, var(--success) 12%, var(--surface));
}
.mch-tag.is-ok::before {
  background: var(--success);
}
.mch-tag.is-doing {
  background: var(--rose-50);
}
.mch-tag.is-doing::before {
  background: var(--rose-500);
}
.mch-tag.is-wait {
  background: color-mix(in srgb, var(--warning) 12%, var(--surface));
}
.mch-tag.is-wait::before {
  background: var(--warning);
}
.mch-tag.is-off {
  color: var(--ink-soft);
  background: var(--bg-deep);
}

/* 三态：加载骨架 / 空态 / 错误态 */
.mch-skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.mch-skel i {
  display: block;
  height: var(--space-6);
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: mch-pulse 1.4s ease-in-out infinite;
}
.mch-skel i:nth-child(2) {
  width: 92%;
}
.mch-skel i:nth-child(3) {
  width: 78%;
}
@keyframes mch-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
@media (prefers-reduced-motion: reduce) {
  .mch-skel i {
    animation: none;
  }
}
.mch-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8) var(--space-5);
  text-align: center;
}
.mch-state-ico {
  font-size: var(--fs-xl);
  line-height: 1;
  color: var(--ink-soft);
}
.mch-state-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  color: var(--ink);
}
.mch-state-desc {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  max-width: 40ch;
}
.mch-state .el-button {
  margin-top: var(--space-2);
}

/* 分段切换 */
.mch-seg {
  display: inline-flex;
  flex-wrap: wrap;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  overflow: hidden;
  background: var(--surface);
}
.mch-seg button {
  font: inherit;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  background: transparent;
  border: 0;
  padding: var(--space-1) var(--space-4);
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
}
.mch-seg button:hover {
  color: var(--rose-600);
}
.mch-seg button.on {
  color: var(--surface);
  background: var(--rose-500);
}

.mch-pager {
  display: flex;
  justify-content: center;
  padding: var(--space-4) 0 0;
}

/* 表单控件对齐 4px 栅格（覆盖 Element Plus 的 11px / 15px 内边距） */
.mch-root .el-input__wrapper,
.mch-root .el-select__wrapper,
.mch-root .el-textarea__inner {
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--border) inset;
  background: var(--surface);
}
.mch-root .el-textarea__inner {
  padding: var(--space-2) var(--space-3);
  line-height: var(--lh-body);
}
.mch-root .el-input__inner,
.mch-root .el-select__placeholder {
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.mch-root .el-input__inner {
  height: var(--space-5);
}
.mch-root .el-select__wrapper {
  min-height: var(--space-6);
}
.mch-root .el-button {
  font-size: var(--fs-sm);
}
.mch-root .el-button--small {
  height: var(--space-6);
  padding: 0 var(--space-3);
}
.mch-root .el-form-item {
  margin-bottom: var(--space-4);
}
.mch-root .el-form-item__label {
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.mch-root .el-dialog {
  border-radius: var(--radius-lg);
}
.mch-root .el-dialog__header {
  padding: var(--space-4) var(--space-5);
  margin-right: 0;
  border-bottom: 1px solid var(--border);
}
.mch-root .el-dialog__title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  color: var(--ink);
}
.mch-root .el-dialog__body {
  padding: var(--space-5);
}
.mch-root .el-dialog__footer {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--border);
}
.mch-root .el-radio-group {
  gap: var(--space-4);
}
.mch-root .el-radio {
  margin-right: 0;
  font-size: var(--fs-sm);
}
.mch-root .el-pagination {
  --el-pagination-font-size: var(--fs-sm);
  gap: var(--space-1);
}
.mch-root .el-pagination button,
.mch-root .el-pager li {
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
}
.mch-root .el-message-box {
  border-radius: var(--radius-lg);
}
.mch-root .el-rate {
  --el-rate-fill-color: var(--gold-500);
  --el-rate-void-color: var(--border);
  --el-rate-disabled-void-color: var(--border);
}
.mch-root .el-rate__icon {
  margin-right: var(--space-1);
  font-size: var(--fs-base);
}
</style>
