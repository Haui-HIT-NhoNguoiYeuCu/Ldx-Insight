<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.pages.simulator.name });
definePageMeta({ layout: 'default' });

// 1. Import các component biểu đồ từ vue-chartjs
import { Line } from 'vue-chartjs';
// 2. Import và đăng ký các thành phần của Chart.js
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  PointElement,
  LineElement,
  CategoryScale,
  LinearScale,
} from 'chart.js';

// 3. Đăng ký các thành phần
ChartJS.register(
  Title,
  Tooltip,
  Legend,
  PointElement,
  LineElement,
  CategoryScale,
  LinearScale
);

// 4. State (dùng ref)
const infrastructure = ref(70);
const humanResources = ref(65);
const finance = ref(60);
const policy = ref(75);

// 5. Dữ liệu phái sinh (dùng computed, thay cho useMemo)
const prediction = computed(() => {
  const avg =
    (infrastructure.value +
      humanResources.value +
      finance.value +
      policy.value) /
    4;
  return Math.round(avg);
});

// Dữ liệu gốc cho biểu đồ
const trendData = computed(() => {
  return Array.from({ length: 12 }, (_, i) => ({
    month: `T${i + 1}`,
    value: Math.round(prediction.value * (0.7 + (i / 12) * 0.3)),
  }));
});

// 6. Chuyển đổi dữ liệu cho Line Chart
const lineChartData = computed(() => ({
  labels: trendData.value.map(d => d.month),
  datasets: [
    {
      label: 'Tiềm năng (%)',
      data: trendData.value.map(d => d.value),
      borderColor: '#00d4ff',
      pointBackgroundColor: '#00d4ff',
      pointRadius: 5,
      pointHoverRadius: 7,
      tension: 0.3, // Gần giống 'monotone'
    },
  ],
}));

// 7. Cấu hình cho biểu đồ (theme tối)
const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false, // Rất quan trọng để giữ chiều cao
  plugins: {
    legend: {
      display: false, // Biểu đồ gốc không có legend
    },
    tooltip: {
      backgroundColor: '#1e293b',
      borderColor: '#475569',
      borderWidth: 1,
      titleColor: '#e2e8f0',
      bodyColor: '#e2e8f0',
    },
  },
  scales: {
    x: {
      ticks: { color: '#94a3b8' },
      grid: { color: '#334155' },
    },
    y: {
      ticks: { color: '#94a3b8' },
      grid: { color: '#334155' },
    },
  },
}));
</script>

<template>
  <div
    class="min-h-screen bg-linear-to-br from-slate-950 via-slate-900 to-slate-950"
  >
    <main class="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <NuxtLink
        to="/"
        class="mb-6 flex items-center gap-2 text-cyan-400 transition hover:text-cyan-300"
      >
        <Icon name="lucide:arrow-left" class="h-4 w-4" />
        Quay lại Dashboard
      </NuxtLink>

      <h2 class="mb-8 text-3xl font-bold text-white">Mô phỏng Dự đoán</h2>

      <div class="grid grid-cols-1 gap-8 lg:grid-cols-2">
        <div
          class="h-fit rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
        >
          <h3
            class="mb-6 flex items-center gap-2 text-lg font-semibold text-white"
          >
            <Icon name="lucide:sliders" class="h-5 w-5 text-cyan-400" />
            Điều chỉnh Chỉ số
          </h3>

          <div class="space-y-6">
            <div>
              <div class="mb-2 flex justify-between">
                <label class="text-sm font-medium text-slate-300"
                  >Cơ sở hạ tầng</label
                >
                <span class="font-bold text-cyan-400"
                  >{{ infrastructure }}%</span
                >
              </div>
              <input
                type="range"
                min="0"
                max="100"
                v-model.number="infrastructure"
                class="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-700 accent-cyan-400"
              />
            </div>

            <div>
              <div class="mb-2 flex justify-between">
                <label class="text-sm font-medium text-slate-300"
                  >Nhân lực</label
                >
                <span class="font-bold text-cyan-400"
                  >{{ humanResources }}%</span
                >
              </div>
              <input
                type="range"
                min="0"
                max="100"
                v-model.number="humanResources"
                class="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-700 accent-cyan-400"
              />
            </div>

            <div>
              <div class="mb-2 flex justify-between">
                <label class="text-sm font-medium text-slate-300"
                  >Tài chính</label
                >
                <span class="font-bold text-cyan-400">{{ finance }}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                v-model.number="finance"
                class="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-700 accent-cyan-400"
              />
            </div>

            <div>
              <div class="mb-2 flex justify-between">
                <label class="text-sm font-medium text-slate-300"
                  >Chính sách</label
                >
                <span class="font-bold text-cyan-400">{{ policy }}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                v-model.number="policy"
                class="h-2 w-full cursor-pointer appearance-none rounded-lg bg-slate-700 accent-cyan-400"
              />
            </div>
          </div>

          <div class="mt-8 border-t border-slate-700 pt-6">
            <p class="mb-2 text-sm text-slate-400">Dự đoán Tiềm năng</p>
            <p class="text-4xl font-bold text-cyan-400">{{ prediction }}%</p>
            <p class="mt-2 text-sm text-slate-400">
              {{
                prediction >= 80
                  ? '🚀 Rất cao'
                  : prediction >= 60
                    ? '📈 Trung bình'
                    : '⚠️ Cần cải thiện'
              }}
            </p>
          </div>
        </div>

        <div
          class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
        >
          <h3 class="mb-4 text-lg font-semibold text-white">
            Xu hướng 12 tháng
          </h3>
          <div class="h-[400px]">
            <Line :data="lineChartData" :options="chartOptions" />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
