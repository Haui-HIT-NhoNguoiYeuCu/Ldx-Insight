export default defineAppConfig({
  title: 'Open Linked Hub',
  cookieKeys: {
    accessToken: 'open-linked-hub:access-token',
    refreshToken: 'open-linked-hub:refresh-token',
  },
  pages: {
    home: {
      path: '/',
      name: 'Trang chủ',
    },
    data: {
      path: '/data',
      name: 'Dữ liệu',
    },
    login: {
      path: '/login',
      title: 'Đăng nhập',
    },
    register: {
      path: '/register',
      title: 'Đăng ký',
    },
    profile: {
      path: '/users/:id',
      title: 'Trang cá nhân',
    },
    'not-found': {
      path: '/:pathMatch(.*)*',
      title: 'Không tìm thấy',
    },
  },
});
