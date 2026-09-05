<template>
  <div class="article-create-page">
    <div class="workspace">
      <!-- ============ 左侧：文章流程监控 ============ -->
      <aside class="panel panel-flow">
        <div class="panel-header">
          <span class="panel-title">
            <RocketOutlined />
            生成流程
          </span>
        </div>

        <div class="flow-list">
          <div v-for="(stage, index) in stages" :key="stage.key" class="flow-item" :class="stage.status">
            <div class="flow-line-wrap">
              <div class="flow-node">
                <span class="flow-index">{{ index + 1 }}</span>
                <CheckOutlined v-if="stage.status === 'done'" class="node-icon node-done" />
                <LoadingOutlined v-else-if="stage.status === 'running'" class="node-icon node-running" />
                <CloseCircleOutlined v-else-if="stage.status === 'error'" class="node-icon node-error" />
              </div>
              <div v-if="index < stages.length - 1" class="flow-line" :class="{ active: stage.status === 'done' }" />
            </div>
            <div class="flow-content">
              <span class="flow-label">{{ stage.label }}</span>
              <span class="flow-desc">{{ stage.desc }}</span>
            </div>
          </div>
        </div>

        <div v-if="isDone" class="flow-footer">
          <CheckCircleFilled />
          文章生成完成
        </div>
      </aside>

      <!-- ============ 中间：创作区 ============ -->
      <main class="panel panel-compose">
        <div class="compose-topbar">
          <div class="compose-title">
            <h2>文章创作台</h2>
            <span class="compose-sub">多智能体协作，一键生成图文并茂的完整文章</span>
          </div>
          <a-tag :color="statusColor">{{ statusText }}</a-tag>
        </div>

        <div class="composer-bar">
          <div class="composer-inputs">
            <a-input
              v-model:value="topic"
              class="topic-input"
              placeholder="输入文章选题，例如：2026年最值得关注的 AI 应用趋势"
              :disabled="isGenerating"
              allow-clear
              @press-enter="startGeneration"
            />
            <a-select
              v-model:value="style"
              class="style-select"
              :options="STYLE_OPTIONS"
              placeholder="选择写作风格"
              :disabled="isGenerating"
              allow-clear
            />
          </div>
          <a-button
            type="primary"
            class="generate-btn"
            :loading="isGenerating"
            @click="startGeneration"
          >
            <template #icon><ThunderboltOutlined /></template>
            {{ isGenerating ? '创作中...' : '开始创作' }}
          </a-button>
        </div>

        <div ref="docScrollRef" class="doc-scroll" @scroll="onDocScroll">
          <!-- 空态 -->
          <div v-if="!hasResult && !isGenerating" class="doc-empty">
            <div class="doc-empty-icon"><ThunderboltOutlined /></div>
            <h3>开始你的创作</h3>
            <p>输入选题并选择风格，AI 将自动完成标题、大纲、正文与配图的生成。</p>
            <p class="doc-empty-tip">也可以从右侧「选题推荐」中挑选一个灵感。</p>
          </div>

          <article v-else class="doc">
            <!-- 主标题 / 副标题 -->
            <header v-if="titleResult.main_title" class="doc-title">
              <h1>{{ titleResult.main_title }}</h1>
              <p v-if="titleResult.sub_title">{{ titleResult.sub_title }}</p>
            </header>

            <!-- 大纲 -->
            <section v-if="outlineSections.length" class="doc-section">
              <h2 class="doc-section-title">文章大纲</h2>
              <div class="outline-list">
                <div v-for="(sec, i) in outlineSections" :key="i" class="outline-item">
                  <span class="outline-num">{{ sec.section_id ?? i + 1 }}</span>
                  <div class="outline-body">
                    <h3>{{ sec.section_title }}</h3>
                    <ul v-if="sec.key_points && sec.key_points.length">
                      <li v-for="(point, j) in sec.key_points" :key="j">{{ point }}</li>
                    </ul>
                  </div>
                </div>
              </div>
            </section>
            <section v-else-if="outlineRunning" class="doc-section">
              <h2 class="doc-section-title">文章大纲</h2>
              <a-skeleton active :paragraph="{ rows: 3 }" />
            </section>

            <!-- 完整图文（合成完成后：正文 + 内嵌配图） -->
            <section v-if="fullContent" class="doc-section">
              <h2 class="doc-section-title">完整文章</h2>
              <div class="doc-body" v-html="fullContentHtml" />
            </section>

            <!-- 正文（流式） -->
            <section v-else-if="contentText" class="doc-section">
              <h2 class="doc-section-title">正文</h2>
              <div class="doc-body" v-html="displayContentHtml" />
            </section>

            <!-- 配图（合成前的实时配图流） -->
            <section v-if="images.length && !fullContent" class="doc-section">
              <h2 class="doc-section-title">配图（{{ images.length }}）</h2>
              <div class="gallery">
                <figure v-for="(img, i) in images" :key="i" class="gallery-item">
                  <img :src="img.url" :alt="img.description || img.keywords" loading="lazy" />
                  <figcaption>{{ img.sectionTitle || img.description || img.keywords }}</figcaption>
                </figure>
              </div>
            </section>
          </article>

          <button
            v-if="!followBottom && isGenerating"
            type="button"
            class="back-to-bottom"
            aria-label="回到底部"
            @click="scrollToBottom"
          >
            <ArrowDownOutlined />
          </button>
        </div>
      </main>

      <!-- ============ 右侧：辅助面板 ============ -->
      <aside class="panel panel-aux">
        <div class="aux-card">
          <div class="panel-header">
            <span class="panel-title">
              <BulbOutlined />
              选题推荐
            </span>
            <a-button type="text" size="small" class="refresh-btn" @click="rotateTopics">
              <template #icon><ReloadOutlined /></template>
              换一批
            </a-button>
          </div>

          <div class="topic-list">
            <button v-for="(t, i) in topicSuggestions" :key="i" class="topic-item" @click="applyTopic(t.title)">
              <span class="topic-tag">{{ t.tag }}</span>
              <span class="topic-title">{{ t.title }}</span>
            </button>
          </div>
        </div>

        <div class="aux-card">
          <div class="panel-header">
            <span class="panel-title">
              <PieChartOutlined />
              文章完成度
            </span>
          </div>

          <div class="completeness" role="status" aria-live="polite">
            <a-progress
              type="circle"
              :percent="completeness"
              :stroke-color="progressColor"
              :width="120"
            />
            <div class="complete-info">
              <span class="complete-percent">{{ completeness }}%</span>
              <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </div>
          </div>

          <div class="stat-grid">
            <div class="stat-item">
              <span class="stat-num">{{ outlineSections.length }}</span>
              <span class="stat-label">章节</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ wordCount }}</span>
              <span class="stat-label">字数</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ images.length }}</span>
              <span class="stat-label">配图</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ elapsed }}</span>
              <span class="stat-label">耗时(秒)</span>
            </div>
          </div>
        </div>

        <div v-if="isDone" class="aux-card">
          <a-button block type="primary" ghost class="aux-action" @click="copyMarkdown">
            <template #icon><CopyOutlined /></template>
            复制 Markdown
          </a-button>
          <a-button block class="aux-action" @click="resetAll">
            <template #icon><ReloadOutlined /></template>
            重新创作
          </a-button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { message } from 'ant-design-vue'
