<template>
  <button
    class="icon-action"
    type="button"
    :aria-label="label"
    :title="label"
    :disabled="disabled"
    @click="emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<style scoped>
.icon-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.icon-action:hover:not(:disabled) {
  background: var(--bg-secondary);
  border-color: var(--border-default);
  color: var(--primary-600);
}

.icon-action:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--primary-100);
}

.icon-action:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
}
</style>
