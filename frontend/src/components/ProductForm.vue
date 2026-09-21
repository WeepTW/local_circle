<script setup lang="ts">
import { reactive } from "vue";
import type { Product, ProductUpdate } from "../types";
const props = defineProps<{ product: Product; busy: boolean }>();
const emit = defineEmits<{ save: [data: ProductUpdate]; cancel: [] }>();
const form = reactive<ProductUpdate>({
  productName: props.product.productName,
  price: props.product.price,
  feeRate: props.product.feeRate,
  active: props.product.active,
  version: props.product.version,
});
</script>
<template>
  <div class="overlay">
    <section
      class="dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="product-title"
    >
      <p class="eyebrow">PRODUCT MASTER</p>
      <h2 id="product-title">編輯商品 · {{ product.productCode }}</h2>
      <p class="muted">變更會套用到共用商品目錄，已保存的金額快照不變。</p>
      <form @submit.prevent="!busy && emit('save', { ...form })">
        <label
          >產品名稱<input
            v-model="form.productName"
            required
            maxlength="160"
            :disabled="busy"
        /></label>
        <label
          >參考價格<input
            v-model.number="form.price"
            type="number"
            min="0"
            max="999999999999"
            step="0.00000001"
            required
            :disabled="busy"
        /></label>
        <label
          >手續費率（0.001 = 0.1%）<input
            v-model.number="form.feeRate"
            type="number"
            min="0"
            max="1"
            step="0.000000000001"
            required
            :disabled="busy"
        /></label>
        <label class="check"
          ><input
            type="checkbox"
            v-model="form.active"
            :disabled="busy"
          />商品啟用</label
        >
        <footer>
          <button
            type="button"
            class="ghost"
            @click="emit('cancel')"
            :disabled="busy"
          >
            取消</button
          ><button class="primary" :disabled="busy">
            {{ busy ? "更新中…" : "確認更新" }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
