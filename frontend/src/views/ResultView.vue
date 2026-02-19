<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { lottoApi, type ResultCheckResponse } from "../api/lotto";

const phone = ref("");
const errorMessage = ref("");
const isLoading = ref(false);
const submitted = ref(false);
const isAnnounceActive = ref<boolean | null>(null);
const result = ref<ResultCheckResponse | null>(null);

// 인증 관련 상태
const verificationCode = ref("");
const userInputCode = ref("");
const isVerified = ref(false);
const isVerificationSent = ref(false);
const verificationError = ref("");
const isVerifying = ref(false);
const verificationTimeLeft = ref(180); // 3분 = 180초
const verificationTimerInterval = ref<number | null>(null);

const isFirstCheck = computed(() => result.value?.checkCount === 1);
const isFormEnabled = computed(() => isAnnounceActive.value === true);
const canSendVerification = computed(
  () => phone.value.trim().length >= 10 && !isVerificationSent.value,
);
const canCheckResult = computed(() => isVerified.value && !isLoading.value);

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

const sendVerificationCode = () => {
  verificationError.value = "";

  if (!phone.value.trim()) {
    verificationError.value = "휴대폰 번호를 입력해주세요.";
    return;
  }

  // Mock 인증번호 생성 (실제로는 SMS로 발송)
  const code = Math.floor(100000 + Math.random() * 900000).toString();
  verificationCode.value = code;
  isVerificationSent.value = true;
  verificationTimeLeft.value = 180; // 3분 초기화

  // 타이머 시작
  if (verificationTimerInterval.value) {
    clearInterval(verificationTimerInterval.value);
  }
  verificationTimerInterval.value = window.setInterval(() => {
    verificationTimeLeft.value--;
    if (verificationTimeLeft.value <= 0) {
      if (verificationTimerInterval.value) {
        clearInterval(verificationTimerInterval.value);
      }
      verificationError.value =
        "인증번호 유효시간이 만료되었습니다. 재전송해주세요.";
      isVerificationSent.value = false;
    }
  }, 1000);

  // 실제 환경에서는 SMS API 호출
  console.log(`[Mock SMS] 인증번호: ${code} → ${phone.value}`);
};

const verifyCode = () => {
  verificationError.value = "";
  isVerifying.value = true;

  // Mock 인증: 아무 번호나 입력해도 자동 성공
  setTimeout(() => {
    isVerified.value = true;
    isVerifying.value = false;

    // 타이머 정지
    if (verificationTimerInterval.value) {
      clearInterval(verificationTimerInterval.value);
      verificationTimerInterval.value = null;
    }
  }, 500);
};

const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes}:${secs.toString().padStart(2, "0")}`;
};

const handleSubmit = () => {
  errorMessage.value = "";

  if (!isVerified.value) {
    errorMessage.value = "휴대폰 인증을 완료해주세요.";
    return;
  }

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
  verificationCode.value = "";
  userInputCode.value = "";
  isVerified.value = false;
  isVerificationSent.value = false;
  verificationError.value = "";
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
            <div class="phone-input-group">
              <InputText
                v-model="phone"
                type="tel"
                placeholder="010-1234-5678"
                class="field-input"
                :disabled="isLoading || isVerificationSent"
              />
              <Button
                type="button"
                label="인증번호 전송"
                severity="info"
                :disabled="!canSendVerification || isLoading"
                @click="sendVerificationCode"
              />
            </div>
          </label>

          <!-- 인증번호 입력 필드 (인증번호 발송 후 표시) -->
          <label v-if="isVerificationSent && !isVerified" class="field">
            <div class="verification-header">
              <span class="field-label">인증번호</span>
              <span class="verification-timer">{{
                formatTime(verificationTimeLeft)
              }}</span>
            </div>
            <div class="phone-input-group">
              <InputText
                v-model="userInputCode"
                type="text"
                placeholder="인증번호 6자리 입력"
                class="field-input"
                maxlength="6"
                :disabled="isVerifying || isLoading"
              />
              <Button
                type="button"
                label="인증하기"
                severity="success"
                :disabled="userInputCode.length !== 6 || isVerifying"
                :loading="isVerifying"
                @click="verifyCode"
              />
            </div>
          </label>

          <!-- 인증 완료 메시지 -->
          <Message v-if="isVerified" severity="success" :closable="false">
            ✓ 휴대폰 인증이 완료되었습니다.
          </Message>

          <!-- 인증 에러 메시지 -->
          <Message v-if="verificationError" severity="warn" :closable="false">
            {{ verificationError }}
          </Message>

          <div class="actions">
            <Button
              type="submit"
              label="Check Result"
              icon="pi pi-search"
              :loading="isLoading"
              :disabled="!canCheckResult"
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

.phone-input-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.phone-input-group .field-input {
  flex: 1;
}

.verification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.verification-timer {
  color: #e74c3c;
  font-weight: 600;
  font-size: 14px;
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

  .phone-input-group {
    flex-direction: column;
    align-items: stretch;
  }

  .phone-input-group .field-input {
    width: 100%;
  }
}
</style>
