<template>
  <header class="page-header">
    <div class="page-header__content">
      <p v-if="eyebrow" class="page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="page-header__title" data-testid="page-header-title">{{ title }}</h1>
      <p v-if="description" class="page-header__description" data-testid="page-header-description">
        {{ description }}
      </p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions" data-testid="page-header-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
  eyebrow?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border-light);
}

.page-header__content {
  min-width: 0;
}

.page-header__eyebrow {
  margin: 0 0 var(--space-1);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--primary-600);
}

.page-header__title {
  margin: 0;
  font-size: var(--text-3xl);
  line-height: var(--leading-tight);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

.page-header__description {
  margin: var(--space-2) 0 0;
  font-size: var(--text-base);
  line-height: var(--leading-normal);
  color: var(--text-secondary);
}

.page-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-2);
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .page-header {
    flex-direction: column;
  }

  .page-header__actions {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
