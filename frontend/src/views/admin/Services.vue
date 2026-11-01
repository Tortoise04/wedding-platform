<template>
  <div class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">本页 <em>{{ list.length }}</em> 项 · 待审核 <em>{{ pendingCount }}</em> 项</p>
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
        <p class="adm-state-title">服务列表没能加载</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>

      <div v-else-if="!list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">{{ status === 'PENDING' ? '待审服务已清空' : '没有匹配的服务' }}</p>
        <p class="adm-state-desc">{{ status === 'PENDING' ? '待审队列空着，新的提交会排到这里。' : '换个审核状态看看，或查看全部服务。' }}</p>
        <el-button size="small" @click="status = ''; load()">查看全部服务</el-button>
      </div>

      <el-table v-else v-loading="loading" :data="list" max-height="620">
        <el-table-column prop="name" label="服务名称" min-width="240" show-overflow-tooltip />
        <el-table-column label="商家" width="160">
          <template #default="s">{{ s.row.merchantName || ('商家#' + s.row.merchantId) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="120">
          <template #default="s"><span class="adm-amount">¥{{ s.row.price }}</span></template>
        </el-table-column>
        <el-table-column label="审核状态" width="120">
          <template #default="s">
            <div>
              <span class="adm-tag" :class="TAG[s.row.auditStatus]">{{ AUDIT[s.row.auditStatus] }}</span>
              <div v-if="s.row.auditOperatorId != null" class="adm-cell-sub">审核人#{{ s.row.auditOperatorId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="s"><span class="adm-mono">{{ s.row.createdAt }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="right">
          <template #default="s">
            <template v-if="s.row.auditStatus === 'PENDING'">
              <el-button size="small" type="primary" @click="audit(s.row, true)">通过</el-button>
              <el-button size="small" @click="reject(s.row)">驳回</el-button>
            </template>
            <template v-else-if="s.row.auditStatus === 'APPROVED'">
              <el-button v-if="s.row.shelfStatus === 1" size="small" plain @click="offShelf(s.row)">下架</el-button>
              <el-button v-else size="small" type="primary" @click="onShelf(s.row)">上架</el-button>
            </template>
            <span v-else class="adm-cell-sub">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { notifyError } from '@/api/http'

const AUDIT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const TAG = { PENDING: 'is-wait', APPROVED: 'is-ok', REJECTED: 'is-off' }

const list = ref([])
const merchants = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('PENDING')
const merchantId = ref(null)

const pendingCount = computed(() => list.value.filter(s => s.auditStatus === 'PENDING').length)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params = {}
    if (status.value) params.auditStatus = status.value
    if (merchantId.value) params.merchantId = merchantId.value
    list.value = (await http.get('/admin/services', { params })) || []
  } catch (e) {
    error.value = e?.message || '接口 /admin/services 请求失败'
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

async function audit(row, approved) {
  try {
    await http.post(`/admin/services/${row.id}/audit`, null, { params: { approved } })
    ElMessage.success(approved ? '已通过' : '已驳回')
    load()
  } catch (e) {
    notifyError(e, '操作失败，请重试')
  }
}

async function reject(row) {
  try {
    await ElMessageBox.confirm(`确认驳回「${row.name}」？驳回后商家需重新提交。`, '驳回确认', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '再看看',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  audit(row, false)
}

// 下架走处罚:不再直改 shelf_status,而是开一张 SERVICE_OFF 罚单(留痕 + 可解除)。
async function offShelf(row) {
  let reason = ''
  try {
    const r = await ElMessageBox.prompt(
      '下架后买家看不到该服务，会记入处罚记录。请写明下架原因：',
      '下架服务 · ' + row.name,
      { inputType: 'textarea', inputValidator: v => (v && String(v).trim() ? true : '下架原因必填') }
    )
    reason = String(r.value).trim()
  } catch (e) {
    return // 用户取消
  }
  try {
    await http.post('/admin/penalties', { merchantId: row.merchantId, penaltyType: 'SERVICE_OFF', targetId: row.id, reason })
    ElMessage.success('已下架（已记入处罚）')
    load()
  } catch (e) {
    notifyError(e, '操作失败，请重试')
  }
}

// 上架保持直改(恢复上架不属于处罚)。
async function onShelf(row) {
  try {
    await http.post(`/admin/services/${row.id}/shelf`, null, { params: { shelfStatus: 1 } })
    ElMessage.success('已上架')
    load()
  } catch (e) {
    notifyError(e, '操作失败，请重试')
  }
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
</style>
