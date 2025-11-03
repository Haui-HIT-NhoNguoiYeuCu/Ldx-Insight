export interface LoginRequest {
  email: string;
  password: string;
}
export type LoginResponse = ApiResponse<{
  accessToken: string;
  refreshToken: string;
}>;
