<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.pages.data.name });
definePageMeta({ layout: 'default' });

type Dataset = {
  id: number;
  name: string;
  category: keyof typeof categoryIconNames | 'All' | string;
  description: string;
  downloads: number;
  views: number;
  updated: string;
  format: string;
};

const categoryIconNames = {
  Agriculture: 'lucide:leaf',
  Health: 'lucide:heart',
  Finance: 'lucide:dollar-sign',
  Education: 'lucide:book-open',
  Transportation: 'lucide:truck',
  Environment: 'lucide:leaf',
} as const;

const formatColors: Record<string, string> = {
  CSV: 'bg-blue-100 text-blue-700',
  JSON: 'bg-orange-100 text-orange-700',
  Excel: 'bg-green-100 text-green-700',
  GeoJSON: 'bg-purple-100 text-purple-700',
  PDF: 'bg-red-100 text-red-700',
};

const datasets = ref<Dataset[]>([
  {
    id: 1,
    name: 'Agricultural Production 2024',
    category: 'Agriculture',
    description: 'Comprehensive data on crop yields and production statistics',
    downloads: 1240,
    views: 5320,
    updated: '2024-12-15',
    format: 'CSV, JSON',
  },
  {
    id: 2,
    name: 'Healthcare Statistics',
    category: 'Health',
    description: 'Regional health metrics and hospital performance data',
    downloads: 892,
    views: 3210,
    updated: '2024-12-10',
    format: 'Excel, CSV',
  },
  {
    id: 3,
    name: 'Financial Market Data',
    category: 'Finance',
    description: 'Stock prices and market indicators for analysis',
    downloads: 2150,
    views: 8940,
    updated: '2024-12-18',
    format: 'JSON, CSV',
  },
  {
    id: 4,
    name: 'Education Enrollment',
    category: 'Education',
    description: 'Student enrollment and graduation rates by institution',
    downloads: 654,
    views: 2890,
    updated: '2024-12-12',
    format: 'CSV, Excel',
  },
  {
    id: 5,
    name: 'Transportation Network',
    category: 'Transportation',
    description: 'Traffic patterns and public transportation usage',
    downloads: 1876,
    views: 6540,
    updated: '2024-12-16',
    format: 'GeoJSON, CSV',
  },
  {
    id: 6,
    name: 'Environmental Monitoring',
    category: 'Environment',
    description: 'Air quality and environmental sensor readings',
    downloads: 945,
    views: 4120,
    updated: '2024-12-14',
    format: 'JSON, PDF',
  },
]);

const categories = [
  'All',
  'Agriculture',
  'Health',
  'Finance',
  'Education',
  'Transportation',
  'Environment',
];

const selectedCategory = ref('All');
const searchTerm = ref('');

const filteredDatasets = computed(() => {
  const term = searchTerm.value.toLowerCase();
  return datasets.value.filter(d => {
    const matchesCategory =
      selectedCategory.value === 'All' || d.category === selectedCategory.value;
    const matchesSearch =
      d.name.toLowerCase().includes(term) ||
      d.description.toLowerCase().includes(term);
    return matchesCategory && matchesSearch;
  });
});

function formatsOf(formatString: string) {
  return formatString.split(',').map(f => f.trim());
}
</script>

<template>
  <div>
    <main class="bg-background min-h-screen">
      <!-- Header -->
      <section
        class="from-primary via-accent to-secondary text-primary-foreground bg-linear-to-r py-12"
      >
        <div class="mx-auto max-w-7xl px-4 text-white sm:px-6 lg:px-8">
          <h1 class="mb-4 text-4xl font-bold">Datasets</h1>
          <p class="text-lg opacity-90">
            Explore {{ datasets.length }} datasets across multiple categories
          </p>
        </div>
      </section>

      <!-- Content -->
      <div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 gap-8 lg:grid-cols-4">
          <!-- Sidebar -->
          <div class="lg:col-span-1">
            <!-- Search -->
            <div class="mb-8">
              <h3 class="mb-4 font-bold">Search</h3>
              <div class="relative">
                <Icon
                  name="lucide:search"
                  class="text-muted-foreground absolute top-3 left-3 h-4 w-4"
                />
                <input
                  v-model="searchTerm"
                  type="text"
                  placeholder="Search datasets..."
                  class="border-border focus:ring-primary w-full rounded-lg border py-2 pr-4 pl-10 focus:ring-2 focus:outline-none"
                />
              </div>
            </div>

            <!-- Categories -->
            <div>
              <h3 class="mb-4 font-bold">Categories</h3>
              <div class="space-y-2">
                <button
                  v-for="category in categories"
                  :key="category"
                  @click="selectedCategory = category"
                  class="flex w-full items-center gap-3 rounded-lg px-4 py-2 text-left transition-colors"
                  :class="
                    selectedCategory === category
                      ? 'bg-primary text-primary-'
                      : 'bg-gray-100 hover:bg-gray-200'
                  "
                >
                  <Icon
                    v-if="category !== 'All'"
                    :name="
                      categoryIconNames[
                        category as keyof typeof categoryIconNames
                      ]
                    "
                    class="h-4 w-4"
                    :class="
                      selectedCategory === category
                        ? 'text-primary-foreground'
                        : 'text-primary'
                    "
                  />
                  <span>{{ category }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Main Content -->
          <div class="lg:col-span-3">
            <div class="space-y-4">
              <template v-if="filteredDatasets.length > 0">
                <NuxtLink
                  v-for="dataset in filteredDatasets"
                  :key="dataset.id"
                  :to="`/data/${dataset.id}`"
                  class="block"
                >
                  <div
                    class="bg-card border-border cursor-pointer rounded-lg border p-6 transition-shadow hover:shadow-lg"
                  >
                    <div class="mb-3 flex items-start justify-between">
                      <div class="flex-1">
                        <div class="mb-2 flex items-center gap-3">
                          <Icon
                            v-if="dataset.category !== 'All'"
                            :name="
                              categoryIconNames[
                                dataset.category as keyof typeof categoryIconNames
                              ]
                            "
                            class="text-primary h-5 w-5"
                          />
                          <h3 class="text-primary text-xl font-bold">
                            {{ dataset.name }}
                          </h3>
                        </div>
                        <p class="text-muted-foreground mb-3">
                          {{ dataset.description }}
                        </p>
                        <div class="flex flex-col gap-3">
                          <div>
                            <span
                              class="bg-primary/10 text-primary inline-block rounded-full px-3 py-1 text-sm font-medium"
                            >
                              {{ dataset.category }}
                            </span>
                          </div>
                          <div class="flex flex-wrap gap-2">
                            <span
                              v-for="f in formatsOf(dataset.format)"
                              :key="f"
                              class="inline-block rounded-full px-3 py-1 text-sm font-medium"
                              :class="
                                formatColors[f] || 'bg-gray-100 text-gray-700'
                              "
                            >
                              {{ f }}
                            </span>
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
                          <span>{{ dataset.downloads }} downloads</span>
                        </div>
                        <div class="flex items-center gap-2">
                          <Icon name="lucide:eye" class="h-4 w-4" />
                          <span>{{ dataset.views }} views</span>
                        </div>
                        <div>Updated: {{ dataset.updated }}</div>
                      </div>
                      <UButton> View Details </UButton>
                    </div>
                  </div>
                </NuxtLink>
              </template>
              <div v-else class="py-12 text-center">
                <p class="text-muted-foreground text-lg">No datasets found</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>
