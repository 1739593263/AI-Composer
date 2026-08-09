import myAxios from '@/request'

/**
 * 用户注册
 */
export async function userRegister(userRegisterRequest: API.UserRegisterRequest) {
    return myAxios.post<API.BaseResponse<number>>('/user/register', userRegisterRequest)
}

/**
 * 用户登录
 */
export async function userLogin(userLoginRequest: API.UserLoginRequest) {
    return myAxios.post<API.BaseResponse<API.LoginUserVO>>('/user/login', userLoginRequest)
}

/**
 * 获取当前登录用户
 */
export async function getLoginUser() {
    return myAxios.get<API.BaseResponse<API.LoginUserVO>>('/user/get/login')
}

/**
 * 用户注销
 */
export async function userLogout() {
    return myAxios.post<API.BaseResponse<boolean>>('/user/logout')
}
