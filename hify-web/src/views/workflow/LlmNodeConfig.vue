<template>
  <div>
    <el-form-item label="做什么">
      <el-input
        :model-value="config.prompt"
        type="textarea" :rows="5"
        :placeholder="placeholder"
        @update:model-value="updateField('prompt', $event)"
      />
      <div class="type-hint">告诉 AI 要做什么。可以用 <code v-pre>{{节点名.字段}}</code> 引用前面步骤的结果</div>
    </el-form-item>
    <el-form-item label="最多重试">
      <el-input-number :model-value="config.maxRetries" :min="0" :max="5" @update:model-value="updateField('maxRetries', $event)" />
    </el-form-item>
    <el-form-item label="工具调用">
      <el-switch :model-value="config.toolsEnabled" size="small" @update:model-value="updateField('toolsEnabled', $event)" />
      <span class="type-hint">开启后 LLM 可自主判断是否调用 Agent 绑定的全部 MCP 工具</span>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ config: Record<string, any> }>()
const emit = defineEmits<{ 'update:config': [value: Record<string, any>] }>()

const placeholder = `告诉 AI 要做什么，比如：

你是一个客服助手。分析用户消息，提取意图和订单号。
仅返回一行 JSON：
{"intent": "查订单|退款|其他", "orderId": "订单号或null", "hasOrderId": true或false}

用户消息：{{input.user_message}}`

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
