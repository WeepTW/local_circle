<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue";
import { api, ApiError } from "../api";
const props = defineProps<{ accountId: number; masked: string }>();
const number = ref("");
const error = ref("");
const busy = ref(false);
let active = true;
let timer: ReturnType<typeof setTimeout> | undefined;
function hide() {
  number.value = "";
  clearTimeout(timer);
}
async function reveal() {
  if (number.value) {
    hide();
    return;
  }
  busy.value = true;
  error.value = "";
  try {
    const result = await api.accountNumber(props.accountId);
    if (!active) return;
    number.value = result.accountNumber;
    timer = setTimeout(hide, 30000);
  } catch (e) {
    if (active)
      error.value =
        e instanceof ApiError && e.problem.code === "ACCOUNT_NUMBER_REQUIRED"
          ? "舊資料僅有末四碼，請在編輯喜好時輸入完整示範帳號。"
          : "無法查看帳號，請稍後重試。";
  } finally {
    if (active) busy.value = false;
  }
}
onBeforeUnmount(() => {
  active = false;
  hide();
});
</script>
<template>
  <span>{{ number || masked }}</span>
  <button type="button" class="ghost" :disabled="busy" @click="reveal">
    {{ number ? "隱藏帳號" : "查看完整帳號" }}
  </button>
  <small v-if="error" role="status">{{ error }}</small>
</template>