import {
  ArrowDownOutlined,
  BulbOutlined,
  CheckCircleFilled,
  CheckOutlined,
  CloseCircleOutlined,
  CopyOutlined,
  LoadingOutlined,
  PieChartOutlined,
  ReloadOutlined,
  RocketOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import { markdownToHtml } from '@/utils/markdown'
import { useArticleCreateStore } from '@/stores/articleCreate'

const STYLE_OPTIONS = [
  { label: '科技视角', value: 'tech' },
  { label: '教程指南', value: 'educational' },
  { label: '观点评论', value: 'opinion' },
  { label: '轻松幽默', value: 'humorous' },
  { label: '情感共鸣', value: 'emotional' },
]

const TOPIC_POOL: { tag: string; title: string }[] = [
  { tag: '趋势', title: '2026年最值得关注的 AI 应用趋势' },
  { tag: '教程', title: 'DeepSeek 提示词工程实战指南' },
  { tag: '职场', title: '普通人如何用 AI 提升工作效率' },
  { tag: '教程', title: '零基础入门大模型微调' },
  { tag: '创意', title: 'AI 绘画从入门到进阶全攻略' },
  { tag: '创业', title: 'AIGC 内容创业的十个商业机会' },
  { tag: '前沿', title: '多智能体协作框架深度解析' },
  { tag: '工具', title: '如何构建属于自己的 AI 知识库' },
  { tag: '评测', title: '开源大模型横向评测：谁是性价比之王' },
  { tag: '观点', title: '生成式 AI 对设计师职业的冲击与机遇' },
  { tag: '趋势', title: '2026 智能体元年：Agent 将如何重塑软件' },
  { tag: '职场', title: '写给程序员的 AI 转型路线图' },
]

// ===================== 状态（保存在 Pinia，页面切换不丢失） =====================
const createStore = useArticleCreateStore()
const {
  stages,
  topic,
  style,
  titleResult,
  outlineSections,
  contentText,
  fullContent,
  images,
  isGenerating,
  isDone,
  hasError,
  elapsed,
} = storeToRefs(createStore)

const outlineRunning = computed(() => {
  const s = stages.value.find((item) => item.key === 'outline')
  return s?.status === 'running'
})

const hasResult = computed(
  () =>
    !!titleResult.value.main_title ||
    outlineSections.value.length > 0 ||
    !!contentText.value ||
    !!fullContent.value ||
    images.value.length > 0,
)

const displayContentHtml = computed(() => {
  const base = contentText.value || fullContent.value || ''
  const html = markdownToHtml(base)
  return isGenerating ? html + '<span class="doc-cursor">▍</span>' : html
})

// 合成完成后的完整图文（含内嵌配图）
const fullContentHtml = computed(() => markdownToHtml(fullContent.value || ''))

// ===================== 中间区自动滚动 =====================
const docScrollRef = ref<HTMLElement | null>(null)
// 是否跟随底部（用户上滑阅读时自动暂停跟随）
const followBottom = ref(true)

function scrollToBottom() {
  const el = docScrollRef.value
  if (!el) return
  el.scrollTop = el.scrollHeight
  followBottom.value = true
}

function onDocScroll() {
  const el = docScrollRef.value
  if (!el) return
  const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  followBottom.value = nearBottom
}

// 生成内容发生变化时，若处于跟随状态则自动滚到最底部
watch(
  [displayContentHtml, fullContentHtml, () => outlineSections.value.length, () => images.value.length, () => titleResult.value.main_title],
  () => {
    if (followBottom.value) {
      nextTick(scrollToBottom)
    }
  },
)

const completeness = computed(() => {
  const total = stages.value.length
  let score = 0
  stages.value.forEach((s) => {
    if (s.status === 'done') {
      score += 1
    } else if (s.status === 'running') {
      score += 0.5
    }
  })
  return Math.round((score / total) * 100)
})

const progressColor = computed(() => {
  if (hasError.value) return '#ef4444'
  if (isDone.value) return '#22c55e'
  return '#7c3aed'
})

const statusText = computed(() => {
  if (isDone.value) return '已完成'
  if (hasError.value) return '生成失败'
  if (isGenerating.value) return '创作中'
  return '未开始'
})

const statusColor = computed(() => {
  if (isDone.value) return 'success'
  if (hasError.value) return 'error'
  if (isGenerating.value) return 'processing'
  return 'default'
})

const wordCount = computed(() => countWords(contentText.value || fullContent.value))

function countWords(text: string): number {
  if (!text) return 0
  const cjk = (text.match(/[\u4e00-\u9fa5]/g) ?? []).length
  const latin = (text.replace(/[\u4e00-\u9fa5]/g, ' ').match(/[A-Za-z0-9]+/g) ?? []).length
  return cjk + latin
}

// ===================== 开始创作 =====================
function startGeneration() {
  createStore.startGeneration()
}

// ===================== 重置 =====================
function resetAll() {
  createStore.resetResult()
  followBottom.value = true
}

// ===================== 辅助面板 =====================
const topicSuggestions = ref(TOPIC_POOL.slice(0, 5))

function rotateTopics() {
  const pool = [...TOPIC_POOL]
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const tmp = pool[i] as { tag: string; title: string }
    pool[i] = pool[j] as { tag: string; title: string }
    pool[j] = tmp
  }
  topicSuggestions.value = pool.slice(0, 5)
}

