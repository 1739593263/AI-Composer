import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

/** localStorage 持久化键 */
const STORAGE_KEY = 'article-history-state'

interface PersistedState {
    current: number
    pageSize: number
    statusFilter: string
}

function loadInitial(): PersistedState {
    const defaults: PersistedState = { current: 1, pageSize: 10, statusFilter: '' }
    try {
        const raw = localStorage.getItem(STORAGE_KEY)
        if (!raw) {
            return defaults
        }
        const data = JSON.parse(raw) as Partial<PersistedState>
        return {
            current: typeof data.current === 'number' && data.current > 0 ? data.current : defaults.current,
            pageSize: typeof data.pageSize === 'number' && data.pageSize > 0 ? data.pageSize : defaults.pageSize,
            statusFilter: typeof data.statusFilter === 'string' ? data.statusFilter : defaults.statusFilter,
        }
    } catch (e) {
        console.error('读取创作历史页状态失败', e)
        return defaults
    }
}

/**
 * 创作历史页面的列表状态（页数 / 每页条数 / 状态筛选）
 * 存入 Pinia 并持久化到 localStorage：
 * 1. 页面切换（如查看文章后返回）时页数保持不变
 * 2. 刷新浏览器后仍能恢复上次的分页与筛选状态
 */
export const useArticleHistoryStore = defineStore('articleHistory', () => {
    const initial = loadInitial()

    // 当前页
    const current = ref(initial.current)
    // 每页条数
    const pageSize = ref(initial.pageSize)
    // 状态筛选（空串 = 全部）
    const statusFilter = ref(initial.statusFilter)

    // 状态变化时持久化到 localStorage
    watch([current, pageSize, statusFilter], () => {
        try {
            localStorage.setItem(
                STORAGE_KEY,
                JSON.stringify({
                    current: current.value,
                    pageSize: pageSize.value,
                    statusFilter: statusFilter.value,
                } satisfies PersistedState),
            )
        } catch (e) {
            console.error('保存创作历史页状态失败', e)
        }
    })

    /**
     * 重置筛选与分页到初始状态
     */
    function reset() {
        current.value = 1
        pageSize.value = 10
        statusFilter.value = ''
    }

    /**
     * 更新分页 / 筛选状态
     */
    function setState(next: { current?: number; pageSize?: number; statusFilter?: string }) {
        if (next.current !== undefined) {
            current.value = next.current
        }
        if (next.pageSize !== undefined) {
            pageSize.value = next.pageSize
        }
        if (next.statusFilter !== undefined) {
            statusFilter.value = next.statusFilter
        }
    }

    return { current, pageSize, statusFilter, reset, setState }
})