import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

// Hify 设计系统
import './styles/design-system.css'
import './styles/global.css'
import { elementPlusCSSVars } from './styles/element-plus-theme'

const app = createApp(App)

// 注入 Element Plus 主题覆盖样式
const styleEl = document.createElement('style')
styleEl.textContent = elementPlusCSSVars
styleEl.setAttribute('data-hify-theme', '')
document.head.appendChild(styleEl)

app.use(ElementPlus)
app.use(router)
app.mount('#app')
