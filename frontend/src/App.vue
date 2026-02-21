<script setup lang="ts">
import { onMounted, provide, ref } from "vue";
import { useRouter } from "vue-router";
import { RouterView } from "vue-router";
import AppLayout from "./components/AppLayout.vue";
import { checkAndUpdateFirstVisit } from "./utils/visitTracker";

const router = useRouter();
const isFirstVisit = ref(false);

onMounted(() => {
  // 최초 접속 여부 확인 및 업데이트
  isFirstVisit.value = checkAndUpdateFirstVisit();

  // 최초 접속 시 자동으로 event 페이지로 이동
  if (isFirstVisit.value && router.currentRoute.value.path === "/") {
    router.push("/event");
  }

  // 최초 접속 여부를 자식 컴포넌트에 제공
  provide("isFirstVisit", isFirstVisit);
});
</script>

<template>
  <AppLayout>
    <RouterView />
  </AppLayout>
</template>

<style>
@import url("https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;600;700&display=swap");

:root {
  --app-bg-start: #f6f3ef;
  --app-bg-end: #eef5ff;
  --app-ink: #1f2430;
  --app-muted: #5b6476;
  --app-surface: #ffffff;
  --lotto-number-bg-start: #eff6ff;
  --lotto-number-bg-end: #dbeafe;
  --lotto-number-color: #2563eb;
  --lotto-number-border: #93c5fd;
}

@media (prefers-color-scheme: dark) {
  :root {
    --app-bg-start: #1a1d29;
    --app-bg-end: #0f1218;
    --app-ink: #e5e7eb;
    --app-muted: #9ca3af;
    --app-surface: #1f2937;
    --lotto-number-bg-start: #1e3a8a;
    --lotto-number-bg-end: #1e40af;
    --lotto-number-color: #93c5fd;
    --lotto-number-border: #3b82f6;
  }
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: "DM Sans", "Segoe UI", sans-serif;
  color: var(--app-ink);
  background:
    radial-gradient(120% 80% at 15% 10%, var(--app-bg-start), transparent),
    linear-gradient(180deg, var(--app-bg-start) 0%, var(--app-bg-end) 100%);
}

#app {
  min-height: 100vh;
}
</style>
