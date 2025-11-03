export const useApi = () => {
  const { $http } = useNuxtApp();

  return {
    auth: {
      login: (body: LoginRequest) =>
        $http<LoginResponse>('/auth/login', { method: 'POST', body }),
    },

    user: {
      getUsers: () => {
        const url = `/users`;
        return $http<UserResponse>(url);
      },
      getUserInfo: (studentCode?: string) => {
        const url = studentCode ? `/users/${studentCode}` : '/users/me';
        return $http<UserResponse>(url);
      },
      getUserById: (id: string | number) => $http<UserResponse>(`/users/${id}`),
    },

    dataset: {
      list: () => $http<Dataset[]>('/datasets'),
      detail: (id: string | number) => $http<Dataset>(`/datasets/${id}`),
    },
  };
};
