export interface User {
  id: number;
  name: string;
  email: string;
}

export interface UserResponse {
  id: string | undefined;
  name: string | undefined;
  email: string | undefined;
  avatar?: string | undefined;
}
