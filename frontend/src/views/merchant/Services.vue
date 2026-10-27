<template>
  <div class="mch-page">
    <header class="mch-head">
      <p class="mch-sub">
        共 <em>{{ total }}</em> 个服务 · 本页已上架 <em>{{ onShelfCount }}</em> 个
      </p>
      <div class="mch-actions">
        <el-button type="primary" size="small" @click="openPublish">发布服务</el-button>
      </div>
    </header>

    <section v-if="error" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">!</p>
        <p class="mch-state-title">服务列表没有加载出来</p>
        <p class="mch-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重试</el-button>
      </div>
    </section>

    <section v-else-if="loading" class="mch-list">
      <div v-for="i in 3" :key="i" class="sk-row">
        <span class="sk-thumb" />
        <span class="sk-lines"><i /><i /></span>
      </div>
    </section>

    <section v-else-if="!services.length" class="mch-panel">
      <div class="mch-state">
        <p class="mch-state-ico">◇</p>
        <p class="mch-state-title">还没有发布服务</p>
        <p class="mch-state-desc">把你的第一套婚礼方案写成服务，买家才能在缘集上看到并预订。</p>
        <el-button size="small" type="primary" @click="openPublish">发布第一个服务</el-button>
      </div>
    </section>

    <section v-else class="mch-list">
      <article v-for="row in services" :key="row.id" class="mch-row">
        <div class="mch-thumb">
          <ServiceCover :service="row" ratio="1 / 1" />
        </div>
        <div class="mch-row-main">
          <h3 class="mch-row-name">{{ row.name }}</h3>
          <p class="mch-row-sub mch-two-line">{{ row.description || '还没有写简介' }}</p>
          <div class="mch-row-meta">
            <span>{{ catName(row.categoryId) }}</span>
            <span class="mch-tag" :class="auditTone(row.auditStatus)">{{ auditText(row.auditStatus) }}</span>
            <span class="mch-tag" :class="row.shelfStatus === 1 ? 'is-ok' : 'is-off'">
              {{ row.shelfStatus === 1 ? '已上架' : '已下架' }}
            </span>
            <span>成交 {{ row.salesCount || 0 }}</span>
            <span>浏览 {{ row.viewCount || 0 }}</span>
          </div>
        </div>
        <div class="mch-row-side">
          <span class="mch-amount">{{ money(row.price) }}</span>
          <div class="mch-row-acts">
            <button type="button" class="mch-act" @click="openEdit(row)">编辑</button>
            <button type="button" class="mch-act" :class="row.shelfStatus === 1 ? 'is-muted' : 'is-rose'" @click="toggleShelf(row)">
              {{ row.shelfStatus === 1 ? '下架' : '上架' }}
            </button>
            <button type="button" class="mch-act" @click="openSchedules(row)">档期</button>
            <button type="button" class="mch-act" @click="openCase(row)">案例</button>
          </div>
        </div>
      </article>
    </section>

    <div v-if="!loading && !error && total > size" class="mch-pager">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size"
        :current-page="page + 1" @current-change="onPage" />
    </div>

    <!-- 发布 / 编辑 -->
    <el-dialog v-model="formVisible" :title="editingId ? '编辑服务' : '发布服务'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="服务名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="计价单位"><el-input v-model="form.unit" placeholder="每场 / 每套 / 每天" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="内容明细"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="封面">
          <div class="mch-up">
            <div class="mch-up-row">
              <el-upload :show-file-list="false" :http-request="uploadCover" accept=".jpg,.jpeg,.png,.webp,.gif">
                <el-button :loading="coverUploading">{{ form.coverUrl ? '重新选择' : '上传封面' }}</el-button>
              </el-upload>
              <img v-if="form.coverUrl" :src="form.coverUrl" class="mch-cover-preview" alt="封面预览" />
              <el-button v-if="form.coverUrl" link type="danger" @click="form.coverUrl = ''">移除</el-button>
            </div>
            <p class="mch-up-hint">不上传时系统按分类自动配图</p>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 档期:上半看已有档期可删,下半新增 -->
    <el-dialog v-model="schedVisible" title="设置档期" width="520px">
      <div class="dlg-title">已有档期</div>
      <el-table :data="schedules" v-loading="schedLoading" size="small" max-height="240" empty-text="还没有设置档期">
        <el-table-column label="日期" width="170">
          <template #default="s">{{ fmtDate(s.row.scheduleDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="s">
            <span class="mch-tag" :class="s.row.status === 1 ? 'is-ok' : 'is-off'">
              {{ s.row.status === 1 ? '可预约' : '已被预约占用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="s">
            <el-button size="small" @click="removeSchedule(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="dlg-title">新增档期</div>
      <el-date-picker v-model="dates" type="dates" placeholder="选择可预约日期" style="width:100%" value-format="YYYY-MM-DD" />
      <p class="mch-up-hint">已发布的日期一旦被订单占用会自动变为已占用</p>
      <template #footer>
        <el-button @click="schedVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitSchedules">保存</el-button>
      </template>
    </el-dialog>

    <!-- 案例:上半看已有案例可编辑/删除,下半新增(编辑时同一表单改走 PUT) -->
    <el-dialog v-model="caseVisible" :title="caseEditingId ? '编辑案例' : '添加案例'" width="600px" @close="resetCaseForm">
      <div class="dlg-title">已有案例</div>
      <el-table :data="cases" v-loading="caseLoading" size="small" max-height="240" empty-text="还没有上传案例">
        <el-table-column label="图片" width="90">
          <template #default="s">
            <el-image :src="s.row.imageUrl" fit="cover" class="case-thumb" :preview-src-list="[s.row.imageUrl]" preview-teleported />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column label="分类" width="90">
          <template #default="s">{{ s.row.category || '未分类' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="视频" width="70">
          <template #default="s">
            <el-button v-if="s.row.videoUrl" link type="primary" size="small" @click="showVideo(s.row.videoUrl)">有</el-button>
            <span v-else class="mch-none">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="s">
            <el-button size="small" @click="editCase(s.row)">编辑</el-button>
            <el-button size="small" @click="removeCase(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="dlg-title">{{ caseEditingId ? '编辑案例' : '新增案例' }}</div>
      <el-form :model="caseForm" label-width="80px">
        <el-form-item label="图片">
          <div class="mch-up">
            <div class="mch-up-row">
              <el-upload :show-file-list="false" :http-request="uploadCaseImage" accept=".jpg,.jpeg,.png,.webp,.gif">
                <el-button :loading="caseUploading">{{ caseForm.imageUrl ? '重新选择' : '上传图片' }}</el-button>
              </el-upload>
              <img v-if="caseForm.imageUrl" :src="caseForm.imageUrl" class="case-thumb case-form-thumb" alt="案例图片预览" />
              <el-button v-if="caseForm.imageUrl" link type="danger" @click="caseForm.imageUrl = ''">移除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="案例视频">
          <div class="mch-up">
            <div class="mch-up-row">
              <el-upload :show-file-list="false" :http-request="uploadCaseVideo" accept=".mp4,.webm,.mov">
                <el-button :loading="caseVideoUploading">{{ caseForm.videoUrl ? '重新选择' : '上传视频' }}</el-button>
              </el-upload>
              <video v-if="caseForm.videoUrl" :src="caseForm.videoUrl" controls preload="metadata" class="mch-video-preview" />
              <el-button v-if="caseForm.videoUrl" link type="danger" @click="caseForm.videoUrl = ''">移除</el-button>
            </div>
            <p class="mch-up-hint">mp4 / webm / mov,不超过 50MB</p>
          </div>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="caseForm.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="caseForm.category" placeholder="不分类" clearable style="width:100%">
            <el-option label="不分类" value="" />
            <el-option v-for="c in caseCategories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="caseForm.description" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="caseForm.sortOrder" :min="0" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="caseVisible = false">取消</el-button>
        <el-button type="primary" :loading="caseSubmitting" :disabled="caseUploading || caseVideoUploading" @click="submitCase">{{ caseEditingId ? '保存修改' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <!-- 案例视频预览:表格里点「有」打开 -->
    <el-dialog v-model="videoVisible" title="案例视频" width="560px">
      <video v-if="videoSrc" :src="videoSrc" controls preload="metadata" class="mch-video-full" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '@/api/http'
import ServiceCover from '@/components/ServiceCover.vue'

const AUDIT = {
  PENDING: { label: '待审核', tone: 'is-wait' },
  APPROVED: { label: '已通过', tone: 'is-ok' },
  REJECTED: { label: '已驳回', tone: 'is-off' }
}

const services = ref([])
const categories = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(10)
const total = ref(0)

const onShelfCount = computed(() => services.value.filter(s => s.shelfStatus === 1).length)
const catMap = computed(() => {
  const map = {}
  categories.value.forEach(c => { map[c.id] = c.name })
  return map
})
function catName(id) { return catMap.value[id] || '未分类' }
function auditText(s) { return (AUDIT[s] && AUDIT[s].label) || '未知状态' }
function auditTone(s) { return (AUDIT[s] && AUDIT[s].tone) || 'is-off' }

function money(v) {
  const n = Number(v || 0)
  return '¥' + n.toLocaleString('zh-CN', {
    minimumFractionDigits: Number.isInteger(n) ? 0 : 2,
    maximumFractionDigits: 2
  })
}

const formVisible = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const coverUploading = ref(false)
const form = reactive({ name: '', categoryId: null, price: null, unit: '', description: '', content: '', coverUrl: '' })

const schedVisible = ref(false)
const schedId = ref(null)
const dates = ref([])
const schedules = ref([])
const schedLoading = ref(false)

const caseVisible = ref(false)
const caseServiceId = ref(null)
const caseEditingId = ref(null)
const caseForm = reactive({ imageUrl: '', videoUrl: '', title: '', description: '', category: '', sortOrder: 0 })
// 案例分类的可选项,空串代表不分类
const caseCategories = ['场布', '仪式', '宴席', '外景', '其他']
const cases = ref([])
const caseLoading = ref(false)
const caseUploading = ref(false)
const caseVideoUploading = ref(false)
const caseSubmitting = ref(false)
// 表格里点「有」时,把视频地址塞进这个弹窗单独播放
const videoVisible = ref(false)
const videoSrc = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 服务端分页:返回 {list, total, totalPages, page, size}
    const data = await http.get('/merchant/services', { params: { page: page.value, size: size.value } })
    services.value = data.list
    total.value = data.total
  } catch (e) {
    services.value = []
    total.value = 0
    error.value = (e && e.message) || '服务列表加载失败，请稍后重试。'
  } finally {
    loading.value = false
  }
}

function onPage(p) { page.value = p - 1; load() }

function resetForm() {
  Object.assign(form, { name: '', categoryId: null, price: null, unit: '', description: '', content: '', coverUrl: '' })
}

function openPublish() { editingId.value = null; resetForm(); formVisible.value = true }
function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name, categoryId: row.categoryId, price: Number(row.price), unit: row.unit,
    description: row.description, content: row.content, coverUrl: row.coverUrl
  })
  formVisible.value = true
}

