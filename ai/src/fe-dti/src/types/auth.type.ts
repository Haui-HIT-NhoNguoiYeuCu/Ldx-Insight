export interface LoginRequest {
  username: string;
  password: string;
}
export type AuthResponse = ApiResponse<{
  token: string;
}>;
