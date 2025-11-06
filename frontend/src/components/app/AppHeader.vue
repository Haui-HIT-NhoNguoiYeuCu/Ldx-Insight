<template>
  <nav class="sticky top-0 z-50 border-b border-gray-400 bg-white">
    <div class="mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex h-16 items-center justify-between">
        <!-- Logo -->
        <NuxtLink
          to="/"
          class="text-primary flex items-center gap-2 text-xl font-bold"
        >
          <Icon name="lucide:database" class="h-6 w-6" />
          <span>Open Linked Hub</span>
        </NuxtLink>

        <!-- Menu -->
        <div class="flex items-center gap-8">
          <NuxtLink
            to="/"
            :class="[
              'text-base font-medium transition-colors',
              isActive('/')
                ? 'text-primary'
                : 'text-foreground hover:text-primary',
            ]"
          >
            Trang chủ
          </NuxtLink>

          <NuxtLink
            to="/data"
            :class="[
              'text-base font-medium transition-colors',
              isActive('/data')
                ? 'text-primary'
                : 'text-foreground hover:text-primary',
            ]"
          >
            Dữ liệu
          </NuxtLink>

          <!-- <NuxtLink
            to="/instructions"
            :class="[
              'text-sm font-medium transition-colors',
              isActive('/instructions')
                ? 'text-primary'
                : 'text-foreground hover:text-primary',
            ]"
          >
            Instructions
          </NuxtLink>

          <NuxtLink
            to="/news"
            :class="[
              'text-sm font-medium transition-colors',
              isActive('/news')
                ? 'text-primary'
                : 'text-foreground hover:text-primary',
            ]"
          >
            News
          </NuxtLink> -->
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router';

const route = useRoute();

const isActive = (path: string) => route.path === path;
</script>
