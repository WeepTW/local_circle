<script setup lang="ts">
import { computed, ref } from "vue";
import type { Product, Account, Preference, Save } from "../types";
import { productName } from "../format";
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
  props.editing?.productId ?? props.initialProductId ?? props.products[0]?.productId ?? 0,
);
const accountId = ref(
  props.editing?.accountId ?? props.accounts[0]?.accountId ?? 0,
);
const quantity = ref(props.editing?.plannedQuantity ?? 1);
const selected = computed(() =>
  props.products.find((p) => p.productId === productId.value),
);
const valid = computed(
  () =>
    !!selected.value &&
    props.accounts.some((a) => a.accountId === accountId.value) &&
    Number.isInteger(quantity.value) &&
    quantity.value >= 1 &&
    quantity.value <= 1000000,
);
const base = computed(() => (selected.value?.price ?? 0) * quantity.value);
const fee = computed(() => base.value * (selected.value?.feeRate ?? 0));
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
        保存規劃，不會執行交易或扣款。更新時會重新取得商品當下的參考價格與費率。
      </p>
      <form
        @submit.prevent="
          valid &&
          !busy &&
          emit('save', { productId, accountId, plannedQuantity: quantity })
        "
      >
        <label
          >商品<select v-model="productId" required :disabled="busy">
            <option :value="0" disabled>請選擇商品</option>
            <option
              v-for="p in products"
              :key="p.productId"
              :value="p.productId"
            >
              {{ p.productCode }} · {{ productName(p.productName) }}
            </option>
          </select></label
        >
        <label
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
            <dd>NT$ {{ money(selected?.price ?? 0) }}</dd>
          </div>
          <div>
            <dt>手續費率</dt>
            <dd>{{ money((selected?.feeRate ?? 0) * 100) }}%</dd>
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
        <p v-if="!accounts.length" role="status">沒有可選的有效帳戶。</p>
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
