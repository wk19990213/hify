<template>
  <div class="page-header">
    <div class="header-left">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="description" class="page-desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="header-right">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.header-left { flex: 1; }
.page-title { margin: 0 0 4px 0; font-size: 22px; font-weight: 600; }
.page-desc { margin: 0; color: var(--text-secondary); font-size: 14px; }
.header-right { display: flex; gap: 8px; flex-shrink: 0; }
</style>
