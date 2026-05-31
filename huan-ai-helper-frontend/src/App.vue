<template>
  <a-config-provider
    :theme="{
      algorithm: theme.darkAlgorithm,
      token: {
        colorPrimary: '#0C72FF',
        colorBgContainer: '#0d0d0d',
        colorBgElevated: '#1e1e1e',
        colorBorder: '#2a2a2a',
        colorText: '#ececec',
        colorTextSecondary: '#b4b4b4',
        colorTextTertiary: '#8e8e8e',
      },
    }"
  >
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { theme } from 'ant-design-vue'
</script>