function applyTopic(title: string) {
  topic.value = title
  message.info('已填入选题，点击「开始创作」即可生成')
}

async function copyMarkdown() {
  const text = fullContent.value || contentText.value
  if (!text) {
    message.warning('暂无可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动选择复制')
  }
}

// 回到创作页时：若生成中但 SSE 已断开则重连（状态保存在 Pinia）
onMounted(() => {
  createStore.ensureConnected()
})
</script>

<style scoped>
.article-create-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, #fafaff 0%, #f4f5fb 100%);
}

.workspace {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
  padding: 16px 24px;
}

.panel {
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1px solid rgba(124, 58, 237, 0.08);
  border-radius: 14px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04), 0 8px 24px rgba(124, 58, 237, 0.06);
  overflow: hidden;
}

/* ==================== 通用面板头部 ==================== */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #f1f0f7;
  flex-shrink: 0;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.panel-title .anticon {
  color: #7c3aed;
}

/* ==================== 左侧：流程监控 ==================== */
.panel-flow {
  width: 244px;
  flex-shrink: 0;
}

.flow-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px 16px;
}

.flow-item {
  display: flex;
  gap: 12px;
  position: relative;
}

.flow-line-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 28px;
  flex-shrink: 0;
}

.flow-node {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  background: #f3f4f6;
  color: #9ca3af;
  transition: all 0.25s ease;
}

