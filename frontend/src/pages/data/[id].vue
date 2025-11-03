<!-- pages/data/[id].vue -->
<script setup lang="ts">
definePageMeta({ layout: 'default' });

// Lấy id từ route
const route = useRoute();
const datasetId = computed(() => Number(route.params.id as string));

// ====== Mock datasets (giữ nguyên như file TSX) ======
type DataColumn = { name: string; type: string; description: string };
type Dataset = {
  id: number;
  name: string;
  category: string;
  description: string;
  downloads: number;
  views: number;
  updated: string;
  format: string;
  fullDescription: string;
  dataColumns: DataColumn[];
  fileSize: string;
  recordCount: string;
  updateFrequency: string;
  license: string;
  contact: string;
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
    fullDescription: `This dataset contains comprehensive agricultural production data for 2024, including:

• Crop yields by region and crop type
• Production volumes and trends
• Seasonal variations and patterns
• Weather impact analysis
• Farmer demographics and farm sizes
• Equipment and technology usage
• Market prices and economic indicators
• Sustainability metrics

The data is collected from agricultural departments across multiple regions and updated monthly. It provides valuable insights for agricultural planning, research, and policy making.`,
    dataColumns: [
      {
        name: 'Region',
        type: 'String',
        description: 'Geographic region identifier',
      },
      {
        name: 'Crop Type',
        type: 'String',
        description: 'Type of crop produced',
      },
      {
        name: 'Yield (tons)',
        type: 'Number',
        description: 'Production yield in metric tons',
      },
      {
        name: 'Area (hectares)',
        type: 'Number',
        description: 'Cultivated area in hectares',
      },
      { name: 'Date', type: 'Date', description: 'Data collection date' },
      {
        name: 'Quality Score',
        type: 'Number',
        description: 'Quality rating 0-100',
      },
    ],
    fileSize: '45.2 MB',
    recordCount: '125,000+',
    updateFrequency: 'Monthly',
    license: 'Creative Commons Attribution 4.0',
    contact: 'agriculture@data.gov',
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
    fullDescription: `Healthcare statistics dataset containing:

• Hospital bed capacity and occupancy rates
• Patient admission and discharge data
• Disease prevalence by region
• Healthcare worker statistics
• Medical equipment inventory
• Patient satisfaction scores
• Emergency department metrics
• Vaccination coverage rates

This data supports healthcare planning, resource allocation, and public health research.`,
    dataColumns: [
      {
        name: 'Hospital ID',
        type: 'String',
        description: 'Unique hospital identifier',
      },
      {
        name: 'Beds Available',
        type: 'Number',
        description: 'Total hospital beds',
      },
      {
        name: 'Occupancy Rate',
        type: 'Number',
        description: 'Percentage occupancy',
      },
      {
        name: 'Patients Admitted',
        type: 'Number',
        description: 'Monthly admissions',
      },
      {
        name: 'Staff Count',
        type: 'Number',
        description: 'Total healthcare staff',
      },
      { name: 'Date', type: 'Date', description: 'Data collection date' },
    ],
    fileSize: '32.8 MB',
    recordCount: '89,000+',
    updateFrequency: 'Weekly',
    license: 'Creative Commons Attribution 4.0',
    contact: 'health@data.gov',
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
    fullDescription: `Comprehensive financial market data including:

• Stock prices and trading volumes
• Market indices and trends
• Currency exchange rates
• Commodity prices
• Interest rates
• Market volatility indicators
• Trading patterns and anomalies
• Economic indicators

Real-time and historical data for financial analysis and research.`,
    dataColumns: [
      { name: 'Symbol', type: 'String', description: 'Stock ticker symbol' },
      { name: 'Open Price', type: 'Number', description: 'Opening price' },
      { name: 'Close Price', type: 'Number', description: 'Closing price' },
      { name: 'Volume', type: 'Number', description: 'Trading volume' },
      { name: 'Date', type: 'Date', description: 'Trading date' },
      {
        name: 'Market Cap',
        type: 'Number',
        description: 'Market capitalization',
      },
    ],
    fileSize: '128.5 MB',
    recordCount: '500,000+',
    updateFrequency: 'Daily',
    license: 'Commercial License',
    contact: 'finance@data.gov',
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
    fullDescription: `Education statistics covering:

• Student enrollment by level and institution
• Graduation and completion rates
• Student demographics
• Faculty information
• Course offerings and enrollment
• Educational outcomes
• Scholarship and financial aid data
• School performance metrics

Essential for education planning and policy development.`,
    dataColumns: [
      {
        name: 'Institution',
        type: 'String',
        description: 'School/University name',
      },
      {
        name: 'Students Enrolled',
        type: 'Number',
        description: 'Total enrollment',
      },
      {
        name: 'Graduation Rate',
        type: 'Number',
        description: 'Percentage graduated',
      },
      {
        name: 'Faculty Count',
        type: 'Number',
        description: 'Number of faculty',
      },
      { name: 'Year', type: 'Number', description: 'Academic year' },
      { name: 'Level', type: 'String', description: 'Education level' },
    ],
    fileSize: '18.3 MB',
    recordCount: '45,000+',
    updateFrequency: 'Annually',
    license: 'Creative Commons Attribution 4.0',
    contact: 'education@data.gov',
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
    fullDescription: `Transportation data including:

• Traffic flow and congestion patterns
• Public transportation ridership
• Vehicle registration and types
• Road network information
• Accident and incident data
• Parking availability
• Transit schedules and performance
• Commute patterns

Supports urban planning and transportation optimization.`,
    dataColumns: [
      {
        name: 'Route ID',
        type: 'String',
        description: 'Transportation route identifier',
      },
      { name: 'Vehicles', type: 'Number', description: 'Number of vehicles' },
      { name: 'Passengers', type: 'Number', description: 'Passenger count' },
      {
        name: 'Congestion Level',
        type: 'String',
        description: 'Traffic congestion level',
      },
      { name: 'Date', type: 'Date', description: 'Data collection date' },
      { name: 'Time', type: 'String', description: 'Time of day' },
    ],
    fileSize: '67.4 MB',
    recordCount: '200,000+',
    updateFrequency: 'Real-time',
    license: 'Creative Commons Attribution 4.0',
    contact: 'transport@data.gov',
  },
  {
    id: 6,
    name: 'Environmental Monitoring',
    category: 'Environment',
    description: 'Air quality and environmental sensor readings',
    downloads: 945,
    views: 4120,
    updated: '2024-12-14',
    format: 'JSON, NetCDF',
    fullDescription: `Environmental monitoring data featuring:

• Air quality measurements (PM2.5, PM10, O3, NO2)
• Temperature and humidity readings
• Water quality parameters
• Soil composition data
• Vegetation indices
• Weather patterns
• Pollution source tracking
• Environmental health indicators

Critical for environmental protection and public health.`,
    dataColumns: [
      {
        name: 'Station ID',
        type: 'String',
        description: 'Monitoring station identifier',
      },
      {
        name: 'PM2.5',
        type: 'Number',
        description: 'Fine particulate matter (µg/m³)',
      },
      { name: 'Temperature', type: 'Number', description: 'Temperature (°C)' },
      {
        name: 'Humidity',
        type: 'Number',
        description: 'Relative humidity (%)',
      },
      { name: 'Date', type: 'Date', description: 'Measurement date' },
      { name: 'AQI', type: 'Number', description: 'Air Quality Index' },
    ],
    fileSize: '54.1 MB',
    recordCount: '150,000+',
    updateFrequency: 'Hourly',
    license: 'Creative Commons Attribution 4.0',
    contact: 'environment@data.gov',
  },
]);

