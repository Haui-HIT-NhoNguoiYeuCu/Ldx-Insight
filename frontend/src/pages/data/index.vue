<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.pages.data.name });
definePageMeta({ layout: 'default' });

const route = useRoute();
const router = useRouter();
const q = ref<string>((route.query.q as string) || '');
const page = ref<number>(Number(route.query.page) || 1);
const pageSize = ref<number>(10);
const sortBy = ref<'viewCount' | 'downloadCount' | 'updatedAt' | 'createdAt'>(
  'downloadCount'
);
const sortDir = ref<'asc' | 'desc'>('desc');
const sortParam = computed(() => `${sortBy.value},${sortDir.value}`);
const selectedCategory = ref((route.query.category as string) || '');

const api = useApi();
const {
  data: datasetRes,
  pending: datasetPending,
  execute: fetchDatasets,
} = await useAsyncData(
  'datasets',
  () =>
    api.dataset.list({
      q: q.value || '',
      category: selectedCategory.value || '',
      page: page.value - 1,
      size: pageSize.value,
      sort: sortParam.value,
    }),
  { immediate: false }
);

const { data: categoriesRes, execute: fetchCategories } = await useAsyncData(
  'categories',
  () => api.dataset.category(),
  {
    immediate: false,
  }
);

onMounted(() => {
  if (!datasetRes.value) fetchDatasets();
  if (!categoriesRes.value) fetchCategories();
});

watch([q, selectedCategory, page, pageSize, sortParam], () => {
  fetchDatasets();
});

const datasets = computed(() => datasetRes.value?.content ?? []);
const totalPages = computed(() => datasetRes.value?.totalPages ?? 1);
const totalElements = computed(() => datasetRes.value?.totalElements ?? 0);
const categories = computed(() => categoriesRes.value?.slice(1));

const onSearch = () => {
  page.value = 1;
  fetchDatasets();
};

const refresh = () => {
  page.value = 1;
  q.value = '';
  fetchDatasets();
  router.push('/data');
};
</script>

<template>
  <div>
    <main class="bg-background min-h-screen">
      <!-- Header -->
      <section
        class="from-primary via-accent to-secondary text-primary-foreground bg-linear-to-r py-12"
      >
        <div class="mx-auto max-w-7xl px-4 text-white sm:px-6 lg:px-8">
          <h1 class="mb-4 text-4xl font-bold">Dữ liệu</h1>
          <p class="text-lg opacity-90">
            Khám phá {{ totalElements }} bộ dữ liệu theo danh mục
          </p>
        </div>
      </section>

      <!-- Content -->
      <div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <!-- Search -->
        <div class="mb-8">
          <h3 class="mb-4 font-bold">Tìm kiếm</h3>
          <div class="flex gap-4">
            <div class="relative flex-1">
              <Icon
                name="lucide:search"
                class="text-muted-foreground absolute top-3 left-3 h-4 w-4"
              />
              <input
                v-model="q"
                type="text"
                placeholder="Tìm kiếm bộ dữ liệu..."
                class="border-border focus:ring-primary w-full rounded-lg border py-2 pr-4 pl-10 focus:ring-2 focus:outline-none"
                @keydown.enter="onSearch"
              />
            </div>
            <UButton class="px-4" @click="onSearch">Tìm kiếm</UButton>
            <UButton
              class="px-4"
              icon="solar:refresh-linear"
              color="neutral"
              variant="outline"
              @click="refresh"
            />
          </div>
        </div>
        <div class="grid grid-cols-1 gap-8 lg:grid-cols-4">
          <!-- Sidebar -->
          <div class="lg:col-span-1">
            <!-- Categories -->
            <div>
              <h3 class="mb-4 font-bold">Categories</h3>
              <div class="space-y-2" v-if="categories">
                <button
                  @click="selectedCategory = ''"
                  class="flex w-full items-center gap-3 rounded-lg px-4 py-2 text-left transition-colors"
                  :class="
                    selectedCategory === ''
                      ? 'bg-primary text-primary-'
                      : 'bg-gray-100 hover:bg-gray-200'
                  "
                >
                  <span>All</span>
                </button>
                <div v-for="category in categories" :key="category">
                  <button
                    @click="selectedCategory = category"
                    class="flex w-full items-center gap-3 rounded-lg px-4 py-2 text-left transition-colors"
                    :class="
                      selectedCategory === category
                        ? 'bg-primary text-primary-'
                        : 'bg-gray-100 hover:bg-gray-200'
                    "
                  >
                    <span>{{ category }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Main Content -->
          <div class="lg:col-span-3">
            <div class="space-y-4">
              <template v-if="datasets.length > 0">
                <NuxtLink
                  v-for="dataset in datasets"
                  :key="dataset.id"
                  :to="`/data/${dataset.id}`"
                  class="block"
                >
                  <div
                    class="bg-card border-border cursor-pointer rounded-lg border bg-white p-6 transition-shadow hover:shadow-lg"
                  >
                    <div class="mb-3 flex items-start justify-between">
                      <div class="flex-1">
                        <div class="mb-2 flex items-center gap-3">
                          <h3 class="mb-3 text-xl font-bold text-black/70">
                            {{ dataset.title }}
                          </h3>
                        </div>
                        <div class="flex flex-col gap-3">
                          <div v-if="dataset.category">
                            Danh mục:
                            <UBadge
                              class="bg-primary/10 text-primary inline-block rounded-full px-3 py-1 text-sm font-medium"
                            >
                              {{ dataset.category }}
                            </UBadge>
                          </div>
                          <div
                            class="flex flex-wrap gap-2"
                            v-if="dataset.tags && dataset.tags.length > 0"
                          >
                            Thẻ:
                            <UBadge
                              v-for="tag in dataset.tags"
                              :key="tag"
                              class="inline-block rounded-full px-3 py-1 text-sm font-medium"
                              color="neutral"
                              variant="outline"
                            >
                              {{ tag }}
                            </UBadge>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div
                      class="border-border flex items-center justify-between border-t pt-4"
                    >
                      <div class="text-muted-foreground flex gap-6 text-sm">
                        <div class="flex items-center gap-2">
                          <Icon name="lucide:download" class="h-4 w-4" />
                          <span>{{ dataset.downloadCount }} downloads</span>
                        </div>
                        <div class="flex items-center gap-2">
                          <Icon name="lucide:eye" class="h-4 w-4" />
                          <span>{{ dataset.viewCount }} views</span>
                        </div>
                        <div v-if="dataset.updatedAt">
                          Updated: {{ dataset.updatedAt }}
                        </div>
                      </div>
                      <UButton> Chi tiết </UButton>
                    </div>
                  </div>
                </NuxtLink>
              </template>
              <div v-else class="py-12 text-center">
                <p class="text-muted-foreground text-lg">No datasets found</p>
              </div>
            </div>
            <div class="mt-6 flex items-center justify-between">
              <span class="text-muted-foreground text-sm">
                Tổng: {{ totalElements }} bản ghi • Trang {{ page }} /
                {{ totalPages }}
              </span>

              <UPagination
                v-model="page"
                :total="totalElements"
                show-controls
                show-edges
              />
            </div>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div
        class="fixed top-9 right-0 bottom-0 left-0 z-50 mt-6 flex items-center justify-center bg-white/60"
        v-if="datasetPending"
      >
        <UIcon name="line-md:loading-loop" class="text-primary size-12" />
      </div>
    </main>
  </div>
</template>
