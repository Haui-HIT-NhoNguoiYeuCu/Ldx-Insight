import type { CookieOptions } from '#app';

export function getCookie(key: string, d?: string) {
  const cookie = d ? useCookie(key, { default: () => d }) : useCookie(key);
  return cookie.value;
}

export function setCookie(
  key: string,
  value: string,
  options?: CookieOptions
): void {
  const cookie = useCookie(key, { ...options, readonly: false });
  cookie.value = value;
}

export function removeCookie(key: string): void {
  const cookie = useCookie(key);
  cookie.value = undefined;
}
