/**
 * SSE 工具函数
 * 管理建立SSE连接，接收数据，关闭SSE连接事务
 */
import { API_BASE_URL } from "@/request";

export interface SSEMessage {
    type: string
    data?: any
    [key: string]: any
}

export interface SSEOptions {
    onMessage: (message: SSEMessage) => void
    onError?: (error: Event) => void
    onComplete?: () => void
}

/**
 * 建立 SSE 连接
 */
export const connectSSE = (taskId: string, options: SSEOptions): EventSource => {
    const { onMessage, onError, onComplete } = options

    // 跨域直连后端，必须携带 Cookie（后端已配置 CORS）
    const eventSource = new EventSource(`${API_BASE_URL}/article/progress/${taskId}`, {
        withCredentials: true,
    })

    /**
     * 接收流式信息
     * @param event
     */
    eventSource.onmessage = (event) => {
        try {
            const message: SSEMessage = JSON.parse(event.data)
            onMessage(message)

            // 检查是否完成
            if (message.type === 'ALL_COMPLETE' || message.type === 'ERROR') {
                eventSource.close()
                onComplete?.()
            }
        } catch (error) {
            console.error('SSE 消息解析失败:', error)
        }
    }

    eventSource.onerror = (error) => {
        console.error('SSE 连接错误:', error)
        onError?.(error)
        eventSource.close()
    }

    return eventSource
}

/**
 * 关闭 SSE 连接
 */
export const closeSSE = (eventSource: EventSource | null) => {
    if (eventSource) {
        eventSource.close()
    }
}

/** SSE 连接句柄（可外部主动关闭） */
export interface SSEConnection {
    close(): void
}

export interface SSERetryOptions {
    /** 最大重连次数，超过后调用 onFailed，默认 5 */
    maxAttempts?: number
    /** 基础重连间隔（ms），指数退避，默认 1000 */
    baseDelayMs?: number
    /** 重连次数耗尽后触发（由上层决定查状态 / 续接 / 放弃） */
    onFailed?: () => void
}

/** 收到这些类型的消息即视为流程终止，停止重连 */
const TERMINAL_MESSAGE_TYPES = ['ALL_COMPLETE', 'ERROR']

/**
 * 带自动重连的 SSE 连接
 *
 * - 断线后指数退避自动重连（1s / 2s / 4s ...，上限 30s + 随机抖动）
 * - 收到终止消息（ALL_COMPLETE / ERROR）后停止重连并触发 onComplete
 * - 重连次数耗尽后触发 onFailed（不再自动重试）
 * - close() 可随时主动关闭并取消待执行的重连
 */
export const connectSSEWithRetry = (
    taskId: string,
    options: SSEOptions,
    retryOptions: SSERetryOptions = {},
): SSEConnection => {
    const { onMessage, onComplete } = options
    const { maxAttempts = 5, baseDelayMs = 1000, onFailed } = retryOptions

    let eventSource: EventSource | null = null
    let retryTimer: number | undefined
    let closed = false
    let attempt = 0

    const stopReconnect = () => {
        if (retryTimer) {
            window.clearTimeout(retryTimer)
            retryTimer = undefined
        }
    }

    const open = () => {
        if (closed) {
            return
        }

        eventSource = new EventSource(`${API_BASE_URL}/article/progress/${taskId}`, {
            withCredentials: true,
        })
        // 连接成功后重置重试计数
        eventSource.onopen = () => {
            attempt = 0
        }

        eventSource.onmessage = (event) => {
            try {
                const message: SSEMessage = JSON.parse(event.data)
                onMessage(message)

                // 收到完成 / 失败消息：关闭连接并停止重连
                if (TERMINAL_MESSAGE_TYPES.includes(message.type)) {
                    closed = true
                    eventSource?.close()
                    eventSource = null
                    stopReconnect()
                    onComplete?.()
                }
            } catch (error) {
                console.error('SSE 消息解析失败:', error)
            }
        }

        eventSource.onerror = (error) => {
            console.error('SSE 连接错误:', error)
            eventSource?.close()
            eventSource = null

            // 流程已正常终止 / 已主动关闭，不再重连
            if (closed) {
                return
            }

            // 超过最大重连次数：交给上层处理
            attempt += 1
            if (attempt > maxAttempts) {
                closed = true
                stopReconnect()
                onFailed?.()
                return
            }

            // 指数退避 + 抖动，避免同时刻风暴重连
            const delay = Math.min(baseDelayMs * 2 ** (attempt - 1), 30000) + Math.random() * 500
            retryTimer = window.setTimeout(open, delay)
        }
    }

    open()

    return {
        close() {
            closed = true
            stopReconnect()
            eventSource?.close()
            eventSource = null
        },
    }
}