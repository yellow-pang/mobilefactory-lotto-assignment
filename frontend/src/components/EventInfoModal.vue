<script setup lang="ts">
import { ref, watch } from "vue";
import Dialog from "primevue/dialog";
import Button from "primevue/button";

interface EventInfo {
  description: string;
  eventStart: string;
  eventEnd: string;
  announceStart: string;
  announceEnd: string;
  message: string;
}

interface Props {
  isOpen: boolean;
  eventInfo: EventInfo;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  close: [];
}>();

const isVisible = ref(props.isOpen);

// isOpen이 true가 되면 열기 (닫혀있을 때만)
watch(
  () => props.isOpen,
  (newValue) => {
    if (newValue && !isVisible.value) {
      isVisible.value = true;
    }
  },
);

const handleClose = () => {
  isVisible.value = false;
  emit("close");
};
</script>

<template>
  <Dialog
    v-model:visible="isVisible"
    header="🎯 로또 이벤트 안내"
    :modal="true"
    :style="{ width: '90vw', maxWidth: '500px' }"
    class="event-info-modal"
    @update:visible="handleClose"
  >
    <div class="modal-content">
      <p class="modal-description">
        {{ eventInfo.description }}
      </p>

      <div class="modal-info-section">
        <div class="info-group">
          <span class="info-label">이벤트 기간</span>
          <span class="info-value"
            >{{ eventInfo.eventStart }} ~ {{ eventInfo.eventEnd }}</span
          >
        </div>
        <div class="info-group">
          <span class="info-label">발표 기간</span>
          <span class="info-value"
            >{{ eventInfo.announceStart }} ~ {{ eventInfo.announceEnd }}</span
          >
        </div>
      </div>

      <p class="modal-message">
        {{ eventInfo.message }}
      </p>
    </div>

    <template #footer>
      <Button label="확인" icon="pi pi-check" @click="handleClose" autofocus />
    </template>
  </Dialog>
</template>

<style scoped>
.modal-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.modal-description {
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
  color: var(--app-ink);
}

.modal-info-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
  background-color: var(--app-surface);
  border-radius: 0.5rem;
  border: 1px solid #e0e0e0;
}

.info-group {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.info-label {
  font-weight: 600;
  color: var(--app-muted);
  min-width: 80px;
}

.info-value {
  font-size: 0.95rem;
  color: var(--app-ink);
  text-align: right;
}

.modal-message {
  font-size: 0.9rem;
  color: var(--app-muted);
  margin: 0;
  font-style: italic;
  text-align: center;
}
</style>
