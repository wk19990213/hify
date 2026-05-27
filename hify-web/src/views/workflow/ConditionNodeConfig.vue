<template>
  <div>
    <el-form-item label="条件">
      <el-input :model-value="config.expression" placeholder="比如：{{查订单.hasOrderId}} == true" @update:model-value="updateField('expression', $event)" />
      <div class="type-hint">
        支持 <code>==</code>（等于）和 <code>!=</code>（不等于）。结果决定走「条件成立」还是「条件不成立」分支
      </div>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ config: Record<string, any> }>()
const emit = defineEmits<{ 'update:config': [value: Record<string, any>] }>()

function updateField(key: string, value: any) {
  emit('update:config', { ...props.config, [key]: value })
}
</script>

<style scoped>
.type-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.type-hint code {
  background: var(--el-fill-color-light);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}
</style>
