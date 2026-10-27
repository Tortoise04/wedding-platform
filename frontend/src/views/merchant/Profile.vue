<template>
  <div class="mch-page">
    <header class="mch-head">
      <p class="mch-sub">店铺资料 · 修改后立即对买家和平台生效</p>
    </header>

    <section v-if="err" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">店铺资料没有加载出来</p>
        <p class="mch-state-desc">{{ err }}</p>
        <el-button size="small" @click="load">重试</el-button>
      </div>
    </section>

    <section v-else-if="loading" class="mch-panel pf-panel">
      <div class="sk-lines">
        <i v-for="i in 5" :key="i" />
      </div>
    </section>

    <section v-else class="pf-grid">
      <div class="mch-panel">
        <div class="mch-panel-head">
          <h2 class="mch-panel-title">基础资料</h2>
        </div>
        <div class="mch-panel-body">
          <el-form :model="form" label-width="96px">
            <el-form-item label="店铺名称">
              <el-input :model-value="shop.shopName" disabled />
            </el-form-item>
            <el-form-item label="经营范围">
              <el-input v-model="form.businessScope" maxlength="100" />
            </el-form-item>
            <el-form-item label="联系方式">
              <el-input v-model="form.contactPhone" maxlength="30" />
            </el-form-item>
            <el-form-item label="店铺简介">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" />
            </el-form-item>
            <el-form-item label="店铺Logo">
              <div class="pf-up">
                <div class="pf-up-row">
                  <el-upload :show-file-list="false" :http-request="uploadLogo" accept=".jpg,.jpeg,.png,.webp,.gif">
                    <el-button :loading="logoUploading">{{ form.logo ? '重新选择' : '上传图片' }}</el-button>
                  </el-upload>
                  <img v-if="form.logo" :src="form.logo" class="pf-logo-thumb" alt="店铺 Logo 预览" />
                  <a v-if="form.logo" :href="form.logo" target="_blank" rel="noopener" class="pf-file-link">查看原图</a>
                  <el-button v-if="form.logo" link type="danger" @click="form.logo = ''">移除</el-button>
                </div>
                <p class="pf-up-hint">
                  <template v-if="!form.logo">选填，不上传时按店铺名首字生成；</template>图片 jpg / jpeg / png / webp / gif，不超过 5MB
                </p>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <aside class="mch-panel pf-side">
        <div class="mch-panel-head">
          <h2 class="mch-panel-title">认证信息</h2>
        </div>
        <div class="mch-panel-body">
          <dl class="mch-kv">
            <div class="mch-kv-row">
              <dt>审核状态</dt>
              <dd>
                <span class="mch-tag" :class="audit.tone">{{ audit.label }}</span>
              </dd>
            </div>
            <div class="mch-kv-row">
              <dt>入驻编号</dt>
              <dd><span class="mch-mono">#{{ shop.id || '-' }}</span></dd>
            </div>
            <div class="mch-kv-row">
              <dt>审核时间</dt>
              <dd>{{ fmt(shop.auditTime) || '—' }}</dd>
            </div>
            <div class="mch-kv-row">
              <dt>经营状态</dt>
              <dd>{{ shop.status === 1 ? '正常营业' : '已停业' }}</dd>
            </div>
          </dl>
          <p v-if="shop.auditStatus === 'REJECTED'" class="pf-note">{{ shop.rejectReason || '审核未通过，请修改资料后重新提交。' }}</p>
          <p v-else class="pf-note">审核由平台统一处理，通过后可发布服务并接单。</p>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http, { notifyError } from '@/api/http'

const AUDIT = {
  APPROVED: { label: '已通过', tone: 'is-ok' },
  PENDING: { label: '待审核', tone: 'is-wait' },
  REJECTED: { label: '已驳回', tone: 'is-off' }
}

const shop = ref({})
const saving = ref(false)
const loading = ref(false)
const err = ref('')
const form = reactive({ businessScope: '', contactPhone: '', description: '', logo: '' })

const logoUploading = ref(false)
// 图片单文件上限,与后端 FileController.MAX_SIZE 保持一致
const MAX_SIZE = 5 * 1024 * 1024

const audit = computed(() => AUDIT[shop.value.auditStatus] || { label: '未知', tone: 'is-off' })

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '' }

async function load() {
  loading.value = true
  err.value = ''
  try {
    shop.value = await http.get('/merchant/my')
    form.businessScope = shop.value.businessScope
    form.contactPhone = shop.value.contactPhone
    form.description = shop.value.description
    form.logo = shop.value.logo || ''
  } catch (e) {
    err.value = (e && e.message) || '店铺资料加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

// 走 http 实例而不是裸 axios,才能带上拦截器注入的 token;POST /files/upload 返回 { url: '/uploads/yyyyMM/uuid.ext' }
async function uploadLogo(options) {
  if (options.file.size > MAX_SIZE) { ElMessage.warning('图片不能超过 5MB'); return }
  logoUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    form.logo = data.url
  } catch (e) {
    // 失败不留半个地址,保存时发送的是空串
    form.logo = ''
    notifyError(e, '上传失败，请重新选择图片')
  } finally {
    logoUploading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await http.put('/merchants/profile', form)
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.pf-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
  align-items: start;
}
.pf-note {
  margin: var(--space-4) 0 0;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink-soft);
}
.pf-panel {
  padding: var(--space-5);
}
.pf-up {
  width: 100%;
}
.pf-up-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.pf-up-hint {
  margin: var(--space-2) 0 0;
  font-size: var(--fs-xs);
  line-height: var(--lh-body);
  color: var(--ink-soft);
}
.pf-logo-thumb {
  display: block;
  width: 96px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.pf-file-link {
  font-size: var(--fs-sm);
  color: var(--rose-500);
  text-decoration: underline;
}
.pf-file-link:hover {
  color: var(--rose-600);
}
.sk-lines {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
.sk-lines i {
  display: block;
  height: var(--space-6);
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: sk-pulse 1.4s ease-in-out infinite;
}
@keyframes sk-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.55;
  }
}
@media (prefers-reduced-motion: reduce) {
  .sk-lines i {
    animation: none;
  }
}
@media (max-width: 1080px) {
  .pf-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
