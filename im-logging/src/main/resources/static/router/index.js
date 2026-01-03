const {createRouter, createWebHashHistory} = VueRouter;
import {load} from '../lib/loader.js';

const pathname = window.location.pathname || '/';
const basePath = pathname.replace(/\/logs\/ui\/?$/, '');
const routerBase = pathname.replace(/\/$/, '');

const routes = [
    {
        path: '/',
        component: () => load(`${basePath}/view/Home.vue`),
        children: [
            {path: '', redirect: '/logs'},
            {path: 'logs', component: () => load(`${basePath}/view/Logs.vue`)},
            {path: 'manage', component: () => load(`${basePath}/view/LogManage.vue`)},
            {path: 'analysis', component: () => load(`${basePath}/view/LogAnalysis.vue`)}
        ]
    },
    {
        path: '/error',
        component: () => load(`${basePath}/view/Error.vue`)
    }
];

const router = createRouter({
    history: createWebHashHistory(routerBase),
    routes
});

export default router;
