import {createRouter, createWebHistory} from "vue-router";
import BasicLayout from "@/layouts/BasicLayout.vue";
import UserLoginPage from "@/pages/user/UserLoginPage.vue";
import UserRegisterPage from "@/pages/user/UserRegisterPage.vue";
import UserProfilePage from "@/pages/user/UserProfilePage.vue";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: '主页',
            component: BasicLayout,
            children: [
                {
                    path: '',
                    name: '首页',
                    component: () => import('@/pages/HomePage.vue'),
                },
                {
                    path: 'admin/userManage',
                    name: '用户管理',
                    component: () => import('@/pages/admin/UserManagePage.vue'),
                },
            ],
        },
        {
            path: '/user/login',
            name: '用户登录',
            component: UserLoginPage,
        },
        {
            path: '/user/register',
            name: '用户注册',
            component: UserRegisterPage,
        },
        {
            path: '/user/profile',
            name: '个人信息',
            component: UserProfilePage,
        },
    ],
})

export default router
