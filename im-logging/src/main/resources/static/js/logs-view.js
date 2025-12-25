const LogsComponent = {
    template: '#logs-template',
    setup() {
        const {ref, reactive, computed, onMounted, watch, getCurrentInstance} = Vue;

        // -----------------------------
        // UI 与状态
        // -----------------------------
        const ui = reactive({
            sidebarOpen: true,      // 侧栏展开/折叠
            liveMode: true,         // 实时 Tail 模式
            loading: false,         // 全局 loading
            wsConnected: false,     // websocket 连接状态
            detailVisible: false,   // 详情抽屉是否展开
            detail: null,           // 详情数据
            showTestIngest: false,  // 测试采集抽屉
            realtimeCount: 0,       // 实时接收到的日志计数
            realtimeLimit: 500      // 实时列表最大条目数
        });

        // 过滤条件与分页
        const filters = reactive({
            module: '',
            service: '',
            env: 'dev',
            level: '',
            keyword: '',
            range: [new Date(Date.now() - 3600 * 1000), new Date()] // 默认最近 1 小时
        });

        const pagination = reactive({page: 1, size: 100, total: 0});

        // 日志数据：logs 为静态查询结果；realtimeLogs 为实时 Tail
        const logs = ref([]);
        const realtimeLogs = ref([]);

        // 服务列表与筛选输入（侧栏）
        const services = ref([]);
        const uiServiceFilter = ref('');
        // 防抖定时器（服务筛选）
        let serviceFilterTimer = null;

        // Tree Ref
        const serviceTreeRef = ref(null);

        // 简单的测试发送负载
        const testPayload = reactive({
            module: 'im-logging',
            service: 'test-service',
            env: 'dev',
            level: 'INFO',
            message: 'This is a test log from UI.'
        });

        // stomp websocket 客户端引用
        let stompClient = null;

        // 可选的日志等级列表
        const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];

        // 日期快捷项
        const dateShortcuts = [
            {text: '最近15分钟', value: () => [new Date(Date.now() - 15 * 60 * 1000), new Date()]},
            {text: '最近1小时', value: () => [new Date(Date.now() - 3600 * 1000), new Date()]},
            {text: '最近24小时', value: () => [new Date(Date.now() - 24 * 3600 * 1000), new Date()]},
            {text: '今日', value: () => [new Date(new Date().setHours(0, 0, 0, 0)), new Date()]}
        ];

        // ECharts 实例
        let chart = null;

        // -----------------------------
        // 计算属性
        // -----------------------------
        const displayLogs = computed(() => ui.liveMode ? realtimeLogs.value : logs.value);

        // 服务树数据（将 services 转成 tree）
        const computedServiceTree = computed(() => {
            const filtered = services.value.filter(s => s.toLowerCase().includes(uiServiceFilter.value.toLowerCase()));
            return [{
                label: filters.env.toUpperCase(),
                children: filtered.map(s => ({label: s, value: s}))
            }];
        });

        const treeProps = {children: 'children', label: 'label'};

        // -----------------------------
        // 工具函数：格式化时间，JSON 高亮等
        // -----------------------------
        /** 格式化 ISO 时间 -> 本地短格式 */
        function formatTimestamp(iso) {
            if (!iso) return '';
            const d = new Date(iso);
            const pad = n => String(n).padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
        }

        /** JSON 美化并加 HTML 高亮（用于展示） */
        function prettyJson(obj) {
            try {
                const s = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
                // 安全转义
                const esc = s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                // 简单高亮（key/string/number/boolean/null）
                return esc.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(\.\d+)?)/g, match => {
                    let cls = 'json-number';
                    if (/^"/.test(match)) cls = /:$/.test(match) ? 'json-key' : 'json-string';
                    else if (/true|false/.test(match)) cls = 'json-boolean';
                    else if (/null/.test(match)) cls = 'json-null';
                    return `<span class="${cls}">${match}</span>`;
                });
            } catch (e) {
                return String(obj);
            }
        }

        /** 根据 level 返回 badge class */
        function levelBadgeClass(l) {
            if (!l) return 'pill-info';
            switch (l.toUpperCase()) {
                case 'TRACE':
                    return 'pill-trace';
                case 'DEBUG':
                    return 'pill-debug';
                case 'INFO':
                    return 'pill-info';
                case 'WARN':
                    return 'pill-warn';
                case 'ERROR':
                    return 'pill-error';
                default:
                    return 'pill-trace';
            }
        }

        /** 根据 traceId 构造追踪系统链接（可改为实际地址） */
        function traceLink(traceId) {
            return `/trace/${encodeURIComponent(traceId)}`;
        }

        // -----------------------------
        // 数据加载：服务列表、日志、直方图
        // -----------------------------
        /** 加载服务列表（支持 env 参数） */
        async function loadServices() {
            try {
                const res = await axios.get('/api/logs/meta/services', {params: {env: filters.env}});
                // 兼容未严格返回 data.data 的情况
                services.value = res?.data?.data || res?.data || [];
            } catch (e) {
                console.warn('loadServices error', e);
                services.value = [];
            }
        }

        /** 查询日志（非实时模式） */
        async function searchLogs() {
            if (ui.liveMode) return; // 实时模式由 ws 提供
            ui.loading = true;
            try {
                const params = {
                    module: filters.module || undefined,
                    service: filters.service || undefined,
                    env: filters.env || undefined,
                    level: filters.level || undefined,
                    keyword: filters.keyword || undefined,
                    page: pagination.page - 1,
                    size: pagination.size
                };
                if (filters.range && filters.range.length === 2) {
                    params.start = filters.range[0].toISOString();
                    params.end = filters.range[1].toISOString();
                }
                const res = await axios.get('/api/logs', {params});
                logs.value = res?.data?.data || res?.data || [];
                // 如果后端返回总数，请替换为实际字段
                pagination.total = res?.data?.total ?? Math.max(0, logs.value.length);
            } catch (e) {
                console.error('searchLogs failed', e);
                logs.value = [];
            } finally {
                ui.loading = false;
            }
        }

        // -----------------------------
        // 实时 WebSocket 连接（SockJS + STOMP）
        // -----------------------------
        function connectSocket() {
            try {
                const socket = new SockJS('/ws');
                stompClient = Stomp.over(socket);
                // 关闭内置 debug 输出
                stompClient.debug = null;
                stompClient.connect({}, frame => {
                    ui.wsConnected = true;
                    // 订阅日志 topic（后端需要推送到 /topic/logs）
                    stompClient.subscribe('/topic/logs', msg => {
                        if (!ui.liveMode) return;
                        try {
                            const payload = JSON.parse(msg.body);
                            handleRealtimeLog(payload);
                        } catch (e) {
                            console.warn('invalid realtime payload', e);
                        }
                    });
                }, error => {
                    ui.wsConnected = false;
                    console.warn('stomp connect failed', error);
                    // 自动重连（指数回退可在生产中实现）
                    setTimeout(connectSocket, 3000);
                });
            } catch (e) {
                console.error('connectSocket error', e);
                ui.wsConnected = false;
            }
        }

        /** 处理单条实时日志（客户端过滤 + 限量） */
        function handleRealtimeLog(logItem) {
            // 客户端基础过滤：关键字匹配（可扩展）
            if (filters.keyword && !JSON.stringify(logItem).toLowerCase().includes(filters.keyword.toLowerCase())) return;
            realtimeLogs.value.unshift(logItem);
            ui.realtimeCount++;
            if (realtimeLogs.value.length > ui.realtimeLimit) realtimeLogs.value.length = ui.realtimeLimit;
        }

        /** 切换实时模式 */
        function toggleLiveMode(v) {
            if (v) {
                // 切到实时：清空实时缓存并确保已连接
                realtimeLogs.value = [];
                ui.realtimeCount = 0;
                if (!ui.wsConnected) connectSocket();
            } else {
                // 离开实时：加载普通查询
                searchLogs();
            }
        }

        // -----------------------------
        // 交互事件
        // -----------------------------
        function onEnvChange() {
            loadServices();
            searchLogs();
        }

        function onServiceNodeClick(node) {
            if (node && node.value) {
                filters.service = node.value;
                if (!ui.liveMode) searchLogs();
            }
        }

        function onServiceFilterInput() {
            // 防抖：避免在每次按键都触发树过滤或远程搜索
            if (serviceFilterTimer) clearTimeout(serviceFilterTimer);
            serviceFilterTimer = setTimeout(() => {
                // 触发 tree 的内部过滤（在 template 中引用 ref）
                try {
                    const tree = serviceTreeRef.value;
                    if (tree && tree.filter) tree.filter(uiServiceFilter.value);
                } catch (e) { /* ignore */
                }
            }, 250);
        }

        function onPageChange(page) {
            pagination.page = page;
            searchLogs();
        }

        function onPageSizeChange(size) {
            pagination.size = size;
            pagination.page = 1;
            searchLogs();
        }

        /** 行点击打开详情 */
        function openDetail(row) {
            ui.detail = row;
            ui.detailVisible = true;
        }

        // -----------------------------
        // 导出、测试发送
        // -----------------------------
        function exportNdjson() {
            const params = new URLSearchParams();
            if (filters.service) params.append('service', filters.service);
            if (filters.env) params.append('env', filters.env);
            if (filters.level) params.append('level', filters.level);
            if (filters.keyword) params.append('keyword', filters.keyword);
            if (filters.range && filters.range.length === 2) {
                params.append('start', filters.range[0].toISOString());
                params.append('end', filters.range[1].toISOString());
            }
            const url = '/api/logs/export?' + params.toString();
            window.open(url, '_blank');
        }

        /** 发送测试日志（用于验证采集链路） */
        async function sendTestIngest() {
            try {
                const payload = {...testPayload, timestamp: new Date().toISOString()};
                await axios.post('/api/logs', payload);
                ElementPlus.ElMessage.success('测试日志已发送');
                ui.showTestIngest = false;
            } catch (e) {
                ElementPlus.ElMessage.error('发送失败：' + (e?.message || '未知错误'));
                console.error(e);
            }
        }

        // Dummy renderChart if not exists
        function renderChart(data) {
            // Placeholder for ECharts rendering
        }

        // -----------------------------
        // 生命周期：初始化
        // -----------------------------
        onMounted(() => {
            // 初始化数据
            loadServices();
            if (!ui.liveMode) searchLogs();
            // 建立 WS（如果后端未就绪，connectSocket 会自动重连）
            connectSocket();
            // 初始化空图表
            renderChart({buckets: [], counts: []});
        });

        // watch: env 或 service 变化自动刷新
        watch(() => filters.service, () => {
            if (!ui.liveMode) searchLogs();
        });

        // 返回给模板使用的变量和方法
        return {
            ui, filters, pagination, logs, realtimeLogs, services,
            testPayload, levels, dateShortcuts,
            displayLogs, computedServiceTree, treeProps,
            formatTimestamp, prettyJson, levelBadgeClass, traceLink,
            loadServices, searchLogs, connectSocket,
            toggleLiveMode, onEnvChange, onServiceNodeClick, onServiceFilterInput,
            onPageChange, onPageSizeChange, openDetail, exportNdjson, sendTestIngest,
            serviceTreeRef
        };
    }
};
