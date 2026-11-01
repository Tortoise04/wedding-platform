<template>
  <div class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">本页 <em>{{ list.length }}</em> 条 · 待审核 <em>{{ pendingCount }}</em> 条</p>
    </div>

    <section class="adm-panel">
      <div class="adm-toolbar">
        <div class="adm-field">
          <span class="adm-label">审核状态</span>
          <el-select v-model="status" size="small" class="sel" placeholder="全部" @change="load">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回 / 已隐藏" value="REJECTED" />
          </el-select>
        </div>
        <div class="adm-field">
          <span class="adm-label">商家</span>
          <el-select v-model="merchantId" size="small" class="sel-merchant" placeholder="全部商家" clearable @change="load">
            <el-option v-for="m in merchants" :key="m.id" :label="m.shopName" :value="m.id" />
          </el-select>
        </div>
      </div>

      <div v-if="loading && !list.length" class="adm-skel"><i></i><i></i><i></i><i></i><i></i></div>

      <div v-else-if="error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">评价列表没能加载</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>

      <div v-else-if="!list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">{{ status === 'PENDING' ? '待审评价已清空' : '没有匹配的评价' }}</p>
        <p class="adm-state-desc">{{ status === 'PENDING' ? '待审队列空着，新的评价会排到这里。' : '换个审核状态看看，或查看全部评价。' }}</p>
        <el-button size="small" @click="status = ''; load()">查看全部评价</el-button>
      </div>

      <el-table v-else v-loading="loading" :data="list">
        <el-table-column label="评分" width="140">
          <template #default="s"><el-rate :model-value="s.row.score" disabled /></template>
        </el-table-column>
        <el-table-column label="商家" width="160">
          <template #default="s">{{ s.row.merchantName || ('商家#' + s.row.merchantId) }}</template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="300">
          <template #default="s">
            <div class="adm-two-line eval-body">{{ s.row.content }}</div>
            <!-- 审核要判断内容与媒体是否匹配：列表里就直接给缩略图与视频入口 -->
            <div v-if="imgList(s.row.images).length" class="eval-shots">
              <el-image
                v-for="(src, i) in imgList(s.row.images)"
                :key="src + i"
                :src="src"
                fit="cover"
                class="eval-thumb"
                :preview-src-list="imgList(s.row.images)"
                :initial-index="i"
                preview-teleported
              />
            </div>
            <video v-if="s.row.videoUrl" :src="s.row.videoUrl" controls preload="metadata" class="eval-video"></video>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="120">
          <template #default="s">
            <div>
              <el-tooltip
                v-if="s.row.auditStatus === 'REJECTED' && s.row.hiddenAt"
                content="通过后被人工隐藏，对买家不可见。"
                placement="top"
              >
                <span class="adm-tag is-off">已隐藏</span>
              </el-tooltip>
              <span v-else class="adm-tag" :class="TAG[s.row.auditStatus]">{{ auditText(s.row) }}</span>
              <div v-if="s.row.auditOperatorId != null" class="adm-cell-sub">审核人#{{ s.row.auditOperatorId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="180">
          <template #default="s"><span class="adm-cell-sub">{{ s.row.rejectReason || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="right">
          <template #default="s">
            <el-button v-if="s.row.auditStatus === 'PENDING'" size="small" type="primary" @click="openAudit(s.row)">审核</el-button>
            <el-button v-else-if="s.row.auditStatus === 'APPROVED'" size="small" plain :loading="busyRow === s.row" @click="hideEvaluation(s.row)">隐藏</el-button>
            <el-button v-else-if="s.row.hiddenAt" size="small" :loading="busyRow === s.row" @click="restoreEvaluation(s.row)">恢复</el-button>
            <span v-else class="adm-cell-sub">已驳回</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="auditVisible" title="审核评价" width="440px">
      <el-form label-width="88px">
        <el-form-item label="结论">
          <el-radio-group v-model="form.approved">
            <el-radio :value="true">通过（公开展示）</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.approved === false" label="驳回原因">
          <el-input v-model="form.rejectReason" type="textarea" :rows="3" placeholder="说明不予公开的原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="submit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { notifyError } from '@/api/http'

