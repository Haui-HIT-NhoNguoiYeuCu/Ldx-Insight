<script setup lang="ts">
// Các composable của Nuxt được auto-import, bạn có thể giữ nguyên như dưới
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.title });
definePageMeta({ layout: 'default' });

const api = useApi();
const { data, pending, error, execute } = await useAsyncData(
  'users',
  () => api.user.getUsers(),
  { immediate: false }
);

onMounted(() => {
  if (!data.value) execute();
});

const searchQuery = ref('');
const filters = ['All Categories', 'Recent', 'Popular', 'Most Downloaded'];
</script>

<template>
  <div
    class="from-primary via-accent to-secondary text-primary-foreground flex flex-1 items-center justify-center bg-linear-to-br py-20"
  >
    <div class="mx-auto px-4 text-white sm:px-6 lg:px-8">
      <div class="mb-12 text-center">
        <h1 class="mb-4 text-5xl font-bold">Open Linked Hub</h1>
        <p class="mb-8 text-xl opacity-90">
          Khám phá, tìm hiểu và truy cập các bộ dữ liệu công khai
        </p>
      </div>

      <div class="mx-auto mb-12 max-w-2xl">
        <div class="flex gap-2">
          <div class="relative flex-1 rounded-3xl">
            <Icon
              name="lucide:search"
              class="absolute top-3.5 left-4 h-5 w-5 opacity-50"
            />
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search datasets..."
              class="bg-primary-foreground text-foreground placeholder-muted-foreground focus:ring-accent w-full rounded-3xl border-2 py-3 pr-4 pl-12 focus:ring-1 focus:outline-none"
            />
          </div>
          <UButton class="cursor-pointer rounded-3xl px-8 text-base">
            Search
          </UButton>
        </div>
      </div>

      <div class="flex flex-wrap justify-center gap-3">
        <UButton
          v-for="filter in filters"
          :key="filter"
          color="neutral"
          variant="outline"
          class="hover:bg-primary/30 cursor-pointer rounded-3xl bg-transparent px-4 text-white"
        >
          {{ filter }}
        </UButton>
      </div>
    </div>
  </div>
</template>
