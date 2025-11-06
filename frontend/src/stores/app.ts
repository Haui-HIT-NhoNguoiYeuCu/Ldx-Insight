import { defineStore } from 'pinia';

export interface AppState {
  theme: 'light' | 'dark';
}

export const useAppStore = defineStore('app', {
  state: (): AppState => ({
    theme: 'light',
  }),
  actions: {
    toggleTheme() {
      this.theme = this.theme === 'light' ? 'dark' : 'light';
    },
  },
});
