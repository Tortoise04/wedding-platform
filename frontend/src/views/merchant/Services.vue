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
            <button type="button" class="mch-act" @click="openSchedules(row)">时间场地</button>
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
        <!-- 可约日期(可选):与「时间场地」弹窗同一套生成规则(起止日期 × 星期 × 时段 × 场地),
             留空就只保存服务;填了就等服务落库拿到 id 再一键生成档期 -->
        <el-collapse v-model="publishGenOpen" class="pub-gen">
          <el-collapse-item name="dates" title="可约日期（可选，也可稍后在「时间场地」里补）">
            <div class="pub-row">
              <span class="pub-lb">起止日期</span>
              <el-date-picker v-model="publishGen.range" type="daterange" value-format="YYYY-MM-DD" unlink-panels
                start-placeholder="开始日期" end-placeholder="结束日期" style="width:100%" />
            </div>
            <div class="pub-row">
              <span class="pub-lb">星期规则</span>
              <div class="pub-fill">
                <el-checkbox v-model="pubAllWeek">全选</el-checkbox>
                <el-checkbox v-model="pubWeekendOnly">仅周末</el-checkbox>
                <el-checkbox-group v-model="publishGen.weekdays" class="sched-week">
                  <el-checkbox v-for="w in WEEK" :key="w.value" :value="w.value">{{ w.label }}</el-checkbox>
                </el-checkbox-group>
              </div>
            </div>
            <div class="pub-row">
              <span class="pub-lb">可约时段</span>
              <el-checkbox-group v-model="publishGen.slots">
                <el-checkbox v-for="s in slotOptions" :key="s" :value="s">{{ s }}</el-checkbox>
              </el-checkbox-group>
            </div>
            <div class="pub-row">
              <span class="pub-lb">场地</span>
              <el-input v-model="publishGen.venue" placeholder="选填:宴会厅 A / 户外草坪" maxlength="100" show-word-limit clearable />
            </div>
            <p class="sched-preview">{{ publishPreviewText }}</p>
            <p class="mch-up-hint">留空只保存服务；填写后保存时一并写入档期，已存在的自动跳过</p>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 时间与场地占用:上半看已有占用(行内编辑 / 勾选批量删除),下半按规则批量生成(起止日期 × 星期 × 时段) -->
    <el-dialog v-model="schedVisible" title="时间与场地占用" width="640px">
      <div class="dlg-title">已有占用</div>
      <div class="sched-bar">
        <span class="mch-up-hint">已勾选 {{ schedSelected.length }} 条</span>
        <el-button size="small" type="danger" plain :disabled="!schedSelected.length" @click="removeSelectedSchedules">
          批量删除
        </el-button>
      </div>
      <el-table :data="schedules" v-loading="schedLoading" size="small" max-height="240" row-key="id"
        empty-text="还没有设置占用" @selection-change="onSchedSelect">
        <el-table-column type="selection" width="42" :selectable="schedSelectable" />
        <el-table-column label="日期" width="130">
          <template #default="s">{{ fmtDate(s.row.scheduleDate) }}</template>
        </el-table-column>
        <el-table-column label="时段" width="86">
          <template #default="s">{{ s.row.slot || '整天' }}</template>
        </el-table-column>
        <el-table-column prop="venue" label="场地" show-overflow-tooltip>
          <template #default="s">{{ s.row.venue || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="s">
            <span class="mch-tag" :class="s.row.status === 1 ? 'is-ok' : 'is-off'">
              {{ s.row.status === 1 ? '可预约' : '已被订单占用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="128">
          <template #default="s">
            <el-button size="small" :disabled="s.row.status !== 1"
              :title="s.row.status !== 1 ? '已被订单占用' : '修改时段 / 场地'" @click="openSchedEdit(s.row)">编辑</el-button>
            <el-button size="small" :disabled="s.row.status !== 1"
              :title="s.row.status !== 1 ? '已被订单占用' : '删除这条占用'" @click="removeSchedule(s.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="dlg-title">批量生成占用</div>
      <el-form :model="generator" label-width="86px" size="small">
        <el-form-item label="起止日期">
          <el-date-picker v-model="generator.range" type="daterange" value-format="YYYY-MM-DD" unlink-panels
            start-placeholder="开始日期" end-placeholder="结束日期" style="width:100%" />
        </el-form-item>
        <el-form-item label="星期规则">
          <el-checkbox v-model="allWeek">全选</el-checkbox>
          <el-checkbox v-model="weekendOnly">仅周末</el-checkbox>
          <el-checkbox-group v-model="generator.weekdays" class="sched-week">
            <el-checkbox v-for="w in WEEK" :key="w.value" :value="w.value">{{ w.label }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="可约时段">
          <el-checkbox-group v-model="generator.slots">
            <el-checkbox v-for="s in slotOptions" :key="s" :value="s">{{ s }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="场地">
          <el-input v-model="generator.venue" placeholder="选填:宴会厅 A / 户外草坪" maxlength="100" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <p class="sched-preview">{{ previewText }}</p>
      <p class="mch-up-hint">同一服务同一天可以分别占用「上午」和「下午」；已被订单占用的日期不能改也不能删</p>
      <template #footer>
        <el-button @click="schedVisible = false">关闭</el-button>
        <el-button type="primary" :loading="schedSubmitting" @click="submitSchedules">保存</el-button>
      </template>
    </el-dialog>

    <!-- 行内编辑:只改时段与场地(日期不可改,改日期等于换一条档期) -->
    <el-dialog v-model="schedEditVisible" title="修改占用" width="400px">
      <el-form :model="schedEditing" label-width="70px" size="small">
        <el-form-item label="日期">
          <span>{{ schedEditing.date }}</span>
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="schedEditing.slot" style="width:100%">
            <el-option v-for="s in slotOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="场地">
          <el-input v-model="schedEditing.venue" placeholder="选填" maxlength="100" show-word-limit clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schedEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="schedEditSubmitting" @click="submitSchedEdit">保存</el-button>
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
        <el-form-item label="说明">
          <el-input
            v-model="caseForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="这场案例怎么布置、流程怎么走、用了什么花材/器乐/灯光，都可以写清楚，最多 500 字"
          />
        </el-form-item>
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
const slotOptions = ['整天', '上午', '下午', '晚间']
// getDay() 的取值:0=周日 … 6=周六,按周一到周日排给用户看
const WEEK = [
  { value: 1, label: '周一' }, { value: 2, label: '周二' }, { value: 3, label: '周三' },
  { value: 4, label: '周四' }, { value: 5, label: '周五' }, { value: 6, label: '周六' },
  { value: 0, label: '周日' }
]
// 批量生成器:起止日期 + 星期规则 + 可约时段 + 场地
// 生成规则只有一份(generatedDatesOf / allWeekOf / weekendOnlyOf),"时间场地"弹窗与发布弹窗各持一份状态
function emptyGen() { return { range: [], weekdays: [0, 1, 2, 3, 4, 5, 6], slots: ['整天'], venue: '' } }
const generator = reactive(emptyGen())
// 发布 / 编辑弹窗内嵌的同一套生成器:日期区留空即不写档期
const publishGen = reactive(emptyGen())
const publishGenOpen = ref([])
const schedSubmitting = ref(false)
const schedules = ref([])
const schedLoading = ref(false)
const schedSelected = ref([])
// 行内编辑:只改时段 / 场地
const schedEditVisible = ref(false)
const schedEditSubmitting = ref(false)
const schedEditing = reactive({ id: null, date: '', slot: '整天', venue: '' })

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

function openPublish() {
  editingId.value = null
  resetForm()
  Object.assign(publishGen, emptyGen())
  publishGenOpen.value = []
  formVisible.value = true
}
function openEdit(row) {
  editingId.value = row.id
  Object.assign(publishGen, emptyGen())
  publishGenOpen.value = []
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
    const isEdit = !!editingId.value
    let serviceId = editingId.value
    if (isEdit) {
      await http.put(`/merchant/services/${serviceId}`, body)
    } else {
      // 发布接口回传落库后的服务体(带 id),档期要挂在这个 id 上
      const created = await http.post('/merchant/services', body)
      serviceId = created && created.id
    }
    await writeSchedulesAfterSave(serviceId, isEdit)
    formVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

// 服务先落库,再写可约日期:档期是可选动作,写失败不能回滚服务,所以整段自己 catch,只强调「服务已保存」。
async function writeSchedulesAfterSave(serviceId, isEdit) {
  const days = generatedDatesOf(publishGen)
  const savedTip = isEdit ? '已保存服务' : '服务已提交审核'
  // ① 日期区留空:一个档期请求都不发
  if (!days.length) {
    ElMessage.success(`${savedTip}，可稍后在「时间场地」补齐可约日期`)
    return
  }
  // ② 填了日期却没选时段:同样不写,把原因说清楚
  if (!publishGen.slots.length) {
    ElMessage.warning('服务已保存，但档期未写入：未选择可约时段')
    return
  }
  if (!serviceId) {
    ElMessage.warning('服务已保存，但档期未写入：没有拿到服务 id')
    return
  }
  try {
    // 后端按 dates × slots 落库,已存在的跳过并回传 created / skipped 明细
    const data = await http.post(`/merchant/services/${serviceId}/schedules`, {
      dates: days,
      slots: [...publishGen.slots],
      venue: publishGen.venue ? publishGen.venue : null
    })
    const created = (data && data.createdCount) || 0
    const skipped = (data && data.skippedCount) || 0
    ElMessage.success(`服务已保存，可约日期新增 ${created} 条` + (skipped ? `，跳过 ${skipped} 条(已存在)` : ''))
  } catch (e) {
    // 服务已经在库里了:不回滚、不重抛,只把失败原因说清楚(拦截器多半已弹过一次原因)
    ElMessage.warning(`服务已保存，但档期未写入：${(e && e.message) || '请求失败'}`)
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
  Object.assign(generator, { range: [], weekdays: [0, 1, 2, 3, 4, 5, 6], slots: ['整天'], venue: '' })
  schedules.value = []
  schedSelected.value = []
  schedVisible.value = true
  loadSchedules()
}

// 「全选 / 仅周末」做成 weekdays 的两种快捷写法:勾了就等于选中对应几天,取消就清空
// 写成 allWeekOf(gen) 这类纯函数,发布弹窗与「时间场地」弹窗共用一份判定,各自只包一层 computed
function allWeekOf(gen) { return gen.weekdays.length === 7 }
function weekendOnlyOf(gen) { return gen.weekdays.length === 2 && gen.weekdays.includes(0) && gen.weekdays.includes(6) }
function setWeekOf(gen, v) { gen.weekdays = v ? [0, 1, 2, 3, 4, 5, 6] : [] }
function setWeekendOf(gen, v) { gen.weekdays = v ? [0, 6] : [] }

const allWeek = computed({ get: () => allWeekOf(generator), set: v => setWeekOf(generator, v) })
const weekendOnly = computed({ get: () => weekendOnlyOf(generator), set: v => setWeekendOf(generator, v) })
const pubAllWeek = computed({ get: () => allWeekOf(publishGen), set: v => setWeekOf(publishGen, v) })
const pubWeekendOnly = computed({ get: () => weekendOnlyOf(publishGen), set: v => setWeekendOf(publishGen, v) })

// 起止日期 + 星期规则 → 实际要写的日期列表(YYYY-MM-DD,后端按 LocalDate 收)
function generatedDates() { return generatedDatesOf(generator) }

function generatedDatesOf(gen) {
  const [start, end] = gen.range || []
  if (!start || !end) return []
  const from = new Date(start + 'T00:00:00')
  const to = new Date(end + 'T00:00:00')
  if (Number.isNaN(from.getTime()) || Number.isNaN(to.getTime()) || from > to) return []
  if (!gen.weekdays.length) return []
  const picked = new Set(gen.weekdays)
  const out = []
  for (const d = new Date(from); d <= to; d.setDate(d.getDate() + 1)) {
    if (picked.has(d.getDay())) out.push(toYmd(d))
  }
  return out
}

function toYmd(d) {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

// 提交前的估算:拿本地已有档期去重,告诉用户"将新增 N 行、跳过 M 行"
const previewText = computed(() => {
  const days = generatedDates()
  const days0 = days.length
  if (!days0) return '先选起止日期与星期规则,这里会显示将生成多少行'
  if (!generator.slots.length) return `共 ${days0} 天 · 请至少选择一个可约时段`
  const existing = new Set(schedules.value.map(s => `${fmtDate(s.scheduleDate)}|${s.slot || '整天'}`))
  let skip = 0
  days.forEach(d => generator.slots.forEach(sl => { if (existing.has(`${d}|${sl}`)) skip += 1 }))
  const total = days0 * generator.slots.length
  return `共 ${days0} 天 · ${generator.slots.length} 个时段 · 将新增 ${total - skip} 行、跳过 ${skip} 行(已存在)`
})

// 发布弹窗的预览:不为这个可选区多查一次已有档期,只报将提交多少行(重复的由后端跳过)
const publishPreviewText = computed(() => {
  const days = generatedDatesOf(publishGen)
  if (!days.length) return '留空则只保存服务；填了起止日期与星期规则，这里会显示将生成多少行'
  if (!publishGen.slots.length) return `共 ${days.length} 天 · 请至少选择一个可约时段`
  return `共 ${days.length} 天 · ${publishGen.slots.length} 个时段 · 将提交 ${days.length * publishGen.slots.length} 行（已存在的自动跳过）`
})

function schedSelectable(row) { return row.status === 1 }
function onSchedSelect(rows) { schedSelected.value = rows }

async function submitSchedules() {
  const days = generatedDates()
  if (!days.length) { ElMessage.warning('请选择起止日期与星期规则'); return }
  if (!generator.slots.length) { ElMessage.warning('请至少选择一个可约时段'); return }
  if (days.length * generator.slots.length > 400) {
    ElMessage.warning('一次最多生成 400 条占用,请缩小日期范围')
    return
  }
  schedSubmitting.value = true
  try {
    // 后端按 dates × slots 落库,已存在的跳过并回传 created / skipped 明细
    const data = await http.post(`/merchant/services/${schedId.value}/schedules`, {
      dates: days,
      slots: generator.slots,
      venue: generator.venue ? generator.venue : null
    })
    const created = (data && data.createdCount) || 0
    const skipped = (data && data.skippedCount) || 0
    const msg = `新增 ${created} 条` + (skipped ? `，跳过 ${skipped} 条(已存在)` : '')
    if (skipped) ElMessage.warning(msg)
    else ElMessage.success(msg)
    generator.venue = ''
    loadSchedules()
  } finally {
    schedSubmitting.value = false
  }
}

async function removeSchedule(row) {
  if (row.status !== 1) { ElMessage.warning('该日期已被订单占用，不能修改或删除'); return }
  try {
    await ElMessageBox.confirm(`确定删除 ${fmtDate(row.scheduleDate)} ${row.slot || '整天'} 的占用吗`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch {
    return
  }
  await http.delete(`/merchant/services/${schedId.value}/schedules/${row.id}`)
  ElMessage.success('已删除')
  loadSchedules()
}

// 勾选后批量删除:一条被订单占用就整批拒绝(后端同事务),所以这里先挡掉不该勾的行
async function removeSelectedSchedules() {
  const rows = schedSelected.value
  if (!rows.length) return
  if (rows.some(r => r.status !== 1)) { ElMessage.warning('其中包含已被订单占用的日期，不能删除'); return }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${rows.length} 条占用吗`, '提示', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
  } catch {
    return
  }
  const removed = await http.delete(`/merchant/services/${schedId.value}/schedules`, { data: { ids: rows.map(r => r.id) } })
  ElMessage.success(`已删除 ${typeof removed === 'number' ? removed : rows.length} 条`)
  schedSelected.value = []
  loadSchedules()
}

function openSchedEdit(row) {
  if (row.status !== 1) { ElMessage.warning('该日期已被订单占用，不能修改或删除'); return }
  Object.assign(schedEditing, { id: row.id, date: fmtDate(row.scheduleDate), slot: row.slot || '整天', venue: row.venue || '' })
  schedEditVisible.value = true
}

async function submitSchedEdit() {
  schedEditSubmitting.value = true
  try {
    await http.put(`/merchant/services/${schedId.value}/schedules/${schedEditing.id}`, {
      slot: schedEditing.slot,
      venue: schedEditing.venue ? schedEditing.venue : null
    })
    ElMessage.success('已修改')
    schedEditVisible.value = false
    loadSchedules()
  } finally {
    schedEditSubmitting.value = false
  }
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
/* 已有占用上方的工具栏:左边已勾选条数,右边批量删除 */
.sched-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
/* 星期勾选与「全选 / 仅周末」同一行,窄屏自动换行 */
.sched-week {
  display: inline-flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-left: var(--space-2);
}
/* 发布弹窗里的可约日期折叠区:与「时间场地」弹窗同结构,只是没有外层 el-form 的 label 栅格 */
.pub-gen {
  margin-bottom: var(--space-3);
}
.pub-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  font-size: var(--fs-sm);
}
.pub-lb {
  flex: 0 0 68px;
  color: var(--ink-soft);
}
.pub-fill {
  flex: 1 1 auto;
  min-width: 0;
}
/* 提交前的预览行:将新增 N 行、跳过 M 行 */
.sched-preview {
  margin: var(--space-2) 0 0;
  padding: var(--space-2) var(--space-3);
  font-size: var(--fs-xs);
  color: var(--ink);
  background: var(--bg-deep);
  border-radius: var(--radius-sm);
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
