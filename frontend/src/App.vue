<template>
  <n-config-provider :theme="theme">
    <n-message-provider>
      <router-view />
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { darkTheme } from 'naive-ui'

const theme = ref(null)
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}
</style>

