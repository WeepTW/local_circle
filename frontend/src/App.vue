<script setup lang="ts">
import { useRegistry } from "./composables/useRegistry";
import { money } from "./api";
import { productName } from "./format";
import PreferenceForm from "./components/PreferenceForm.vue";
import ProductForm from "./components/ProductForm.vue";
import MarketPanel from "./components/MarketPanel.vue";
const isShowcase = import.meta.env.VITE_SHOWCASE === "true";
const {
  actor,
  products,
  allProducts,
  accounts,
  preferences,
  identity,
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
  changeIdentity,
  save,
  remove,
  updateProduct,
  canEdit,
  date,
} = useRegistry();
</script>
<template>
  <div class="shell">
    <aside class="side">
      <RouterLink class="brand" to="/preferences"
        ><span class="brand-icon">◌</span> local_circle</RouterLink
      >
      <p class="side-sub">把喜好留在這裡。</p>
      <nav aria-label="主導覽">
        <RouterLink to="/preferences">我的喜好</RouterLink
        ><RouterLink to="/products">商品目錄</RouterLink
        ><RouterLink to="/accounts">帳戶參照</RouterLink
        ><RouterLink v-if="actor?.role === 'ADMIN'" to="/admin"
          >商品管理</RouterLink
        >
      </nav>
      <div class="side-bottom">
        <span class="badge demo">PREFERENCE REGISTRY</span>
        <p>所有價格皆為參考資料<br />僅保存，不執行交易</p>
      </div>
    </aside>
    <main>
      <p v-if="isShowcase" class="notice" role="note">
        互動展示：資料僅保留於目前分頁，重新整理即重設，不會連線銀行或執行交易。
      </p>
      <header class="top">
        <div>
          <p class="eyebrow">FINANCIAL PREFERENCE REGISTRY</p>
          <h1>{{ title }}</h1>
          <p class="muted">整理你的金融商品喜好，保留每次規劃。</p>
        </div>
        <label class="identity"
          >檢視角色<select
            v-model="identity"
            @change="changeIdentity"
            :disabled="busy || loading"
          >
            <option value="1">一般使用者</option>
            <option value="2">其他使用者</option>
            <option value="3">台股商品管理員</option>
            <option value="4">全球商品管理員</option>
          </select></label
        >
      </header>
      <div v-if="error" role="alert" class="error">
        <strong>操作未完成</strong>
        <p>{{ error }}</p>
        <button class="ghost" @click="load" :disabled="busy || loading">
          重新整理
        </button>
      </div>
      <p v-if="notice" role="status" class="success">{{ notice }}</p>
      <MarketPanel v-if="isShowcase && section === 'products'" :disabled="busy || loading || showForm || !!productEditing" @updated="load" />
      <p v-if="loading" role="status" class="loading">正在讀取資料…</p>
      <template v-else-if="actor">
        <template v-if="section === 'preferences'">
          <section class="stats" aria-label="喜好摘要">
            <div class="card stat">
              <span>已保存項目</span
              ><strong>{{ preferences.length }}<small> 項</small></strong>
            </div>
            <div class="card stat">
              <span>預計總金額</span
              ><strong><small>NT$ </small>{{ money(total) }}</strong>
            </div>
            <div class="card stat">
              <span>總手續費</span
              ><strong><small>NT$ </small>{{ money(fees) }}</strong>
            </div>
          </section>
          <section class="card panel">
            <div class="toolbar">
              <div>
                <h2>保存的規劃</h2>
                <p class="muted">聯絡信箱：{{ actor.email }}</p>
              </div>
              <button
                class="primary"
                :disabled="busy"
                @click="
                  editing = undefined;
                  showForm = true;
                "
              >
                ＋ 保存喜好
              </button>
            </div>
            <div v-if="!preferences.length" class="empty">
              <span class="empty-icon">＋</span>
              <h3>從第一個喜好開始</h3>
              <p>選擇商品、預計數量與帳戶，保存你的規劃。</p>
            </div>
            <div v-else class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>商品 / 保存時間</th>
                    <th>預計數量</th>
                    <th>預計扣款帳號</th>
                    <th class="money">快照單價 / 費率</th>
                    <th class="money">本金 / 手續費</th>
                    <th class="money">預計總金額</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="p in preferences" :key="p.preferenceId">
                    <td>
                      <span class="code">{{ p.productCode }}</span
                      ><strong class="block">{{
                        productName(p.productName)
                      }}</strong
                      ><small class="muted">{{ date(p.savedAt) }}</small>
                    </td>
                    <td>{{ p.plannedQuantity }}</td>
                    <td>
                      {{ p.maskedAccount
                      }}<small class="block muted">{{ p.userEmail }}</small>
                    </td>
                    <td class="money">
                      {{ money(p.priceSnapshot)
                      }}<small class="block muted"
                        >{{ money(p.feeRateSnapshot * 100) }}%</small
                      >
                    </td>
                    <td class="money">
                      {{ money(p.baseAmount)
                      }}<small class="block muted">{{
                        money(p.totalFee)
                      }}</small>
                    </td>
                    <td class="money total">{{ money(p.totalAmount) }}</td>
                    <td class="actions">
                      <button
                        class="ghost"
                        :disabled="busy"
                        @click="
                          editing = p;
                          showForm = true;
                        "
                      >
                        編輯</button
                      ><button
                        class="danger"
                        :disabled="busy"
                        @click="deleting = p"
                      >
                        刪除
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </template>
        <section
          v-else-if="section === 'products' || section === 'admin'"
          class="product-grid"
        >
          <p v-if="section === 'admin' && actor.role !== 'ADMIN'" role="alert">
            此身分沒有商品管理權限。
          </p>
          <article
            class="card product-card"
            v-for="p in section === 'admin' ? allProducts : products"
            :key="p.productId"
          >
            <div class="toolbar">
              <span class="code">{{ p.productCode }}</span
              ><span class="badge" :class="{ demo: !p.active }">{{
                p.active ? "有效商品" : "已停用"
              }}</span>
            </div>
            <h2>{{ productName(p.productName) }}</h2>
            <p class="product-price"><small>NT$ </small>{{ money(p.price) }}</p>
            <p class="muted">
              手續費率 {{ money(p.feeRate * 100) }}% · {{ p.currency }}
            </p>
            <button
              v-if="section === 'admin' && canEdit(p)"
              class="primary"
              @click="productEditing = p"
              :disabled="busy"
            >
              編輯商品
            </button>
            <p v-else-if="section === 'admin'" class="muted">
              此商品由其他管理群組維護
            </p>
          </article>
        </section>
        <section v-else-if="section === 'accounts'" class="card panel">
          <h2>我的有效帳戶</h2>
          <p class="muted">帳戶已預先登錄，系統只保存參照，不接收完整帳號。</p>
          <div v-for="a in accounts" :key="a.accountId" class="account-row">
            <strong>{{ a.maskedAccount }}</strong
            ><span class="badge">{{ a.currency }}</span>
          </div>
          <p v-if="!accounts.length" class="empty">沒有有效帳戶。</p>
        </section>
      </template>
      <div class="notice">
        <strong>這是喜好紀錄，不是交易指示。</strong>0050、0052、GLOBAL_TOP10
        的價格與費率皆為 參考資料，沒有即時行情、下單或扣款。
      </div>
      <footer class="page-footer">
        local_circle <span>金融商品喜好紀錄 · 金額最多顯示 8 位小數</span>
      </footer>
    </main>
    <PreferenceForm
      v-if="showForm"
      :products="products"
      :accounts="accounts"
      :editing="editing"
      :busy="busy"
      @save="save"
      @cancel="showForm = false"
    />
    <ProductForm
      v-if="productEditing"
      :product="productEditing"
      :busy="busy"
      @save="updateProduct"
      @cancel="productEditing = undefined"
    />
    <div v-if="deleting" class="overlay">
      <section
        class="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-title"
      >
        <p class="eyebrow">DELETE PREFERENCE</p>
        <h2 id="delete-title">刪除此喜好？</h2>
        <p>{{ deleting.productName }}</p>
        <p class="muted">只移除此筆保存紀錄，商品目錄不受影響。</p>
        <footer>
          <button class="ghost" :disabled="busy" @click="deleting = undefined">
            取消</button
          ><button class="danger" :disabled="busy" @click="remove">
            {{ busy ? "刪除中…" : "確認刪除" }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>
