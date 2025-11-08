<script setup lang="ts">
import { useRoute } from 'vue-router';

const authStore = useAuthStore();
const { loggedIn } = storeToRefs(authStore);
const route = useRoute();

const isActive = (path: string) => route.path === path;

const onLogout = () => {
  authStore.logOut();
};
</script>
<template>
  <header
    class="sticky top-0 z-50 border-b border-slate-800 bg-slate-950 backdrop-blur-sm"
  >
    <div class="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-lg bg-linear-to-br from-cyan-400 to-blue-600"
          >
            <Icon name="lucide:zap" class="h-6 w-6 text-white" />
          </div>
          <h1 class="text-2xl font-bold text-white">DTI Predictor</h1>
        </div>
        <nav class="hidden gap-8 md:flex">
          <NuxtLink to="/" class="font-medium text-cyan-400">
            Dashboard
          </NuxtLink>
          <NuxtLink
            to="/diagnose"
            class="text-slate-400 transition hover:text-white"
          >
            Chẩn đoán
          </NuxtLink>
          <!-- <NuxtLink
            to="/simulator"
            class="text-slate-400 transition hover:text-white"
          >
            Mô phỏng
          </NuxtLink> -->
        </nav>
      </div>
    </div>
  </header>
</template>