async function submitForm() {
  if (!form.name.trim()) return ElMessage.warning('请填写服务名称')
  if (form.categoryId == null) return ElMessage.warning('请选择服务分类')
  if (form.price == null || form.price === '') return ElMessage.warning('请填写服务价格')
  submitting.value = true
  try {
    const body = { ...form }
    if (editingId.value) await http.put(`/merchant/services/${editingId.value}`, body)
    else await http.post('/merchant/services', body)
    ElMessage.success('已保存')
    formVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggleShelf(row) {
  try {
    await http.post(`/merchant/services/${row.id}/shelf`, null, { params: { shelfStatus: row.shelfStatus === 1 ? 0 : 1 } })
    ElMessage.success('已切换上架状态')
    load()
  } catch {
    // 拦截器已提示失败原因
  }
}

// 后端 LocalDate 序列化成 YYYY-MM-DD 字符串,兜底只取前 10 位
function fmtDate(v) { return v ? String(v).slice(0, 10) : '' }

// 拉取当前服务的已有档期(后端已按日期升序)
async function loadSchedules() {
  schedLoading.value = true
  try {
    schedules.value = await http.get(`/merchant/services/${schedId.value}/schedules`)
  } finally {
    schedLoading.value = false
  }
}

function openSchedules(row) {
  schedId.value = row.id
  dates.value = []
  schedules.value = []
  schedVisible.value = true
  loadSchedules()
}

async function submitSchedules() {
  if (!dates.value.length) { ElMessage.warning('请选择日期'); return }
  const picked = dates.value.length
  const added = await http.post(`/merchant/services/${schedId.value}/schedules`, { dates: dates.value })
  // 后端对已存在的日期会跳过,返回的只是真正新增的那几条
  const addedCount = Array.isArray(added) ? added.length : picked
  if (addedCount < picked) {
    ElMessage.warning(`新增 ${addedCount} 条,${picked - addedCount} 条该日期已有档期已跳过`)
  } else {
    ElMessage.success('档期已设置')
  }
  dates.value = []
  loadSchedules()
}

async function removeSchedule(row) {
  try {
    await ElMessageBox.confirm(`确定删除 ${fmtDate(row.scheduleDate)} 的档期吗`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch {
    return
  }
  await http.delete(`/merchant/services/${schedId.value}/schedules/${row.id}`)
  ElMessage.success('已删除')
  loadSchedules()
}

// 拉取当前服务的已有案例(后端已按 sortOrder 升序)
async function loadCases() {
  caseLoading.value = true
  try {
    cases.value = await http.get(`/merchant/services/${caseServiceId.value}/cases`)
  } finally {
    caseLoading.value = false
  }
}

// 清空表单并退出编辑态,回到"新增案例"
function resetCaseForm() {
  caseEditingId.value = null
  Object.assign(caseForm, { imageUrl: '', videoUrl: '', title: '', description: '', category: '', sortOrder: 0 })
}

function openCase(row) {
  caseServiceId.value = row.id
  cases.value = []
  resetCaseForm()
  caseVisible.value = true
  loadCases()
}

// 点「编辑」把该条填进下半表单,提交时走 PUT
function editCase(row) {
  caseEditingId.value = row.id
  Object.assign(caseForm, {
    imageUrl: row.imageUrl || '', videoUrl: row.videoUrl || '', title: row.title || '',
    description: row.description || '', category: row.category || '', sortOrder: row.sortOrder ?? 0
  })
}

async function submitCase() {
  const url = (caseForm.imageUrl || '').trim()
  if (!url) { ElMessage.warning('请上传案例图片'); return }
  caseSubmitting.value = true
  try {
    const body = {
      ...caseForm,
      imageUrl: url,
      // 没传视频就显式给空串,后端按可选字段处理
      videoUrl: (caseForm.videoUrl || '').trim(),
      category: (caseForm.category || '').trim() || null
    }
    if (caseEditingId.value) await http.put(`/merchant/services/${caseServiceId.value}/cases/${caseEditingId.value}`, body)
    else await http.post(`/merchant/services/${caseServiceId.value}/cases`, body)
    ElMessage.success(caseEditingId.value ? '案例已修改' : '案例已添加')
    resetCaseForm()
    loadCases()
  } finally {
    caseSubmitting.value = false
  }
}

async function removeCase(row) {
  try {
    await ElMessageBox.confirm(`确定删除案例「${row.title || '未命名'}」吗`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch {
    return
  }
  await http.delete(`/merchant/services/${caseServiceId.value}/cases/${row.id}`)
  ElMessage.success('已删除')
  // 删掉的正是正在编辑的那条时,顺手回到新增态
  if (caseEditingId.value === row.id) resetCaseForm()
  loadCases()
}

// 走 http 实例而不是裸 axios,才能拿到拦截器的 token 与统一错误提示
async function uploadCaseImage(options) {
  caseUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    caseForm.imageUrl = data.url
  } catch {
    // 拦截器已提示失败原因,保留表单内容方便重试
  } finally {
    caseUploading.value = false
  }
}

// 封面同理:走 http 实例拿 token,form.coverUrl 仍是最终提交值
async function uploadCover(options) {
  coverUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    form.coverUrl = data.url
  } catch {
    // 拦截器已提示失败原因,保留表单内容方便重试
  } finally {
    coverUploading.value = false
  }
}

// 视频体积大,先在前端拦一道,免得白传一遍才被后端拒
async function uploadCaseVideo(options) {
  if (options.file.size > 50 * 1024 * 1024) {
    ElMessage.warning('视频不能超过 50MB')
    return
  }
  caseVideoUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    // 默认 timeout 15s 对几十兆的视频太短,单独放宽
    const data = await http.post('/files/upload', fd, { timeout: 120000 })
    caseForm.videoUrl = data.url
  } catch {
    // 拦截器已提示失败原因,保留表单内容方便重试
  } finally {
    caseVideoUploading.value = false
  }
}

function showVideo(url) {
  videoSrc.value = url
  videoVisible.value = true
}

onMounted(async () => {
  try {
    categories.value = await http.get('/categories')
  } catch {
    categories.value = []
  }
  load()
})
</script>

<style scoped>
.dlg-title {
  margin: 0 0 var(--space-2);
  font-size: var(--fs-xs);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--ink-soft);
}
.dlg-title:not(:first-child) {
  margin-top: var(--space-5);
}
.case-thumb {
  width: var(--space-8);
  height: var(--space-8);
  border-radius: var(--radius-sm);
  display: block;
}
.case-form-thumb {
  margin-top: var(--space-2);
}
.mch-up {
  width: 100%;
}
.mch-up-row {
  display: flex;
  gap: var(--space-2);
  align-items: center;
  flex-wrap: wrap;
}
.mch-up-hint {
  margin: var(--space-2) 0 0;
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
.mch-cover-preview {
  display: block;
  width: 96px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.mch-video-preview {
  display: block;
  height: 96px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.mch-video-full {
  display: block;
  width: 100%;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
}
.mch-none {
  color: var(--ink-soft);
}
.sk-row {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: var(--space-5);
  align-items: center;
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
}
.sk-thumb {
  display: block;
  width: 88px;
  height: 88px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: sk-pulse 1.4s ease-in-out infinite;
}
.sk-lines {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
.sk-lines i {
  display: block;
  height: var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  animation: sk-pulse 1.4s ease-in-out infinite;
}
.sk-lines i:nth-child(2) {
  width: 60%;
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
  .sk-thumb,
  .sk-lines i {
    animation: none;
  }
}
</style>
