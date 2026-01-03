// 创建 axios 实例
const pathname = window.location.pathname || '/';
const basePath = pathname.replace(/\/logs\/ui\/?$/, '');
const service = axios.create({
    // 如果需要统一前缀，可以在这里设置，例如 '/api'
    // baseURL: process.env.VUE_APP_BASE_API,
    baseURL: basePath || undefined,
    timeout: 15000 // 请求超时时间
});

// request 拦截器
service.interceptors.request.use(
    config => {
        // 在这里可以添加 token 等头部信息
        // if (store.getters.token) {
        //   config.headers['Authorization'] = getToken()
        // }
        return config;
    },
    error => {
        console.error('Request Error:', error);
        return Promise.reject(error);
    }
);

// response 拦截器
service.interceptors.response.use(
    response => {
        const res = response.data;
        // 这里可以根据后端约定的状态码进行统一处理
        // 例如 code !== 200 则提示错误
        return res.data || res;
    },
    error => {
        console.error('Response Error:', error);
        // 可以结合 ElementPlus 提示错误
        if (window.ElementPlus && window.ElementPlus.ElMessage) {
            window.ElementPlus.ElMessage.error(error.message || '请求失败');
        }
        return Promise.reject(error);
    }
);

export default service;
