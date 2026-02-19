<template>
  <section class="page">
    <Card>
      <template #title>Event Entry</template>
      <template #subtitle
        >Submit your phone number to receive a lotto ticket.</template
      >
      <template #content>
        <div class="placeholder">
          <Message severity="info" :closable="false">
            Event entry UI will be added in the next step.
          </Message>
        </div>
      </template>
    </Card>
  </section>
</template>

<style scoped>
.page {
  width: min(720px, 100%);
}

.placeholder {
  display: grid;
  gap: 12px;
}
</style>
