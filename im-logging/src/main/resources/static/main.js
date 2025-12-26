const {createApp, defineAsyncComponent} = Vue;
import {load} from './lib/loader.js';
import router from './router/index.js';

const App = defineAsyncComponent(() => load('/App.vue'));

const app = createApp(App);

// 注册 ElementPlus
app.use(ElementPlus, {
    locale: ElementPlusLocaleZhCn
});

// 注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.use(router);
app.mount('#app');
