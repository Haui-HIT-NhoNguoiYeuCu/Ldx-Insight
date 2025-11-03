import type { RouteLocationNormalized } from 'vue-router';

export default defineNuxtRouteMiddleware(
  (to: RouteLocationNormalized, from: RouteLocationNormalized) => {
    console.log('Auth middleware:', to.path);
  }
);
