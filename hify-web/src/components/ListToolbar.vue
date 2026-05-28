<template>
  <div class="list-toolbar">
    <div v-if="$slots.filters" class="list-toolbar__filters">
      <slot name="filters" />
    </div>
    <div v-if="$slots.actions" class="list-toolbar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
}

.list-toolbar__filters,
.list-toolbar__actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.list-toolbar__filters {
  flex: 1;
  flex-wrap: wrap;
}

.list-toolbar__actions {
  flex-shrink: 0;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .list-toolbar__actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
