<template>
  <div id="userRegisterPage">
    <div class="form-card">
      <h2 class="form-title">创建账号</h2>
      <p class="form-subtitle">注册开启您的 AI 创作之旅</p>
      <a-form :model="formState" @finish="handleSubmit">
        <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
          <a-input v-model:value="formState.userAccount" placeholder="请输入账号" size="large" />
        </a-form-item>
        <a-form-item
            name="userPassword"
            :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码不能小于 8 位' },
          ]"
        >
          <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" size="large" />
        </a-form-item>

        <a-form-item
            name="checkPassword"
            :rules="[
            { required: true, message: '请确认密码' },
            { validator: validateCheckPassword },
          ]"
        >
          <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" size="large" />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block>
            注册
          </a-button>
        </a-form-item>
      </a-form>
      <div class="form-footer">
        <span>已有账号？</span>
        <RouterLink to="/user/login">立即登录</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">

import {useRouter} from "vue-router";
import {reactive} from "vue";
import {message} from "ant-design-vue";
import {userRegister} from "@/api/user";

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

// 验证确认密码
const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #faf5ff 0%, #ffffff 50%, #eff6ff 100%);
}

.form-card {
  width: 100%;
  max-width: 400px;
  padding: 40px 32px;
  background: #ffffff;
  border: 1px solid rgba(124, 58, 237, 0.1);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(124, 58, 237, 0.08);
}

.form-title {
  margin-bottom: 8px;
  text-align: center;
  font-size: 24px;
  color: #0f172a;
}

.form-subtitle {
  margin-bottom: 24px;
  text-align: center;
  color: #6b7280;
}

.form-footer {
  margin-top: 16px;
  text-align: center;
  color: #6b7280;
}

.form-footer a {
  color: #7c3aed;
}
</style>
