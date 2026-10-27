<template>
  <div class="ap-page">
    <header class="ap-head">
      <p class="ap-eyebrow">商家入驻</p>
      <h1 class="ap-title">把店铺开进缘集</h1>
      <p class="ap-lead">
        填写店铺与资质信息，平台会在一个工作日内完成审核。审核通过后即可发布服务、接单并回复买家咨询。
      </p>
    </header>

    <!-- 表单视图:进入页面不请求接口,避免未入驻用户的红色业务错误提示 -->
    <section v-if="mode === 'form'" class="ap-card">
      <div class="ap-card-head">
        <h2 class="ap-card-title">申请资料</h2>
        <el-button link class="ap-link" :loading="loading" @click="loadStatus">已提交过? 查看审核状态</el-button>
      </div>

      <el-form :model="form" label-width="96px" class="ap-form">
        <div class="ap-group">
          <p class="ap-group-title">店铺信息</p>
          <el-form-item label="店铺名称" required><el-input v-model="form.shopName" maxlength="40" placeholder="如 缘集优选婚庆" /></el-form-item>
          <el-form-item label="经营范围" required><el-input v-model="form.businessScope" maxlength="100" placeholder="如 婚庆综合 / 套餐定制" /></el-form-item>
          <el-form-item label="联系方式" required><el-input v-model="form.contactPhone" maxlength="30" placeholder="手机号或固定电话" /></el-form-item>
          <el-form-item label="店铺简介">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" placeholder="选填，介绍你的擅长风格与服务范围" />
          </el-form-item>
          <el-form-item label="店铺Logo">
            <div class="ap-up">
              <div class="ap-up-row">
                <el-upload :show-file-list="false" :http-request="uploadLogo" accept=".jpg,.jpeg,.png,.webp,.gif">
                  <el-button :loading="logoUploading">{{ form.logo ? '重新选择' : '上传图片' }}</el-button>
                </el-upload>
                <img v-if="form.logo" :src="form.logo" class="ap-logo-thumb" alt="店铺 Logo 预览" />
                <a v-if="form.logo" :href="form.logo" target="_blank" rel="noopener" class="ap-file-link">查看原图</a>
                <el-button v-if="form.logo" link type="danger" @click="form.logo = ''">移除</el-button>
              </div>
              <p class="ap-up-hint">
                <template v-if="!form.logo">选填，不上传时按店铺名首字生成；</template>图片 jpg / jpeg / png / webp / gif，不超过 5MB
              </p>
            </div>
          </el-form-item>
        </div>

        <div class="ap-group">
          <p class="ap-group-title">资质材料</p>
          <el-form-item label="营业执照" required>
            <div class="ap-up">
              <div class="ap-up-row">
                <span v-if="form.licenseUrl" class="ap-file-name">{{ licenseFileName }}</span>
                <a v-if="form.licenseUrl" :href="form.licenseUrl" target="_blank" rel="noopener" class="ap-file-link">查看</a>
                <el-upload :show-file-list="false" :http-request="uploadLicense" accept=".pdf,.jpg,.jpeg,.png,.webp,.gif">
                  <el-button :loading="licenseUploading">{{ form.licenseUrl ? '重新选择' : '上传文件' }}</el-button>
                </el-upload>
                <el-button v-if="form.licenseUrl" link type="danger" @click="removeLicense">移除</el-button>
              </div>
              <p class="ap-up-hint">支持 PDF 或图片（jpg / jpeg / png / webp / gif），单份不超过 5MB</p>
            </div>
          </el-form-item>
          <el-form-item label="服务资质" required>
            <div class="ap-up">
              <div class="ap-up-row">
                <span v-if="form.qualificationUrl" class="ap-file-name">{{ qualificationFileName }}</span>
                <a v-if="form.qualificationUrl" :href="form.qualificationUrl" target="_blank" rel="noopener" class="ap-file-link">查看</a>
                <el-upload :show-file-list="false" :http-request="uploadQualification" accept=".pdf,.jpg,.jpeg,.png,.webp,.gif">
                  <el-button :loading="qualificationUploading">{{ form.qualificationUrl ? '重新选择' : '上传文件' }}</el-button>
                </el-upload>
                <el-button v-if="form.qualificationUrl" link type="danger" @click="removeQualification">移除</el-button>
              </div>
              <p class="ap-up-hint">支持 PDF 或图片（jpg / jpeg / png / webp / gif），单份不超过 5MB</p>
            </div>
          </el-form-item>
        </div>

        <div class="ap-foot">
          <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
          <el-button @click="back">返回</el-button>
          <p class="hint">营业执照与服务资质支持 PDF 或图片，单份不超过 5MB。提交后资料进入平台审核,审核期间不能重复提交。</p>
        </div>
      </el-form>
    </section>

    <!-- 状态视图:仅在点击查看后出现 -->
    <section v-else class="ap-card">
      <div class="ap-card-head">
        <h2 class="ap-card-title">审核状态</h2>
        <span class="ap-tag" :class="tone(status.auditStatus)">{{ auditText(status.auditStatus) }}</span>
      </div>

      <dl class="ap-kv">
        <div><dt>店铺名称</dt><dd>{{ status.shopName || '—' }}</dd></div>
        <div><dt>经营范围</dt><dd>{{ status.businessScope || '—' }}</dd></div>
        <div><dt>联系电话</dt><dd>{{ status.contactPhone || '—' }}</dd></div>
        <div>
          <dt>营业执照</dt>
          <dd><a v-if="status.licenseUrl" :href="status.licenseUrl" target="_blank" rel="noopener" class="ap-file-link">查看</a><span v-else>—</span></dd>
        </div>
        <div>
          <dt>服务资质</dt>
          <dd><a v-if="status.qualificationUrl" :href="status.qualificationUrl" target="_blank" rel="noopener" class="ap-file-link">查看</a><span v-else>—</span></dd>
        </div>
        <div v-if="status.description"><dt>店铺简介</dt><dd>{{ status.description }}</dd></div>
        <div v-if="status.logo"><dt>店铺Logo</dt><dd><img :src="status.logo" class="ap-logo-thumb" alt="店铺 Logo" /></dd></div>
      </dl>

      <p class="hint">{{ tip }}</p>
      <el-button class="ap-back" @click="back">返回</el-button>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { notifyError } from '@/api/http'

