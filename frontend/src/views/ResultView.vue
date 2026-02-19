<script setup lang="ts">
import { computed, ref } from "vue";
import { lottoApi, type ResultCheckResponse } from "../api/lotto";

const phone = ref("");
const errorMessage = ref("");
const isLoading = ref(false);
const submitted = ref(false);
const checkCount = ref(0);
const result = ref<ResultCheckResponse | null>(null);

const isFirstCheck = computed(() => checkCount.value === 0);

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
      checkCount.value += 1;
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
  checkCount.value = 0;
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
        <form class="form" @submit.prevent="handleSubmit">
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

        <Message v-if="errorMessage" severity="warn" :closable="false">
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
