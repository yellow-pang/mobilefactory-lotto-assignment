<script setup lang="ts">
import { RouterView } from "vue-router";
import AppLayout from "./components/AppLayout.vue";
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
    linear-gradient(180deg, #f7f9fc 0%, var(--app-bg-end) 100%);
}

#app {
  min-height: 100vh;
}
</style>
