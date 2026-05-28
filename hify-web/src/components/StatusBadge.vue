<template>
  <span
    class="status-badge"
    :class="`status-badge--${status}`"
    data-testid="status-badge"
  >
    <span class="status-badge__dot" aria-hidden="true" />
    {{ text }}
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  text: string
  status?: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
}>(), {
  status: 'neutral',
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  max-width: 100%;
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  background: var(--bg-secondary);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  line-height: var(--leading-tight);
  white-space: nowrap;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
  flex-shrink: 0;
}

.status-badge--success {
  background: var(--success-50);
  border-color: var(--success-500);
  color: var(--success-600);
}

.status-badge--warning {
  background: var(--warning-50);
  border-color: var(--warning-500);
  color: var(--warning-600);
}

.status-badge--danger {
  background: var(--error-50);
  border-color: var(--error-500);
  color: var(--error-600);
}

.status-badge--info {
  background: var(--info-50);
  border-color: var(--info-500);
  color: var(--info-600);
}

.status-badge--neutral {
  background: var(--bg-secondary);
  border-color: var(--border-default);
  color: var(--text-secondary);
}
</style>
