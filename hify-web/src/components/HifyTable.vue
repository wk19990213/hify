<template>
  <div class="hify-table-wrapper">
    <el-table
      v-loading="loading"
      :data="tableData"
      stripe
      class="hify-table"
      @sort-change="handleSortChange"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align || 'left'"
        :sortable="col.sortable"
        :fixed="col.fixed"
      >
        <template #default="scope">
          <!-- 使用动态 slot -->
          <slot
            v-if="col.slot"
            :name="col.slot"
            v-bind="scope"
          />
          <span v-else-if="col.formatter">{{ col.formatter(scope.row[col.prop], scope.row) }}</span>
          <span v-else-if="col.type === 'datetime'">{{ formatDateTime(scope.row[col.prop]) }}</span>
          <span v-else>{{ scope.row[col.prop] }}</span>
        </template>
      </el-table-column>

      <!-- 空状态 -->
      <template #empty>
        <el-empty
          :description="emptyText"
          :image-size="120"
        >
          <template #image>
            <div class="empty-icon">
              <el-icon :size="60" color="var(--gray-300)">
                <Document />
              </el-icon>
            </div>
          </template>
        </el-empty>
      </template>
    </el-table>

    <!-- 分页 -->
    <div v-if="showPagination && total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="pageSizes"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts" generic="T extends Record<string, any>">
import { ref, watch, onMounted } from 'vue'
import { Document } from '@element-plus/icons-vue'
import type { PageResult } from '@/types/common'
import { formatDateTime } from '@/utils/date'

// 列配置类型
export interface TableColumn<T = any> {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  fixed?: 'left' | 'right' | boolean
  slot?: string
  formatter?: (val: any, row: T) => string
  type?: 'datetime'
}

// API 类型
export type FetchApi<T> = (params: {
  page: number
  pageSize: number
  sortField?: string
  sortOrder?: string
  [key: string]: any
}) => Promise<PageResult<T>>

// Props
interface Props {
  columns: TableColumn<T>[]
  api: FetchApi<T>
  showPagination?: boolean
  pageSizes?: number[]
  defaultPageSize?: number
  emptyText?: string
  extraParams?: Record<string, any>
}

const props = withDefaults(defineProps<Props>(), {
  showPagination: true,
  pageSizes: () => [10, 20, 50, 100],
  defaultPageSize: 20,
  emptyText: '暂无数据',
  extraParams: () => ({})
})

// Emits
const emit = defineEmits<{
  'data-loaded': [data: T[]]
  'data-error': [error: any]
}>()

// Slots 类型定义
defineSlots<{
  [key: string]: (props: {
    row: T
    $index: number
    [key: string]: any
  }) => any
}>()

// 状态
const loading = ref(false)
const tableData = ref<T[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(props.defaultPageSize)
const sortField = ref('')
const sortOrder = ref('')

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
      ...(sortField.value && {
        sortField: sortField.value,
        sortOrder: sortOrder.value
      }),
      ...props.extraParams
    }

    const res = await props.api(params)
    tableData.value = res.list || []
    total.value = res.total || 0
    emit('data-loaded', tableData.value as T[])
  } catch (error) {
    console.error('Failed to fetch table data:', error)
    emit('data-error', error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 刷新（暴露给外部）
const refresh = (resetPage = false) => {
  if (resetPage) {
    page.value = 1
  }
  fetchData()
}

// 分页事件
const handlePageChange = (val: number) => {
  page.value = val
  fetchData()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  page.value = 1
  fetchData()
}

// 排序事件
const handleSortChange = ({ prop, order }: { prop: string; order: string }) => {
  sortField.value = prop || ''
  sortOrder.value = order === 'ascending' ? 'asc' : order === 'descending' ? 'desc' : ''
  fetchData()
}

// 监听额外参数变化
watch(() => props.extraParams, () => {
  refresh(true)
}, { deep: true })

// 初始化
onMounted(() => {
  fetchData()
})

// 暴露方法
defineExpose({
  refresh,
  tableData,
  total,
  loading,
  page,
  pageSize
})
</script>

<style scoped>
.hify-table-wrapper {
  background: var(--bg-primary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.hify-table {
  --el-table-header-bg-color: var(--bg-secondary);
  --el-table-row-hover-bg-color: var(--bg-secondary);
}

.hify-table :deep(th.el-table__cell) {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: var(--space-2) var(--space-3);
  background: var(--bg-secondary);
}

.hify-table :deep(td.el-table__cell) {
  padding: var(--space-3);
  font-size: var(--text-sm);
  color: var(--text-primary);
}

/* 空状态自定义 */
.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  margin-bottom: var(--space-4);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--border-light);
}

.pagination-wrapper :deep(.el-pagination) {
  font-weight: var(--font-medium);
}

.pagination-wrapper :deep(.el-pager li) {
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.pagination-wrapper :deep(.el-pager li.is-active) {
  background: var(--primary-600);
  box-shadow: var(--shadow-primary);
}
</style>
