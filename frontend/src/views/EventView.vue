<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { lottoApi, type ParticipateResponse } from "@/api/lotto";

const phone = ref("");
const submitted = ref(false);
const errorMessage = ref("");
const isLoading = ref(false);
const isEventActive = ref<boolean | null>(null);

const result = ref<ParticipateResponse | null>(null);

const showResult = computed(() => submitted.value && !errorMessage.value);
const isFormEnabled = computed(() => isEventActive.value === true);

onMounted(async () => {
  try {
    const active = await lottoApi.checkEventActive();
    isEventActive.value = active;
  } catch (error) {
    // API 호출 실패 시 기한 외로 간주
    isEventActive.value = false;
  }
});

const handleSubmit = () => {
  errorMessage.value = "";
  submitted.value = false;

  if (!phone.value.trim()) {
    errorMessage.value = "Please enter your phone number.";
    return;
  }

  isLoading.value = true;
  lottoApi
    .participate({ phone: phone.value })
    .then((data) => {
      result.value = data;
      submitted.value = true;
    })
    .catch((error) => {
      errorMessage.value =
        error?.message || "Failed to participate. Please try again.";
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const resetForm = () => {
  phone.value = "";
  submitted.value = false;
  errorMessage.value = "";
  result.value = null;
};
</script>

<template>
  <section class="page">
    <Card>
      <template #title>Event Entry</template>
      <template #subtitle>
        Submit your phone number to receive a lotto ticket.
      </template>
      <template #content>
        <!-- 기한 외 메시지 -->
        <Message v-if="isEventActive === false" severity="error" :closable="false">
          <strong>Event Period Has Ended</strong><br />
          Unfortunately, the event period has closed. Please wait for the next event.
        </Message>

        <!-- 확인중 로딩 -->
        <Message v-else-if="isEventActive === null" severity="info" :closable="false">
          Loading event information...
        </Message>

        <!-- 기간 내 입력 폼 -->
        <form v-if="isFormEnabled" class="form" @submit.prevent="handleSubmit">
          <label class="field">
            <span class="field-label">Phone Number</span>
            <InputText
              v-model="phone"
              type="tel"
              placeholder="010-1234-5678"
              class="field-input"
              :disabled="isLoading"
            />
          </label>
          <div class="actions">
            <Button
              type="submit"
              label="Participate"
              icon="pi pi-ticket"
              :loading="isLoading"
            />
            <Button
              type="button"
              label="Reset"
              severity="secondary"
              outlined
              @click="resetForm"
              :disabled="isLoading"
            />
          </div>
        </form>

        <Message v-if="errorMessage && isFormEnabled" severity="warn" :closable="false">
          {{ errorMessage }}
        </Message>

        <div v-if="showResult" class="result">
          <div class="result-title">Ticket Issued</div>
          <div class="result-grid">
            <div class="result-item">
              <span class="result-label">Participant ID</span>
              <span class="result-value">{{ result?.participantId }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">Lotto Number</span>
              <span class="result-value">{{ result?.lottoNumber }}</span>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </section>
</template>

<style scoped>
.page {
  width: min(720px, 100%);
}

.form {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field-label {
  font-size: 14px;
  color: var(--app-muted);
}

.field-input {
  width: 100%;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.result {
  margin-top: 18px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(31, 36, 48, 0.08);
}

.result-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.result-grid {
  display: grid;
  gap: 12px;
}

.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(31, 36, 48, 0.06);
}

.result-label {
  color: var(--app-muted);
  font-size: 13px;
}

.result-value {
  font-weight: 600;
  letter-spacing: 0.4px;
}

@media (max-width: 640px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
