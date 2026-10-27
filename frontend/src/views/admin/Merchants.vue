<template>
  <div class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">本页 <em>{{ list.length }}</em> 家 · 待审核 <em>{{ pendingCount }}</em> 家</p>
    </div>

    <section class="adm-panel">
      <div class="adm-toolbar">
        <div class="adm-field is-grow">
          <span class="adm-label">关键字</span>
          <el-input v-model="keyword" size="small" clearable placeholder="搜索店铺名 / 联系电话" @keyup.enter="load" />
        </div>
        <div class="adm-field">
          <span class="adm-label">审核状态</span>
          <el-select v-model="status" size="small" class="sel" placeholder="全部" @change="load">
            <el-option label="全部" value="" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </div>
        <div class="adm-actions">
          <el-button size="small" type="primary" @click="load">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>

      <div v-if="loading && !list.length" class="adm-skel"><i></i><i></i><i></i><i></i><i></i></div>

      <div v-else-if="error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">商家列表没能加载</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>

      <div v-else-if="!list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">没有匹配的商家</p>
        <p class="adm-state-desc">换个状态或关键字再查一次。</p>
        <el-button size="small" @click="reset">清空筛选</el-button>
      </div>

      <el-table v-else v-loading="loading" :data="list">
        <el-table-column label="店铺" min-width="220">
          <template #default="s">
            <div class="shop">{{ s.row.shopName }}</div>
            <div class="adm-cell-sub adm-two-line">{{ s.row.businessScope || '未填写经营范围' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="contactPhone" label="联系方式" width="150" />
        <el-table-column label="审核状态" width="120">
          <template #default="s">
            <span class="adm-tag" :class="TAG[s.row.auditStatus]">{{ AUDIT[s.row.auditStatus] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="right">
          <template #default="s">
            <el-button v-if="s.row.auditStatus === 'PENDING'" size="small" type="primary" @click="openAudit(s.row)">审核</el-button>
            <span v-else class="adm-cell-sub">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="auditVisible" title="审核入驻申请" width="440px">
      <el-form label-width="88px">
        <el-form-item label="结论">
          <el-radio-group v-model="form.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.approved === false" label="驳回原因">
          <el-input v-model="form.rejectReason" type="textarea" :rows="3" placeholder="写清驳回理由，商家可据此整改" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
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
const keyword = ref('')

const pendingCount = computed(() => list.value.filter(m => m.auditStatus === 'PENDING').length)

const auditVisible = ref(false)
const auditId = ref(null)
const form = reactive({ approved: true, rejectReason: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    if (status.value) params.auditStatus = status.value
    const kw = keyword.value.trim()
    if (kw) params.keyword = kw
    list.value = (await http.get('/admin/merchants', { params })) || []
  } catch (e) {
    error.value = e?.message || '接口 /admin/merchants 请求失败'
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  status.value = ''
  load()
}

function openAudit(row) {
  auditId.value = row.id
  form.approved = true
  form.rejectReason = ''
  auditVisible.value = true
}

async function submit() {
  if (form.approved === false && !form.rejectReason.trim()) return ElMessage.warning('请填写驳回原因')
  try {
    await http.post(`/admin/merchants/${auditId.value}/audit`, form)
    ElMessage.success('已审核')
    auditVisible.value = false
    load()
  } catch (e) {
    notifyError(e, '提交失败，请重试')
  }
}

onMounted(load)
</script>

<style scoped>
.sel {
  width: 128px;
}
.shop {
  color: var(--ink);
}
</style>
