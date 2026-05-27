<template>
  <div class="health-cell">
    <template v-if="health">
      <el-tag :type="tagType" size="small">
        {{ label }}
      </el-tag>
      <span v-if="health.avgLatencyMs" class="health-latency" :title="`${health.avgLatencyMs}ms`">
        {{ health.avgLatencyMs }}ms
      </span>
    </template>
    <el-tag v-else type="info" size="small">未知</el-tag>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Health {
  status?: string | null
  avgLatencyMs?: number | null
}

const props = defineProps<{ health?: Health | null }>()

const statusMap: Record<string, { label: string; type: string }> = {
  HEALTHY: { label: '正常', type: 'success' },
  UNHEALTHY: { label: '故障', type: 'danger' },
  DEGRADED: { label: '降级', type: 'warning' },
  UNKNOWN: { label: '未知', type: 'info' },
}

const tagType = computed(() => statusMap[props.health?.status || 'UNKNOWN']?.type || 'info')
const label = computed(() => statusMap[props.health?.status || 'UNKNOWN']?.label || '未知')
</script>

<style scoped>
.health-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}
.health-latency {
  font-size: 12px;
  color: var(--text-secondary);
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
