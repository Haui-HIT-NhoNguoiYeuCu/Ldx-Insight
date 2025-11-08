export const useApi = () => {
  const { $http } = useNuxtApp();
  const config = useRuntimeConfig();
  const API_BASE = config.public.apiBase;

  return {
    metadata: () => $http(`${API_BASE}/metadata`),
    register: (body: LoginRequest) =>
      $http<AuthResponse>(`${API_BASE}/auth/register`, {
        method: 'POST',
        body,
      }),
  };
};
