<script setup lang="ts">
const appConfig = useAppConfig();
useHead({ titleTemplate: `%s | ${appConfig.title}` });

const toaster = { position: 'top-right' };
</script>
<template>
  <UApp :toaster="toaster">
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </UApp>
</template>
