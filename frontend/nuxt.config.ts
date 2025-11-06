export default defineNuxtConfig({
  srcDir: 'src/',
  modules: ['@nuxt/ui', '@pinia/nuxt', '@vueuse/nuxt', '@nuxt/image'],
  css: ['~/assets/css/main.css'],
  dir: { public: '../public' },
  ui: { global: true },
  imports: { dirs: ['stores', 'types'] },
  plugins: ['~/plugins/http.ts'],
  runtimeConfig: {
    public: {
      apiBase: process.env.API_BASE_URL || '/api',
    },
  },
});
