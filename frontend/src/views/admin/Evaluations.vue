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
            <el-option label="已驳回" value="REJECTED" />
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
            <span class="adm-tag" :class="TAG[s.row.auditStatus]">{{ AUDIT[s.row.auditStatus] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="驳回原因" min-width="180">
          <template #default="s"><span class="adm-cell-sub">{{ s.row.rejectReason || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="right">
          <template #default="s">
            <el-button v-if="s.row.auditStatus === 'PENDING'" size="small" type="primary" @click="openAudit(s.row)">审核</el-button>
            <span v-else class="adm-cell-sub">已处理</span>
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
import { ElMessage } from 'element-plus'
import http, { notifyError } from '@/api/http'

const AUDIT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const TAG = { PENDING: 'is-wait', APPROVED: 'is-ok', REJECTED: 'is-off' }

const list = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('PENDING')

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
    list.value = (await http.get('/admin/evaluations', { params: status.value ? { auditStatus: status.value } : {} })) || []
  } catch (e) {
    error.value = e?.message || '接口 /admin/evaluations 请求失败'
  } finally {
    loading.value = false
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

onMounted(load)
</script>

<style scoped>
.sel {
  width: 128px;
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
