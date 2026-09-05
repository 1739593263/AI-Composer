<template>
  <div class="article-detail-page">
    <div class="detail-card">
      <!-- 顶栏 -->
      <div class="detail-head">
        <a-button class="back-btn" @click="goBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回历史
        </a-button>
        <a-button type="primary" class="export-btn" :disabled="!contentText" @click="exportArticle">
          <template #icon><DownloadOutlined /></template>
          导出文章
        </a-button>
      </div>

      <!-- 杂志封面：封面图 + 标题 -->
      <div v-if="coverImage" class="article-cover">
        <div class="cover-img-wrap">
          <img class="cover-img" :src="coverImage" alt="封面" />
          <div class="cover-overlay" />
          <div class="cover-text">
            <h2 class="cover-title">{{ title }}</h2>
            <p v-if="subTitle" class="cover-subtitle">{{ subTitle }}</p>
          </div>
        </div>
      </div>
      <h2 v-else class="detail-title">{{ title }}</h2>

      <!-- 文章内容 -->
      <div class="doc-body">
        <div v-if="loading" class="doc-state">
          <a-spin size="large" />
        </div>
        <div v-else-if="error" class="doc-state doc-error">文章加载失败，请稍后重试</div>
        <article v-else class="view-doc" v-html="contentHtml" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { getArticle } from '@/api/aicomposer/articleController'
import { markdownToHtml } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()

// 从 /article/detail?taskId=xxx 读取
const taskId = (route.query.taskId as string) || ''
const loading = ref(true)
const error = ref(false)
const title = ref('文章详情')
const subTitle = ref('')
const coverImage = ref('')
const contentText = ref('')

const contentHtml = computed(() => markdownToHtml(contentText.value))

async function loadArticle() {
  if (!taskId) {
    error.value = true
    loading.value = false
    return
  }
  try {
    const res = await getArticle({ taskId })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      contentText.value = data.fullContent || data.content || ''
      title.value = data.mainTitle || data.topic || '文章详情'
      subTitle.value = data.subTitle || ''
      coverImage.value = data.coverImage || ''
    } else {
      error.value = true
    }
  } catch (e) {
    console.error('加载文章详情失败', e)
    error.value = true
  } finally {
    loading.value = false
  }
}

function exportArticle() {
  if (!contentText.value) {
    message.warning('暂无可导出的内容')
    return
  }
  const blob = new Blob([contentText.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title.value || 'article'}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  message.success('文章已导出')
}

function goBack() {
  // 返回历史页（Pinia 中保留了分页/筛选状态）
  router.push('/article/history')
}

onMounted(loadArticle)
</script>

<style scoped>
.article-detail-page {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
  background: linear-gradient(180deg, #fafaff 0%, #f4f5fb 100%);
}

.detail-card {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px 28px 32px;
  background: #ffffff;
  border: 1px solid rgba(124, 58, 237, 0.08);
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04), 0 8px 24px rgba(124, 58, 237, 0.06);
}

/* 顶栏 */
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f1f0f7;
}

.back-btn,
.export-btn {
  flex-shrink: 0;
}

.detail-title {
  margin: 0 0 20px;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.35;
  color: #0f172a;
}

/* 杂志封面 */
.article-cover {
  margin-bottom: 24px;
}

.cover-img-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid rgba(124, 58, 237, 0.12);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08), 0 16px 40px rgba(124, 58, 237, 0.14);
}

.cover-img {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.06) 0%, rgba(15, 23, 42, 0.3) 50%, rgba(15, 23, 42, 0.85) 100%);
}

.cover-text {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24px;
  text-align: left;
}

.cover-title {
  margin: 0 0 6px;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.3;
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.35);
}

.cover-subtitle {
  margin: 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.export-btn {
  flex-shrink: 0;
  border: none;
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.28);
}

.export-btn:not(:disabled):hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #f472b6 100%);
}

/* 加载 / 错误状态 */
.doc-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.doc-error {
  color: #ef4444;
  font-size: 14px;
}

/* 文章正文 */
.view-doc {
  font-size: 15px;
  line-height: 1.85;
  color: #374151;
  word-break: break-word;
}

/* v-html 注入的内容没有 data-v 属性，必须用 :deep() 才能命中 */
.view-doc :deep(h1) {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.35;
  color: #0f172a;
}

.view-doc :deep(h2) {
  margin: 24px 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.view-doc :deep(h3) {
  margin: 20px 0 10px;
  font-size: 17px;
  font-weight: 600;
  color: #111827;
}

.view-doc :deep(p) {
  margin: 0 0 14px;
}

.view-doc :deep(strong) {
  color: #111827;
}

.view-doc :deep(ul),
.view-doc :deep(ol) {
  margin: 0 0 14px;
  padding-left: 22px;
}

.view-doc :deep(li) {
  margin-bottom: 4px;
}

.view-doc :deep(blockquote) {
  margin: 0 0 14px;
  padding: 10px 14px;
  border-left: 3px solid #7c3aed;
  background: #faf9fe;
  border-radius: 0 8px 8px 0;
  color: #4b5563;
}

.view-doc :deep(code) {
  padding: 2px 6px;
  font-size: 13px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #be185d;
}

.view-doc :deep(pre) {
  margin: 0 0 14px;
  padding: 14px 16px;
  overflow-x: auto;
  border-radius: 10px;
  background: #0f172a;
}

.view-doc :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #e2e8f0;
}

/* 正文图片：自适应宽度，禁止溢出卡片边框 */
.view-doc :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 16px auto;
  border-radius: 12px;
  border: 1px solid rgba(124, 58, 237, 0.12);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08), 0 12px 32px rgba(124, 58, 237, 0.12);
}

.view-doc :deep(a) {
  color: #7c3aed;
}

@media (max-width: 768px) {
  .article-detail-page {
    padding: 12px;
  }

  .detail-card {
    padding: 16px;
  }

  .detail-head {
    flex-wrap: wrap;
  }
}
</style>