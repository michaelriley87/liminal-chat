import { createApp } from 'vue'

import App from './App.vue'
import router from './router'

import './assets/theme.css'
import './assets/base.css'

createApp(App).use(router).mount('#app')