const router = useRouter()
const mode = ref('form')
const loading = ref(false)
const submitting = ref(false)
const status = ref({})
const form = reactive({ shopName: '', businessScope: '', contactPhone: '', description: '', logo: '', licenseUrl: '', qualificationUrl: '' })

// 服务端会把文件重命名成 UUID,原文件名只在前端记着用于展示,移除时一并清掉
const logoUploading = ref(false)
const licenseUploading = ref(false)
const qualificationUploading = ref(false)
const licenseFileName = ref('')
const qualificationFileName = ref('')
// 图片/文档单文件上限,与后端 FileController.MAX_SIZE 保持一致
const MAX_SIZE = 5 * 1024 * 1024

// 必填项及其中文名,用于逐项提示缺哪一项
const REQUIRED = [['shopName', '店铺名称'], ['businessScope', '经营范围'], ['contactPhone', '联系方式'], ['licenseUrl', '营业执照'], ['qualificationUrl', '服务资质']]

// 后端审核枚举一律转中文,不把英文状态直接暴露给用户
const AUDIT_TEXT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const AUDIT_TONE = { PENDING: 'is-wait', APPROVED: 'is-ok', REJECTED: 'is-off' }
function auditText(v) { return AUDIT_TEXT[v] || '未知' }
function tone(v) { return AUDIT_TONE[v] || 'is-off' }

const tip = computed(() => ({
  PENDING: '资料已提交,等待平台审核',
  APPROVED: '审核已通过,商家端已开通',
  REJECTED: '审核未通过,如需重新申请请联系平台管理员'
}[status.value.auditStatus] || ''))

// 请求体七个字段与 MerchantApplyRequest 逐字对应,userId 与 auditStatus 由服务端决定
function payload() {
  return {
    shopName: form.shopName.trim(),
    businessScope: form.businessScope.trim(),
    contactPhone: form.contactPhone.trim(),
    description: form.description.trim(),
    logo: form.logo.trim(),
    licenseUrl: form.licenseUrl.trim(),
    qualificationUrl: form.qualificationUrl.trim()
  }
}

// 走 http 实例而不是裸 axios,才能带上拦截器注入的 token;POST /files/upload 返回 { url: '/uploads/yyyyMM/uuid.ext' }
async function upload(options, busy, field, nameField) {
  if (options.file.size > MAX_SIZE) { ElMessage.warning('文件不能超过 5MB'); return }
  busy.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    form[field] = data.url
    if (nameField) nameField.value = options.file.name
  } catch (e) {
    // 失败不留半个地址:字段清空后用户重新选,必填校验也能照常拦下
    form[field] = ''
    if (nameField) nameField.value = ''
    notifyError(e, '上传失败，请重新选择文件')
  } finally {
    busy.value = false
  }
}

function uploadLogo(options) { return upload(options, logoUploading, 'logo') }
function uploadLicense(options) { return upload(options, licenseUploading, 'licenseUrl', licenseFileName) }
function uploadQualification(options) { return upload(options, qualificationUploading, 'qualificationUrl', qualificationFileName) }

function removeLicense() { form.licenseUrl = ''; licenseFileName.value = '' }
function removeQualification() { form.qualificationUrl = ''; qualificationFileName.value = '' }

