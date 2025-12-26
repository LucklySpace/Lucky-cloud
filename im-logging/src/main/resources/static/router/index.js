const {createRouter, createWebHashHistory} = VueRouter;
import {load} from '../lib/loader.js';

const routes = [
    {
        path: '/',
        component: () => load('/view/Home.vue'),
        children: [
            {path: '', redirect: '/logs'},
            {path: 'logs', component: () => load('/view/Logs.vue')},
            {path: 'manage', component: () => load('/view/LogManage.vue')},
            {path: 'analysis', component: () => load('/view/LogAnalysis.vue')}
        ]
    },
    {
        path: '/error',
        component: () => load('/view/Error.vue')
    }
];

const router = createRouter({
    history: createWebHashHistory(),
    routes
});

export default router;
