import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { api, ApiError, money } from "../api";
import type {
  Actor,
  Product,
  Account,
  Preference,
  Save,
  ProductUpdate,
} from "../types";
export function useRegistry() {
  const route = useRoute();
  const actor = ref<Actor>(),
    products = ref<Product[]>([]),
    allProducts = ref<Product[]>([]),
    accounts = ref<Account[]>([]),
    preferences = ref<Preference[]>([]);
  const loading = ref(false),
    busy = ref(false),
    error = ref(""),
    notice = ref(""),
    showForm = ref(false);
  const editing = ref<Preference>(),
    deleting = ref<Preference>(),
    productEditing = ref<Product>();
  const section = computed(() => route.path.slice(1) || "preferences");
  const title = computed(
    () =>
      ({
        preferences: "我的喜好清單",
        products: "商品目錄",
        accounts: "帳戶參照",
        admin: "商品管理",
      })[section.value] ?? "我的喜好清單",
  );
  const total = computed(() =>
      preferences.value.reduce((s, p) => s + p.totalAmount, 0),
    ),
    fees = computed(() =>
      preferences.value.reduce((s, p) => s + p.totalFee, 0),
    );
  let generation = 0;
  function showError(e: unknown) {
    error.value =
      e instanceof ApiError
        ? (e.problem.status === 409
            ? "資料已被更新，請關閉表單、重新整理後再試。"
            : e.problem.status === 403
              ? "此身分沒有操作權限。"
              : e.message) +
          (e.problem.traceId ? " · 追蹤碼 " + e.problem.traceId : "")
        : e instanceof Error
          ? e.message
          : "發生錯誤，請重試。";
  }
  async function load() {
    const g = ++generation;
    loading.value = true;
    error.value = "";
    try {
      const [me, p, a, rows] = await Promise.all([
        api.me(),
        api.products(),
        api.accounts(),
        api.preferences(),
      ]);
      const all = me.role === "ADMIN" ? await api.adminProducts() : [];
      if (g !== generation) return;
      actor.value = me;
      products.value = p;
      accounts.value = a;
      preferences.value = rows;
      allProducts.value = all;
    } catch (e) {
      if (g === generation) showError(e);
    } finally {
      if (g === generation) loading.value = false;
    }
  }
  async function mutate(
    operation: () => Promise<unknown>,
    success: () => void,
    message: string,
  ) {
    if (busy.value) return;
    busy.value = true;
    error.value = "";
    try {
      await operation();
      success();
      notice.value = message;
      await load();
    } catch (e) {
      showError(e);
    } finally {
      busy.value = false;
    }
  }
  async function save(data: Save) {
    const current = editing.value;
    await mutate(
      () =>
        current
          ? api.update(current.preferenceId, {
              ...data,
              version: current.version,
            })
          : api.save(data),
      () => {
        showForm.value = false;
      },
      "喜好已保存。",
    );
  }
  async function remove() {
    const current = deleting.value;
    if (!current) return;
    await mutate(
      () => api.remove(current.preferenceId, current.version),
      () => {
        deleting.value = undefined;
      },
      "喜好已刪除。",
    );
  }
  async function updateProduct(data: ProductUpdate) {
    const current = productEditing.value;
    if (!current) return;
    await mutate(
      () => api.product(current.productId, data),
      () => {
        productEditing.value = undefined;
      },
      "商品已更新，既有保存金額不變。",
    );
  }
  const canEdit = (p: Product) =>
    actor.value?.role === "ADMIN" && actor.value.labels.includes(p.ownerLabel);
  const date = (value: string) => new Date(value).toLocaleString("zh-TW");
  watch(
    () => route.path,
    () => {
      notice.value = "";
    },
  );
  onMounted(load);

  return {
    actor,
    products,
    allProducts,
    accounts,
    preferences,
    loading,
    busy,
    error,
    notice,
    showForm,
    editing,
    deleting,
    productEditing,
    section,
    title,
    total,
    fees,
    load,
    save,
    remove,
    updateProduct,
    canEdit,
    date,
  };
}
