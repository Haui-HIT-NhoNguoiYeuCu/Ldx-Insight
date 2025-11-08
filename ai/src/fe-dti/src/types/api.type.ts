export type SuccessMessageResponse = 'success';
export type ErrorMessageResponse = 'error';
export type MessageResponse = SuccessMessageResponse | ErrorMessageResponse;

export interface ApiResponse<T = any> {
  code: number;
  message: MessageResponse;
  data: T;
  error: string;
}

export interface ApiErrorResponse {
  code: number;
  message: string;
  stack?: string;
}
