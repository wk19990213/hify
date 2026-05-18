<template>
  <div class="design-system-demo">
    <!-- 标题 -->
    <header class="demo-header">
      <h1 class="demo-title">Hify Design System</h1>
      <p class="demo-subtitle">浅底 + 科技感 / 蓝紫主色 / 青绿辅色</p>
    </header>

    <!-- 主色阶 -->
    <section class="demo-section">
      <h2 class="demo-section-title">主色阶（蓝紫系）</h2>
      <div class="color-grid">
        <div v-for="(color, i) in primaryScale" :key="i" class="color-swatch">
          <div class="color-box" :style="{ background: color.hex }"></div>
          <span class="color-name">{{ color.name }}</span>
          <span class="color-hex">{{ color.hex }}</span>
        </div>
      </div>
    </section>

    <!-- 辅色阶 -->
    <section class="demo-section">
      <h2 class="demo-section-title">辅色阶（青色/薄荷绿）</h2>
      <div class="color-grid">
        <div v-for="(color, i) in accentScale" :key="i" class="color-swatch">
          <div class="color-box" :style="{ background: color.hex }"></div>
          <span class="color-name">{{ color.name }}</span>
          <span class="color-hex">{{ color.hex }}</span>
        </div>
      </div>
    </section>

    <!-- 按钮展示 -->
    <section class="demo-section">
      <h2 class="demo-section-title">按钮样式</h2>
      <div class="demo-row">
        <button class="hify-btn hify-btn-primary">主按钮 Primary</button>
        <button class="hify-btn hify-btn-secondary">次要按钮 Secondary</button>
        <button class="hify-btn hify-btn-accent">辅色按钮 Accent</button>
      </div>
    </section>

    <!-- Element Plus 按钮 -->
    <section class="demo-section">
      <h2 class="demo-section-title">Element Plus 按钮</h2>
      <div class="demo-row">
        <el-button type="primary">主按钮</el-button>
        <el-button type="success">成功</el-button>
        <el-button type="warning">警告</el-button>
        <el-button type="danger">危险</el-button>
        <el-button>默认按钮</el-button>
        <el-button type="primary" plain>朴素按钮</el-button>
      </div>
    </section>

    <!-- 标签 Tag -->
    <section class="demo-section">
      <h2 class="demo-section-title">标签 Tag</h2>
      <div class="demo-row">
        <span class="hify-tag hify-tag-primary">Primary</span>
        <span class="hify-tag hify-tag-accent">Accent</span>
        <span class="hify-tag hify-tag-success">Success</span>
      </div>
      <div class="demo-row" style="margin-top: 12px;">
        <el-tag type="primary">标签一</el-tag>
        <el-tag type="success">标签二</el-tag>
        <el-tag type="warning">标签三</el-tag>
        <el-tag type="danger">标签四</el-tag>
        <el-tag type="info">标签五</el-tag>
      </div>
    </section>

    <!-- 状态指示 -->
    <section class="demo-section">
      <h2 class="demo-section-title">状态指示器</h2>
      <div class="demo-row">
        <span class="hify-status hify-status-success">
          <span class="hify-status-dot"></span>
          <span>运行中</span>
        </span>
        <span class="hify-status hify-status-warning">
          <span class="hify-status-dot"></span>
          <span>警告</span>
        </span>
        <span class="hify-status hify-status-error">
          <span class="hify-status-dot"></span>
          <span>错误</span>
        </span>
        <span class="hify-status hify-status-processing">
          <span class="hify-status-dot"></span>
          <span>处理中</span>
        </span>
      </div>
    </section>

    <!-- 卡片 -->
    <section class="demo-section">
      <h2 class="demo-section-title">卡片组件</h2>
      <div class="demo-row">
        <div class="hify-card" style="width: 320px; padding: 24px;">
          <h3 style="font-size: 16px; font-weight: 600; margin-bottom: 8px;">Agent 配置</h3>
          <p style="color: var(--text-secondary); font-size: 14px;">配置您的 AI Agent 模型参数和行为设置</p>
          <div style="margin-top: 16px;">
            <el-button type="primary" size="small">编辑配置</el-button>
          </div>
        </div>

        <el-card style="width: 320px; margin-left: 16px;">
          <template #header>
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-icon><InfoFilled /></el-icon>
              <span style="font-weight: 600;">Element Plus 卡片</span>
            </div>
          </template>
          <p style="color: var(--text-secondary);">使用 Element Plus 的 Card 组件，自动应用主题样式</p>
          <div style="margin-top: 16px;">
            <el-tag type="primary">默认标签</el-tag>
          </div>
        </el-card>
      </div>
    </section>

    <!-- 输入框 -->
    <section class="demo-section">
      <h2 class="demo-section-title">输入组件</h2>
      <div class="demo-row" style="align-items: flex-start;">
        <div style="width: 300px;">
          <input class="hify-input" placeholder="原生输入框" />
        </div>
        <div style="width: 300px; margin-left: 16px;">
          <el-input v-model="inputValue" placeholder="Element Plus 输入框" />
        </div>
      </div>
    </section>

    <!-- 表格 -->
    <section class="demo-section">
      <h2 class="demo-section-title">表格</h2>
      <el-table :data="tableData" style="width: 100%" stripe>
        <el-table-column prop="name" label="Agent 名称" />
        <el-table-column prop="model" label="模型" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.statusType">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tokens" label="Token 消耗" align="right" />
      </el-table>
    </section>

    <!-- 阴影层级 -->
    <section class="demo-section">
      <h2 class="demo-section-title">阴影层级</h2>
      <div class="demo-row">
        <div class="shadow-demo" style="box-shadow: var(--shadow-sm)">Shadow SM</div>
        <div class="shadow-demo" style="box-shadow: var(--shadow-md)">Shadow MD</div>
        <div class="shadow-demo" style="box-shadow: var(--shadow-lg)">Shadow LG</div>
        <div class="shadow-demo" style="box-shadow: var(--shadow-xl)">Shadow XL</div>
        <div class="shadow-demo" style="box-shadow: var(--shadow-primary)">Primary</div>
      </div>
    </section>

    <!-- 圆角 -->
    <section class="demo-section">
      <h2 class="demo-section-title">圆角系统</h2>
      <div class="demo-row">
        <div class="radius-demo" style="border-radius: var(--radius-none)">None</div>
        <div class="radius-demo" style="border-radius: var(--radius-sm)">SM</div>
        <div class="radius-demo" style="border-radius: var(--radius-md)">MD</div>
        <div class="radius-demo" style="border-radius: var(--radius-lg)">LG</div>
        <div class="radius-demo" style="border-radius: var(--radius-xl)">XL</div>
        <div class="radius-demo" style="border-radius: var(--radius-full)">Full</div>
      </div>
    </section>

    <!-- 动画 -->
    <section class="demo-section">
      <h2 class="demo-section-title">过渡动效</h2>
      <div class="demo-row">
        <button class="hify-btn hify-btn-primary" @click="showAnimation = true">触发动画</button>
      </div>
      <div v-if="showAnimation" class="demo-row" style="margin-top: 16px;">
        <div class="anim-demo animate-fade-in">Fade In</div>
        <div class="anim-demo animate-fade-in-up">Fade In Up</div>
        <div class="anim-demo animate-scale-in">Scale In</div>
      </div>
    </section>

    <!-- 侧边栏模拟 -->
    <section class="demo-section">
      <h2 class="demo-section-title">深色侧边栏</h2>
      <div class="demo-row" style="background: var(--bg-dark); padding: 24px; border-radius: var(--radius-lg);">
        <div style="width: 240px; background: linear-gradient(180deg, var(--bg-dark) 0%, var(--bg-dark-elevated) 100%); border-radius: var(--radius-lg); padding: 16px;">
          <div style="color: white; font-weight: 600; font-size: 18px; margin-bottom: 24px; display: flex; align-items: center; gap: 8px;">
            <span style="width: 28px; height: 28px; background: linear-gradient(135deg, var(--primary-500), var(--accent-500)); border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 12px;">H</span>
            Hify
          </div>
          <nav style="display: flex; flex-direction: column; gap: 4px;">
            <a class="hify-sidebar-item active" style="padding: 10px 12px; display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 14px;">Dashboard</span>
            </a>
            <a class="hify-sidebar-item" style="padding: 10px 12px; display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 14px;">Agents</span>
            </a>
            <a class="hify-sidebar-item" style="padding: 10px 12px; display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 14px;">Knowledge</span>
            </a>
            <a class="hify-sidebar-item" style="padding: 10px 12px; display: flex; align-items: center; gap: 8px;">
              <span style="font-size: 14px;">Settings</span>
            </a>
          </nav>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { InfoFilled } from '@element-plus/icons-vue'

