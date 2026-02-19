<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { lottoApi, type ResultCheckResponse } from "../api/lotto";

const phone = ref("");
const errorMessage = ref("");
const isLoading = ref(false);
const submitted = ref(false);
const isAnnounceActive = ref<boolean | null>(null);
const result = ref<ResultCheckResponse | null>(null);

const isFirstCheck = computed(() => result.value?.checkCount === 1);
const isFormEnabled = computed(() => isAnnounceActive.value === true);

onMounted(async () => {
  try {
    const active = await lottoApi.checkAnnounceActive();
    isAnnounceActive.value = active;
  } catch (error) {
    // API 호출 실패 시 기한 외로 간주
    isAnnounceActive.value = false;
  }
});

const resultMessage = computed(() => {
  if (!result.value) return "";

  if (isFirstCheck.value) {
    if (result.value.rank === null || result.value.rank === undefined) {
      return "Not a winner this time.";
    }
    return `Congratulations! Rank ${result.value.rank}.`;
  }

  return result.value.isWinner
    ? "Winner confirmed."
    : "Not a winner this time.";
});

const resultTone = computed(() =>
  resultMessage.value.includes("Congratulations") ||
  resultMessage.value.includes("Winner")
    ? "success"
    : "warn",
);

const handleSubmit = () => {
  errorMessage.value = "";

  if (!phone.value.trim()) {
    errorMessage.value = "Please enter your phone number.";
    return;
  }

  isLoading.value = true;

  lottoApi
    .checkResult({ phone: phone.value })
    .then((data) => {
      result.value = data;
      submitted.value = true;
    })
    .catch((error) => {
      errorMessage.value =
        error?.message || "Failed to check result. Please try again.";
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const resetForm = () => {
  phone.value = "";
  errorMessage.value = "";
  submitted.value = false;
  result.value = null;
};
</script>

<template>
  <section class="page">
    <Card>
      <template #title>Result Check</template>
      <template #subtitle>
        Check your winning status during the announcement period.
      </template>
      <template #content>
        <!-- 기한 외 메시지 -->
        <Message
          v-if="isAnnounceActive === false"
          severity="error"
          :closable="false"
        >
          <strong>Announcement Period Has Ended</strong><br />
          The announcement period has closed. Thank you for participating.
        </Message>

        <!-- 확인중 로딩 -->
        <Message
          v-else-if="isAnnounceActive === null"
          severity="info"
          :closable="false"
        >
          Loading announcement information...
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
              label="Check Result"
              icon="pi pi-search"
              :loading="isLoading"
              :disabled="isLoading"
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

        <Message
          v-if="errorMessage && isFormEnabled"
          severity="warn"
          :closable="false"
        >
          {{ errorMessage }}
        </Message>

        <div v-if="submitted && result" class="result">
          <div class="result-header">
            <div class="result-title">Status</div>
            <span class="result-badge">
              {{ isFirstCheck ? "First Check" : "Re-check" }}
            </span>
          </div>
          <Message :severity="resultTone" :closable="false">
            {{ resultMessage }}
          </Message>
          <div v-if="isFirstCheck" class="hint">
            First check shows rank. Re-check hides the rank.
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
  display: grid;
  gap: 10px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  font-weight: 600;
}

.result-badge {
  font-size: 12px;
  color: var(--app-muted);
  background: #f3f6fb;
  padding: 4px 10px;
  border-radius: 999px;
}

.hint {
  color: var(--app-muted);
  font-size: 12px;
}

@media (max-width: 640px) {
  .actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
