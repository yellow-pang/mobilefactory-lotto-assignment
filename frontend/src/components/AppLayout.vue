<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const isActive = (path: string) => route.path.startsWith(path);
const go = (path: string) => {
  if (route.path !== path) {
    router.push(path);
  }
};
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand">
        <div class="brand-mark" />
        <div>
          <div class="brand-title">로또 이벤트</div>
          <div class="brand-subtitle">참여 및 결과</div>
        </div>
      </div>
      <div class="nav-actions">
        <Button
          label="이벤트 참여"
          :outlined="!isActive('/event')"
          @click="go('/event')"
        />
        <Button
          label="결과 확인"
          :outlined="!isActive('/result')"
          @click="go('/result')"
        />
      </div>
    </header>

    <main class="app-main">
      <slot />
    </main>

    <footer class="app-footer">
      <span>MobileFactory 로또 과제</span>
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 36px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(31, 36, 48, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffb347, #ff6f61);
  box-shadow: 0 10px 20px rgba(255, 111, 97, 0.25);
}

.brand-title {
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.3px;
}

.brand-subtitle {
  font-size: 13px;
  color: var(--app-muted);
}

.nav-actions {
  display: flex;
  gap: 12px;
}

.app-main {
  flex: 1;
  padding: 36px;
  display: flex;
  justify-content: center;
}

.app-footer {
  padding: 16px 36px 28px;
  color: var(--app-muted);
  font-size: 12px;
}

@media (max-width: 768px) {
  .app-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 20px 20px;
  }

  .app-main {
    padding: 24px 20px;
  }

  .nav-actions {
    width: 100%;
  }

  .nav-actions :deep(.p-button) {
    flex: 1;
    justify-content: center;
  }
}
</style>
