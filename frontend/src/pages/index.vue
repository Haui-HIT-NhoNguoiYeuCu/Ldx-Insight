<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.title });
definePageMeta({ layout: 'default' });

const router = useRouter();
const q = ref('');
const filters = [
  {
    title: 'Tất cả',
    category: '',
  },
  { title: 'Văn hoá du lịch', category: 'Văn hoá du lịch' },
  { title: 'Giáo dục', category: 'Giáo dục' },
  { title: 'Kinh tế', category: 'Kinh tế' },
];

const onSearch = () => {
  if (!q.value) return;
  router.push({
    path: '/data',
    query: { q: q.value },
  });
};

const navigateToCategory = (category: string) => {
  router.push({
    path: '/data',
    query: { category: category },
  });
};
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
              v-model="q"
              type="text"
              placeholder="Tìm kiếm dữ liệu..."
              class="bg-primary-foreground text-foreground placeholder-muted-foreground focus:ring-accent w-full rounded-3xl border-2 py-3 pr-4 pl-12 focus:ring-1 focus:outline-none"
              @keydown.enter="onSearch"
            />
          </div>
          <UButton
            class="cursor-pointer rounded-3xl px-8 text-base"
            @click="onSearch"
          >
            Tìm kiếm
          </UButton>
        </div>
      </div>

      <div class="flex flex-wrap justify-center gap-3">
        <UButton
          v-for="filter in filters"
          :key="filter.category"
          color="neutral"
          variant="outline"
          class="hover:bg-primary/30 cursor-pointer rounded-3xl bg-transparent px-4 text-white"
          @click="navigateToCategory(filter.category)"
        >
          {{ filter.title }}
        </UButton>
      </div>
    </div>
  </div>
</template>
