<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ title: appConfig.pages['not-found'].title });
definePageMeta({ layout: 'blank' });
</script>

<template>
  <UError
    :error="{
      statusCode: 404,
      statusMessage: 'Page not found',
      message: 'The page you are looking for does not exist.',
    }"
  />
</template>
