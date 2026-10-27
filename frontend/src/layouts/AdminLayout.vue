<template>
  <div class="adm-root">
    <aside class="side">
      <div class="brand">
        <router-link to="/admin/dashboard" class="brand-name">缘集</router-link>
        <span class="brand-sub">管理端</span>
      </div>
      <nav class="nav">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav-item"
          :class="{ on: $route.path === m.path }"
        >
          <el-icon class="nav-ico"><component :is="m.icon" /></el-icon>
          <span>{{ m.label }}</span>
        </router-link>
      </nav>
      <div class="side-foot">平台运营后台 · v1</div>
    </aside>

    <div class="body">
      <header class="topbar">
        <div class="tb-left">
          <h1 class="tb-title">{{ current.label }}</h1>
          <p class="tb-crumb">平台管理<span class="tb-sep">/</span>{{ current.label }}</p>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearAuth, getRole, getUserId } from '@/store/auth'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/admin/dashboard', label: '数据看板', icon: 'DataAnalysis' },
  { path: '/admin/merchants', label: '商家审核', icon: 'Shop' },
  { path: '/admin/services', label: '服务审核', icon: 'Goods' },
  { path: '/admin/evaluations', label: '评价审核', icon: 'Star' },
  { path: '/admin/orders', label: '订单监管', icon: 'List' },
  { path: '/admin/disputes', label: '争议仲裁', icon: 'Warning' },
  { path: '/admin/penalties', label: '违规处罚', icon: 'Lock' },
  { path: '/admin/content', label: '公告资讯', icon: 'Document' }
]

const ROLE_TEXT = { ADMIN: '管理员', MERCHANT: '商家', USER: '用户' }
const roleText = computed(() => ROLE_TEXT[getRole()] || '管理员')
const userId = computed(() => getUserId())
const current = computed(() => menus.find(m => m.path === route.path) || { label: '平台管理' })

function logout() {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.adm-root {
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
</style>

<style>
/* ==========================================================================
   管理端整端共用样式：统一页头 / 面板 / 筛选行 / 表格行高 / 状态标签 / 三态。
   全部挂在 .adm-root 之下，离开管理端不会外泄。
   ========================================================================== */
/* el-select 的占位文本默认是 3.4:1 的浅灰（EP 的 #a8abb2），拉回 --ink-soft 才过 AA */
.adm-root .el-select__placeholder {
  color: var(--ink-soft);
}

/* 分页按钮底色拉回令牌，避免 Element Plus 的 #f0f2f5 成为第 N 个色 */
.adm-root .el-pagination.is-background .btn-prev,
.adm-root .el-pagination.is-background .btn-next,
.adm-root .el-pagination.is-background .el-pager li {
  background: var(--bg-deep);
  color: var(--ink-soft);
}
.adm-root .el-pagination.is-background .el-pager li.is-active {
  background: var(--rose-500);
  color: var(--surface);
}

.adm-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

/* 页头行：左侧计数摘要，右侧主操作（页标题由顶部条承担，不重复） */
.adm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  min-height: var(--space-6);
}
.adm-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 600;
  line-height: var(--lh-ui);
  color: var(--ink);
}
.adm-sub {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.adm-sub em {
  font-family: var(--font-display);
  font-style: normal;
  font-size: var(--fs-lg);
  color: var(--rose-600);
  padding: 0 var(--space-1);
}

/* 面板：表格与图表的唯一容器 */
.adm-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}
.adm-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.adm-panel-title {
  font-family: var(--font-display);
  font-size: var(--fs-base);
  font-weight: 600;
  color: var(--ink);
}

/* 筛选行：一行放完（关键字 + 状态下拉 + 操作） */
.adm-toolbar {
  display: flex;
  align-items: flex-end;
  gap: var(--space-3);
  flex-wrap: wrap;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
}
.adm-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}
.adm-field.is-grow {
  flex: 1;
  min-width: 200px;
}
.adm-label {
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  color: var(--ink-soft);
}
.adm-toolbar .adm-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-left: auto;
}

