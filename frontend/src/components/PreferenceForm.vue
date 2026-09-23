<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { Product, Account, Preference, Save } from "../types";
import { productName as displayName } from "../format";
import { money } from "../api";
const props = defineProps<{
  products: Product[];
  accounts: Account[];
  editing?: Preference;
  initialProductId?: number;
  busy: boolean;
}>();
const emit = defineEmits<{ save: [data: Save]; cancel: [] }>();
const productId = ref(
  props.editing
    ? (props.editing.productId ?? 0)
    : (props.initialProductId ?? props.products[0]?.productId ?? 0),
);
const source = computed(() =>
  props.products.find((p) => p.productId === productId.value),
);
const productName = ref(
  props.editing?.productName ?? source.value?.productName ?? "",
);
const price = ref(props.editing?.priceSnapshot ?? source.value?.price ?? 0);
const feeRate = ref(
  props.editing?.feeRateSnapshot ?? source.value?.feeRate ?? 0,
);
const accountId = ref(
  props.editing?.accountId ?? props.accounts[0]?.accountId ?? 0,
);
const accountNumber = ref("");
const newAccount = ref(!props.accounts.length);
const quantity = ref(props.editing?.plannedQuantity ?? 1);
watch(productId, () => {
  if (source.value) {
    productName.value = source.value.productName;
    price.value = source.value.price;
    feeRate.value = source.value.feeRate;
  }
});
const base = computed(() => price.value * quantity.value);
const fee = computed(() => base.value * feeRate.value);
const valid = computed(
  () =>
    !!productName.value.trim() &&
    productName.value.length <= 160 &&
    Number.isFinite(price.value) &&
    price.value >= 0 &&
    price.value <= 999999999999 &&
    Number.isFinite(feeRate.value) &&
    feeRate.value >= 0 &&
    feeRate.value <= 1 &&
    Number.isInteger(quantity.value) &&
    quantity.value >= 1 &&
    quantity.value <= 1000000 &&
    base.value + fee.value < 1e16 &&
    (newAccount.value
      ? /^[0-9]{6,32}$/.test(accountNumber.value)
      : props.accounts.some((a) => a.accountId === accountId.value)),
);
function submit() {
  if (!valid.value || props.busy) return;
  emit("save", {
    productId: productId.value || null,
    productName: productName.value.trim(),
    price: price.value,
    feeRate: feeRate.value,
    plannedQuantity: quantity.value,
    ...(newAccount.value
      ? { accountNumber: accountNumber.value }
      : { accountId: accountId.value }),
  });
}
</script>
<template>
  <div class="overlay">
    <section
      role="dialog"
      aria-modal="true"
      aria-labelledby="form-title"
      class="dialog"
    >
      <p class="eyebrow">SAVE PREFERENCE</p>
      <h2 id="form-title">{{ editing ? "編輯喜好" : "保存喜好" }}</h2>
      <p class="muted">
        商品資料只影響自己的喜好。保存規劃，不會執行交易或扣款；請使用示範假帳號。
      </p>
      <form @submit.prevent="submit">
        <label
          >商品<select v-model="productId" :disabled="busy">
            <option :value="0">自行輸入商品</option>
            <option
              v-for="p in products"
              :key="p.productId"
              :value="p.productId"
            >
              {{ p.productCode }} · {{ displayName(p.productName) }}
            </option>
          </select></label
        >
        <label
          >商品名稱<input
            v-model="productName"
            maxlength="160"
            required
            :disabled="busy"
        /></label>
        <label
          >參考單價<input
            v-model.number="price"
            type="number"
            min="0"
            max="999999999999"
            step="0.00000001"
            required
            :disabled="busy"
        /></label>
        <label
          >手續費率<input
            v-model.number="feeRate"
            type="number"
            min="0"
            max="1"
            step="0.000000000001"
            required
            :disabled="busy"
          /><small>以小數輸入，例如 0.001 = 0.1%</small></label
        >
        <label
          >帳戶來源<select v-model="newAccount" :disabled="busy">
            <option :value="false">選擇既有帳戶</option>
            <option :value="true">輸入完整帳號</option>
          </select></label
        >
        <label v-if="newAccount"
          >完整扣款帳號<input
            v-model="accountNumber"
            type="text"
            inputmode="numeric"
            autocomplete="off"
            pattern="[0-9]{6,32}"
            minlength="6"
            maxlength="32"
            required
            :disabled="busy"
          /><small>6–32 位數字，保留開頭的 0；僅供假資料示範。</small></label
        >
        <label v-else
          >預計扣款帳號<select v-model="accountId" required :disabled="busy">
            <option :value="0" disabled>請選擇有效帳戶</option>
            <option
              v-for="a in accounts"
              :key="a.accountId"
              :value="a.accountId"
            >
              {{ a.maskedAccount }} · {{ a.currency }}
            </option>
          </select></label
        >
        <p v-if="!newAccount && !accounts.length" role="status">
          沒有可選的有效帳戶，請改為輸入完整帳號。
        </p>
        <label
          >預計數量<input
            v-model.number="quantity"
            type="number"
            required
            min="1"
            max="1000000"
            step="1"
            :disabled="busy"
        /></label>
        <dl class="calculation">
          <div>
            <dt>參考單價</dt>
            <dd>NT$ {{ money(price) }}</dd>
          </div>
          <div>
            <dt>手續費率</dt>
            <dd>{{ money(feeRate * 100) }}%</dd>
          </div>
          <div>
            <dt>預計手續費</dt>
            <dd>NT$ {{ money(fee) }}</dd>
          </div>
          <div>
            <dt>預計總金額</dt>
            <dd>NT$ {{ money(base + fee) }}</dd>
          </div>
        </dl>
        <footer>
          <button
            type="button"
            class="ghost"
            :disabled="busy"
            @click="emit('cancel')"
          >
            取消</button
          ><button class="primary" :disabled="!valid || busy">
            {{ busy ? "保存中…" : "確認保存" }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
