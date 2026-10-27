<template>
  <div class="adm-page">
    <div class="adm-head">
      <p class="adm-sub">公告 <em>{{ announcements.length }}</em> 条 · 资讯 <em>{{ articles.length }}</em> 条</p>
      <div class="adm-actions">
        <el-button size="small" type="primary" @click="openPublish(tab)">
          {{ tab === 'announcement' ? '发布公告' : '发布资讯' }}
        </el-button>
      </div>
    </div>

    <section class="adm-panel">
      <div class="adm-toolbar">
        <div class="adm-seg">
          <button type="button" :class="{ on: tab === 'announcement' }" @click="tab = 'announcement'">平台公告</button>
          <button type="button" :class="{ on: tab === 'article' }" @click="tab = 'article'">婚庆资讯</button>
        </div>
      </div>

      <div v-if="curLoading && !curList.length" class="adm-skel"><i></i><i></i><i></i><i></i></div>

      <div v-else-if="curError" class="adm-state">
        <span class="adm-state-ico">!</span>
        <p class="adm-state-title">内容列表没能加载</p>
        <p class="adm-state-desc">{{ curError }}</p>
        <el-button size="small" @click="reload">重新加载</el-button>
      </div>

      <div v-else-if="!curList.length" class="adm-state">
        <span class="adm-state-ico">◇</span>
        <p class="adm-state-title">{{ tab === 'announcement' ? '还没有发布过公告' : '还没有发布过资讯' }}</p>
        <p class="adm-state-desc">发布后用户端会在{{ tab === 'announcement' ? '公告栏' : '资讯列表' }}看到。</p>
        <el-button size="small" @click="openPublish(tab)">{{ tab === 'announcement' ? '发布公告' : '发布资讯' }}</el-button>
      </div>

      <el-table v-else v-loading="curLoading" :data="curList">
        <el-table-column label="标题" min-width="320">
          <template #default="s">
            <p class="t-title">{{ s.row.title }}</p>
            <p v-if="s.row.content" class="adm-cell-sub adm-two-line t-sub">{{ s.row.content }}</p>
          </template>
        </el-table-column>
        <el-table-column v-if="tab === 'article'" label="来源" width="140">
          <template #default="s">{{ s.row.author || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="tab === 'article'" label="阅读量" width="100">
          <template #default="s"><span class="adm-mono">{{ s.row.viewCount ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="s">
            <span class="adm-tag" :class="s.row.status === 1 ? 'is-ok' : 'is-off'">{{ s.row.status === 1 ? '已发布' : '草稿' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180">
          <template #default="s"><span class="adm-mono">{{ s.row.publishTime || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="right">
          <template #default="s">
            <el-button size="small" :type="s.row.status === 1 ? '' : 'primary'" plain @click="toggle(tab, s.row)">
              {{ s.row.status === 1 ? '下线' : '上线' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="pubVisible" :title="curType === 'announcement' ? '发布公告' : '发布资讯'" width="560px">
      <el-form :model="pubForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="pubForm.title" placeholder="一句话说清这条内容" /></el-form-item>
        <el-form-item v-if="curType === 'article'" label="来源"><el-input v-model="pubForm.author" placeholder="如：平台编辑部" /></el-form-item>
        <el-form-item v-if="curType === 'article'" label="封面图">
          <div class="cover-up">
            <el-upload :show-file-list="false" :http-request="uploadCover" accept=".jpg,.jpeg,.png,.webp,.gif">
              <el-button :loading="coverUploading">{{ pubForm.coverUrl ? '重新选择' : '选择图片' }}</el-button>
            </el-upload>
            <template v-if="pubForm.coverUrl">
              <img :src="pubForm.coverUrl" class="cover-preview" alt="封面预览" />
              <el-button link type="danger" @click="pubForm.coverUrl = ''">移除</el-button>
            </template>
            <span v-else class="cover-hint">jpg / png / webp，不超过 5MB</span>
          </div>
        </el-form-item>
        <el-form-item label="正文"><el-input v-model="pubForm.content" type="textarea" :rows="4" placeholder="正文内容" /></el-form-item>
        <el-form-item label="处理方式">
          <el-radio-group v-model="pubForm.status">
            <el-radio :value="1">直接发布</el-radio>
            <el-radio :value="0">存草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pubVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPublish">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http, { notifyError } from '@/api/http'

const tab = ref('announcement')
const announcements = ref([])
const articles = ref([])
const loadingAnn = ref(false)
const loadingArt = ref(false)
const errorAnn = ref('')
const errorArt = ref('')

const pubVisible = ref(false)
const curType = ref('announcement')
const pubForm = reactive({ title: '', content: '', author: '', coverUrl: '', status: 1 })
const coverUploading = ref(false)

const curList = computed(() => (tab.value === 'announcement' ? announcements.value : articles.value))
const curLoading = computed(() => (tab.value === 'announcement' ? loadingAnn.value : loadingArt.value))
const curError = computed(() => (tab.value === 'announcement' ? errorAnn.value : errorArt.value))

async function loadAnnouncements() {
  loadingAnn.value = true
  errorAnn.value = ''
  try {
    announcements.value = (await http.get('/admin/announcements')) || []
  } catch (e) {
    errorAnn.value = e?.message || '接口 /admin/announcements 请求失败'
  } finally {
    loadingAnn.value = false
  }
}
async function loadArticles() {
  loadingArt.value = true
  errorArt.value = ''
  try {
    articles.value = (await http.get('/admin/articles')) || []
  } catch (e) {
    errorArt.value = e?.message || '接口 /admin/articles 请求失败'
  } finally {
    loadingArt.value = false
  }
}
function reload() {
  tab.value === 'announcement' ? loadAnnouncements() : loadArticles()
}

function openPublish(type) {
  curType.value = type
  Object.assign(pubForm, { title: '', content: '', author: '', coverUrl: '', status: 1 })
  pubVisible.value = true
}

async function submitPublish() {
  const url = curType.value === 'announcement' ? '/admin/announcements' : '/admin/articles'
  try {
    await http.post(url, pubForm)
    ElMessage.success('已保存')
    pubVisible.value = false
    curType.value === 'announcement' ? loadAnnouncements() : loadArticles()
  } catch (e) {
    notifyError(e, '保存失败，请重试')
  }
}

// 走 http 实例而不是裸 axios，才能拿到拦截器的 token 与统一错误提示
async function uploadCover(options) {
  coverUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    const data = await http.post('/files/upload', fd)
    pubForm.coverUrl = data.url
    ElMessage.success('封面已上传')
  } catch {
    // 拦截器已提示失败原因，保留表单内容方便重试
  } finally {
    coverUploading.value = false
  }
}

async function toggle(type, row) {
  const base = type === 'announcement' ? '/admin/announcements' : '/admin/articles'
  try {
    await http.put(`${base}/${row.id}/status`, null, { params: { status: row.status === 1 ? 0 : 1 } })
    ElMessage.success('已切换')
    type === 'announcement' ? loadAnnouncements() : loadArticles()
  } catch (e) {
    notifyError(e, '状态切换失败')
  }
}

onMounted(() => {
  loadAnnouncements()
  loadArticles()
})
</script>

<style scoped>
.t-title {
  margin: 0;
  color: var(--ink);
}
.t-sub {
  max-width: 60ch;
}
.cover-up {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
.cover-preview {
  width: 96px;
  height: 60px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.cover-hint {
  font-size: var(--fs-xs);
  color: var(--ink-soft);
}
</style>
