<script setup lang="ts">
import { computed, ref } from "vue";

const phone = ref("");
const submitted = ref(false);
const errorMessage = ref("");
const isLoading = ref(false);

const mockResult = ref({
  participantId: 1287,
  lottoNumber: "593821",
});

const showResult = computed(() => submitted.value && !errorMessage.value);

const handleSubmit = () => {
  errorMessage.value = "";
  submitted.value = false;

  if (!phone.value.trim()) {
    errorMessage.value = "Please enter your phone number.";
    return;
  }

  isLoading.value = true;
  setTimeout(() => {
    isLoading.value = false;
    submitted.value = true;
  }, 450);
};

const resetForm = () => {
  phone.value = "";
  submitted.value = false;
  errorMessage.value = "";
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
        <form class="form" @submit.prevent="handleSubmit">
          <label class="field">
            <span class="field-label">Phone Number</span>
            <InputText
              v-model="phone"
              type="tel"
              placeholder="010-1234-5678"
              class="field-input"
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
            />
          </div>
        </form>

        <Message v-if="errorMessage" severity="warn" :closable="false">
          {{ errorMessage }}
        </Message>

        <div v-if="showResult" class="result">
          <div class="result-title">Ticket Issued</div>
          <div class="result-grid">
            <div class="result-item">
              <span class="result-label">Participant ID</span>
              <span class="result-value">{{ mockResult.participantId }}</span>
            </div>
            <div class="result-item">
              <span class="result-label">Lotto Number</span>
              <span class="result-value">{{ mockResult.lottoNumber }}</span>
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
