interface ITokenStore {
  accessToken?: string;
  refreshToken?: string;
}

export const useAuthStore = defineStore('auth', {
  state: (): ITokenStore => {
    const appConfig = useAppConfig();
    return {
      accessToken: getCookie(appConfig.cookieKeys.accessToken) || undefined,
      refreshToken: getCookie(appConfig.cookieKeys.refreshToken) || undefined,
    };
  },

  getters: { loggedIn: ({ accessToken }): boolean => !!accessToken },
  actions: {
    logIn(
      { accessToken, refreshToken }: LoginResponse['data'],
      redirect: boolean = true
    ) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
      const appConfig = useAppConfig();
      setCookie(appConfig.cookieKeys.accessToken, accessToken);
      setCookie(appConfig.cookieKeys.refreshToken, refreshToken);
      if (redirect)
        navigateTo(
          (useRoute().query.redirect as string) || appConfig.pages.home.path
        );
    },
    logOut(options?: { redirect?: string }): void {
      const appConfig = useAppConfig();
      removeCookie(appConfig.cookieKeys.accessToken);
      removeCookie(appConfig.cookieKeys.refreshToken);
      this.accessToken = '';
      this.refreshToken = '';
      if (options && options.redirect)
        navigateTo({
          path: appConfig.pages.login.path,
          query: { redirect: options.redirect },
        });
      else navigateTo(appConfig.pages.login.path);
    },
  },
});

if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useAuthStore, import.meta.hot));
}
