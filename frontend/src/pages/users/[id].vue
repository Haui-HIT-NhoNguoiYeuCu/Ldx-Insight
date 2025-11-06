<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.pages.profile.title });
definePageMeta({
  layout: 'default',
});

const route = useRoute();
const api = useApi();

const { data, pending, error, execute } = await useAsyncData(
  `user-${route.params.id}`,
  () => api.user.getUserById(route.params.id as string),
  { immediate: false }
);

onMounted(() => {
  if (!data.value) execute();
});
</script>

<template>
  <UCard>
    <div v-if="pending" class="text-center">
      <UButton color="neutral" loading />
    </div>
    <div v-else-if="error" class="text-red-500">Lỗi: {{ error.message }}</div>
    <div v-else-if="data">
      <h1 class="text-xl font-bold">{{ data.name }}</h1>
      <p>Email: {{ data.email }}</p>
      <NuxtLink to="/">
        <UButton color="neutral">Quay lại</UButton>
      </NuxtLink>
    </div>
    <div v-else>Không tìm thấy người dùng</div>
  </UCard>
</template>
