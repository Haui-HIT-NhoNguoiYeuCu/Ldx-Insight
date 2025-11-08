<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.pages.diagnose.title });
definePageMeta({ layout: 'default' });

// 1. Import (giữ nguyên)
import { Bar } from 'vue-chartjs';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
} from 'chart.js';

// 2. Đăng ký Chart.js (giữ nguyên)
ChartJS.register(
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale
);

// 3. 🌟 GỌI API METADATA 🌟
const api = useApi();
const { data: metadataRes, pending } = await useAsyncData(
  'metadata',
  () => api.metadata(),
  {
    immediate: true,
  }
);

// 4. 🌟 TẠO ÁNH XẠ TÊN TỈNH (API -> Tên Tiếng Việt) 🌟
// Chúng ta chỉ định nghĩa 4 tỉnh bạn yêu cầu
const provinceNameMapping: { [key: string]: string } = {
  DaNang: 'Đà Nẵng',
  DongThap: 'Đồng Tháp',
  HoChiMinh: 'TP. Hồ Chí Minh',
  ThanhHoa: 'Thanh Hoá',
};

// 5. 🌟 TẠO DANH SÁCH TỈNH TỪ API 🌟
const provinces = computed(() => {
  const data = metadataRes.value?.comparison_2024 || [];
  // Lọc và chuyển đổi tên tỉnh
  return data.map(item => provinceNameMapping[item.province]).filter(Boolean); // Lọc bỏ bất kỳ tỉnh nào không có trong mapping
});

// 6. 🌟 STATE: Đặt tỉnh đầu tiên làm mặc định 🌟
const selectedProvince = ref(provinces.value[0] || 'Đà Nẵng');

// 7. 🌟 DỮ LIỆU PHÁI SINH (computed) TỪ API 🌟
// Đây là trái tim của logic, thay thế hoàn toàn 'diagnoseData' cứng
const data = computed(() => {
  const apiDataList = metadataRes.value?.comparison_2024;
  if (!apiDataList) {
    // Trả về cấu trúc rỗng nếu API chưa tải xong
    return {
      potential: 0,
      actual: 0,
      gap: 0,
      indicators: [],
      recommendations: [],
    };
  }

  // Tìm "key" của API (ví dụ: "HoChiMinh") từ tên Tiếng Việt (ví dụ: "TP. Hồ Chí Minh")
  const apiKey = Object.keys(provinceNameMapping).find(
    key => provinceNameMapping[key] === selectedProvince.value
  );

  // Tìm đúng đối tượng tỉnh trong mảng API
  const apiData = apiDataList.find(item => item.province === apiKey);

  if (!apiData) {
    return {
      potential: 0,
      actual: 0,
      gap: 0,
      indicators: [],
      recommendations: [],
    };
  }

  // Ánh xạ dữ liệu API sang cấu trúc mà template đang dùng
  return {
    // Thẻ thống kê (API là 0.83, template muốn 83.3)
    potential: (apiData.DTI_pred_2024 * 100).toFixed(1),
    actual: (apiData.DTI_true_2024 * 100).toFixed(1),
    gap: (apiData.abs_error * 100).toFixed(1),

    // Biểu đồ cột (theo yêu cầu của bạn)
    indicators: [
      { name: 'Chính quyền số', value: (apiData.CQS_pred * 100).toFixed(1) },
      { name: 'Kinh tế số', value: (apiData.KTS_pred * 100).toFixed(1) },
      { name: 'Xã hội số', value: (apiData.XHS_pred * 100).toFixed(1) }, // Giả định KTS_pred thứ 2 là XHS_pred
    ],
  };
});

// 8. Chuyển đổi dữ liệu cho Bar Chart (Giữ nguyên)
// (Logic này tự động đọc từ 'data.value.indicators' đã được cập nhật ở trên)
const barChartData = computed(() => ({
  labels: data.value.indicators.map(d => d.name),
  datasets: [
    {
      label: 'Điểm số',
      data: data.value.indicators.map(d => d.value),
      backgroundColor: '#00d4ff',
      borderRadius: 4,
    },
  ],
}));

// 9. Cấu hình biểu đồ (Giữ nguyên)
const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
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

      <h2 class="mb-8 text-3xl font-bold text-white">
        Chẩn đoán Tiềm năng Chuyển đổi Số
      </h2>

      <div v-if="pending" class="text-center text-slate-400">
        <Icon name="line-md:loading-loop" class="h-8 w-8" />
        <p>Đang tải dữ liệu metadata...</p>
      </div>

      <div v-else>
        <div class="mb-8">
          <label class="mb-3 block text-sm font-medium text-slate-300"
            >Chọn Tỉnh/Thành phố</label
          >
          <select
            v-model="selectedProvince"
            class="w-full rounded-lg border border-slate-700 bg-slate-800 px-4 py-2 text-white transition focus:border-cyan-400 focus:outline-none md:w-64"
          >
            <option
              v-for="province in provinces"
              :key="province"
              :value="province"
            >
              {{ province }}
            </option>
          </select>
        </div>

        <div class="mb-8 grid grid-cols-1 gap-6 md:grid-cols-3">
          <div
            class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
          >
            <p class="text-sm text-slate-400">Tiềm năng</p>
            <p class="mt-2 text-3xl font-bold text-cyan-400">
              {{ data.potential }}%
            </p>
          </div>
          <div
            class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
          >
            <p class="text-sm text-slate-400">Thực tế</p>
            <p class="mt-2 text-3xl font-bold text-blue-400">
              {{ data.actual }}%
            </p>
          </div>
          <div
            class="rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
          >
            <p class="text-sm text-slate-400">Khoảng cách</p>
            <p class="mt-2 text-3xl font-bold text-orange-400">
              {{ data.gap }}%
            </p>
          </div>
        </div>

        <div
          class="mb-8 rounded-lg border border-slate-700 bg-slate-800/50 p-6 backdrop-blur-sm"
        >
          <h3 class="mb-4 text-lg font-semibold text-white">
            Các trụ cột (CQS, KTS, XHS)
          </h3>
          <div class="h-[300px]">
            <Bar :data="barChartData" :options="chartOptions" />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
