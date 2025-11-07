export const useApi = () => {
  const { $http } = useNuxtApp();
  const config = useRuntimeConfig();
  const API_BASE = config.public.apiBase;

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
      category: () => {
        return $http<string[]>(`${API_BASE}/datasets/categories`);
      },
      list: (params?: DatasetRequestParams) =>
        $http<DatasetResponse>(`${API_BASE}/datasets`, {
          query: params,
        }),
      detail: (id: string | number) => $http<Dataset>(`/datasets/${id}`),
    },
  };
};
