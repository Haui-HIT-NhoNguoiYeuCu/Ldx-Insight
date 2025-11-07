<script setup lang="ts">
const appConfig = useAppConfig();
useSeoMeta({ titleTemplate: appConfig.pages.register.title });
definePageMeta({ layout: 'blank' });

const state = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});

const errors = reactive<{
  username?: string;
  password?: string;
  confirmPassword?: string;
}>({});
const submitting = ref(false);
const showPassword = ref(false);
const toast = useToast();
const authStore = useAuthStore();
const api = useApi();

function validate() {
  errors.username = '';
  errors.password = '';
  errors.confirmPassword = '';

  if (!state.username) {
    errors.username = 'Tài khoản không được để trống';
  }

  if (!state.password) {
    errors.password = 'Mật khẩu không được để trống';
  } else if (state.password.length < 6) {
    errors.password = 'Mật khẩu phải có ít nhất 6 kí tự';
  }

  if (state.password !== state.confirmPassword) {
    errors.confirmPassword = 'Mật khẩu không khớp';
  }

  return !errors.username && !errors.password && !errors.confirmPassword;
}

async function onSubmit() {
  if (!validate()) return;
  try {
    submitting.value = true;

    const response = await api.auth.register({
      username: state.username,
      password: state.password,
    });

    authStore.logIn(response.token);

    toast.add({
      title: 'Đăng ký thành công!',
      description: 'Đã tự động đăng nhập và chuyển hướng về trang chủ.',
      color: 'success',
      icon: 'i-lucide-check-circle',
    });
    submitting.value = false;
  } catch (err: any) {
    console.log(err);
    toast.add({
      title: 'Đăng ký thất bại',
      description: 'Tài khoản này có thể đã tồn tại.',
      color: 'error',
      icon: 'i-lucide-alert-circle',
    });
    submitting.value = false;
  }
}
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <main class="flex-1">
      <div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <NuxtLink
          to="/"
          class="text-muted-foreground hover:text-foreground mb-8 inline-flex items-center gap-2 text-sm"
        >
          <Icon name="lucide:arrow-left" class="h-4 w-4" />
          Quay lại Trang chủ
        </NuxtLink>

        <div class="mx-auto max-w-md">
          <UCard>
            <template #header>
              <div class="text-center">
                <h1 class="text-foreground text-3xl font-bold">
                  Tạo tài khoản
                </h1>
                <p class="text-muted-foreground mt-1">Hãy thực hiện đăng ký</p>
              </div>
            </template>

            <UForm :state="state" @submit="onSubmit" class="space-y-6">
              <!-- Username -->
              <UFormField
                label="Tài khoản"
                name="username"
                :error="errors.username"
              >
                <UInput
                  v-model="state.username"
                  placeholder="Nhập tài khoản"
                  icon="i-lucide:user"
                  class="w-full"
                  @keydown.enter.prevent="onSubmit"
                />
              </UFormField>

              <!-- Password -->
              <UFormField
                label="Mật khẩu"
                name="password"
                :error="errors.password"
              >
                <div class="relative">
                  <UInput
                    v-model="state.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="Enter your password"
                    icon="i-lucide:lock"
                    class="w-full"
                    @keydown.enter.prevent="onSubmit"
                  />
                  <UButton
                    color="gray"
                    variant="ghost"
                    size="xs"
                    class="absolute top-1/2 right-1 -translate-y-1/2"
                    :icon="showPassword ? 'i-lucide:eye-off' : 'i-lucide:eye'"
                    @click="showPassword = !showPassword"
                    aria-label="Toggle password"
                  />
                </div>
              </UFormField>

              <!-- Confirm Password -->
              <UFormField
                label="Nhập lại tài khoản"
                name="confirmPassword"
                :error="errors.confirmPassword"
              >
                <div class="relative">
                  <UInput
                    v-model="state.confirmPassword"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="Nhập lại tài khoản"
                    icon="i-lucide:lock"
                    class="w-full"
                    @keydown.enter.prevent="onSubmit"
                  />
                  <UButton
                    color="gray"
                    variant="ghost"
                    size="xs"
                    class="absolute top-1/2 right-1 -translate-y-1/2"
                    :icon="showPassword ? 'i-lucide:eye-off' : 'i-lucide:eye'"
                    @click="showPassword = !showPassword"
                    aria-label="Toggle password"
                  />
                </div>
              </UFormField>

              <UButton
                type="submit"
                block
                :loading="submitting"
                icon="i-lucide:user-plus"
              >
                Đăng ký
              </UButton>
            </UForm>

            <template #footer>
              <div class="text-center">
                <p class="text-muted-foreground text-sm">
                  Bạn đã có tài khoản?
                  <NuxtLink
                    to="/login"
                    class="text-primary font-medium hover:underline"
                  >
                    Đăng nhập
                  </NuxtLink>
                </p>
              </div>
            </template>
          </UCard>
        </div>
      </div>
    </main>
  </div>
</template>
