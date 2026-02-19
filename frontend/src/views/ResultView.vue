<template>
  <section class="page">
    <Card>
      <template #title>Result Check</template>
      <template #subtitle
        >Check your winning status during the announcement period.</template
      >
      <template #content>
        <div class="placeholder">
          <Message severity="info" :closable="false">
            Result check UI will be added in the next step.
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
