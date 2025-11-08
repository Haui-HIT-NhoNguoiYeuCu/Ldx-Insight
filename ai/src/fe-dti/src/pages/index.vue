<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.title });
definePageMeta({ layout: 'default' });

import { Bar } from 'vue-chartjs';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
} from 'chart.js';

ChartJS.register(
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement
);

const api = useApi();
const { data: metadataRes } = await useAsyncData(
  'metadata',
  () => api.metadata(),
  {
    immediate: true,
  }
);

const barChartData = computed(() => {
  const data = metadataRes.value?.comparison_2024 || [];

  return {
    labels: data.map(d => d.province),
    datasets: [
      {
        label: 'Tiềm năng',
        data: data.map(d => d.DTI_pred_2024 * 100),
        backgroundColor: '#00d4ff',
        borderRadius: 4,
      },
      {
        label: 'Thực tế',
        data: data.map(d => d.DTI_true_2024 * 100),
        backgroundColor: '#0066cc',
        borderRadius: 4,
      },
    ],
  };
});

const averagePotential = computed(() => {
  const data = metadataRes.value?.comparison_2024;
  if (!data || data.length === 0) {
    return '0.0%';
  }
  const total = data.reduce((sum, item) => sum + item.DTI_pred_2024, 0);
  return ((total / data.length) * 100).toFixed(1) + '%';
});

const provinceCount = computed(() => {
  return metadataRes.value?.comparison_2024?.length || 0;
});
</script>

<template>
  <div
    class="min-h-screen bg-linear-to-br from-slate-950 via-slate-900 to-slate-950"
  >
    <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div class="mb-8 grid grid-cols-1 gap-6 md:grid-cols-3">
        <div
          class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm transition hover:border-cyan-500/50"
        >
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-400">Trung bình Tiềm năng</p>
              <p class="mt-2 text-3xl font-bold text-white">
                {{ averagePotential }}
              </p>
            </div>
            <Icon
              name="lucide:trending-up"
              class="h-12 w-12 text-cyan-400 opacity-20"
            />
          </div>
        </div>
        <div
          class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm transition hover:border-cyan-500/50"
        >
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-400">Tỉnh/Thành phố</p>
              <p class="mt-2 text-3xl font-bold text-white">
                {{ provinceCount }}
              </p>
            </div>
            <Icon
              name="lucide:target"
              class="h-12 w-12 text-blue-400 opacity-20"
            />
          </div>
        </div>
        <div
          class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm transition hover:border-cyan-500/50"
        >
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm text-slate-400">Cập nhật gần đây</p>
              <p class="mt-2 text-3xl font-bold text-white">Hôm nay</p>
            </div>
            <Icon
              name="lucide:zap"
              class="h-12 w-12 text-green-400 opacity-20"
            />
          </div>
        </div>
      </div>
      <div class="mb-8 grid grid-cols-1 gap-6">
        <div
          class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
        >
          <h2 class="mb-4 text-lg font-semibold text-white">
            Tiềm năng theo Tỉnh/Thành phố
          </h2>
          <div class="h-[300px]">
            <Bar :data="barChartData" />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
