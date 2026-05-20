<template>
  <div class="kb-page">
    <div class="page-header">
      <div class="header-left">
        <h1>知识库管理</h1>
        <p>上传文档，RAG 检索增强生成</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="handleCreateKB">新建知识库</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col v-for="kb in kbList" :key="kb.id" :span="8">
        <el-card class="kb-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="kb-name">{{ kb.name }}</span>
              <el-button type="danger" link :icon="Delete" @click="handleDeleteKB(kb)" />
            </div>
          </template>
          <p class="kb-desc">{{ kb.description || '暂无描述' }}</p>
          <div class="kb-meta">
            <el-tag size="small">文档: {{ kb.documentCount }}</el-tag>
            <el-tag size="small" type="info">{{ kb.embeddingModel }}</el-tag>
          </div>

          <!-- 文档上传 -->
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="(file: any) => handleUpload(kb.id, file)"
              accept=".pdf,.docx,.md,.txt"
            >
              <el-button size="small" :icon="Upload" type="primary" plain>上传文档</el-button>
            </el-upload>
          </div>

          <!-- RAG 查询 -->
          <div class="query-area">
            <el-input v-model="queries[kb.id]" size="small" placeholder="输入问题检索知识库..." @keyup.enter="handleQuery(kb.id)" />
          </div>

          <!-- 查询结果 -->
          <div v-if="answers[kb.id]" class="answer-box">
            <div class="answer-content">{{ answers[kb.id] }}</div>
            <div class="answer-meta" v-if="latencies[kb.id]">延迟 {{ latencies[kb.id] }}ms</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建知识库弹窗 -->
    <el-dialog v-model="dialogVisible" title="新建知识库" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doCreateKB" :loading="creating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Delete, Upload } from '@element-plus/icons-vue'
import { createKB, listKB, deleteKB, uploadDocument, queryKB } from '@/api/knowledge'
import type { KnowledgeBase } from '@/api/knowledge'
import { notifySuccess, notifyError } from '@/utils/notify'

const kbList = ref<KnowledgeBase[]>([])
const dialogVisible = ref(false)
const creating = ref(false)
const form = reactive({ name: '', description: '' })
const queries = reactive<Record<number, string>>({})
const answers = reactive<Record<number, string>>({})
const latencies = reactive<Record<number, number>>({})

onMounted(() => loadKBList())

const loadKBList = async () => {
  try {
    const res = await listKB()
    kbList.value = res.list
  } catch { /* ignore */ }
}

const handleCreateKB = () => {
  form.name = ''; form.description = ''; dialogVisible.value = true
}

const doCreateKB = async () => {
  if (!form.name.trim()) return
  creating.value = true
  try {
    await createKB(form.name, form.description)
    notifySuccess('知识库创建成功')
    dialogVisible.value = false
    loadKBList()
  } catch {
    notifyError('创建失败')
  } finally { creating.value = false }
}

const handleDeleteKB = async (kb: KnowledgeBase) => {
  try {
    await deleteKB(kb.id)
    notifySuccess('删除成功')
    loadKBList()
  } catch { notifyError('删除失败') }
}

const handleUpload = async (kbId: number, file: File) => {
  try {
    const doc = await uploadDocument(kbId, file)
    notifySuccess(`上传完成: ${doc.name}, ${doc.chunkCount} 个分块`)
    loadKBList()
  } catch { notifyError('上传失败') }
  return false // 阻止 el-upload 默认上传
}

const handleQuery = async (kbId: number) => {
  const q = queries[kbId]?.trim()
  if (!q) return
  try {
    const res = await queryKB(kbId, q)
    answers[kbId] = res.answer
    latencies[kbId] = res.latencyMs
  } catch { notifyError('查询失败') }
}
</script>

<style scoped>
.kb-page { padding: 24px; max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h1 { margin: 0; font-size: 24px; }
.page-header p { color: var(--text-secondary); font-size: 14px; margin: 4px 0 0; }

.kb-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.kb-name { font-weight: 600; font-size: 16px; }
.kb-desc { color: var(--text-secondary); font-size: 14px; min-height: 20px; margin: 0 0 12px; }
.kb-meta { display: flex; gap: 8px; margin-bottom: 12px; }

.upload-area { margin-bottom: 12px; }
.query-area { margin-bottom: 12px; }

.answer-box { background: var(--bg-secondary); border-radius: 8px; padding: 12px; margin-top: 8px; }
.answer-content { font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
.answer-meta { font-size: 12px; color: var(--text-secondary); margin-top: 8px; }
</style>
