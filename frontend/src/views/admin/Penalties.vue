<template>
  <div class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">共 <em>{{ list.length }}</em> 条处罚 · 生效中 <em>{{ activeCount }}</em> 条</p>
      <div class="adm-actions">
        <el-button size="small" type="primary" @click="openCreate">执行处罚</el-button>
      </div>
    </div>

    <section class="adm-panel">
      <div v-if="loading && !list.length" class="adm-skel"><i></i><i></i><i></i><i></i></div>

      <div v-else-if="error" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">处罚记录没能加载</p>
        <p class="adm-state-desc">{{ error }}</p>
        <el-button size="small" @click="load">重新加载</el-button>
      </div>

      <div v-else-if="!list.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">还没有处罚记录</p>
        <p class="adm-state-desc">平台目前没有对任何商家执行过处罚。</p>
        <el-button size="small" @click="openCreate">执行处罚</el-button>
      </div>

      <el-table v-else v-loading="loading" :data="list">
        <el-table-column label="被罚商家" width="180">
          <template #default="s">{{ s.row.merchantName || ('商家#' + s.row.merchantId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="s"><span class="adm-tag">{{ PEN[s.row.penaltyType] || s.row.penaltyType }}</span></template>
        </el-table-column>
        <el-table-column label="原因" min-width="260">
          <template #default="s"><div class="adm-two-line">{{ s.row.reason }}</div></template>
        </el-table-column>
        <el-table-column label="生效时间" width="180">
          <template #default="s"><span class="adm-mono">{{ fmt(s.row.startTime) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="190">
          <template #default="s">
            <span class="adm-tag" :class="s.row.status === 1 ? 'is-doing' : 'is-off'">{{ s.row.status === 1 ? '生效中' : '已解除' }}</span>
            <!-- 解除后这条处罚就只是流水账了,把「谁解的/何时/为什么」就地摊开,免得再去翻审计日志 -->
            <div v-if="s.row.status === 0 && (s.row.releaseTime || s.row.releaseReason || s.row.releaseOperatorId)" class="adm-cell-sub adm-release">
              <div v-if="s.row.releaseTime">{{ fmt(s.row.releaseTime) }}</div>
              <div v-if="s.row.releaseReason">{{ shortReason(s.row.releaseReason) }}</div>
              <div v-if="s.row.releaseOperatorId">操作人#{{ s.row.releaseOperatorId }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="right">
          <template #default="s">
            <el-button v-if="s.row.status === 1" size="small" @click="release(s.row)">解除</el-button>
            <span v-else class="adm-cell-sub">—</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="createVisible" title="执行处罚" width="480px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="被罚商家">
          <el-select v-model="form.merchantId" placeholder="选择商家" class="full">
            <el-option v-for="m in merchants" :key="m.id" :label="m.shopName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="处罚类型">
          <el-radio-group v-model="form.penaltyType">
            <el-radio value="WARNING">警告</el-radio>
            <el-radio value="SERVICE_OFF">服务下架</el-radio>
            <el-radio value="BAN">封禁</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.penaltyType === 'SERVICE_OFF'" label="目标服务 id">
          <el-input-number v-model="form.targetId" :min="1" class="full" />
        </el-form-item>
        <el-form-item label="处罚原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="写清违规事实与依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="danger" @click="submit">执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http, { notifyError } from '@/api/http'

const PEN = { WARNING: '警告', SERVICE_OFF: '服务下架', BAN: '封禁' }

const list = ref([])
const merchants = ref([])
const loading = ref(false)
const error = ref('')

const createVisible = ref(false)
const form = reactive({ merchantId: null, penaltyType: 'WARNING', targetId: null, reason: '' })

const activeCount = computed(() => list.value.filter(p => p.status === 1).length)

async function load() {
  loading.value = true
  error.value = ''
  try {
    // ★ 商家名由后端 list 时直接回填 merchantName（含封禁/未过审），前端不再用公开接口 /merchants/{id} 反查，避免对封禁商家弹 400
    list.value = (await http.get('/admin/penalties')) || []
  } catch (e) {
    error.value = e?.message || '接口 /admin/penalties 请求失败'
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  try {
    merchants.value = (await http.get('/merchants')) || []
  } catch (e) {
    notifyError(e, '商家列表加载失败')
    return
  }
  Object.assign(form, { merchantId: null, penaltyType: 'WARNING', targetId: null, reason: '' })
  createVisible.value = true
}

async function submit() {
  if (!form.merchantId) {
    ElMessage.warning('请选择商家')
    return
  }
  if (form.penaltyType === 'SERVICE_OFF' || form.penaltyType === 'BAN') {
    const label = form.penaltyType === 'BAN' ? '封禁该商家' : '下架该商家的服务'
    try {
      await ElMessageBox.confirm(`确认${label}？该操作会立即对用户端生效。`, '二次确认', {
        confirmButtonText: '确认执行',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return
    }
  }
  try {
    await http.post('/admin/penalties', form)
    ElMessage.success('处罚已执行')
    createVisible.value = false
    load()
  } catch (e) {
    notifyError(e, '执行失败，请重试')
  }
}

// 解除后果按处罚类型分开讲:只有 BAN / SERVICE_OFF 会牵动商家启停与客服上架,WARNING 解除只是归档
const RELEASE_TIP = {
  BAN: '解除后该商家营业状态恢复为启用（若没有其它生效中的封禁）。',
  SERVICE_OFF: '解除后该服务恢复上架（若没有其它生效中的下架处罚）。',
  WARNING: '警告无业务联动，解除只是归档这条记录。'
}

// 后端返回的是 ISO 串（2026-10-31T17:56:05.860318），直接铺在表格里带 T 还带微秒；
// 统一截成"年-月-日 时:分"，与处罚历史页签同一套观感
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

// 单元格太窄,超 20 字截断,完整原因仍留在列表接口里
function shortReason(text) {
  const s = String(text || '')
  return s.length > 20 ? s.slice(0, 20) + '…' : s
}

async function release(row) {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt(
      '解除后不可撤销，会记录操作人与时间。' + (RELEASE_TIP[row.penaltyType] || ''),
      '解除处罚',
      {
        confirmButtonText: '确认解除',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '填写解除原因（必填）',
        // 空原因不该发到后端:后端只会回 400,前端先拦一道
        inputValidator: v => (v && v.trim() ? true : '解除原因不能为空')
      }
    )
    reason = res.value.trim()
  } catch (e) {
    return
  }
  try {
    await http.post('/admin/penalties/' + row.id + '/release', { reason })
    ElMessage.success('已解除')
    load()
  } catch (e) {
    notifyError(e, '解除失败，请重试')
  }
}

onMounted(load)
</script>

<style scoped>
.full {
  width: 100%;
}

.adm-release {
  margin-top: 2px;
  line-height: 1.5;
  word-break: break-all;
}
</style>
