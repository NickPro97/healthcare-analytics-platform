/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Local development: each API prefix goes to the service that owns it.
// In Docker/Kubernetes the same routing is done by nginx (see nginx.conf).
const patient = "http://localhost:8080";
const analytics = "http://localhost:8081";
const notification = "http://localhost:8082";
const assistant = "http://localhost:8083";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/auth": patient,
      "/api/patients": patient,
      "/api/health": patient,
      "/api/alerts": analytics,
      "/api/analytics": analytics,
      "/api/notifications": notification,
      "/api/assistant": assistant,
    },
  },
  build: {
    rollupOptions: {
      output: { manualChunks: { charts: ["recharts"], react: ["react", "react-dom", "react-router-dom"] } },
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
});
