<template>
  <div class="article-history-page">
    <div class="history-card">
      <!-- 页头 -->
      <div class="history-head">
        <div class="head-title">
          <h2>创作历史</h2>
          <span class="head-sub">查看以往 AI 创作的文章</span>
        </div>
        <a-button type="primary" class="head-action" @click="goCreate">
          <template #icon><PlusOutlined /></template>
          新建文章
        </a-button>
      </div>

      <!-- 状态筛选 -->
      <div class="filter-bar">
        <a-radio-group v-model:value="statusFilter" button-style="solid" @change="loadList(1)">
          <a-radio-button value="">全部</a-radio-button>
          <a-radio-button value="PENDING">等待</a-radio-button>
          <a-radio-button value="PROCESSING">处理中</a-radio-button>
          <a-radio-button value="COMPLETED">已完成</a-radio-button>
          <a-radio-button value="FAILED">失败</a-radio-button>
        </a-radio-group>
      </div>

      <!-- 表格 -->
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="onTableChange"
        class="history-table"
      >
        <template #bodyCell="{ column, record }">
          <!-- 选题 -->
          <template v-if="column.key === 'topic'">
            <span class="cell-topic" :title="record.topic">{{ record.topic || '—' }}</span>
          </template>

          <!-- 标题 -->
          <template v-else-if="column.key === 'title'">
            <span class="cell-title" :title="record.mainTitle">{{ record.mainTitle || '—' }}</span>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>

          <!-- 创建时间 -->
          <template v-else-if="column.key === 'createTime'">
            <span class="cell-time">{{ formatTime(record.createTime) }}</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" class="cell-action" @click="viewArticle(record)">
                <template #icon><EyeOutlined /></template>
                查看文章
              </a-button>
              <a-button
                type="link"
                size="small"
                class="cell-action"
                :disabled="!hasContent(record)"
                @click="exportArticle(record)"
              >
                <template #icon><DownloadOutlined /></template>
                导出文章
              </a-button>
              <a-popconfirm title="确定删除这篇文章吗？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger class="cell-action">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>

        <!-- 空状态 -->
        <template #emptyText>
          <div class="table-empty">
            <a-empty description="暂无创作记录">
              <a-button type="primary" size="small" @click="goCreate">去创作第一篇</a-button>
            </a-empty>
          </div>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownloadOutlined,
  EyeOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import {
  deleteArticle,
  listArticle,
} from '@/api/aicomposer/articleController'
import { useArticleHistoryStore } from '@/stores/articleHistory'

const router = useRouter()
const historyStore = useArticleHistoryStore()

// ===================== 列表数据 =====================
const loading = ref(false)
const records = ref<API.ArticleVO[]>([])
const total = ref(0)

// 分页与筛选状态放入 Pinia，返回本页时保持页数不变
const { current, pageSize, statusFilter } = storeToRefs(historyStore)

interface Column {
  title: string
  key: string
  dataIndex?: string
  width?: number
  fixed?: string
}

const columns: Column[] = [
  { title: '选题', key: 'topic', dataIndex: 'topic', width: 220 },
  { title: '标题', key: 'title', dataIndex: 'mainTitle', width: 240 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '创建时间', key: 'createTime', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 240, fixed: 'right' },
]

const pagination = computed(() => ({
  current: current.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

async function loadList(page = current.value) {
  loading.value = true
  try {
    const res = await listArticle({
      current: page,
      pageSize: pageSize.value,
      status: statusFilter.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      records.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
      current.value = page
    } else {
      message.error(res.data.message || '加载失败')
    }
  } catch (e) {
    console.error('加载文章列表失败', e)
    message.error('加载文章列表失败')
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: { current?: number; pageSize?: number }) {
  if (pag.pageSize && pag.pageSize !== pageSize.value) {
    pageSize.value = pag.pageSize
    loadList(1)
  } else if (pag.current && pag.current !== current.value) {
    loadList(pag.current)
  }
}

// ===================== 列展示工具 =====================
const statusMap: Record<string, { text: string; color: string }> = {
  PENDING: { text: '等待处理', color: 'default' },
  PROCESSING: { text: '处理中', color: 'processing' },
  COMPLETED: { text: '已完成', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
}

function statusText(status?: string) {
  return statusMap[status ?? '']?.text ?? status ?? '—'
}

function statusColor(status?: string) {
  return statusMap[status ?? '']?.color ?? 'default'
}

function formatTime(time?: string) {
  if (!time) return '—'
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function hasContent(record: API.ArticleVO) {
  return !!(record.fullContent || record.content)
}

// ===================== 查看文章（跳转到独立详情页） =====================
function viewArticle(record: API.ArticleVO) {
  if (!record.taskId) {
    message.warning('该文章缺少任务信息，无法查看')
    return
  }
  router.push({ path: '/article/detail', query: { taskId: record.taskId } })
}

// ===================== 导出文章 =====================
function exportArticle(record: API.ArticleVO) {
  const text = record.fullContent || record.content || ''
  if (!text) {
    message.warning('该文章暂无可导出的内容')
    return
  }
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${record.mainTitle || record.topic || record.taskId || 'article'}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  message.success('文章已导出')
}

// ===================== 删除 =====================
async function handleDelete(record: API.ArticleVO) {
  try {
    const res = await deleteArticle({ id: record.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 若当前页删空且不是第一页，回退一页
      if (records.value.length === 1 && current.value > 1) {
        loadList(current.value - 1)
      } else {
        loadList()
      }
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (e) {
    console.error('删除失败', e)
    message.error('删除失败')
  }
}

// ===================== 新建 =====================
function goCreate() {
  router.push('/article/create')
}

onMounted(() => {
  // 按 Pinia / localStorage 中保存的页码加载，返回本页时页数不变
  loadList()
})
</script>

<style scoped>
.article-history-page {
  padding: 24px;
  min-height: 0;
  overflow-y: auto;
  background: linear-gradient(180deg, #fafaff 0%, #f4f5fb 100%);
}

.history-card {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px;
  background: #ffffff;
  border: 1px solid rgba(124, 58, 237, 0.08);
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04), 0 8px 24px rgba(124, 58, 237, 0.06);
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.head-title h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.head-sub {
  font-size: 13px;
  color: #9ca3af;
}

.head-action {
  border: none;
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.28);
}

.head-action:not(:disabled):hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #f472b6 100%);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

/* 表格 */
.cell-topic,
.cell-title {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.cell-topic {
  color: #374151;
}

.cell-title {
  font-weight: 500;
  color: #111827;
}

.cell-time {
  color: #6b7280;
  font-variant-numeric: tabular-nums;
}

.cell-action {
  padding: 0 4px;
  height: auto;
}

/* 空状态 */
.table-empty {
  padding: 24px 0;
}

@media (max-width: 768px) {
  .article-history-page {
    padding: 12px;
  }

  .history-card {
    padding: 16px;
  }

  .history-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
