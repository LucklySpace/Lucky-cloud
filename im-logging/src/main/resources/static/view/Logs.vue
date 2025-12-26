<template>
  <div class="app">
    <!-- 主布局：侧栏 + 内容 -->
    <div class="layout">
      <!-- 侧栏：服务树与筛选 -->
      <aside :class="['sidebar', ui.sidebarOpen ? '' : 'collapsed']" aria-label="侧栏" role="navigation">
        <el-input v-model="uiServiceFilter" clearable placeholder="搜索服务..." prefix-icon="Search"
                  size="large"></el-input>

        <div style="flex: 1; overflow: auto; margin-top: 12px">
          <el-tree :data="computedServiceTree" :highlight-current="true" :props="treeProps"
                   default-expand-all @node-click="onServiceNodeClick"></el-tree>
        </div>
      </aside>

      <!-- 主内容 -->
      <main class="content" role="main">
        <!-- 筛选与操作栏 -->
        <div class="filter-bar">
          <!-- 环境选择 -->
          <el-select v-model="filters.env" placeholder="环境" size="large" style="width: 120px" @change="onEnvChange">
            <el-option label="DEV" value="dev"></el-option>
            <el-option label="STAGE" value="test"></el-option>
            <el-option label="PROD" value="prod"></el-option>
          </el-select>

          <!-- 级别 -->
          <el-select v-model="filters.level" clearable placeholder="级别" size="large" style="width: 120px">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l"></el-option>
          </el-select>

          <!-- 关键字 -->
          <el-input v-model="filters.keyword" clearable placeholder="关键字、正则或 JSON 字段" size="large"
                    style="width: 300px"
                    @keyup.enter="searchLogs"></el-input>

          <!-- 时间范围 -->
          <el-date-picker v-model="filters.range" :shortcuts="dateShortcuts" end-placeholder="结束时间" range-separator="—"
                          size="large" start-placeholder="开始时间" style="width: 380px" type="datetimerange"
                          @change="searchLogs"></el-date-picker>

          <div style="flex: 1"></div>

          <!-- 实时开关 & 操作 -->
          <el-switch v-model="ui.liveMode" active-color="#10b981" active-text="实时Tail" inactive-color="#e5e7eb"
                     size="large" @change="toggleLiveMode"></el-switch>
          <el-button :loading="ui.loading" circle icon="Search" size="large" style="margin-left: 12px"
                     type="primary" @click="searchLogs"></el-button>
        </div>

        <!-- 日志表与详情 -->
        <section class="logs-shell">
          <div class="table-wrap">
            <el-table :data="displayLogs" border height="100%" size="default" stripe style="width: 100%"
                      @row-click="openDetail">
              <!-- 可展开 raw JSON -->
              <el-table-column type="expand" width="50">
                <template #default="props">
                  <div style="
                        background: var(--bg-card);
                        padding: 16px;
                        border-radius: 8px;
                        border: 1px solid var(--border);
                      ">
                    <pre class="json mono" v-html="prettyJson(props.row)"></pre>
                  </div>
                </template>
              </el-table-column>

              <!-- 时间 -->
              <el-table-column label="时间" width="200">
                <template #default="{ row }">
                  <div class="mono">{{ formatTimestamp(row.timestamp) }}</div>
                </template>
              </el-table-column>

              <!-- 等级 -->
              <el-table-column align="center" label="级别" width="100">
                <template #default="{ row }">
                  <span :class="['pill', levelBadgeClass(row.level)]">{{ row.level || 'INFO' }}</span>
                </template>
              </el-table-column>

              <!-- 服务 -->
              <el-table-column label="服务" prop="service" show-overflow-tooltip width="180"></el-table-column>

              <!-- message -->
              <el-table-column label="消息" min-width="360" prop="message" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="mono" style="
                        max-height: 56px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                      ">
                    {{ row.message }}
                  </div>
                </template>
              </el-table-column>

              <!-- trace id -->
              <el-table-column label="TraceID" width="220">
                <template #default="{ row }">
                  <el-link v-if="row.traceId" :href="traceLink(row.traceId)" class="mono text-base" target="_blank"
                           underline>{{ row.traceId }}
                  </el-link>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 底部：分页或实时计数 -->
          <div class="footer-bar">
            <div>
              <template v-if="!ui.liveMode">
                <el-pagination :current-page.sync="pagination.page" :page-size="pagination.size"
                               :total="pagination.total" background layout="total, sizes, prev, pager, next"
                               size="default"
                               @current-change="onPageChange" @size-change="onPageSizeChange"></el-pagination>
              </template>
              <template v-else>
                <div style="color: var(--muted); font-size: 14px;">
                  {{ ui.realtimeCount }} 条实时日志 · 显示最近 {{
                    ui.realtimeLimit
                  }} 条
                </div>
              </template>
            </div>

            <div style="color: var(--muted); font-size: 14px">
              Lucky IM · 日志服务面板
            </div>
          </div>
        </section>
      </main>
    </div>

    <!-- 日志详情抽屉 -->
    <el-drawer v-model="ui.detailVisible" size="50%" title="日志详情">
      <div v-if="ui.detail" style="padding: 20px">
        <el-descriptions border column="1" size="large">
          <el-descriptions-item label="时间">{{ formatTimestamp(ui.detail.timestamp) }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <span :class="['pill', levelBadgeClass(ui.detail.level)]">{{ ui.detail.level }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="服务">{{ ui.detail.service }}
            <span style="color: var(--muted)">({{ ui.detail.module || '-' }})</span></el-descriptions-item>
          <el-descriptions-item label="主机">{{ ui.detail.host || ui.detail.address || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="Trace / Span">{{ ui.detail.traceId || '-' }} / {{ ui.detail.spanId || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px; font-weight: 600; font-size: 16px;">Message</div>
        <div style="
              background: var(--bg-card);
              padding: 16px;
              border-radius: 8px;
              margin-top: 8px;
            ">
          <pre class="mono" style="margin: 0; white-space: pre-wrap">
        {{ ui.detail.message }}</pre>
        </div>

        <div v-if="ui.detail.exception" style="margin-top: 16px; font-weight: 600; color: #c62828; font-size: 16px;">
          Exception
        </div>
        <div v-if="ui.detail.exception" style="
              background: #fff5f5;
              padding: 16px;
              border-radius: 8px;
              margin-top: 8px;
              overflow: auto;
              color: #6b1a1a;
            ">
          <pre class="mono" style="margin: 0; white-space: pre-wrap">
        {{ ui.detail.exception }}</pre>
        </div>

        <div style="margin-top: 16px; font-weight: 600; font-size: 16px;">Raw JSON</div>
        <div style="
              background: var(--bg-card);
              padding: 16px;
              border-radius: 8px;
              margin-top: 8px;
              overflow: auto;
            ">
          <pre class="json mono" v-html="prettyJson(ui.detail)"></pre>
        </div>
      </div>
    </el-drawer>

    <!-- 测试数据采集抽屉 -->
    <el-drawer v-model="ui.showTestIngest" size="35%" title="测试数据采集">
      <el-form :model="testPayload" label-position="top" size="large" style="padding: 20px">
        <el-form-item label="环境">
          <el-radio-group v-model="testPayload.env">
            <el-radio label="dev">Dev</el-radio>
            <el-radio label="test">Test</el-radio>
            <el-radio label="prod">Prod</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="服务">
          <el-input v-model="testPayload.service"></el-input>
        </el-form-item>

        <el-form-item label="级别">
          <el-select v-model="testPayload.level" style="width: 100%">
            <el-option value="INFO">INFO</el-option>
            <el-option value="WARN">WARN</el-option>
            <el-option value="ERROR">ERROR</el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="消息">
          <el-input v-model="testPayload.message" rows="6" type="textarea"></el-input>
        </el-form-item>

        <el-button size="large" style="width: 100%" type="primary" @click="sendTestIngest">发送日志</el-button>
      </el-form>
    </el-drawer>
  </div>
</template>

<script>
import {getServices, searchLogs as searchLogsApi, sendTestLog} from '../api/logs.js';

export default {
  setup() {
    const {ref, reactive, computed, onMounted, watch} = Vue;

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
        const res = await getServices({env: filters.env});
        // 兼容未严格返回 data.data 的情况
        services.value = res?.data || res || [];
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
        const res = await searchLogsApi(params);
        logs.value = res?.data || res || [];
        // 如果后端返回总数，请替换为实际字段
        pagination.total = res?.total ?? Math.max(0, logs.value.length);
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

    // 服务筛选输入已通过 computedServiceTree 即时生效，无需额外事件

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
    // 测试发送
    // -----------------------------

    /** 发送测试日志（用于验证采集链路） */
    async function sendTestIngest() {
      try {
        const payload = {...testPayload, timestamp: new Date().toISOString()};
        await sendTestLog(payload);
        ElementPlus.ElMessage.success('测试日志已发送');
        ui.showTestIngest = false;
      } catch (e) {
        // Error handled in request.js interceptor or here
        // console.error(e);
      }
    }

    // （已移除图表渲染，占位函数删除）

    // -----------------------------
    // 生命周期：初始化
    // -----------------------------
    onMounted(() => {
      // 初始化数据
      loadServices();
      if (!ui.liveMode) searchLogs();
      // 建立 WS（如果后端未就绪，connectSocket 会自动重连）
      connectSocket();
      // 初始化完成
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
      toggleLiveMode, onEnvChange, onServiceNodeClick,
      onPageChange, onPageSizeChange, openDetail, sendTestIngest
    };
  }
}
</script>

<style lang="less" scoped>
/**
         * 全局视觉变量
         * 可根据公司设计语言调整对应颜色值
         */
:root {
  --bg-page: #f5f7fb;
  --bg-card: #ffffff;
  --border: #e6edf3;
  --text: #0f1722;
  --muted: #6b7280;
  --primary: #3b82f6;
  --shadow-sm: 0 1px 3px rgba(16, 24, 40, 0.04);
  --shadow-md: 0 6px 18px rgba(16, 24, 40, 0.06);
  --radius: 8px;
}

/* Reset & 基础布局 */
* {
  box-sizing: border-box;
}

.app {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 主布局：侧栏 + 内容 */
.layout {
  display: flex;
  flex: 1;
  min-height: 0;
  /* 让内部滚动容器正确生效 */
  overflow: hidden;
}

/* 侧栏 */
.sidebar {
  width: 260px;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.18s ease;
  padding: 16px;
  gap: 16px;
  box-shadow: var(--shadow-sm);
}

.sidebar.collapsed {
  width: 0;
  padding: 0;
  overflow: hidden;
  border: none;
  box-shadow: none;
}

/* 内容区 */
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  /* 允许子元素溢出滚动 */
}

/* 筛选条 */
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
}

/* 图表容器 */
.chart-area {
  height: 160px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  transition: height 0.18s ease, padding 0.18s ease;
}

.chart-area.collapsed {
  height: 0;
  padding: 0;
  border: none;
  overflow: hidden;
}

/* 日志表格外壳：会撑开剩余高度并允许滚动 */
.logs-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

/* 表格本体占据剩余空间并滚动 */
.table-wrap {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.02));
}

/* 页脚分页区域 */
.footer-bar {
  padding: 16px 20px;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 视觉辅助小组件 */
.pill {
  padding: 6px 10px;
  border-radius: 6px;
  font-weight: 600;
  font-size: 13px;
  color: #fff;
  display: inline-block;
}

.pill-trace {
  background: #374151;
}

.pill-debug {
  background: #2563eb;
}

.pill-info {
  background: #059669;
}

.pill-warn {
  background: #d97706;
}

.pill-error {
  background: #dc2626;
}

/* 代码/JSON 高亮展示 */
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, "Consolas",
  monospace;
  font-size: 14px;
}

pre.json {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

/* 响应式：窄屏时隐藏侧栏 */
@media (max-width: 880px) {
  .sidebar {
    display: none;
  }

  .filter-bar {
    padding: 12px;
  }
}

</style>