const inputValue = ref('')
const showAnimation = ref(false)

const primaryScale = [
  { name: 'primary-50', hex: '#f5f3ff' },
  { name: 'primary-100', hex: '#ede9fe' },
  { name: 'primary-200', hex: '#ddd6fe' },
  { name: 'primary-300', hex: '#c4b5fd' },
  { name: 'primary-400', hex: '#a78bfa' },
  { name: 'primary-500', hex: '#8b5cf6' },
  { name: 'primary-600', hex: '#7c3aed' },
  { name: 'primary-700', hex: '#6d28d9' },
  { name: 'primary-800', hex: '#5b21b6' },
  { name: 'primary-900', hex: '#4c1d95' },
]

const accentScale = [
  { name: 'accent-50', hex: '#ecfeff' },
  { name: 'accent-100', hex: '#cffafe' },
  { name: 'accent-200', hex: '#a5f3fc' },
  { name: 'accent-300', hex: '#67e8f9' },
  { name: 'accent-400', hex: '#22d3ee' },
  { name: 'accent-500', hex: '#06b6d4' },
  { name: 'accent-600', hex: '#0891b2' },
  { name: 'accent-700', hex: '#0e7490' },
  { name: 'accent-800', hex: '#155e75' },
  { name: 'accent-900', hex: '#164e63' },
]

