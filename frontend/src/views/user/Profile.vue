<template>
  <div class="page">
    <header class="masthead">
      <div class="mast-row">
        <div>
          <h1 class="mast-title">个人资料</h1>
          <p class="mast-sub">账号信息用于商家与平台联系你，改完立刻生效。</p>
        </div>
        <el-button v-if="!editing" type="primary" round @click="startEdit">编辑资料</el-button>
      </div>
    </header>

    <!-- ===== 身份卡 ===== -->
    <section class="who">
      <div class="avatar">
        <img v-if="user.avatar" class="avatar-img" :src="user.avatar" :alt="`${displayName}的头像`" />
        <span v-else class="avatar-char">{{ initial }}</span>
      </div>
      <div class="who-text">
        <p class="who-name">{{ displayName }}</p>
        <p class="who-meta">@{{ user.username || '—' }}<span class="dot">·</span>{{ roleLabel }}</p>
      </div>
    </section>

    <!-- ===== 账号信息 ===== -->
    <section class="block">
      <div class="block-head">
        <h2 class="block-title">账号信息</h2>
        <span class="block-hint">用户名与角色不可修改</span>
      </div>

      <div v-if="loading" class="rows">
        <div v-for="i in 4" :key="i" class="sk-line"></div>
      </div>

      <dl v-else-if="!editing" class="rows">
        <div class="row"><dt>用户名</dt><dd>{{ user.username || '—' }}</dd></div>
        <div class="row"><dt>真实姓名</dt><dd>{{ user.realName || '未填写' }}</dd></div>
        <div class="row"><dt>手机号</dt><dd>{{ user.phone || '未填写' }}</dd></div>
        <div class="row"><dt>邮箱</dt><dd>{{ user.email || '未填写' }}</dd></div>
      </dl>

      <el-form v-else :model="form" label-width="88px" class="form">
        <el-form-item label="真实姓名"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存</el-button>
          <el-button @click="cancel">取消</el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- ===== 修改密码 ===== -->
    <section class="block">
      <div class="block-head">
        <h2 class="block-title">修改密码</h2>
        <span class="block-hint">用「用户名 + 注册手机号」核对身份</span>
      </div>
      <el-form :model="pwForm" label-width="88px" class="form">
        <el-form-item label="用户名"><el-input v-model="pwForm.username" disabled /></el-form-item>
        <el-form-item label="注册手机号"><el-input v-model="pwForm.phone" placeholder="与注册时填写的一致" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwForm.newPassword" type="password" show-password placeholder="至少 6 位" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="pwForm.confirm" type="password" show-password placeholder="再输入一次新密码" /></el-form-item>
        <el-form-item>
          <el-button :loading="pwSaving" @click="changePassword">更新密码</el-button>
        </el-form-item>
      </el-form>
    </section>

    <!-- ===== 身份 ===== -->
    <section class="block">
      <h2 class="block-title">身份</h2>
      <dl class="rows">
        <div class="row"><dt>当前身份</dt><dd><span class="pill">{{ roleLabel }}</span></dd></div>
        <div class="row"><dt>说明</dt><dd>身份由平台审核后确定；想接单可先提交「商家入驻」申请。</dd></div>
      </dl>
      <div class="foot">
        <router-link to="/user/merchant-apply" class="act">前往商家入驻</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { getUserId, getRole } from '@/store/auth'

const user = ref({})
const loading = ref(false)
const editing = ref(false)
const saving = ref(false)
const pwSaving = ref(false)
const form = reactive({ realName: '', phone: '', email: '' })
const pwForm = reactive({ username: '', phone: '', newPassword: '', confirm: '' })

const roleLabel = { ADMIN: '管理员', MERCHANT: '商家', USER: '普通用户' }[getRole()] || '普通用户'
const displayName = computed(() => user.value.realName || user.value.username || '缘集用户')
// 没有头像时用暖色底 + 首字兜底，替代原来的渐变色块
const initial = computed(() => displayName.value.charAt(0))

async function load() {
  loading.value = true
  try {
    user.value = await http.get(`/users/${getUserId()}`)
    pwForm.username = user.value.username || ''
    if (!pwForm.phone) pwForm.phone = user.value.phone || ''
  } finally {
    loading.value = false
  }
}

function startEdit() {
  form.realName = user.value.realName || ''
  form.phone = user.value.phone || ''
  form.email = user.value.email || ''
  editing.value = true
}

function cancel() { editing.value = false }

