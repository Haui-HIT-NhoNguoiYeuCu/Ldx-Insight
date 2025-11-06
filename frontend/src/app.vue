<script setup lang="ts">
const appConfig = useAppConfig();

useHead({ titleTemplate: `%s | ${appConfig.title}` });
</script>
<template>
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>
