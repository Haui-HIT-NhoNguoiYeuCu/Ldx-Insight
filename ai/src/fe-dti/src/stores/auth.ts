interface ITokenStore {
  accessToken?: string;
}

export const useAuthStore = defineStore('auth', {
  state: (): ITokenStore => {
    const appConfig = useAppConfig();
    return {
      accessToken: getCookie(appConfig.cookieKeys.accessToken) || undefined,
    };
  },
  getters: { loggedIn: ({ accessToken }): boolean => !!accessToken },
  actions: {
    logIn(token: string, redirect: boolean = true) {
      this.accessToken = token;
      const appConfig = useAppConfig();
      setCookie(appConfig.cookieKeys.accessToken, this.accessToken);
      if (redirect)
        navigateTo(
          (useRoute().query.redirect as string) || appConfig.pages.home.path
        );
    },
    logOut(options?: { redirect?: string }): void {
      const appConfig = useAppConfig();
      removeCookie(appConfig.cookieKeys.accessToken);
      this.accessToken = undefined;
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
