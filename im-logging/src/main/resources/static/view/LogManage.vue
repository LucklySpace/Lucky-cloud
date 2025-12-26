<template>
  <div class="page">
    <div class="toolbar">
      <el-select v-model="filters.env" placeholder="环境" size="large" style="width: 120px">
        <el-option label="DEV" value="dev"></el-option>
        <el-option label="STAGE" value="test"></el-option>
        <el-option label="PROD" value="prod"></el-option>
      </el-select>
      <el-select v-model="filters.service" clearable filterable placeholder="服务" size="large" style="width: 220px">
        <el-option v-for="s in services" :key="s" :label="s" :value="s"></el-option>
      </el-select>
      <el-select v-model="filters.level" clearable placeholder="级别" size="large" style="width: 120px">
        <el-option v-for="l in levels" :key="l" :label="l" :value="l"></el-option>
      </el-select>
      <el-input v-model="filters.module" clearable placeholder="模块" size="large" style="width: 180px"></el-input>
      <el-input v-model="filters.keyword" clearable placeholder="关键字" size="large" style="width: 280px"></el-input>
      <el-date-picker v-model="filters.range" end-placeholder="结束时间" range-separator="至" size="large"
                      start-placeholder="开始时间" type="datetimerange"/>
      <el-button size="large" type="primary" @click="onSearch">查询</el-button>
      <el-button size="large" @click="onExport">导出</el-button>
    </div>
    <div class="content">
      <el-table :data="rows" border height="100%" style="width: 100%" @row-click="openDetail">
        <el-table-column label="时间" width="220">
          <template #default="{ row }">
            <div class="mono">{{ formatTimestamp(row.timestamp) }}</div>
          </template>
        </el-table-column>
        <el-table-column align="center" label="级别" width="100">
          <template #default="{ row }">
            <span :class="['pill', levelBadgeClass(row.level)]">{{ row.level || 'INFO' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="服务" prop="service" show-overflow-tooltip width="180"></el-table-column>
        <el-table-column label="模块" prop="module" show-overflow-tooltip width="160"></el-table-column>
        <el-table-column label="消息" min-width="360" prop="message" show-overflow-tooltip></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
            :current-page="page.index"
            :page-size="page.size"
            :page-sizes="[10,20,50,100]"
            :total="page.total"
            background
            layout="prev, pager, next, sizes, total"
            @size-change="onSizeChange"
            @current-change="onPageChange"
        />
      </div>
    </div>
    <div class="danger">
      <el-date-picker v-model="cleanup.cutoff" placeholder="截止时间" size="large" style="width: 240px"
                      type="datetime"/>
      <el-button :disabled="!cleanup.cutoff" size="large" type="danger" @click="onDeleteBefore">删除截止时间前
      </el-button>
      <el-input v-model="cleanup.module" placeholder="模块" size="large" style="width: 180px; margin-left: 12px"/>
      <el-button :disabled="!cleanup.cutoff || !cleanup.module" size="large" type="danger"
                 @click="onDeleteModuleBefore">按模块删除
      </el-button>
    </div>
    <el-drawer v-model="ui.detailVisible" size="50%" title="日志详情">
      <div v-if="ui.detail" style="padding: 20px">
        <el-descriptions border column="1" size="large">
          <el-descriptions-item label="时间">{{ formatTimestamp(ui.detail.timestamp) }}</el-descriptions-item>
          <el-descriptions-item label="级别">
            <span :class="['pill', levelBadgeClass(ui.detail.level)]">{{ ui.detail.level }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="服务">{{ ui.detail.service }} <span
              style="color: var(--muted)">({{ ui.detail.module || '-' }})</span></el-descriptions-item>
          <el-descriptions-item label="主机">{{ ui.detail.host || ui.detail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Trace / Span">{{ ui.detail.traceId || '-' }} / {{
              ui.detail.spanId || '-'
            }}
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 20px; font-weight: 600; font-size: 16px;">Message</div>
        <div class="card">
          <pre class="mono" style="margin: 0; white-space: pre-wrap">{{ ui.detail.message }}</pre>
        </div>
        <div v-if="ui.detail.exception" style="margin-top: 16px; font-weight: 600; color: #c62828; font-size: 16px;">
          Exception
        </div>
        <div v-if="ui.detail.exception" class="exception">
          <pre class="mono" style="margin: 0; white-space: pre-wrap">{{ ui.detail.exception }}</pre>
        </div>
        <div style="margin-top: 16px; font-weight: 600; font-size: 16px;">Raw JSON</div>
        <div class="card">
          <pre class="json mono" v-html="prettyJson(ui.detail)"></pre>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import {deleteBefore, deleteModuleBefore, getExportUrl, getServices, searchLogs} from '../api/logs.js';

export default {
  setup() {
    const {ref, reactive, computed, onUnmounted, onMounted} = Vue;
    const services = ref([]);
    const levels = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];
    const filters = reactive({
      env: 'dev',
      service: '',
      level: '',
      module: '',
      keyword: '',
      range: []
    });
    const page = reactive({index: 1, size: 20, total: 0});
    const rows = ref([]);
    const ui = reactive({detailVisible: false, detail: null});
    const cleanup = reactive({cutoff: null, module: ''});

    // 添加定时器变量
    let timer = null;

    // 清除定时器的函数
    function clearTimer() {
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
    }

    // 设置定时器的函数
    function setTimer() {
      clearTimer(); // 先清除可能存在的定时器
      timer = setInterval(() => {
        loadServices();
      }, 10000); // 10秒 = 10000毫秒
    }

    async function loadServices() {
      try {
        const res = await getServices({env: filters.env});
        services.value = res?.data || res || [];
      } catch {
        services.value = [];
      }
    }

    async function onSearch() {
      const [start, end] = Array.isArray(filters.range) ? filters.range : [];
      const params = {
        module: filters.module || undefined,
        start: toISO(start),
        end: toISO(end),
        level: filters.level || undefined,
        service: filters.service || undefined,
        env: filters.env || undefined,
        page: page.index,
        size: page.size,
        keyword: filters.keyword || undefined
      };
      try {
        const res = await searchLogs(params);
        if (res && res.content) {
          rows.value = res.content || [];
          page.total = res.totalElements || 0;
        }
      } catch {
        rows.value = [];
        page.total = 0;
      }
    }

    function onExport() {
      const [start, end] = Array.isArray(filters.range) ? filters.range : [];
      const url = getExportUrl({
        module: filters.module || undefined,
        start: toISO(start),
        end: toISO(end),
        level: filters.level || undefined,
        service: filters.service || undefined,
        env: filters.env || undefined,
        page: 0,
        size: 1000,
        keyword: filters.keyword || undefined
      });
      window.open(url, '_blank');
    }

    function onSizeChange(s) {
      page.size = s;
      onSearch();
    }

    function onPageChange(p) {
      page.index = p;
      onSearch();
    }

    function openDetail(row) {
      ui.detail = row;
      ui.detailVisible = true;
    }

    async function onDeleteBefore() {
      if (!cleanup.cutoff) return;
      const cutoff = toISO(cleanup.cutoff);
      try {
        await deleteBefore({cutoff});
        ElementPlus.ElMessage.success('已删除');
        onSearch();
      } catch (e) {
        ElementPlus.ElMessage.error('删除失败');
      }
    }

    async function onDeleteModuleBefore() {
      if (!cleanup.cutoff || !cleanup.module) return;
      const cutoff = toISO(cleanup.cutoff);
      try {
        await deleteModuleBefore({module: cleanup.module, cutoff});
        ElementPlus.ElMessage.success('已删除');
        onSearch();
      } catch (e) {
        ElementPlus.ElMessage.error('删除失败');
      }
    }

    function formatTimestamp(iso) {
      if (!iso) return '';
      const d = new Date(iso);
      const pad = n => String(n).padStart(2, '0');
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
    }

    function prettyJson(obj) {
      try {
        const s = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
        const esc = s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
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

    function toISO(dt) {
      return dt ? new Date(dt).toISOString() : undefined;
    }

    onMounted(() => {
      loadServices();
      onSearch();
      setTimer(); // 启动定时器
    });


    // 组件卸载时清除定时器
    onUnmounted(() => {
      clearTimer();
    });

    return {
      services, levels, filters, page, rows, ui, cleanup,
      formatTimestamp, prettyJson, levelBadgeClass,
      onSearch, onExport, onSizeChange, onPageChange, openDetail,
      onDeleteBefore, onDeleteModuleBefore
    };
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
}

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.pager {
  padding: 12px 16px;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
}

.danger {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px dashed var(--border);
  background: #fffdfd;
}

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

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 14px;
}

.card {
  background: var(--bg-card);
  padding: 16px;
  border-radius: 8px;
  margin-top: 8px;
}

.exception {
  background: #fff5f5;
  padding: 16px;
  border-radius: 8px;
  margin-top: 8px;
  overflow: auto;
  color: #6b1a1a;
}

pre.json {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}
</style>
