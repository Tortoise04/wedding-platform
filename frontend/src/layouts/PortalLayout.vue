<template>
  <div class="portal">
    <header class="nav">
      <div class="nav-inner">
        <router-link to="/home" class="logo">缘集</router-link>
        <nav class="links">
          <router-link to="/home">首页</router-link>
          <router-link to="/services">全部服务</router-link>
          <router-link to="/merchants">找商家</router-link>
          <router-link to="/content">公告资讯</router-link>
        </nav>
        <div class="right">
          <template v-if="loggedIn">
            <span class="role">{{ roleLabel }}</span>
            <el-button size="small" type="primary" round @click="goMine">进入我的</el-button>
            <el-button size="small" round @click="logout">退出</el-button>
          </template>
          <template v-else>
            <el-button size="small" type="primary" round @click="$router.push('/login')">登录</el-button>
          </template>
        </div>
      </div>
    </header>
    <main class="content" :class="{ 'content--flush': isHome, 'content--tail-flush': isDetail }"><router-view /></main>
    <footer class="foot">
      <div class="foot-inner">
        <div class="foot-brand">
          <router-link to="/home" class="foot-logo">缘集</router-link>
          <p class="foot-tag">婚礼策划一站式服务平台，把一生一次的安排交给专业的人</p>
          <p class="foot-note">本站为毕业设计演示项目，不对外运营</p>
        </div>
        <div class="foot-col">
          <h3 class="foot-title">服务分类</h3>
          <router-link to="/services">场地</router-link>
          <router-link to="/services">套餐</router-link>
          <router-link to="/services">司仪</router-link>
          <router-link to="/services">摄影</router-link>
          <router-link to="/services">婚纱</router-link>
          <router-link to="/services">婚车</router-link>
        </div>
        <div class="foot-col">
          <h3 class="foot-title">关于</h3>
          <router-link to="/services">全部服务</router-link>
          <router-link to="/merchants">找商家</router-link>
          <router-link to="/content">公告资讯</router-link>
          <p class="foot-meta">服务时间 09:00–21:00</p>
          <p class="foot-meta">support@yuanji.demo</p>
        </div>
      </div>
      <div class="foot-base">
        <p class="foot-copy">© 2026 缘集 · 婚礼策划一站式服务平台</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRole, getToken, clearAuth } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const loggedIn = computed(() => !!getToken())
const roleLabel = computed(() => ({ ADMIN: '管理端', MERCHANT: '商家端', USER: '用户端' }[getRole()] || '用户端'))
// 首页 Hero 需要贴着导航做全幅，只有首页去掉内容区上留白
const isHome = computed(() => route.path === '/home')
// 商品详情页内容本身就收在版心里，再垫 48px 会在页脚前空出一节
const isDetail = computed(() => route.name === 'serviceDetail')

function goMine() {
  const r = getRole()
  router.push(r === 'ADMIN' ? '/admin' : r === 'MERCHANT' ? '/merchant' : '/user')
}
function logout() { clearAuth(); router.push('/home') }
</script>

<style scoped>
.portal {
  overflow-x: clip;
  min-height: 100vh;
  background: var(--bg);
}

.nav {
  position: sticky;
  top: 0;
  z-index: 10;
  height: var(--nav-h);
  background: rgba(255, 255, 255, .92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}
.nav-inner {
  max-width: var(--container);
  height: 100%;
  margin: 0 auto;
  padding: 0 var(--gutter);
  display: flex;
  align-items: center;
  gap: var(--space-7);
}
.logo {
  flex: none;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: 1;
  color: var(--ink);
  text-decoration: none;
  letter-spacing: .16em;
}
.links {
  display: flex;
  flex: 1 1 auto;
  align-items: center;
  gap: var(--space-6);
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;
}
.links::-webkit-scrollbar { display: none; }
.links a {
  position: relative;
  flex: none;
  padding: var(--space-2) 0;
  color: var(--ink-soft);
  text-decoration: none;
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  transition: color .2s ease;
}
.links a::after {
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
.links a:hover { color: var(--rose-600); }
.links a:active { color: var(--rose-500); }
.links a.router-link-active { color: var(--rose-500); }
.links a.router-link-active::after { transform: scaleX(1); }

.right { display: flex; align-items: center; gap: var(--space-3); }
.role { color: var(--ink-soft); font-size: var(--fs-sm); }

.content {
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-7) var(--gutter);
}
.content--flush { padding-top: 0; }
.content--tail-flush { padding-bottom: 0; }

.foot {
  margin-top: var(--space-9);
  border-top: 1px solid var(--border);
  background: var(--bg-deep);
}
.foot-inner {
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-9) var(--gutter) var(--space-8);
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: var(--space-7);
}
.foot-logo {
  display: inline-block;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  line-height: 1;
  color: var(--ink);
  text-decoration: none;
  letter-spacing: .16em;
}
.foot-tag {
  margin: var(--space-4) 0 var(--space-2);
  max-width: 32ch;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
}
/* --ink-faint 在 --bg-deep 上只有 2.94:1，低于 AA，元信息统一用 --ink-soft（5.41:1） */
.foot-note { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }
.foot-col { display: flex; flex-direction: column; gap: var(--space-2); }
.foot-title { margin: 0 0 var(--space-1); color: var(--ink); font-size: var(--fs-sm); font-weight: 600; line-height: var(--lh-ui); }
.foot-col a {
  align-self: flex-start;
  padding: var(--space-1) 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  text-decoration: none;
  transition: color .2s ease;
}
.foot-col a:hover { color: var(--rose-500); }
.foot-col a:active { color: var(--rose-600); }
.foot-meta { margin: 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.foot-base {
  border-top: 1px solid var(--border);
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-5) var(--gutter);
}
.foot-copy { margin: 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

@media (max-width: 900px) {
  .foot-inner { grid-template-columns: 1fr 1fr; gap: var(--space-6); padding-top: var(--space-8); }
  .foot-brand { grid-column: 1 / -1; }
  .nav-inner { gap: var(--space-5); }
  .logo { font-size: var(--fs-lg); letter-spacing: .12em; }
  .links {
    gap: var(--space-5);
    padding-right: var(--space-4);
    -webkit-mask-image: linear-gradient(90deg, #000 calc(100% - var(--space-5)), transparent);
    mask-image: linear-gradient(90deg, #000 calc(100% - var(--space-5)), transparent);
  }
}
@media (max-width: 560px) {
  .role { display: none; }
  .nav-inner { gap: var(--space-4); }
}
</style>