/* 表格：行高统一 44，表头弱化 */
.adm-panel .el-table {
  --el-table-border-color: var(--border);
  --el-table-row-hover-bg-color: var(--rose-50);
  --el-table-text-color: var(--ink);
  font-size: var(--fs-sm);
}
.adm-panel .el-table th.el-table__cell {
  background: var(--bg-deep);
  padding: 0;
  font-size: var(--fs-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink-soft);
}
.adm-panel .el-table td.el-table__cell {
  padding: 0;
}
.adm-panel .el-table .cell {
  padding: var(--space-3) var(--space-4);
  line-height: var(--lh-ui);
  word-break: break-word;
}
.adm-panel .el-table__inner-wrapper::before {
  display: none;
}
.adm-panel .el-table__empty-block {
  min-height: 0;
}
.adm-two-line {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.adm-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.adm-amount {
  font-family: var(--font-display);
  font-size: var(--fs-base);
  color: var(--rose-600);
}
.adm-cell-sub {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}

/* 状态标签：绿=通过/完成、玫瑰金=进行中、黄=待审、灰=关闭/驳回 */
.adm-tag {
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
.adm-tag::before {
  content: '';
  width: var(--space-1);
  height: var(--space-1);
  border-radius: var(--radius-pill);
  background: var(--ink-faint);
}
.adm-tag.is-ok {
  background: #eaf2ee;
}
.adm-tag.is-ok::before {
  background: var(--success);
}
.adm-tag.is-doing {
  background: var(--rose-50);
}
.adm-tag.is-doing::before {
  background: var(--rose-500);
}
.adm-tag.is-wait {
  background: #f6f1e9;
}
.adm-tag.is-wait::before {
  background: var(--warning);
}
.adm-tag.is-off {
  color: var(--ink-soft);
  background: var(--bg-deep);
}

/* 三态：加载骨架 / 空态 / 错误态 */
.adm-skel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-5);
}
.adm-skel i {
  display: block;
  height: var(--space-4);
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: adm-pulse 1.4s ease-in-out infinite;
}
.adm-skel i:nth-child(2) {
  width: 92%;
}
.adm-skel i:nth-child(3) {
  width: 78%;
}
@keyframes adm-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
@media (prefers-reduced-motion: reduce) {
  .adm-skel i {
    animation: none;
  }
}
.adm-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-8) var(--space-5);
  text-align: center;
}
.adm-state-ico {
  font-size: var(--fs-xl);
  line-height: 1;
  color: var(--ink-faint);
}
.adm-state-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  color: var(--ink);
}
.adm-state-desc {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  max-width: 40ch;
}
.adm-state .el-button {
  margin-top: var(--space-2);
}

/* 分段切换（替代 el-tabs 的大内边距） */
.adm-seg {
  display: inline-flex;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  overflow: hidden;
  background: var(--surface);
}
.adm-seg button {
  font: inherit;
  font-size: var(--fs-sm);
  color: var(--ink-soft);
  background: transparent;
  border: 0;
  padding: var(--space-1) var(--space-4);
  cursor: pointer;
  transition: color 0.15s ease, background 0.15s ease;
}
.adm-seg button:hover {
  color: var(--rose-600);
}
.adm-seg button.on {
  color: var(--surface);
  background: var(--rose-500);
}

/* 分页 */
.adm-pager {
  display: flex;
  justify-content: center;
  padding: var(--space-4) 0 0;
}

/* 表单控件对齐 4px 栅格（覆盖 Element Plus 的 11px / 15px 内边距） */
.adm-root .el-input__wrapper,
.adm-root .el-select__wrapper,
.adm-root .el-textarea__inner {
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--border) inset;
  background: var(--surface);
}
.adm-root .el-textarea__inner {
  padding: var(--space-2) var(--space-3);
  line-height: var(--lh-body);
}
.adm-root .el-input__inner,
.adm-root .el-select__placeholder {
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
}
.adm-root .el-input__inner {
  height: var(--space-5);
}
.adm-root .el-select__wrapper {
  min-height: var(--space-6);
}
.adm-root .el-button {
  font-size: var(--fs-sm);
}
.adm-root .el-button--small {
  height: var(--space-6);
  padding: 0 var(--space-3);
}
.adm-root .el-button--small.is-round {
  padding: 0 var(--space-3);
}
.adm-root .el-form-item {
  margin-bottom: var(--space-4);
}
.adm-root .el-form-item__label {
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.adm-root .el-dialog {
  border-radius: var(--radius-lg);
}
.adm-root .el-dialog__header {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border);
  margin-right: 0;
}
.adm-root .el-dialog__title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  color: var(--ink);
}
.adm-root .el-dialog__body {
  padding: var(--space-5);
}
.adm-root .el-dialog__footer {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--border);
}
.adm-root .el-radio-group {
  gap: var(--space-4);
}
.adm-root .el-radio {
  margin-right: 0;
  font-size: var(--fs-sm);
}
.adm-root .el-pagination {
  --el-pagination-font-size: var(--fs-sm);
  gap: var(--space-1);
}
.adm-root .el-pagination button,
.adm-root .el-pager li {
  border-radius: var(--radius-sm);
  font-size: var(--fs-sm);
}
.adm-root .el-message-box {
  border-radius: var(--radius-lg);
}
.adm-root .el-rate {
  --el-rate-fill-color: var(--gold-500);
  --el-rate-void-color: var(--border);
  --el-rate-disabled-void-color: var(--border);
}
.adm-root .el-rate__icon {
  margin-right: var(--space-1);
  font-size: var(--fs-base);
}
</style>
