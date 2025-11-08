export default defineAppConfig({
  title: 'DTI Predictor',
  cookieKeys: {
    accessToken: 'open-linked-hub:access-token',
    refreshToken: 'open-linked-hub:refresh-token',
  },
  pages: {
    dashboard: {
      path: '/',
      title: 'Dashboard',
    },
    diagnose: {
      path: '/diagnose',
      title: 'Chẩn đoán',
    },
    simulator: {
      path: '/simulator',
      title: 'Mô phỏng',
    },
    'not-found': {
      path: '/:pathMatch(.*)*',
      title: 'Không tìm thấy',
    },
  },
});
