<template>
  <div class="h-full flex flex-col bg-slate-50">
    <!-- Layout -->
    <div class="flex-1 flex min-h-0 overflow-hidden">
      <!-- Service Sidebar -->
      <aside
          :class="['w-64 bg-white border-r border-slate-200 flex flex-col transition-all duration-300 z-10', ui.sidebarOpen ? '' : '-ml-64']">
        <div class="p-4 border-b border-slate-100 bg-slate-50/50">
          <el-input v-model="uiServiceFilter" class="w-full" clearable placeholder="搜索服务..." prefix-icon="Search"/>
        </div>
        <div class="flex-1 overflow-y-auto p-2">
          <el-tree :data="computedServiceTree" :highlight-current="true" :props="treeProps" class="filter-tree"
                   default-expand-all @node-click="onServiceNodeClick">
            <template #default="{ node, data }">
              <div class="flex items-center text-sm py-1">
                <span :class="{ 'font-semibold text-indigo-600': node.isCurrent }">{{ node.label }}</span>
                <span v-if="!data.children"
                      class="ml-auto text-xs text-slate-400 bg-slate-100 px-1.5 rounded-full">Svc</span>
              </div>
            </template>
          </el-tree>
        </div>
        <div class="p-3 border-t border-slate-100 text-center">
          <el-button class="text-slate-400 hover:text-indigo-600" link size="small" type="primary"
                     @click="ui.sidebarOpen = false">
            <el-icon class="mr-1">
              <ArrowLeft/>
            </el-icon>
            收起侧栏
          </el-button>
        </div>
      </aside>

      <!-- Toggle Button (Visible when sidebar closed) -->
      <div v-if="!ui.sidebarOpen" class="absolute left-0 top-1/2 -translate-y-1/2 z-20">
        <el-button circle class="shadow-md" size="small" @click="ui.sidebarOpen = true">
          <el-icon>
            <ArrowRight/>
          </el-icon>
        </el-button>
      </div>

      <!-- Main Content -->
      <main class="flex-1 flex flex-col min-w-0 bg-white">
        <!-- Filter Bar -->
        <div class="p-4 border-b border-slate-100 bg-white shadow-sm flex flex-wrap gap-3 items-center z-10">
          <!-- Environment -->
          <el-select v-model="filters.env" class="w-28" placeholder="环境" @change="onEnvChange">
            <el-option label="DEV" value="dev"/>
            <el-option label="TEST" value="test"/>
            <el-option label="PROD" value="prod"/>
          </el-select>

          <!-- Service (Mobile/Fallback) -->
          <el-select v-model="filters.service" class="w-40" clearable filterable placeholder="选择服务">
            <el-option v-for="s in services" :key="s" :label="s" :value="s"/>
          </el-select>

          <!-- Level -->
          <el-select v-model="filters.level" class="w-32" clearable placeholder="日志级别">
            <el-option v-for="l in levels" :key="l" :label="l" :value="l">
              <span class="flex items-center">
                <span :class="['w-2 h-2 rounded-full mr-2', levelColorDot(l)]"></span>
                {{ l }}
              </span>
            </el-option>
          </el-select>

          <!-- Keyword -->
          <el-input v-model="filters.keyword" class="w-64" clearable placeholder="搜索关键字、TraceID..."
                    prefix-icon="Search"/>

          <div class="flex-1"></div>

          <!-- Realtime Status -->
          <div class="flex items-center text-xs space-x-3 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100">
            <span class="flex items-center text-slate-500">
              <span
                  :class="['w-2 h-2 rounded-full mr-1.5', ui.wsConnected ? 'bg-emerald-500 animate-pulse' : 'bg-rose-500']"></span>
              {{ ui.wsConnected ? '实时连接中' : '连接断开' }}
            </span>
            <span class="w-px h-3 bg-slate-300"></span>
            <span class="text-slate-500">
              缓冲: <span class="font-mono font-semibold text-slate-700">{{
                realtimeLogs.length
              }}</span> / {{ ui.realtimeLimit }}
            </span>
          </div>
        </div>

        <!-- Table Area -->
        <div class="flex-1 overflow-hidden relative">
          <el-table :data="displayLogs"
                    :header-cell-style="{ background: '#f8fafc', color: '#64748b', fontWeight: '600' }"
                    height="100%" highlight-current-row stripe style="width: 100%" @row-click="openDetail">
            <!-- Expand JSON -->
            <el-table-column type="expand" width="48">
              <template #default="props">
                <div class="p-4 bg-slate-50 border-y border-slate-100">
                  <div class="flex justify-between items-center mb-2">
                    <span class="text-xs font-bold text-slate-500 uppercase tracking-wider">Raw JSON Data</span>
                    <el-button bg size="small" text @click.stop="copyJson(props.row)">复制 JSON</el-button>
                  </div>
                  <pre class="text-xs font-mono bg-white p-3 rounded border border-slate-200 overflow-auto max-h-64"
                       v-html="prettyJson(props.row)"></pre>
                </div>
              </template>
            </el-table-column>

            <!-- Timestamp -->
            <el-table-column label="Time" width="180">
              <template #default="{ row }">
                <span class="text-xs font-mono text-slate-600">{{ formatTimestamp(row.timestamp) }}</span>
              </template>
            </el-table-column>

            <!-- Level -->
            <el-table-column align="center" label="Level" width="90">
              <template #default="{ row }">
                <span :class="['px-2 py-0.5 text-xs font-bold rounded shadow-sm', levelBadgeClass(row.level)]">
                  {{ row.level || 'INFO' }}
                </span>
              </template>
            </el-table-column>

            <!-- Service -->
            <el-table-column label="Service" prop="service" show-overflow-tooltip width="160">
              <template #default="{ row }">
                <span class="text-sm text-slate-700 font-medium">{{ row.service }}</span>
              </template>
            </el-table-column>

            <!-- Message -->
            <el-table-column label="Message" min-width="300" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="text-sm text-slate-600 font-mono">{{ row.message }}</span>
              </template>
            </el-table-column>

            <!-- TraceID -->
            <el-table-column label="TraceID" width="180">
              <template #default="{ row }">
                <span v-if="row.traceId"
                      class="text-xs font-mono text-indigo-600 bg-indigo-50 px-1.5 py-0.5 rounded cursor-pointer hover:bg-indigo-100 transition-colors">
                  {{ row.traceId }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </main>
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="ui.detailVisible" :with-header="false" destroy-on-close size="45%" title="日志详情">
      <div v-if="ui.detail" class="h-full flex flex-col">
        <!-- Drawer Header -->
        <div class="px-6 py-4 border-b border-slate-100 flex justify-between items-start bg-slate-50">
          <div>
            <h3 class="text-lg font-bold text-slate-800">Log Details</h3>
            <p class="text-xs text-slate-500 mt-1 font-mono">{{ ui.detail.traceId || 'No Trace ID' }}</p>
          </div>
          <span :class="['px-3 py-1 text-sm font-bold rounded shadow-sm', levelBadgeClass(ui.detail.level)]">
            {{ ui.detail.level || 'INFO' }}
          </span>
        </div>

        <!-- Drawer Content -->
        <div class="flex-1 overflow-y-auto p-6 space-y-6">
          <!-- Basic Info Grid -->
          <div class="grid grid-cols-2 gap-4">
            <div class="p-3 bg-slate-50 rounded-lg border border-slate-100">
              <span class="text-xs text-slate-400 block mb-1">Timestamp</span>
              <span class="text-sm font-mono text-slate-700">{{ formatTimestamp(ui.detail.timestamp) }}</span>
            </div>
            <div class="p-3 bg-slate-50 rounded-lg border border-slate-100">
              <span class="text-xs text-slate-400 block mb-1">Service</span>
              <span class="text-sm font-medium text-slate-700">{{ ui.detail.service }}</span>
            </div>
            <div class="p-3 bg-slate-50 rounded-lg border border-slate-100">
              <span class="text-xs text-slate-400 block mb-1">Host</span>
              <span class="text-sm font-mono text-slate-700">{{ ui.detail.host || ui.detail.address || '-' }}</span>
            </div>
            <div class="p-3 bg-slate-50 rounded-lg border border-slate-100">
              <span class="text-xs text-slate-400 block mb-1">Thread</span>
              <span :title="ui.detail.thread" class="text-sm font-mono text-slate-700 truncate">{{
                  ui.detail.thread ||
                  '-'
                }}</span>
            </div>
          </div>

          <!-- Message -->
          <div>
            <h4 class="text-sm font-bold text-slate-700 mb-2 flex items-center">
              <el-icon class="mr-1 text-slate-400">
                <Message/>
              </el-icon>
              Message
            </h4>
            <div
                class="p-4 bg-slate-50 rounded-lg border border-slate-200 text-sm font-mono text-slate-800 whitespace-pre-wrap break-all">
              {{ ui.detail.message }}
            </div>
          </div>

          <!-- Exception -->
          <div v-if="ui.detail.exception">
            <h4 class="text-sm font-bold text-rose-600 mb-2 flex items-center">
              <el-icon class="mr-1">
                <Warning/>
              </el-icon>
              Exception Stack Trace
            </h4>
            <div
                class="p-4 bg-rose-50 rounded-lg border border-rose-100 text-xs font-mono text-rose-800 whitespace-pre overflow-x-auto">
              {{ ui.detail.exception }}
            </div>
          </div>

          <!-- Context / MDC -->
          <div v-if="ui.detail.mdc && Object.keys(ui.detail.mdc).length > 0">
            <h4 class="text-sm font-bold text-slate-700 mb-2">MDC Context</h4>
            <div class="bg-slate-50 rounded border border-slate-200 overflow-hidden">
              <div v-for="(val, key) in ui.detail.mdc" :key="key" class="flex border-b border-slate-100 last:border-0">
                <div :title="key"
                     class="w-1/3 px-3 py-2 bg-slate-100 text-xs font-semibold text-slate-500 border-r border-slate-200 truncate">
                  {{ key }}
                </div>
                <div :title="val" class="w-2/3 px-3 py-2 text-xs font-mono text-slate-700 truncate">{{ val }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {getServices} from '../api/logs.js';

export default {
  setup() {
    const {ref, reactive, computed, onMounted, onUnmounted} = Vue;

    // UI State
    const ui = reactive({
      sidebarOpen: true,
      wsConnected: false,
      detailVisible: false,
      detail: null,
      realtimeLimit: 1000,
    });

    const filters = reactive({
      service: '',
      env: 'dev',
      level: '',
      keyword: '',
    });

    const realtimeLogs = ref([]);
    const services = ref([]);
    const uiServiceFilter = ref('');
    let ws = null;
    let retryTimer = null;
    let destroyed = false;
    const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];

    // Filtering Logic
    const matchesFilters = (item) => {
      if (filters.keyword) {
        const text = JSON.stringify(item).toLowerCase();
        if (!text.includes(filters.keyword.toLowerCase())) return false;
      }
      if (filters.env && item?.env && item.env.toLowerCase() !== filters.env.toLowerCase()) return false;
      if (filters.level && item?.level && item.level.toUpperCase() !== filters.level.toUpperCase()) return false;
      if (filters.service && item?.service && item.service !== filters.service) return false;
      return true;
    };

    const displayLogs = computed(() => {
      return realtimeLogs.value.filter(matchesFilters).slice(0, ui.realtimeLimit);
    });

    // Service Tree
    const computedServiceTree = computed(() => {
      const filtered = services.value.filter(s => s.toLowerCase().includes(uiServiceFilter.value.toLowerCase()));
      return [{
        label: filters.env.toUpperCase() + " Environment",
        children: filtered.map(s => ({label: s, value: s}))
      }];
    });
    const treeProps = {children: 'children', label: 'label'};

    // Formatters
    const formatTimestamp = (iso) => {
      if (!iso) return '';
      const d = new Date(iso);
      const pad = n => String(n).padStart(2, '0');
      return `${d.getHours()}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
    };

    const prettyJson = (obj) => {
      try {
        const s = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
        const esc = s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        return esc.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(\.\d+)?)/g, match => {
          let cls = 'text-rose-500'; // number
          if (/^"/.test(match)) cls = /:$/.test(match) ? 'text-purple-600' : 'text-green-600'; // key : string
          else if (/true|false/.test(match)) cls = 'text-blue-600'; // boolean
          else if (/null/.test(match)) cls = 'text-slate-400'; // null
          return `<span class="${cls}">${match}</span>`;
        });
      } catch (e) {
        return String(obj);
      }
    };

    const levelBadgeClass = (l) => {
      if (!l) return 'bg-slate-100 text-slate-600';
      switch (l.toUpperCase()) {
        case 'TRACE':
          return 'bg-slate-100 text-slate-500';
        case 'DEBUG':
          return 'bg-blue-100 text-blue-700';
        case 'INFO':
          return 'bg-emerald-100 text-emerald-700';
        case 'WARN':
          return 'bg-amber-100 text-amber-700';
        case 'ERROR':
          return 'bg-rose-100 text-rose-700';
        default:
          return 'bg-slate-100 text-slate-600';
      }
    };

    const levelColorDot = (l) => {
      switch (l.toUpperCase()) {
        case 'TRACE':
          return 'bg-slate-400';
        case 'DEBUG':
          return 'bg-blue-500';
        case 'INFO':
          return 'bg-emerald-500';
        case 'WARN':
          return 'bg-amber-500';
        case 'ERROR':
          return 'bg-rose-500';
        default:
          return 'bg-slate-400';
      }
    }

    // Actions
    const loadServices = async () => {
      try {
        const res = await getServices({env: filters.env});
        services.value = res?.data || res || [];
      } catch (e) {
        services.value = [];
      }
    };

    const connectSocket = () => {
      try {
        if (retryTimer) {
          clearTimeout(retryTimer);
          retryTimer = null;
        }
        if (ws) {
          ws.onclose = null;
          ws.close();
          ws = null;
        }
        const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
        const pathname = location.pathname || '/';
        const basePath = pathname.replace(/\/logs\/ui\/?$/, '');
        const url = `${scheme}://${location.host}${basePath}/ws`;
        ws = new WebSocket(url);
        ws.onopen = () => {
          ui.wsConnected = true;
        };
        ws.onmessage = (evt) => {
          try {
            const payload = JSON.parse(evt.data);
            handleRealtimeLog(payload);
          } catch (e) {
          }
        };
        ws.onerror = () => {
          ui.wsConnected = false;
        };
        ws.onclose = () => {
          ui.wsConnected = false;
          if (destroyed) return;
          retryTimer = setTimeout(connectSocket, 3000);
        };
      } catch (e) {
        ui.wsConnected = false;
      }
    };

    const handleRealtimeLog = (logItem) => {
      realtimeLogs.value.unshift(logItem);
      if (realtimeLogs.value.length > ui.realtimeLimit) realtimeLogs.value.length = ui.realtimeLimit;
    };

    const onEnvChange = () => {
      loadServices();
    };

    const onServiceNodeClick = (node) => {
      if (node && node.value) {
        filters.service = node.value;
      }
    };

    const openDetail = (row) => {
      ui.detail = row;
      ui.detailVisible = true;
    };

    const copyJson = (row) => {
      const text = JSON.stringify(row, null, 2);
      navigator.clipboard.writeText(text).then(() => {
        ElementPlus.ElMessage.success('JSON Copied');
      });
    }

    onMounted(() => {
      loadServices();
      connectSocket();
    });

    onUnmounted(() => {
      destroyed = true;
      if (retryTimer) {
        clearTimeout(retryTimer);
        retryTimer = null;
      }
      if (ws) {
        ws.close();
        ws = null;
      }
    });

    return {
      ui, filters, realtimeLogs, services, levels,
      displayLogs, computedServiceTree, treeProps, uiServiceFilter,
      formatTimestamp, prettyJson, levelBadgeClass, levelColorDot,
      loadServices, onEnvChange, onServiceNodeClick, openDetail, copyJson
    };
  }
};
</script>

<style scoped>
/* Element Plus Tree Customization */
:deep(.el-tree) {
  background: transparent;
}

:deep(.el-tree-node__content) {
  height: 32px;
  border-radius: 6px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f1f5f9;
  /* slate-100 */
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #eef2ff;
  /* indigo-50 */
  color: #4f46e5;
}
</style>