async function save() {
  saving.value = true
  try {
    await http.put('/users/profile', { ...form })
    await load()
    editing.value = false
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

// 后端没有"登录态改密"接口，走已有的密码找回接口（用户名 + 注册手机号核对）
async function changePassword() {
  if (!pwForm.phone.trim()) { ElMessage.warning('请填写注册手机号'); return }
  if (!pwForm.newPassword || pwForm.newPassword.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (pwForm.newPassword !== pwForm.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  pwSaving.value = true
  try {
    await http.post('/users/reset-password', {
      username: pwForm.username,
      phone: pwForm.phone,
      newPassword: pwForm.newPassword
    })
    pwForm.newPassword = ''
    pwForm.confirm = ''
    ElMessage.success('密码已更新')
  } finally {
    pwSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page {
  padding-bottom: var(--space-7);
  /* Element 占位符默认 #9A868D（3.40:1），按 AA 提到 ink-soft */
  --el-text-color-placeholder: var(--ink-soft);
}

/* Element 控件内部留白落到 --space 令牌，去掉 5/11/18px 这类一次性数值 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) { padding: var(--space-1) var(--space-3); }
:deep(.el-textarea__inner) { padding: var(--space-3); }
:deep(.el-select__suffix),
:deep(.el-input__suffix) { gap: var(--space-1); padding-left: var(--space-1); }
:deep(.el-form-item) { margin-bottom: var(--space-4); }
:deep(.el-select__wrapper) { gap: var(--space-1); }


.masthead { padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.mast-row { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: var(--space-4); }
.mast-title { font-size: var(--fs-xl); line-height: var(--lh-ui); font-weight: 600; letter-spacing: .02em; }
.mast-sub { max-width: 46ch; margin: var(--space-3) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }

/* ===== 身份卡 ===== */
.who { display: flex; align-items: center; gap: var(--space-5); margin-top: var(--space-6); }
.avatar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-pill);
  overflow: hidden;
  background: var(--bg-deep);
  border: 1px solid var(--rose-100);
}
.avatar-img { display: block; width: 100%; height: 100%; object-fit: cover; }
.avatar-char {
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  line-height: 1;
  font-weight: 600;
  color: var(--rose-500);
}
.who-text { min-width: 0; }
.who-name { margin: 0; font-family: var(--font-display); font-size: var(--fs-lg); line-height: var(--lh-ui); font-weight: 600; }
.who-meta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); margin: var(--space-2) 0 0; color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-ui); }
.dot { color: var(--ink-soft); }

/* ===== 分组卡片 ===== */
.block {
  margin-top: var(--space-6);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}
.block-head { display: flex; flex-wrap: wrap; align-items: baseline; justify-content: space-between; gap: var(--space-2); }
.block-title { font-size: var(--fs-lg); line-height: var(--lh-ui); font-weight: 600; }
.block-hint { color: var(--ink-soft); font-size: var(--fs-xs); line-height: var(--lh-ui); }

.rows { margin: var(--space-4) 0 0; display: flex; flex-direction: column; gap: var(--space-3); }
.row { display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: var(--space-4); padding-bottom: var(--space-3); border-bottom: 1px solid var(--border); }
.row:last-child { padding-bottom: 0; border-bottom: 0; }
.row dt { color: var(--ink-soft); font-size: var(--fs-sm); line-height: var(--lh-body); }
.row dd { margin: 0; color: var(--ink); font-size: var(--fs-sm); line-height: var(--lh-body); word-break: break-word; }

.pill {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  font-weight: 600;
  letter-spacing: .04em;
  color: var(--rose-500);
}
.pill::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.form { max-width: 460px; margin-top: var(--space-4); }
.foot { display: flex; justify-content: flex-end; margin-top: var(--space-4); }
.act {
  display: inline-flex;
  align-items: center;
  height: var(--space-6);
  padding: 0 var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--ink);
  font-size: var(--fs-sm);
  line-height: var(--lh-ui);
  text-decoration: none;
  transition: border-color .2s ease, color .2s ease;
}
.act:hover { border-color: var(--rose-500); color: var(--rose-500); }

.sk-line { height: var(--space-4); border-radius: var(--radius-sm); background: var(--bg-deep); animation: sk-pulse 1.6s ease-in-out infinite; }
@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .55; }
}
@media (prefers-reduced-motion: reduce) { .sk-line { animation: none; } }

@media (max-width: 640px) {
  .row { grid-template-columns: minmax(0, 1fr); gap: var(--space-1); }
  .who { gap: var(--space-4); }
  .avatar { width: var(--space-7); height: var(--space-7); }
}
</style>
