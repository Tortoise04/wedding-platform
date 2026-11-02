<template>
  <div class="uc">
    <header class="nav">
      <div class="nav-inner">
        <router-link to="/home" class="logo">缘集</router-link>
        <span class="nav-sep" aria-hidden="true"></span>
        <span class="nav-crumb">个人中心</span>
        <div class="right">
          <router-link to="/home" class="nav-link">返回首页</router-link>
          <el-button size="small" round @click="logout">退出登录</el-button>
        </div>
      </div>
    </header>

    <div class="shell">
      <aside class="side">
        <div class="account">
          <div class="account-avatar"><el-icon :size="20"><User /></el-icon></div>
          <div class="account-who">
            <p class="account-name">我的账户</p>
            <p class="account-role">普通用户</p>
          </div>
        </div>

        <nav class="side-nav">
          <router-link v-for="m in menus" :key="m.path" :to="m.path" class="side-link">
            <el-icon class="side-ico"><component :is="m.icon" /></el-icon>
            <span class="side-label">{{ m.label }}</span>
            <span v-if="badgeOf(m.path)" class="nav-badge">{{ badgeOf(m.path) }}</span>
          </router-link>
        </nav>

        <p class="side-note">婚礼服务下单、咨询与售后都在这里</p>
      </aside>

      <main class="main"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, List, Star, View, ChatDotRound, Warning, Shop, Setting } from '@element-plus/icons-vue'
import { clearAuth } from '@/store/auth'
import http from '@/api/http'
import { connect, onMessage } from '@/api/chatSocket'

const router = useRouter()

// 导航项与路由一一对应：merchant-apply 的页面不在本次范围内，但入口必须保留
const menus = [
  { path: '/user/orders', label: '我的订单', icon: List },
  { path: '/user/favorites', label: '我的收藏', icon: Star },
  { path: '/user/browse', label: '浏览记录', icon: View },
  { path: '/user/consultations', label: '我的咨询', icon: ChatDotRound },
  { path: '/user/disputes', label: '争议仲裁', icon: Warning },
  { path: '/user/merchant-apply', label: '商家入驻', icon: Shop },
  { path: '/user/profile', label: '个人资料', icon: Setting }
]

// 未读角标：和商家端「待接单」同一套观感，只是这边换成「还有几条没回我」。
// 只挂在咨询这一项上，数字为 0 时不渲染（0 是个安静的事实，不需要占位置）。
const CONSULT_PATH = '/user/consultations'
const consultUnread = ref(0)
const badgeOf = path => (path === CONSULT_PATH ? consultUnread.value : 0)

async function loadUnread() {
  try {
    const list = await http.get('/consultations')
    consultUnread.value = (list || []).reduce((n, s) => n + (Number(s && s.userUnread) || 0), 0)
  } catch {
    // 侧栏角标是锦上添花，拉不到就当没有，不要把整个布局拖成错误态
    consultUnread.value = 0
  }
}

// 15 秒一轮现在只是兜底：连着 WS 时来一帧就刷（300ms 防抖，连着几条只拉一次），
// 停在订单页也能立刻看到「还有几条没回我」，不必等下一轮轮询。
let unreadTimer = null
let offMsg = null
let badgeDebounce = null
const refreshUnreadSoon = () => {
  if (badgeDebounce) clearTimeout(badgeDebounce)
  badgeDebounce = setTimeout(loadUnread, 300)
}
onMounted(() => {
  loadUnread()
  unreadTimer = setInterval(loadUnread, 15000)
  connect()
  offMsg = onMessage(refreshUnreadSoon)
})
onUnmounted(() => {
  if (unreadTimer) { clearInterval(unreadTimer); unreadTimer = null }
  if (badgeDebounce) { clearTimeout(badgeDebounce); badgeDebounce = null }
  if (offMsg) { offMsg(); offMsg = null }
})

function logout() { clearAuth(); router.push('/login') }
</script>

<style scoped>
.uc { min-height: 100vh; background: var(--bg); }

/* ===== 顶栏：与门户同一语言，只把左侧换成本端标题 ===== */
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
  gap: var(--space-4);
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
.nav-sep { flex: none; width: 0; height: var(--space-4); border-left: 1px solid var(--border); }
.nav-crumb { flex: none; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.right { display: flex; align-items: center; gap: var(--space-4); margin-left: auto; }
.nav-link {
  position: relative;
  padding: var(--space-2) 0;
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  text-decoration: none;
  transition: color .2s ease;
}
.nav-link::after {
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
.nav-link:hover { color: var(--rose-500); }
.nav-link:hover::after { transform: scaleX(1); }

/* ===== 版心 + 侧栏 ===== */
.shell {
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-7) var(--gutter) var(--space-9);
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  gap: var(--space-7);
  align-items: start;
}
.side {
  position: sticky;
  top: calc(var(--nav-h) + var(--space-6));
}
.account {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
}
.account-avatar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: var(--space-7);
  height: var(--space-7);
  border-radius: var(--radius-pill);
  background: var(--bg-deep);
  color: var(--rose-500);
}
.account-who { min-width: 0; }
.account-name { margin: 0; color: var(--ink); font-size: var(--fs-sm); font-weight: 600; line-height: var(--lh-ui); }
.account-role { margin: var(--space-1) 0 0; color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

.side-nav { display: flex; flex-direction: column; padding-top: var(--space-4); }
.side-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  height: var(--space-7);
  padding: 0 var(--space-3);
  border-radius: var(--radius-sm);
  color: var(--ink-soft);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  text-decoration: none;
  transition: color .2s ease, background-color .2s ease;
}
.side-link::before {
  content: '';
  position: absolute;
  left: 0;
  top: var(--space-2);
  bottom: var(--space-2);
  width: 2px;
  border-radius: 2px;
  background: var(--rose-500);
  transform: scaleY(0);
  transition: transform .2s ease;
}
.side-link:hover { color: var(--rose-500); }
.side-link.router-link-active {
  color: var(--rose-500);
  font-weight: 600;
}
.side-link.router-link-active::before { transform: scaleY(1); }
.side-ico { flex: none; font-size: var(--fs-base); color: var(--ink-soft); }
.side-link.router-link-active .side-ico { color: var(--rose-500); }
.side-label { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* 未读角标：靠右贴边，与商家端 .nav-badge 同一口径 */
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

.side-note {
  max-width: 20ch;
  margin: var(--space-6) 0 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
  color: var(--ink-soft);
  font-size: var(--fs-xs);
  line-height: var(--lh-body);
}

.main { min-width: 0; }

@media (max-width: 980px) {
  .shell {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--space-5);
    padding-top: var(--space-5);
  }
  .side { position: static; }
  .account, .side-note { display: none; }
  .side-nav {
    flex-direction: row;
    gap: var(--space-1);
    padding: 0 0 var(--space-3);
    border-bottom: 1px solid var(--border);
    overflow-x: auto;
  }
  .side-link { height: var(--space-6); flex: none; }
  .side-link::before { display: none; }
}
@media (max-width: 640px) {
  .nav-crumb, .nav-sep { display: none; }
  .logo { font-size: var(--fs-lg); letter-spacing: .12em; }
  .nav-inner { gap: var(--space-3); }
  .right { gap: var(--space-3); }
}
</style>
