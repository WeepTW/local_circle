import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath } from "node:url";
export default defineConfig({
  resolve: { alias: { "@login-provider": fileURLToPath(new URL(
    process.env.VITE_DEMO_LOGIN === "true" ? "./src/auth/demo/provider.ts" : "./src/auth/productionProvider.ts",
    import.meta.url,
  )) } },
  base: process.env.VITE_BASE_PATH || "/",
  plugins: [
    vue(),
    {
      name: "showcase-csp",
      transformIndexHtml() {
        return process.env.VITE_SHOWCASE === "true"
          ? [
              {
                tag: "meta",
                attrs: {
                  "http-equiv": "Content-Security-Policy",
                  content:
                    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'",
                },
                injectTo: "head" as const,
              },
            ]
          : [];
      },
    },
  ],
  server: { proxy: { "/api": "http://127.0.0.1:8089" } },
});
