<template>
  <div class="login">
    <div class="glow glow-a"></div>
    <div class="glow glow-b"></div>

    <div class="card">
      <div class="head">
        <router-link to="/home" class="logo">缘集</router-link>
        <p class="tagline">婚礼策划 · 一站式服务平台</p>
      </div>

      <el-form class="form" @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.name" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="onLogin">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="onLogin">登 录</el-button>
      </el-form>

      <div class="tips">
        <span class="tips-label">演示账号（密码 123456）</span>
        <div class="chips">
          <el-tag class="chip" effect="plain" @click="fill('admin')">admin · 管理员</el-tag>
          <el-tag class="chip" effect="plain" type="warning" @click="fill('merchant')">merchant · 商家</el-tag>
          <el-tag class="chip" effect="plain" type="info" @click="fill('user')">user · 用户</el-tag>
        </div>
      </div>

      <div class="reg">还没有账号？<el-link type="primary" underline="never" @click="regVisible = true">立即注册</el-link></div>
      <div class="reg"><el-link type="info" underline="never" @click="resetVisible = true">忘记密码?</el-link></div>

      <router-link to="/home" class="back">← 先随便看看</router-link>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="regVisible" title="注册账号" width="440px" align-center>
      <el-form :model="reg" label-width="70px">
        <el-form-item label="用户名"><el-input v-model="reg.username" placeholder="登录名" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="reg.password" type="password" show-password /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="reg.phone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="onRegister">注册</el-button>
      </template>
    </el-dialog>
    <!-- 密码找回弹窗:本项目无短信服务,身份核对简化为"手机号与注册手机号一致" -->
    <el-dialog v-model="resetVisible" title="找回密码" width="440px" align-center>
      <el-form :model="reset" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="reset.username" placeholder="注册时的登录名" /></el-form-item>
        <el-form-item label="注册手机号"><el-input v-model="reset.phone" placeholder="用于核对身份" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="reset.newPassword" type="password" show-password placeholder="不少于 6 位" /></el-form-item>
      </el-form>
      <div class="hint">系统没有短信服务,凭注册手机号核对身份后可直接重置。</div>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="onReset">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { setAuth } from '@/store/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({ name: '', password: '' })

const regVisible = ref(false)
const regLoading = ref(false)
const reg = reactive({ username: '', password: '', phone: '' })

const resetVisible = ref(false)
const resetLoading = ref(false)
const reset = reactive({ username: '', phone: '', newPassword: '' })

function goHome(role) {
  if (role === 'ADMIN') router.push('/admin')
  else if (role === 'MERCHANT') router.push('/merchant')
  else router.push('/user')
}

async function onLogin() {
  if (!form.name || !form.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try {
    const token = await http.post('/users/login', { name: form.name, password: form.password })
    setAuth(token)
    ElMessage.success('登录成功')
    goHome(localStorage.getItem('role'))
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  if (!reg.username || !reg.password || !reg.phone) { ElMessage.warning('请填写完整信息'); return }
  regLoading.value = true
  try {
    await http.post('/users/register', reg)
    ElMessage.success('注册成功，请登录')
    regVisible.value = false
    form.name = reg.username
    form.password = reg.password
  } finally {
    regLoading.value = false
  }
}

// 三项都必填,先本地拦一次再发请求;失败提示由 http 拦截器统一弹出
async function onReset() {
  if (!reset.username.trim() || !reset.phone.trim() || !reset.newPassword.trim()) {
    ElMessage.warning('请填写用户名、注册手机号与新密码')
    return
  }
  resetLoading.value = true
  try {
    await http.post('/users/reset-password', {
      username: reset.username.trim(),
      phone: reset.phone.trim(),
      newPassword: reset.newPassword
    })
    ElMessage.success('密码已重置,请用新密码登录')
    resetVisible.value = false
    form.name = reset.username.trim()
    form.password = ''
  } finally {
    resetLoading.value = false
  }
}

function fill(name) { form.name = name; form.password = '123456' }
</script>

<style scoped>
.login {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg); position: relative; overflow: hidden;
}
/* 背景装饰光斑 */
.glow { position: absolute; border-radius: 50%; filter: blur(90px); opacity: .5; pointer-events: none; }
.glow-a { width: 520px; height: 520px; background: radial-gradient(circle, #E69ABB, transparent 70%); top: -160px; right: -120px; }
.glow-b { width: 420px; height: 420px; background: radial-gradient(circle, #D9B576, transparent 70%); bottom: -140px; left: -100px; }

.card {
  position: relative; z-index: 1;
  width: 400px; max-width: calc(100vw - 32px);
  background: #fff; border: 1px solid var(--border);
  border-radius: 24px; padding: 42px 38px 34px;
  box-shadow: 0 24px 70px rgba(140, 40, 74, 0.12);
}
.head { text-align: center; margin-bottom: 30px; }
.logo { font-family: var(--font-display); font-size: 34px; font-weight: 700; color: var(--rose-600); letter-spacing: 0.1em; text-decoration: none; }
.tagline { color: var(--ink-soft); font-size: 13px; margin: 8px 0 0; letter-spacing: 0.04em; }

.form :deep(.el-input__wrapper) { border-radius: 12px; }
.submit { width: 100%; height: 48px; font-size: 16px; letter-spacing: 0.28em; border-radius: 12px; margin-top: 4px; }

.tips { margin-top: 28px; padding-top: 22px; border-top: 1px dashed var(--border); }
.tips-label { display: block; font-size: 12px; color: var(--ink-faint); margin-bottom: 10px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chip { cursor: pointer; border-radius: 20px; }
.reg { margin-top: 20px; text-align: center; font-size: 13px; color: var(--ink-soft); }
.hint { font-size: 12px; color: var(--ink-faint); line-height: 1.7; }
.back { display: block; margin-top: 16px; text-align: center; font-size: 13px; color: var(--ink-faint); text-decoration: none; }
.back:hover { color: var(--rose-600); }
</style>
