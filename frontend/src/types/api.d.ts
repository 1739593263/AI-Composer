/**
 * 全局 API 类型声明
 */
declare namespace API {
    /**
     * 通用返回结果
     */
    type BaseResponse<T> = {
        code: number
        data: T
        message: string
    }

    /**
     * 用户注册请求
     */
    type UserRegisterRequest = {
        userAccount: string
        userPassword: string
        checkPassword: string
    }

    /**
     * 用户登录请求
     */
    type UserLoginRequest = {
        userAccount: string
        userPassword: string
    }

    /**
     * 脱敏后的登录用户信息
     */
    type LoginUserVO = {
        id?: number
        userAccount?: string
        userName?: string
        userAvatar?: string
        userProfile?: string
        userRole?: string
        createTime?: string
        updateTime?: string
    }
}
