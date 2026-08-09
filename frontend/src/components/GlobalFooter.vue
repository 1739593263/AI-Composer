<template>
  <a-layout-footer class="global-footer">
    <div class="footer-inner">
      <div class="footer-divider"></div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
</script>

<style scoped>
.global-footer {
  padding: 0;
  background: none;
  line-height: normal;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  text-align: center;
}

.footer-divider {
  height: 2px;
  border-radius: 2px;
  background: linear-gradient(90deg, transparent 0%, #7c3aed 30%, #ec4899 70%, transparent 100%);
}
</style>
