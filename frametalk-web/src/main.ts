import { createApp } from 'vue'

import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { useThemeStore } from './stores/useThemeStore'
import './styles/tokens.css'
import './styles/global.css'

const app = createApp(App)

app.use(pinia)
useThemeStore(pinia).initialize()
app.use(router)
app.mount('#app')
