import { ref } from 'vue'
import { defineStore } from 'pinia'
import { message } from 'ant-design-vue'
import { createArticle } from '@/api/aicomposer/articleController'
import { closeSSE, connectSSE } from '@/utils/sse'
import type { SSEMessage } from '@/utils/sse'

/** 主标题 / 副标题 */
export interface TitleResult {
  main_title?: string
  sub_title?: string
}

/** 大纲小节 */
export interface OutlineSection {
  section_id?: number
  section_title?: string
  key_points?: string[]
}

/** 配图 */
export interface ImageResult {
  image_type?: string
  url?: string
  method?: string
  keywords?: string
  sectionTitle?: string
  description?: string
}

type StageStatus = 'pending' | 'running' | 'done' | 'error'

/** 生成流程阶段 */
export interface FlowStage {
  key: string
  label: string
  desc: string
  status: StageStatus
}

/** 生成流程阶段定义（对应后端 Agent 智能体流程） */
export const FLOW_STAGES: { key: string; label: string; desc: string }[] = [
  { key: 'title', label: '标题生成', desc: '智能体 1 · 生成主副标题' },
  { key: 'outline', label: '大纲生成', desc: '智能体 2 · 构建文章骨架' },
  { key: 'content', label: '正文生成', desc: '智能体 3 · 流式撰写正文' },
  { key: 'imageAnalysis', label: '配图分析', desc: '智能体 4 · 规划配图需求' },
  { key: 'imageGen', label: '配图生成', desc: '智能体 5 · 检索 / 生成配图' },
  { key: 'merge', label: '图文合成', desc: '合成完整图文内容' },
]

/**
 * 文章创作页状态
 * 存入 Pinia，页面切换 / 组件卸载时数据不丢失；
 * SSE 连接在 store 中持有，生成中离开页面也不会断流。
 */
export const useArticleCreateStore = defineStore('articleCreate', () => {
    // ===================== 生成流程状态 =====================
    const stages = ref<FlowStage[]>(FLOW_STAGES.map((s) => ({ ...s, status: 'pending' as StageStatus })))

    function setStage(key: string, status: StageStatus) {
        const stage = stages.value.find((s) => s.key === key)
        if (stage) {
            stage.status = status
        }
    }

    function resetStages() {
        stages.value.forEach((s) => {
            s.status = 'pending'
        })
    }

    // ===================== 生成结果状态 =====================
    const topic = ref('')
    const style = ref<string | undefined>(undefined)

    const taskId = ref('')
    const titleResult = ref<TitleResult>({})
    const outlineSections = ref<OutlineSection[]>([])
    const contentText = ref('')
    const fullContent = ref('')
    const images = ref<ImageResult[]>([])

    const isGenerating = ref(false)
    const isDone = ref(false)
    const hasError = ref(false)
    const elapsed = ref(0)

    // 连接 / 定时器为普通成员（非响应式），SSE 连接跨页面保持接收
    let source: EventSource | null = null
    let timer: number | undefined

    function startTimer() {
        stopTimer()
        elapsed.value = 0
        timer = window.setInterval(() => {
            elapsed.value += 1
        }, 1000)
    }

    function stopTimer() {
        if (timer) {
            window.clearInterval(timer)
            timer = undefined
        }
    }

    function finishGeneration() {
        isGenerating.value = false
        isDone.value = true
        stopTimer()
    }

    function failGeneration() {
        isGenerating.value = false
        hasError.value = true
        stopTimer()
        const running = stages.value.find((s) => s.status === 'running')
        if (running) {
            running.status = 'error'
        }
    }

    // ===================== SSE 处理 =====================
    function connectSource() {
        closeSSE(source)
        source = null
        if (!taskId.value) {
            return
        }
        source = connectSSE(taskId.value, {
            onMessage: handleSseMessage,
            onError: () => {
                message.error('实时连接中断，请稍后查看结果')
            },
            onComplete: () => {
                // 已完成 / 出错时在消息中处理
            },
        })
    }

    /** 回到创作页时恢复可能中断的 SSE 连接 */
    function ensureConnected() {
        if (isGenerating.value && !source) {
            connectSource()
        }
    }

    function handleSseMessage(msg: SSEMessage) {
        const type = msg.type
        switch (type) {
            case 'AGENT1_COMPLETE':
                setStage('title', 'done')
                setStage('outline', 'running')
                if (msg.title) {
                    titleResult.value = msg.title
                }
                break
            case 'AGENT2_STREAMING':
                setStage('outline', 'running')
                break
            case 'AGENT2_COMPLETE':
                setStage('outline', 'done')
                setStage('content', 'running')
                if (Array.isArray(msg.outline)) {
                    outlineSections.value = msg.outline
                }
                break
            case 'AGENT3_STREAMING':
                setStage('content', 'running')
                contentText.value += msg.content ?? ''
                break
            case 'AGENT3_COMPLETE':
                setStage('content', 'done')
                setStage('imageAnalysis', 'running')
                break
            case 'AGENT4_COMPLETE':
                setStage('imageAnalysis', 'done')
                setStage('imageGen', 'running')
                break
            case 'IMAGE_COMPLETE':
                setStage('imageGen', 'running')
                if (msg.image) {
                    images.value.push(msg.image)
                }
                break
            case 'AGENT5_COMPLETE':
                setStage('imageGen', 'done')
                setStage('merge', 'running')
                if (Array.isArray(msg.images) && msg.images.length > 0) {
                    images.value = msg.images
                }
                break
            case 'MERGE_COMPLETE':
                setStage('merge', 'done')
                if (msg.fullContent) {
                    fullContent.value = msg.fullContent
                }
                break
            case 'ALL_COMPLETE':
                setStage('merge', 'done')
                finishGeneration()
                message.success('文章生成完成')
                break
            case 'ERROR':
                failGeneration()
                message.error('文章生成失败，请重试')
                break
        }
    }

    // ===================== 开始创作 =====================
    async function startGeneration() {
        if (!topic.value.trim()) {
            message.warning('请先输入文章选题')
            return
        }
        resetResult()

        try {
            const res = await createArticle({ topic: topic.value.trim(), style: style.value })
            if (res.data.code !== 0) {
                message.error(res.data.message || '创建文章任务失败')
                return
            }
            taskId.value = res.data.data ?? ''
            isGenerating.value = true
            setStage('title', 'running')
            startTimer()

            connectSource()
        } catch (e) {
            console.error('创建文章任务失败', e)
            message.error('创建文章任务失败')
        }
    }

    // ===================== 重置 =====================
    function resetResult() {
        closeSSE(source)
        source = null
        stopTimer()
        resetStages()
        titleResult.value = {}
        outlineSections.value = []
        contentText.value = ''
        fullContent.value = ''
        images.value = []
        taskId.value = ''
        elapsed.value = 0
        isGenerating.value = false
        isDone.value = false
        hasError.value = false
    }

    return {
        stages,
        topic,
        style,
        taskId,
        titleResult,
        outlineSections,
        contentText,
        fullContent,
        images,
        isGenerating,
        isDone,
        hasError,
        elapsed,
        startGeneration,
        ensureConnected,
        resetResult,
    }
})