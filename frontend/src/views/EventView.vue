<script setup lang="ts">
import { computed, inject, onMounted, ref } from "vue";
import { lottoApi, type ParticipateResponse } from "@/api/lotto";

const phone = ref("");
const submitted = ref(false);
const errorMessage = ref("");
const isLoading = ref(false);
const isEventActive = ref<boolean | null>(null);
const isFirstVisit = inject<{ value: boolean }>("isFirstVisit", {
  value: false,
});

// 이벤트 안내 모달 관련 상태
const isShowEventInfoModal = ref(false);
const eventInfo = ref({
  description: "환영합니다. 전화번호를 입력하시면 로또 번호를 전달 드립니다.",
  eventStart: "2025/02/01",
  eventEnd: "2025/03/31",
  announceStart: "2025/04/01",
  announceEnd: "2025/04/15",
  message: "기간 안에 많은 참여 부탁드려요~!",
});

// 인증 관련 상태
const verificationCode = ref("");
const userInputCode = ref("");
const isVerified = ref(false);
const isVerificationSent = ref(false);
const verificationError = ref("");
const isVerifying = ref(false);
const verificationTimeLeft = ref(180); // 3분 = 180초
const verificationTimerInterval = ref<number | null>(null);

const result = ref<ParticipateResponse | null>(null);

const showResult = computed(() => submitted.value && !errorMessage.value);
const isFormEnabled = computed(() => isEventActive.value === true);
const canSendVerification = computed(
  () => phone.value.trim().length >= 10 && !isVerificationSent.value,
);
const canParticipate = computed(() => isVerified.value && !isLoading.value);

onMounted(async () => {
  try {
    const active = await lottoApi.checkEventActive();
    isEventActive.value = active;

    // 최초 접속이면 이벤트 안내 모달 자동 오픈
    if (isFirstVisit.value) {
      isShowEventInfoModal.value = true;
    }
  } catch (error) {
    // API 호출 실패 시 기한 외로 간주
    isEventActive.value = false;
  }
});

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
  submitted.value = false;

  if (!isVerified.value) {
    errorMessage.value = "휴대폰 인증을 완료해주세요.";
    return;
  }

  if (!phone.value.trim()) {
    errorMessage.value = "휴대폰 번호를 입력해주세요.";
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
        error?.message || "참여에 실패했습니다. 다시 시도해주세요.";
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
  verificationCode.value = "";
  userInputCode.value = "";
  isVerified.value = false;
  isVerificationSent.value = false;
  verificationError.value = "";
};
</script>

<template>
  <section class="page">
    <!-- 이벤트 안내 모달 -->
    <Dialog
      v-model:visible="isShowEventInfoModal"
      header="🎯 로또 이벤트 안내"
      :modal="true"
      :style="{ width: '90vw', maxWidth: '500px' }"
      class="event-info-modal"
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
        <Button
          label="확인"
          icon="pi pi-check"
          @click="isShowEventInfoModal = false"
          autofocus
        />
      </template>
    </Dialog>

    <Card>
      <template #title>이벤트 참여</template>
      <template #subtitle>
        휴대폰 번호를 입력하면 로또 번호를 받을 수 있습니다.
      </template>
      <template #content>
        <!-- 최초 접속 환영 메시지 -->
        <Message
          v-if="isFirstVisit.value && isEventActive === true"
          severity="success"
          :closable="false"
          class="welcome-message"
        >
          <strong>🎊 매일 최초 접속을 환영합니다!</strong><br />
          오늘의 로또 이벤트에 참여하세요.
        </Message>

        <!-- 기한 외 메시지 -->
        <Message
          v-if="isEventActive === false"
          severity="error"
          :closable="false"
        >
          <strong>이벤트 기간이 종료되었습니다</strong><br />
          현재는 참여할 수 없습니다. 다음 이벤트를 기다려주세요.
        </Message>

        <!-- 확인중 로딩 -->
        <Message
          v-else-if="isEventActive === null"
          severity="info"
          :closable="false"
        >
          이벤트 정보를 확인하는 중입니다...
        </Message>

        <!-- 기간 내 입력 폼 -->
        <form v-if="isFormEnabled" class="form" @submit.prevent="handleSubmit">
          <label class="field">
            <span class="field-label">휴대폰 번호</span>
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
              label="참여하기"
              icon="pi pi-ticket"
              :loading="isLoading"
              :disabled="!canParticipate"
            />
            <Button
              type="button"
              label="초기화"
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

        <div v-if="showResult" class="result">
          <Message severity="success" :closable="false" class="success-message">
            <strong>참여가 완료되었습니다!</strong><br />
            로또 번호가 SMS로 발송되었습니다.
          </Message>
          <div class="result-title">발급된 로또 번호</div>
          <div class="lotto-number-display">
            {{ result?.lottoNumber }}
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

.success-message {
  margin-bottom: 16px;
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
  background: var(--app-surface);
  border: 1px solid rgba(31, 36, 48, 0.08);
}

@media (prefers-color-scheme: dark) {
  .result {
    background: rgba(31, 41, 55, 0.6);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.result-title {
  font-weight: 600;
  margin-bottom: 12px;
  text-align: center;
  color: var(--app-ink);
}

.lotto-number-display {
  padding: 24px 20px;
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--lotto-number-color);
  background: linear-gradient(
    135deg,
    var(--lotto-number-bg-start) 0%,
    var(--lotto-number-bg-end) 100%
  );
  border-radius: 12px;
  border: 2px solid var(--lotto-number-border);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
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
  background: var(--app-surface);
  border: 1px solid rgba(31, 36, 48, 0.06);
}

@media (prefers-color-scheme: dark) {
  .result-item {
    background: rgba(31, 41, 55, 0.4);
    border: 1px solid rgba(255, 255, 255, 0.08);
  }
}

.result-label {
  color: var(--app-muted);
  font-size: 13px;
}

.result-value {
  font-weight: 600;
  letter-spacing: 0.4px;
}

.welcome-message {
  margin-bottom: 16px;
  animation: slideDown 0.4s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-content {
  display: grid;
  gap: 16px;
  padding: 8px 0;
}

.modal-description {
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
  color: var(--app-ink);
  font-weight: 500;
}

.modal-info-section {
  background: rgba(59, 130, 246, 0.05);
  border-left: 4px solid #3b82f6;
  padding: 16px;
  border-radius: 8px;
  display: grid;
  gap: 12px;
}

.info-group {
  display: grid;
  gap: 6px;
}

.info-label {
  font-size: 13px;
  color: var(--app-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 15px;
  color: var(--app-ink);
  font-weight: 500;
}

.modal-message {
  margin: 0;
  padding: 12px 16px;
  background: rgba(34, 197, 94, 0.05);
  border-radius: 8px;
  color: #16a34a;
  font-weight: 500;
  text-align: center;
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
