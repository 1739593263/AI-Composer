<template>
  <a-layout-header class="global-header">
    <div class="header-inner">
      <div class="header-left">
        <a class="brand" href="/">
          <div class="brand-icon">
            <AudioOutlined />
          </div>
          <span class="brand-name">AI Composer</span>
        </a>
      </div>

      <div class="header-center">
        <a-input-search
          class="header-search"
          placeholder="搜索作品、风格或关键词"
          allow-clear
        />
      </div>

      <div class="header-right">
        <div class="header-actions">
          <a-button type="text" class="action-btn">
            <template #icon><BellOutlined /></template>
          </a-button>

          <template v-if="isLogin">
            <a-dropdown placement="bottomRight">
              <a-button type="text" class="user-btn">
                <span class="user-name">{{ loginUserStore.loginUser.userName }}</span>
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="profile" @click="router.push('/user/profile')">
                    <UserOutlined />
                    <span>个人信息</span>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="handleLogout">
                    <LogoutOutlined />
                    <span>退出登录</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>

          <template v-else>
            <span class="not-login-text">当前无用户登录</span>
            <a-button type="primary" size="middle" @click="router.push('/user/login')">
              登录
            </a-button>
            <a-button type="link" size="middle" @click="router.push('/user/register')">
              注册
            </a-button>
          </template>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { message } from "ant-design-vue";
import { useLoginUserStore } from "@/stores/loginUser";
import { userLogout } from "@/api/user";
import { DEFAULT_USERNAME } from "@/constants/user";

const router = useRouter()
const loginUserStore = useLoginUserStore()

const isLogin = computed(() => {
  return loginUserStore.loginUser.userName && loginUserStore.loginUser.userName !== DEFAULT_USERNAME
})

const handleLogout = async () => {
  await userLogout()
  loginUserStore.setLoginUser({ userName: DEFAULT_USERNAME })
  message.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  padding: 0 24px;
  line-height: 64px;
  background: linear-gradient(135deg, #faf5ff 0%, #ffffff 45%, #eff6ff 100%);
  border-bottom: 1px solid rgba(124, 58, 237, 0.08);
}

.header-inner {
  max-width: 1280px;
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.header-left {
  flex-shrink: 0;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #0f172a;
}

.brand-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #ffffff;
  border-radius: 10px;
  background: linear-gradient(135deg, #7c3aed 0%, #ec4899 100%);
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
  background: linear-gradient(90deg, #7c3aed 0%, #ec4899 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-center {
  flex: 1;
  max-width: 560px;
}

.header-search {
  width: 100%;
}

.header-right {
  flex-shrink: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #4b5563;
  border-radius: 8px;
  transition: color 0.2s, background-color 0.2s;
}

.action-btn:hover {
  color: #7c3aed;
  background-color: rgba(124, 58, 237, 0.08);
}

.user-btn {
  height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #0f172a;
  border-radius: 8px;
}

.user-name {
  font-size: 15px;
  font-weight: 500;
}

.not-login-text {
  margin-right: 8px;
  font-size: 14px;
  color: #6b7280;
}

@media (max-width: 768px) {
  .global-header {
    padding: 0 16px;
  }

  .brand-name {
    display: none;
  }

  .header-center {
    max-width: none;
  }

  .not-login-text {
    display: none;
  }
}
</style>
