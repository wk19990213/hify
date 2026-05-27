<template>
  <el-dialog v-model="visible" :title="isLogin ? '登录' : '注册'" width="400px" :close-on-click-modal="false" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="0" @submit.prevent>
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" @keydown.enter="handleSubmit" />
      </el-form-item>
      <el-form-item v-if="!isLogin" prop="displayName">
        <el-input v-model="form.displayName" placeholder="显示名称（可选）" />
      </el-form-item>
      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" style="margin-bottom: 16px" />
      <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
        {{ isLogin ? '登 录' : '注 册' }}
      </el-button>
    </el-form>
    <template #footer>
      <div class="auth-footer">
        <el-button link type="primary" @click="isLogin = !isLogin; errorMsg = ''">
          {{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuth } from '@/composables/useAuth'
import type { FormInstance, FormRules } from 'element-plus'

const { login, register } = useAuth()

const visible = ref(false)
const isLogin = ref(true)
const loading = ref(false)
const errorMsg = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  displayName: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度 3~50', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '长度 6~100', trigger: 'blur' },
  ],
}

function open(mode?: 'login' | 'register') {
  isLogin.value = mode !== 'register'
  errorMsg.value = ''
  form.username = ''
  form.password = ''
  form.displayName = ''
  visible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  errorMsg.value = ''
  try {
    if (isLogin.value) {
      await login(form.username, form.password)
    } else {
      await register(form.username, form.password, form.displayName || undefined)
    }
    visible.value = false
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || e?.message || '操作失败'
  } finally {
    loading.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.auth-footer {
  display: flex;
  justify-content: center;
  width: 100%;
}
</style>
