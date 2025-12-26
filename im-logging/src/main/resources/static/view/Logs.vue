<template>
  <div class="app">
    <!-- 主布局：侧栏 + 内容 -->
    <div class="layout">
      <!-- 侧栏：服务树与筛选 -->
      <aside :class="['sidebar', ui.sidebarOpen ? '' : 'collapsed']" aria-label="侧栏" role="navigation">
        <el-input v-model="uiServiceFilter" clearable placeholder="搜索服务..." prefix-icon="Search"
                  size="large"></el-input>

        <div style="flex: 1; overflow: auto; margin-top: 12px">
          <el-tree :data="computedServiceTree" :highlight-current="true" :props="treeProps" default-expand-all
                   @node-click="onServiceNodeClick"></el-tree>
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
                    style="width: 300px"></el-input>

          <div style="flex: 1"></div>

          <!-- 实时模式 -->
          <span class="mono" style="color: var(--muted);">实时模式</span>
        </div>

        <!-- 日志表与详情 -->
        <section class="logs-shell">
          <div class="table-wrap">
            <el-table-v2 :data="displayLogs" border height="100%" size="default" stripe style="width: 100%"
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
              <el-table-column label="时间" width="250">
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
            </el-table-v2>
          </div>

          <!-- 底部：实时计数 -->
          <div class="footer-bar">
            <div>
              <div style="color: var(--muted); font-size: 14px;">
                {{ ui.realtimeCount }} 条实时日志 · 显示最近 {{ ui.realtimeLimit }} 条
              </div>
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
  </div>
</template>

<script>
import {getServices} from '../api/logs.js';

export default {
  setup() {
    const {ref, reactive, computed, onMounted, watch} = Vue;

    // -----------------------------
    // UI 与状态
    // -----------------------------
    const ui = reactive({
      sidebarOpen: true,      // 侧栏展开/折叠
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
    });

    const realtimeLogs = ref([]);

    // 服务列表与筛选输入（侧栏）
    const services = ref([]);
    const uiServiceFilter = ref('');

    // stomp websocket 客户端引用
    let stompClient = null;

    // 可选的日志等级列表
    const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];

    // -----------------------------
    // 计算属性
    // -----------------------------
    function matchesFilters(item) {
      // 关键字匹配（整条 JSON）
      if (filters.keyword) {
        const text = JSON.stringify(item).toLowerCase();
        if (!text.includes(filters.keyword.toLowerCase())) return false;
      }
      // 环境匹配（若日志带 env）
      if (filters.env && item?.env && item.env.toLowerCase() !== filters.env.toLowerCase()) return false;
      // 级别匹配
      if (filters.level && item?.level && item.level.toUpperCase() !== filters.level.toUpperCase()) return false;
      // 服务匹配
      if (filters.service && item?.service && item.service !== filters.service) return false;

      return true;
    }

    const displayLogs = computed(() => {
      const list = realtimeLogs.value.filter(matchesFilters);
      return list.slice(0, ui.realtimeLimit);
    });

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

    // 已移除静态查询模式

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
      console.log('logItem', logItem);
      ui.realtimeCount++;
      if (realtimeLogs.value.length > ui.realtimeLimit) realtimeLogs.value.length = ui.realtimeLimit;
    }

    // 已移除模式切换

    // -----------------------------
    // 交互事件
    // -----------------------------
    function onEnvChange() {
      loadServices();
    }

    function onServiceNodeClick(node) {
      if (node && node.value) {
        filters.service = node.value;
      }
    }

    /** 行点击打开详情 */
    function openDetail(row) {
      ui.detail = row;
      ui.detailVisible = true;
    }

    onMounted(() => {
      // 初始化数据
      loadServices();
      // 建立 WS（如果后端未就绪，connectSocket 会自动重连）
      connectSocket();
      // 初始化完成
    });

    // 已移除静态查询刷新 watch

    // 返回给模板使用的变量和方法
    return {
      ui, filters, realtimeLogs, services, levels,
      displayLogs, computedServiceTree, treeProps,
      formatTimestamp, prettyJson, levelBadgeClass, traceLink,
      loadServices, connectSocket,
      onEnvChange, onServiceNodeClick,
      openDetail
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