.flow-item.running .flow-node {
  background: #fff;
  border: 2px solid #7c3aed;
  color: #7c3aed;
}

.flow-item.done .flow-node {
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
  color: #ffffff;
  border: none;
}

.flow-item.error .flow-node {
  background: #fee2e2;
  color: #ef4444;
  border: none;
}

.node-icon {
  font-size: 14px;
}

.node-running {
  color: #7c3aed;
}

.flow-line {
  width: 2px;
  flex: 1;
  min-height: 22px;
  margin: 4px 0;
  background: #e5e7eb;
  border-radius: 1px;
  transition: background 0.3s ease;
}

.flow-line.active {
  background: linear-gradient(180deg, #7c3aed 0%, #ec4899 100%);
}

.flow-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 22px;
  min-width: 0;
}

.flow-label {
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
  transition: color 0.25s ease;
}

.flow-desc {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
}

.flow-item.running .flow-label {
  color: #7c3aed;
}

.flow-item.done .flow-label {
  color: #111827;
}

.flow-item.error .flow-label {
  color: #ef4444;
}

.flow-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #16a34a;
  background: #f0fdf4;
  border-top: 1px solid #dcfce7;
}

/* ==================== 中间：创作区 ==================== */
.panel-compose {
  flex: 1;
  min-width: 0;
  position: relative;
}

/* 回到底部悬浮按钮 */
.back-to-bottom {
  position: absolute;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #ffffff;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
  box-shadow: 0 4px 14px rgba(124, 58, 237, 0.35);
  cursor: pointer;
  z-index: 10;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.back-to-bottom:hover {
  transform: translateX(-50%) translateY(-2px);
  box-shadow: 0 6px 18px rgba(124, 58, 237, 0.4);
}

.back-to-bottom:focus-visible {
  outline: 2px solid #7c3aed;
  outline-offset: 2px;
}

.compose-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f0f7;
  flex-shrink: 0;
}

.compose-title h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.compose-sub {
  margin-left: 10px;
  font-size: 12px;
  color: #9ca3af;
}

.composer-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f0f7;
  flex-shrink: 0;
}

.composer-inputs {
  flex: 1;
  display: flex;
  gap: 10px;
  min-width: 0;
}

.topic-input {
  flex: 1;
  min-width: 0;
}

.style-select {
  width: 150px;
  flex-shrink: 0;
}

.generate-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 22px;
  font-weight: 600;
  border: none;
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
  box-shadow: 0 4px 12px rgba(124, 58, 237, 0.28);
}

.generate-btn:not(:disabled):hover {
  background: linear-gradient(135deg, #8b5cf6 0%, #f472b6 100%);
}

.doc-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px 40px;
}

/* 空态 */
.doc-empty {
  height: 100%;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #6b7280;
}

.doc-empty-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #7c3aed;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.12), rgba(236, 72, 153, 0.12));
  margin-bottom: 18px;
}

.doc-empty h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}

.doc-empty p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
}

.doc-empty-tip {
  margin-top: 4px;
  color: #9ca3af;
}

/* 文章文档 */
.doc {
  max-width: 860px;
  margin: 0 auto;
}

.doc-title {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f1f0f7;
}

.doc-title h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.35;
  letter-spacing: 0.5px;
  color: #0f172a;
}

.doc-title p {
  margin: 0;
  font-size: 15px;
  color: #6b7280;
  line-height: 1.6;
}

.doc-section {
  margin-bottom: 28px;
}

.doc-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.doc-section-title::before {
  content: '';
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #7c3aed 0%, #ec4899 100%);
}

