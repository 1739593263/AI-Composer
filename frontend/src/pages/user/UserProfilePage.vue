<template>
  <div id="userProfilePage">
    <div class="profile-card">
      <h2 class="profile-title">个人信息</h2>
      <a-descriptions bordered :column="1">
        <a-descriptions-item label="用户名">{{ loginUserStore.loginUser.userName }}</a-descriptions-item>
        <a-descriptions-item label="账号">{{ loginUserStore.loginUser.userAccount }}</a-descriptions-item>
        <a-descriptions-item label="头像">{{ loginUserStore.loginUser.userAvatar }}</a-descriptions-item>
        <a-descriptions-item label="简介">{{ loginUserStore.loginUser.userProfile }}</a-descriptions-item>
        <a-descriptions-item label="角色">{{ loginUserStore.loginUser.userRole }}</a-descriptions-item>
      </a-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
import {useLoginUserStore} from "@/stores/loginUser";

const loginUserStore = useLoginUserStore()
</script>

<style scoped>
#userProfilePage {
  padding: 24px;
  max-width: 720px;
  margin: 0 auto;
}

.profile-card {
  padding: 32px;
  background: #ffffff;
  border: 1px solid rgba(124, 58, 237, 0.1);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(124, 58, 237, 0.08);
}

.profile-title {
  margin-bottom: 24px;
  font-size: 24px;
  color: #0f172a;
}
</style>