// 纯空白也算未填,返回第一个未通过校验的字段中文名
function missingField() {
  const hit = REQUIRED.find(([key]) => !form[key].trim())
  return hit ? hit[1] : ''
}

function back() { router.push('/home') }

async function loadStatus() {
  loading.value = true
  try {
    status.value = await http.get('/merchant/my')
    mode.value = 'status'
  } catch {
    // 未入驻时服务端返回业务错误,拦截器已提示,留在表单视图
  } finally {
    loading.value = false
  }
}

async function submit() {
  const miss = missingField()
  if (miss) { ElMessage.warning('请填写' + miss); return }
  submitting.value = true
  try {
    await http.post('/merchant/apply', payload())
    afterSubmit()
  } catch {
    // 失败提示由 http 拦截器统一弹出,表单内容保留
  } finally {
    submitting.value = false
  }
}

// 服务端已把审核状态置为 PENDING,页面直接用本地提交的数据展示
function afterSubmit() {
  ElMessage.success('申请已提交')
  status.value = { ...payload(), auditStatus: 'PENDING' }
  mode.value = 'status'
}
</script>

<style scoped>
/* 本页挂在 UserLayout 下,拿不到 MerchantLayout 的 .mch-* 原子类,样式自带一套 */
.ap-page {
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-8) var(--space-5) var(--space-9);
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}
.ap-head {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.ap-eyebrow {
  margin: 0;
  font-size: var(--fs-xs);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--rose-500);
}
.ap-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  font-weight: 600;
  color: var(--ink);
}
.ap-lead {
  margin: 0;
  max-width: 46ch;
  font-size: var(--fs-sm);
  line-height: var(--lh-body);
  color: var(--ink-soft);
}
.ap-card {
  padding: var(--space-6);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}
.ap-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--border);
}
.ap-card-title {
  margin: 0;
  font-size: var(--fs-lg);
  font-weight: 600;
  color: var(--ink);
}
.ap-link {
  height: var(--space-6);
  padding: 0 var(--space-2);
  font-size: var(--fs-sm);
  color: var(--rose-600);
}
.ap-form {
  padding-top: var(--space-4);
}
/* Element Plus 默认间距（18 / 11 / 5px）不在 --space-* 栅格上，本页自带覆盖 */
.ap-form :deep(.el-form-item) {
  margin-bottom: var(--space-4);
}
.ap-form :deep(.el-input__wrapper) {
  padding: 1px var(--space-3);
}
.ap-form :deep(.el-textarea__inner) {
  padding: var(--space-2) var(--space-3);
}
.ap-group + .ap-group {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  border-top: 1px solid var(--border);
}
.ap-group-title {
  margin: 0 0 var(--space-2);
  font-size: var(--fs-xs);
  letter-spacing: 0.08em;
  color: var(--ink-soft);
}
.ap-foot {
  margin-top: var(--space-5);
}
.hint {
  margin: var(--space-3) 0 0;
  font-size: var(--fs-xs);
  line-height: var(--lh-body);
  color: var(--ink-soft);
}
.ap-kv {
  margin: var(--space-4) 0 0;
  display: grid;
  gap: 0;
}
.ap-kv > div {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: var(--space-4);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border);
}
.ap-kv dt {
  font-size: var(--fs-sm);
  color: var(--ink-soft);
}
.ap-kv dd {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--ink);
  word-break: break-all;
}
.ap-up {
  width: 100%;
}
.ap-up-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}
.ap-up-hint {
  margin: var(--space-2) 0 0;
  font-size: var(--fs-xs);
  line-height: var(--lh-body);
  color: var(--ink-soft);
}
.ap-logo-thumb {
  display: block;
  width: 96px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.ap-file-name {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--fs-sm);
  color: var(--ink);
}
.ap-file-link {
  font-size: var(--fs-sm);
  color: var(--rose-500);
  text-decoration: underline;
}
.ap-file-link:hover {
  color: var(--rose-600);
}
.ap-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-pill);
  font-size: var(--fs-xs);
  color: var(--ink);
  background: var(--bg-deep);
}
.ap-tag::before {
  content: '';
  width: var(--space-1);
  height: var(--space-1);
  border-radius: 50%;
  background: var(--ink-faint);
}
.ap-tag.is-ok {
  background: color-mix(in srgb, var(--el-color-success) 12%, var(--surface));
}
.ap-tag.is-ok::before {
  background: var(--el-color-success);
}
.ap-tag.is-doing {
  background: var(--rose-50);
}
.ap-tag.is-doing::before {
  background: var(--rose-500);
}
.ap-tag.is-wait {
  background: color-mix(in srgb, var(--el-color-warning) 12%, var(--surface));
}
.ap-tag.is-wait::before {
  background: var(--el-color-warning);
}
.ap-tag.is-off {
  color: var(--ink-soft);
}
.ap-back {
  margin-top: var(--space-4);
}
</style>
