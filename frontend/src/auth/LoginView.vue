<script setup lang="ts">
import { ref } from "vue";
import type { LoginProvider, LoginSession } from "./contracts";
const props = defineProps<{ provider: LoginProvider }>();
const emit = defineEmits<{ authenticated: [session: LoginSession] }>();
const username = ref(""), password = ref(""), selected = ref(""), busy = ref(false), error = ref("");
function fillSuggestion() {
  const choice = props.provider.suggestions?.find(a => a.id === selected.value);
  username.value = choice?.username ?? "";
  password.value = choice?.password ?? "";
  error.value = "";
}
async function login() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const session = await props.provider.authenticate({ username: username.value, password: password.value });
    password.value = "";
    emit("authenticated", session);
  } catch (e) {
    error.value = e instanceof Error ? e.message : "登入失敗，請稍後重試。";
  } finally { busy.value = false; }
}
</script>
<template>
  <main class="login-shell">
    <section class="card login-card" aria-labelledby="login-title">
      <p class="eyebrow">LOCAL_CIRCLE</p>
      <h1 id="login-title">登入</h1>
      <p class="muted">登入以管理金融商品喜好與保存的規劃。</p>
      <form @submit.prevent="login">
        <fieldset :disabled="busy" class="login-fields">
          <template v-if="provider.suggestions?.length">
            <label for="login-suggestion">示範帳密選項</label>
            <select id="login-suggestion" v-model="selected" @change="fillSuggestion">
              <option value="">選擇角色以填入帳號與密碼</option>
              <option v-for="choice in provider.suggestions" :key="choice.id" :value="choice.id">
                {{ choice.label }} · {{ choice.username }} / {{ choice.password }}
              </option>
            </select>
            <p class="muted login-hint">公開示範帳密，僅供體驗；選取後請按「登入」。</p>
          </template>
          <label for="login-username">帳號</label>
          <input id="login-username" name="username" v-model="username" autocomplete="username" required maxlength="254" />
          <label for="login-password">密碼</label>
          <input id="login-password" name="password" type="password" v-model="password" autocomplete="current-password" required maxlength="128" />
          <p v-if="error" class="error" role="alert">{{ error }}</p>
          <button type="submit" class="primary">{{ busy ? "登入中…" : "登入" }}</button>
        </fieldset>
      </form>
    </section>
  </main>
</template>
