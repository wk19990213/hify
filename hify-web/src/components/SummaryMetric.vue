<template>
  <section class="summary-metric">
    <div class="summary-metric__body">
      <p class="summary-metric__label" data-testid="summary-metric-label">{{ label }}</p>
      <p class="summary-metric__value" data-testid="summary-metric-value">{{ value }}</p>
    </div>
    <div v-if="$slots.default" class="summary-metric__meta">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
}>()
</script>

<style scoped>
.summary-metric {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-4);
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.summary-metric__body {
  min-width: 0;
}

.summary-metric__label {
  margin: 0;
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  color: var(--text-secondary);
}

.summary-metric__value {
  margin: var(--space-2) 0 0;
  font-size: var(--text-3xl);
  line-height: var(--leading-tight);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

.summary-metric__meta {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  color: var(--text-secondary);
}
</style>
