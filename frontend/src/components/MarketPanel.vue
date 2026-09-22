<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import { parseFeed, isFresh, type MarketFeed } from "../market";
import { applyShowcaseQuotes } from "../showcaseApi";
const props = defineProps<{ disabled: boolean }>();
const emit = defineEmits<{ updated: [] }>();
const feed = ref<MarketFeed>();
const error = ref("");
const loading = ref(false);
let timer: ReturnType<typeof setInterval>;
let controller: AbortController | undefined;
let disposed = false;
async function refresh() {
  if (loading.value) return;
  loading.value = true;
  controller = new AbortController();
  const timeout = setTimeout(() => controller?.abort(), 10000);
  try {
    const response = await fetch(`${import.meta.env.BASE_URL}data/quotes.sample.json`, {
      cache: "no-store", signal: controller.signal,
    });
    if (!response.ok) throw new Error();
    const value = parseFeed(await response.json());
    if (value.source !== "sample") throw new Error("Unexpected quote source");
    if (!disposed) { feed.value = value; error.value = ""; }
  } catch {
    if (!disposed) error.value = "行情讀取失敗，請重試；保留的資料不代表最新行情。";
  } finally { clearTimeout(timeout); loading.value = false; }
}
function apply() {
  if (!feed.value || props.disabled || error.value) return;
  applyShowcaseQuotes(feed.value);
  emit("updated");
}
onMounted(() => { void refresh(); timer = setInterval(() => { if (!document.hidden) void refresh(); }, 60000); });
onUnmounted(() => { disposed = true; clearInterval(timer); controller?.abort(); });
</script>
<template>
  <section class="card panel" aria-label="行情來源">
    <div class="toolbar"><h2>行情資料</h2><button class="ghost" :disabled="loading" @click="refresh">更新行情</button></div>
    <p class="muted">範例行情 · 非即時報價。每 60 秒檢查資料檔，不模擬市場漲跌。</p>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-for="q in feed?.quotes" :key="q.symbol">
      {{ q.symbol }} · NT$ {{ q.price }} · {{ new Date(q.asOf).toLocaleString("zh-TW") }}
      <span v-if="feed?.source !== 'sample' && !isFresh(q)">（已過期／非近期成交）</span>
    </p>
    <button class="ghost" :disabled="disabled || loading || !!error || !feed?.quotes.length" @click="apply">套用範例報價</button>
    <p class="muted">只更新目前分頁商品價格；既有喜好的保存金額不變。GLOBAL_TOP10 沒有對應報價。</p>
  </section>
</template>