// REJECTED 在本页既可能是"审核驳回"也可能是"通过后人工隐藏"，对外都不可见。
// 两者靠 hidden_at 区分：hidden_at 非空 = 人工隐藏，显示「已隐藏」；为空 = 审核驳回，显示「已驳回」。
const AUDIT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const TAG = { PENDING: 'is-wait', APPROVED: 'is-ok', REJECTED: 'is-off' }

// 状态列文案：REJECTED 按 hidden_at 二选一，其余直接取 AUDIT。
function auditText(row) {
  if (row.auditStatus !== 'REJECTED') return AUDIT[row.auditStatus]
  return row.hiddenAt ? '已隐藏' : '已驳回'
}

const list = ref([])
const merchants = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('PENDING')
const merchantId = ref(null)

const pendingCount = computed(() => list.value.filter(e => e.auditStatus === 'PENDING').length)

// images 是逗号分隔的 url 字符串；空串 / null 一律当没有，不渲染空壳
function imgList(v) { return String(v || '').split(',').map(s => s.trim()).filter(Boolean) }

const auditVisible = ref(false)
const auditId = ref(null)
const form = reactive({ approved: true, rejectReason: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    if (status.value) params.auditStatus = status.value
    if (merchantId.value) params.merchantId = merchantId.value
    list.value = (await http.get('/admin/evaluations', { params })) || []
  } catch (e) {
    error.value = e?.message || '接口 /admin/evaluations 请求失败'
  } finally {
    loading.value = false
  }
}

// 商家下拉数据源:列表页首次进来拉一次即可,商家增删不频繁。
// 失败静默吞掉 —— 下拉为空只是少了筛选维度,不该拖垮主列表。
async function loadMerchants() {
  try {
    merchants.value = (await http.get('/admin/merchants')) || []
  } catch (e) {
    // 忽略
  }
}

function openAudit(row) {
  auditId.value = row.id
  form.approved = true
  form.rejectReason = ''
  auditVisible.value = true
}

const submitting = ref(false)

async function submit() {
  if (submitting.value) return
  submitting.value = true
  try {
    await http.post(`/admin/evaluations/${auditId.value}/audit`, form)
    ElMessage.success('已审核')
    auditVisible.value = false
    load()
  } catch (e) {
    notifyError(e, '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 隐藏/恢复共用的行级忙碌锁：存整行对象，连点只发一次请求。
const busyRow = ref(null)

async function runRowOp(row, fn) {
  if (busyRow.value) return
  busyRow.value = row
  try {
    await fn()
  } finally {
    busyRow.value = null
  }
}

// 隐藏一条已通过的评价：原因必填（走 prompt 的 textarea），成功后重算服务评分并刷新列表。
async function hideEvaluation(row) {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt(
      '隐藏后该评价从店铺页与商品页消失，服务评分也会重算。请写明隐藏原因：',
      '隐藏评价',
      { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '隐藏原因必填') }
    )
    reason = String(r.value).trim()
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post(`/admin/evaluations/${row.id}/hide`, { reason })
      ElMessage.success('已隐藏')
      load()
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

// 恢复一条已隐藏的评价：二次确认后回到 APPROVED，服务评分重算。
async function restoreEvaluation(row) {
  try {
    await ElMessageBox.confirm(
      '恢复后该评价重新出现在店铺页与商品页，服务评分也会重算。',
      '恢复评价',
      { type: 'warning', confirmButtonText: '恢复' }
    )
  } catch (e) {
    return // 用户取消
  }
  await runRowOp(row, async () => {
    try {
      await http.post(`/admin/evaluations/${row.id}/restore`)
      ElMessage.success('已恢复')
      load()
    } catch (e) {
      notifyError(e, '操作失败，请重试')
    }
  })
}

onMounted(() => { load(); loadMerchants() })
</script>

<style scoped>
.sel {
  width: 128px;
}
.sel-merchant {
  width: 180px;
}
/* 评价正文保留用户输入的换行；.adm-two-line 仍是全局的两行截断，这里只补换行语义 */
.eval-body {
  white-space: pre-wrap;
}
/* 审核列里的媒体：缩略图小一号，视频收在小窗里，点图可预览原图 */
.eval-shots {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}
.eval-thumb {
  display: block;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  background: var(--bg-deep);
  cursor: zoom-in;
}
.eval-video {
  display: block;
  width: 100%;
  max-width: 200px;
  margin-top: var(--space-2);
  border-radius: var(--radius-sm);
  background: #000;
}
</style>
