import { createApp } from "vue";
import {
  createRouter,
  createWebHistory,
  createWebHashHistory,
} from "vue-router";
import AuthGate from "./auth/AuthGate.vue";
import "./style.css";
const Page = { render: () => null };
const router = createRouter({
  history:
    import.meta.env.VITE_SHOWCASE === "true"
      ? createWebHashHistory(import.meta.env.BASE_URL)
      : createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: "/", redirect: "/preferences" },
    ...["preferences", "products", "accounts", "admin"].map((path) => ({
      path: "/" + path,
      component: Page,
    })),
    { path: "/:pathMatch(.*)*", redirect: "/preferences" },
  ],
});
createApp(AuthGate).use(router).mount("#app");