const tableData = [
  { name: '客服助手', model: 'GPT-4', status: '运行中', statusType: 'success', tokens: '1.2M' },
  { name: '代码审查', model: 'Claude 3', status: '已暂停', statusType: 'warning', tokens: '856K' },
  { name: '文档生成', model: 'Gemini Pro', status: '错误', statusType: 'danger', tokens: '234K' },
  { name: '数据分析', model: 'GPT-3.5', status: '运行中', statusType: 'success', tokens: '2.1M' },
]
</script>

<style scoped>
.design-system-demo {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-8);
  background: var(--bg-secondary);
  min-height: 100vh;
}

.demo-header {
  text-align: center;
  margin-bottom: var(--space-12);
  padding: var(--space-8) 0;
}

.demo-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  background: linear-gradient(135deg, var(--primary-600), var(--accent-500));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: var(--space-3);
}

.demo-subtitle {
  font-size: var(--text-lg);
  color: var(--text-secondary);
}

.demo-section {
  margin-bottom: var(--space-10);
  background: var(--bg-primary);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  box-shadow: var(--shadow-sm);
}

.demo-section-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--border-light);
}

.demo-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

/* 色板展示 */
.color-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: var(--space-2);
}

.color-swatch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.color-box {
  width: 100%;
  height: 60px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.color-name {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  font-weight: var(--font-medium);
}

.color-hex {
  font-size: 10px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* 阴影展示 */
.shadow-demo {
  width: 100px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

/* 圆角展示 */
.radius-demo {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-500);
  color: white;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

/* 动画展示 */
.anim-demo {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-500), var(--accent-500));
  color: white;
  border-radius: var(--radius-lg);
  font-weight: var(--font-medium);
}

@media (max-width: 768px) {
  .color-grid {
    grid-template-columns: repeat(5, 1fr);
  }

  .demo-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
