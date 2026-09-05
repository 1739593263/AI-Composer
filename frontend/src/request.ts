import axios from 'axios'
import { message } from 'ant-design-vue'

// 后端 API 基础地址（axios 与 SSE 共用，修改时保持两处一致）
export const API_BASE_URL = 'http://localhost:8567/api'

// 创建 Axios 实例
const myAxios = axios.create({
    baseURL: API_BASE_URL,
    timeout: 60000,
    withCredentials: true,  // 必须！携带 Cookie
})

// 全局响应拦截器
myAxios.interceptors.response.use(
    function (response) {
        const { data } = response
        // 未登录
        if (data.code === 40100) {
            if (
                !response.request.responseURL.includes('user/get/login') &&
                !window.location.pathname.includes('/user/login')
            ) {
                message.warning('请先登录')
                window.location.href = `/user/login?redirect=${window.location.href}`
            }
        }
        return response
    },
    function (error) {
        return Promise.reject(error)
    },
)

export default myAxios