/* 大纲 */
.outline-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.outline-item {
  display: flex;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #faf9fe;
  border: 1px solid rgba(124, 58, 237, 0.08);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.outline-item:hover {
  border-color: rgba(124, 58, 237, 0.28);
  box-shadow: 0 2px 10px rgba(124, 58, 237, 0.08);
}

.outline-num {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  border-radius: 6px;
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
}

.outline-body {
  min-width: 0;
}

.outline-body h3 {
  margin: 2px 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.outline-body ul {
  margin: 0;
  padding-left: 18px;
}

.outline-body li {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.7;
}

/* 正文（markdown 渲染） */
.doc-body {
  font-size: 15px;
  line-height: 1.85;
  color: #374151;
  word-break: break-word;
}

.doc-body :deep(h2) {
  margin: 24px 0 12px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.doc-body :deep(h3) {
  margin: 20px 0 10px;
  font-size: 17px;
  font-weight: 600;
  color: #111827;
}

.doc-body :deep(p) {
  margin: 0 0 14px;
}

.doc-body :deep(strong) {
  color: #111827;
  font-weight: 600;
}

.doc-body :deep(ul),
.doc-body :deep(ol) {
  margin: 0 0 14px;
  padding-left: 22px;
}

.doc-body :deep(li) {
  margin-bottom: 4px;
}

.doc-body :deep(blockquote) {
  margin: 0 0 14px;
  padding: 10px 14px;
  border-left: 3px solid #7c3aed;
  background: #faf9fe;
  border-radius: 0 8px 8px 0;
  color: #4b5563;
}

.doc-body :deep(code) {
  padding: 2px 6px;
  font-size: 13px;
  border-radius: 4px;
  background: #f3f4f6;
  color: #be185d;
}

.doc-body :deep(pre) {
  margin: 0 0 14px;
  padding: 14px 16px;
  overflow-x: auto;
  border-radius: 10px;
  background: #0f172a;
}

.doc-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #e2e8f0;
}

.doc-body :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 8px 0;
}

.doc-body :deep(a) {
  color: #7c3aed;
}

.doc-body :deep(.doc-cursor) {
  display: inline-block;
  width: 3px;
  height: 1em;
  margin-left: 2px;
  vertical-align: -0.15em;
  background: #7c3aed;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

/* 配图 */
.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.gallery-item {
  margin: 0;
  border-radius: 10px;
  overflow: hidden;
  background: #f9fafb;
  border: 1px solid #f1f0f7;
}

.gallery-item img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.gallery-item figcaption {
  padding: 8px 10px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ==================== 右侧：辅助面板 ==================== */
.panel-aux {
  width: 300px;
  flex-shrink: 0;
  overflow-y: auto;
}

.aux-card {
  padding: 16px 18px;
  border-bottom: 1px solid #f1f0f7;
}

.refresh-btn {
  font-size: 12px;
  color: #7c3aed;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.topic-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  text-align: left;
  border: 1px solid #f1f0f7;
  border-radius: 10px;
  background: #faf9fe;
  cursor: pointer;
  transition: all 0.2s ease;
}

.topic-item:hover {
  border-color: rgba(124, 58, 237, 0.35);
  background: #ffffff;
  box-shadow: 0 2px 10px rgba(124, 58, 237, 0.1);
  transform: translateY(-1px);
}

.topic-item:focus-visible {
  outline: 2px solid #7c3aed;
  outline-offset: 2px;
}

.topic-tag {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 11px;
  font-weight: 600;
  color: #7c3aed;
  border-radius: 4px;
  background: rgba(124, 58, 237, 0.1);
}

.topic-title {
  font-size: 13px;
  color: #374151;
  line-height: 1.5;
}

/* 完成度 */
.completeness {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 14px;
}

.complete-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.complete-percent {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  border-radius: 10px;
  background: #faf9fe;
  border: 1px solid rgba(124, 58, 237, 0.08);
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: #7c3aed;
}

.stat-label {
  font-size: 11px;
  color: #9ca3af;
}

.aux-action {
  margin-bottom: 10px;
}

.aux-action:last-child {
  margin-bottom: 0;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1100px) {
  .article-create-page {
    flex: none;
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .workspace {
    flex-direction: column;
    height: auto;
  }

  .panel-flow,
  .panel-aux {
    width: 100%;
  }

  .panel-compose {
    min-height: 60vh;
  }

  .style-select {
    width: 130px;
  }
}

@media (max-width: 640px) {
  .workspace {
    padding: 12px;
  }

  .composer-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .composer-inputs {
    flex-direction: column;
  }

  .style-select {
    width: 100%;
  }

  .compose-sub {
    display: none;
  }
}
</style>
