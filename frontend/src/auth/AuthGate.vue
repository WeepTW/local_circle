<script setup lang="ts">
import { onMounted, shallowRef, ref } from "vue";
import App from "../App.vue";
import LoginView from "./LoginView.vue";
import { loadLoginProvider } from "./provider";
import type { LoginProvider, LoginSession } from "./contracts";
import { setIdentity } from "../api";
const provider = shallowRef<LoginProvider>();
const session = shallowRef<LoginSession>();
const error = ref("");
onMounted(async () => {
  setIdentity("");
  try { provider.value = await loadLoginProvider(); }
  catch { error.value = "登入畫面載入失敗，請重新整理後再試。"; }
});
function authenticate(value: LoginSession) {
  setIdentity(value.userId);
  session.value = value;
}
function logout() {
  session.value = undefined;
  setIdentity("");
}
</script>
<template>
  <App v-if="session" :key="session.userId" @logout="logout" />
  <LoginView v-else-if="provider" :provider="provider" @authenticated="authenticate" />
  <p v-else-if="error" role="alert">{{ error }}</p>
  <p v-else role="status">正在載入登入畫面…</p>
</template>