// Tìm dataset theo id
const dataset = computed(() =>
  datasets.value.find(d => d.id === datasetId.value)
);

// SEO động
useSeoMeta({
  title: () => (dataset.value ? dataset.value.name : 'Dataset not found'),
  description: () => dataset.value?.description ?? 'Dataset detail',
});
</script>

<template>
  <div>
    <Navigation />

    <main class="bg-background min-h-screen">
      <!-- Not found -->
      <section
        v-if="!dataset"
        class="flex min-h-[60vh] items-center justify-center"
      >
        <div class="text-center">
          <h1 class="mb-4 text-2xl font-bold">Dataset not found</h1>
          <NuxtLink to="/data" class="text-primary hover:underline">
            Back to datasets
          </NuxtLink>
        </div>
      </section>

      <!-- Header -->
      <section
        v-else
        class="from-primary via-accent to-secondary text-primary-foreground bg-linear-to-r py-8"
      >
        <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <NuxtLink
            to="/data"
            class="mb-4 flex w-fit items-center gap-2 transition-opacity hover:opacity-80"
          >
            <Icon name="lucide:arrow-left" class="h-5 w-5" />
            <span>Back to Datasets</span>
          </NuxtLink>
          <h1 class="mb-2 text-4xl font-bold text-white">
            {{ dataset!.name }}
          </h1>
          <p class="text-lg text-white opacity-90">
            {{ dataset!.description }}
          </p>
        </div>
      </section>

      <!-- Content -->
      <div v-if="dataset" class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 gap-8 lg:grid-cols-3">
          <!-- Main Content -->
          <div class="space-y-8 lg:col-span-2">
            <!-- Overview -->
            <div class="bg-card border-border rounded-lg border p-6">
              <h2 class="mb-4 text-2xl font-bold">Overview</h2>
              <p class="text-muted-foreground whitespace-pre-line">
                {{ dataset.fullDescription }}
              </p>
            </div>

            <!-- Data Structure -->
            <div class="bg-card border-border rounded-lg border p-6">
              <h2 class="mb-4 text-2xl font-bold">Data Structure</h2>
              <div class="overflow-x-auto">
                <table class="w-full text-sm">
                  <thead>
                    <tr class="border-border border-b">
                      <th class="px-4 py-3 text-left font-semibold">
                        Column Name
                      </th>
                      <th class="px-4 py-3 text-left font-semibold">
                        Data Type
                      </th>
                      <th class="px-4 py-3 text-left font-semibold">
                        Description
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(col, idx) in dataset.dataColumns"
                      :key="idx"
                      class="border-border hover:bg-muted/50 border-b"
                    >
                      <td class="text-primary px-4 py-3 font-mono">
                        {{ col.name }}
                      </td>
                      <td class="text-muted-foreground px-4 py-3">
                        {{ col.type }}
                      </td>
                      <td class="text-muted-foreground px-4 py-3">
                        {{ col.description }}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Sidebar -->
          <div class="space-y-6 lg:col-span-1">
            <!-- Quick Stats -->
            <div class="bg-card border-border rounded-lg border p-6">
              <h3 class="mb-4 text-lg font-bold">Statistics</h3>
              <div class="space-y-4">
                <div class="flex items-center gap-3">
                  <Icon name="lucide:download" class="text-primary h-5 w-5" />
                  <div>
                    <p class="text-muted-foreground text-sm">Downloads</p>
                    <p class="font-bold">
                      {{ dataset.downloads.toLocaleString() }}
                    </p>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <Icon name="lucide:eye" class="text-primary h-5 w-5" />
                  <div>
                    <p class="text-muted-foreground text-sm">Views</p>
                    <p class="font-bold">
                      {{ dataset.views.toLocaleString() }}
                    </p>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <Icon name="lucide:calendar" class="text-primary h-5 w-5" />
                  <div>
                    <p class="text-muted-foreground text-sm">Last Updated</p>
                    <p class="font-bold">{{ dataset.updated }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Dataset Info -->
            <div class="bg-card border-border rounded-lg border p-6">
              <h3 class="mb-4 text-lg font-bold">Dataset Info</h3>
              <div class="space-y-3 text-sm">
                <div>
                  <p class="text-muted-foreground">Category</p>
                  <p class="text-primary font-semibold">
                    {{ dataset.category }}
                  </p>
                </div>
                <div>
                  <p class="text-muted-foreground">Format</p>
                  <p class="font-semibold">{{ dataset.format }}</p>
                </div>
                <div>
                  <p class="text-muted-foreground">File Size</p>
                  <p class="font-semibold">{{ dataset.fileSize }}</p>
                </div>
                <div>
                  <p class="text-muted-foreground">Records</p>
                  <p class="font-semibold">{{ dataset.recordCount }}</p>
                </div>
                <div>
                  <p class="text-muted-foreground">Update Frequency</p>
                  <p class="font-semibold">{{ dataset.updateFrequency }}</p>
                </div>
                <div>
                  <p class="text-muted-foreground">License</p>
                  <p class="font-semibold">{{ dataset.license }}</p>
                </div>
              </div>
            </div>

            <!-- Contact -->
            <div class="bg-card border-border rounded-lg border p-6">
              <h3 class="mb-4 text-lg font-bold">Contact</h3>
              <p class="text-muted-foreground mb-4 text-sm">
                For questions or support:
              </p>
              <p class="text-primary font-mono text-sm">
                {{ dataset.contact }}
              </p>
            </div>

            <!-- Actions -->
            <div class="space-y-3">
              <button
                class="bg-primary text-primary-foreground flex w-full items-center justify-center gap-2 rounded-lg px-4 py-3 font-semibold text-white transition-opacity hover:opacity-90"
              >
                <Icon name="lucide:download" class="h-5 w-5" />
                Download Dataset
              </button>
              <button
                class="border-border hover:bg-muted flex w-full items-center justify-center gap-2 rounded-lg border px-4 py-3 font-semibold transition-colors"
              >
                <Icon name="lucide:share-2" class="h-5 w-5" />
                Share
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>
