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
                    path: 'article/create',
                    name: '文章创作',
                    component: () => import('@/pages/article/ArticleCreatePage.vue'),
                },
                {
                    path: 'article/history',
                    name: '创作历史',
                    component: () => import('@/pages/article/ArticleHistoryPage.vue'),
                },
                {
                    path: 'article/detail',
                    name: '文章查看',
                    component: () => import('@/pages/article/ArticleDetailPage.vue'),
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
