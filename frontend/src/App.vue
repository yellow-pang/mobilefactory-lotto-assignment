<script setup lang="ts">
import { onMounted, provide, ref } from "vue";
import { useRouter } from "vue-router";
import { RouterView } from "vue-router";
import AppLayout from "./components/AppLayout.vue";
import { checkAndUpdateFirstVisit } from "./utils/visitTracker";
import { lottoApi } from "./api/lotto";

const router = useRouter();
const isFirstVisit = ref(false);

/**
 * 현재 시점에 따라 올바른 페이지로 자동 라우팅
 * - 이벤트 기간: /event (참여)
 * - 발표 기간: /result (결과 조회)
 */
const routeToCorrectPage = async () => {
  try {
    const isEventActive = await lottoApi.checkEventActive();
    const isAnnounceActive = await lottoApi.checkAnnounceActive();

    const currentPath = router.currentRoute.value.path;

    if (isEventActive && currentPath !== "/event") {
      // 이벤트 기간 중 → /event로 이동
      router.push("/event");
    } else if (isAnnounceActive && currentPath !== "/result") {
      // 발표 기간 중 → /result로 이동
      router.push("/result");
    } else if (!isEventActive && !isAnnounceActive) {
      // 기간 외 → /event 기본값
      if (currentPath !== "/event") {
        router.push("/event");
      }
    }
  } catch (error) {
    console.error("기간 확인 오류:", error);
    // 오류 시 /event를 기본값으로 설정
    if (router.currentRoute.value.path === "/") {
      router.push("/event");
    }
  }
};

onMounted(async () => {
  // 최초 접속 여부 확인 및 업데이트
  isFirstVisit.value = checkAndUpdateFirstVisit();

  // 현재 시점에 맞는 페이지로 라우팅
  await routeToCorrectPage();

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
