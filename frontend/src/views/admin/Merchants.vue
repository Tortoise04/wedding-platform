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
        <el-table-column label="营业状态" width="100">
          <template #default="s">
            <span class="adm-tag" :class="s.row.status === 1 ? 'is-ok' : 'is-off'">{{ s.row.status === 1 ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="right">
          <template #default="s">
            <!-- 待审核的行只留「详情」且高亮：审核动作已经搬进抽屉，列表不再有第二个入口 -->
            <el-button size="small" :type="s.row.auditStatus === 'PENDING' ? 'primary' : ''" @click="openDetail(s.row)">详情</el-button>
            <span v-if="s.row.auditStatus === 'REJECTED'" class="adm-cell-sub">已驳回</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <MerchantDetailDrawer v-model="detailVisible" :merchant="detailRow" @updated="onUpdated" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import http from '@/api/http'
import MerchantDetailDrawer from '@/components/MerchantDetailDrawer.vue'

const AUDIT = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const TAG = { PENDING: 'is-wait', APPROVED: 'is-ok', REJECTED: 'is-off' }

const list = ref([])
const loading = ref(false)
const error = ref('')
const status = ref('PENDING')
const keyword = ref('')

const pendingCount = computed(() => list.value.filter(m => m.auditStatus === 'PENDING').length)

const detailVisible = ref(false)
const detailRow = ref(null)

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

function openDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

// 抽屉里审核 / 停用成功后会 emit updated：先刷新列表，再按 id 回填抽屉里的行对象，
// 让抽屉里的状态标签立刻变新；若该店已不再符合当前筛选，抽屉里的数据就是过期的，直接关掉。
async function onUpdated() {
  const keepId = detailRow.value ? detailRow.value.id : null
  await load()
  const fresh = keepId === null ? null : list.value.find(m => m.id === keepId)
  if (fresh) {
    detailRow.value = fresh
    return
  }
  detailVisible.value = false
  detailRow.value = null
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
