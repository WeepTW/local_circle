import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "pages-tests",
  workers: 1,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: process.env.PAGES_URL ?? "http://127.0.0.1:4173/local_circle/",
    viewport: { width: 1440, height: 1000 },
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  webServer: process.env.PAGES_URL
    ? undefined
    : {
        command:
          "npm exec vite preview -- --host 127.0.0.1 --port 4173 --strictPort",
        env: { VITE_BASE_PATH: "/local_circle/" },
        url: "http://127.0.0.1:4173/local_circle/",
        reuseExistingServer: false,
      },
});
