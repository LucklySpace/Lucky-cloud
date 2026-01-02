<template>
  <div class="flex flex-col h-full bg-slate-50">
    <!-- Search Toolbar -->
    <div class="bg-white border-b border-slate-200 p-4 flex flex-wrap gap-3 items-center shadow-sm z-10">
      <el-select v-model="filters.env" class="w-32" placeholder="环境">
        <el-option label="DEV" value="dev"></el-option>
        <el-option label="STAGE" value="test"></el-option>
        <el-option label="PROD" value="prod"></el-option>
      </el-select>
      <el-select v-model="filters.service" class="w-56" clearable filterable placeholder="服务">
        <el-option v-for="s in services" :key="s" :label="s" :value="s"></el-option>
      </el-select>
      <el-select v-model="filters.level" class="w-32" clearable placeholder="级别">
        <el-option v-for="l in levels" :key="l" :label="l" :value="l"></el-option>
      </el-select>
      <el-input v-model="filters.module" class="w-48" clearable placeholder="模块"></el-input>
      <el-input v-model="filters.keyword" class="w-64" clearable placeholder="关键字" prefix-icon="Search"></el-input>
      <el-date-picker v-model="filters.range" class="!w-80" end-placeholder="结束时间"
                      range-separator="至" start-placeholder="开始时间" type="datetimerange"/>
      <div class="flex-1"></div>
      <el-button icon="Search" type="primary" @click="onSearch">查询</el-button>
      <el-button icon="Download" @click="onExport">导出</el-button>
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-hidden p-4">
      <div class="bg-white rounded-lg shadow border border-slate-200 h-full flex flex-col">
        <el-table :data="rows" :header-cell-style="{background: '#f8fafc', color: '#475569', fontWeight: '600'}" border height="100%" style="width: 100%; flex: 1"
                  @row-click="openDetail">
          <el-table-column label="时间" width="200">
            <template #default="{ row }">
              <div class="font-mono text-xs text-slate-600">{{ formatTimestamp(row.timestamp) }}</div>
            </template>
          </el-table-column>
          <el-table-column align="center" label="级别" width="90">
            <template #default="{ row }">
              <span :class="['px-2 py-1 rounded text-xs font-bold text-white', levelBadgeClass(row.level)]">
                {{ row.level || 'INFO' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="服务" prop="service" show-overflow-tooltip width="160">
            <template #default="{ row }">
              <span class="font-medium text-slate-700">{{ row.service }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模块" prop="module" show-overflow-tooltip width="140">
            <template #default="{ row }">
              <span class="text-slate-500">{{ row.module }}</span>
            </template>
          </el-table-column>
          <el-table-column label="消息" min-width="300" prop="message" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="font-mono text-sm text-slate-800">{{ row.message }}</span>
            </template>
          </el-table-column>
        </el-table>

        <!-- Pagination -->
        <div class="p-3 border-t border-slate-100 flex justify-end bg-white">
          <el-pagination
              :current-page="page.index"
              :page-size="page.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="page.total"
              background
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="onSizeChange"
              @current-change="onPageChange"
          />
        </div>
      </div>
    </div>

    <!-- Danger Zone (Collapsible or Bottom Bar) -->
    <div class="bg-red-50 border-t border-red-100 p-3 flex gap-4 items-center px-6">
      <div class="text-red-800 font-bold flex items-center gap-2 text-sm">
        <el-icon>
          <Warning/>
        </el-icon>
        数据清理
      </div>
      <el-date-picker v-model="cleanup.cutoff" class="!w-56" placeholder="选择截止时间" size="default" type="datetime"/>
      <el-button :disabled="!cleanup.cutoff" plain size="small" type="danger" @click="onDeleteBefore">
        删除截止时间前的数据
      </el-button>
      <div class="w-px h-4 bg-red-200 mx-2"></div>
      <el-input v-model="cleanup.module" class="!w-40" placeholder="指定模块" size="default"/>
      <el-button :disabled="!cleanup.cutoff || !cleanup.module" plain size="small" type="danger"
                 @click="onDeleteModuleBefore">按模块删除
      </el-button>
    </div>

    <!-- Detail Drawer -->
    <el-drawer v-model="ui.detailVisible" destroy-on-close size="50%" title="日志详情">
      <div v-if="ui.detail" class="p-6 space-y-6">
        <!-- Basic Info Grid -->
        <div class="grid grid-cols-2 gap-4 bg-slate-50 p-4 rounded-lg border border-slate-100">
          <div class="space-y-1">
            <div class="text-xs text-slate-500 uppercase font-bold tracking-wider">Time</div>
            <div class="font-mono text-sm">{{ formatTimestamp(ui.detail.timestamp) }}</div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-slate-500 uppercase font-bold tracking-wider">Level</div>
            <div>
              <span :class="['px-2 py-0.5 rounded text-xs font-bold text-white', levelBadgeClass(ui.detail.level)]">
                {{ ui.detail.level }}
              </span>
            </div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-slate-500 uppercase font-bold tracking-wider">Service</div>
            <div class="font-medium text-slate-700">{{ ui.detail.service }}</div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-slate-500 uppercase font-bold tracking-wider">Module</div>
            <div class="text-slate-600">{{ ui.detail.module || '-' }}</div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-slate-500 uppercase font-bold tracking-wider">Host / Address</div>
            <div class="font-mono text-xs">{{ ui.detail.host || ui.detail.address || '-' }}</div>
          </div>
          <div class="space-y-1">
            <div class="text-xs text-slate-500 uppercase font-bold tracking-wider">Trace / Span</div>
            <div class="font-mono text-xs">{{ ui.detail.traceId || '-' }} / {{ ui.detail.spanId || '-' }}</div>
          </div>
        </div>

        <!-- Message -->
        <div>
          <div class="text-sm font-bold text-slate-700 mb-2">Message</div>
          <div class="bg-slate-900 rounded-lg p-4 overflow-auto max-h-60 border border-slate-700 shadow-inner">
            <pre class="font-mono text-sm text-slate-300 whitespace-pre-wrap break-all">{{ ui.detail.message }}</pre>
          </div>
        </div>

        <!-- Exception -->
        <div v-if="ui.detail.exception">
          <div class="text-sm font-bold text-red-600 mb-2 flex items-center gap-2">
            <el-icon>
              <WarnTriangleFilled/>
            </el-icon>
            Exception
          </div>
          <div
              class="bg-red-50 rounded-lg p-4 overflow-auto max-h-96 border border-red-100 text-red-800 font-mono text-xs whitespace-pre-wrap">
            {{ ui.detail.exception }}
          </div>
        </div>

        <!-- Raw JSON -->
        <div>
          <div class="text-sm font-bold text-slate-700 mb-2">Raw JSON</div>
          <div class="bg-slate-50 rounded-lg p-4 overflow-auto border border-slate-200">
            <pre class="json font-mono text-xs leading-5" v-html="prettyJson(ui.detail)"></pre>
          </div>
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

    let timer = null;

    function clearTimer() {
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
    }

    function setTimer() {
      clearTimer();
      timer = setInterval(() => {
        loadServices();
      }, 10000);
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
          let cls = 'text-blue-600';
          if (/^"/.test(match)) cls = /:$/.test(match) ? 'text-purple-600 font-semibold' : 'text-green-600';
          else if (/true|false/.test(match)) cls = 'text-orange-600 font-bold';
          else if (/null/.test(match)) cls = 'text-gray-500 italic';
          return `<span class="${cls}">${match}</span>`;
        });
      } catch (e) {
        return String(obj);
      }
    }

    function levelBadgeClass(l) {
      if (!l) return 'bg-slate-500';
      switch (l.toUpperCase()) {
        case 'TRACE':
          return 'bg-slate-500';
        case 'DEBUG':
          return 'bg-blue-600';
        case 'INFO':
          return 'bg-emerald-600';
        case 'WARN':
          return 'bg-amber-500';
        case 'ERROR':
          return 'bg-red-600';
        default:
          return 'bg-slate-500';
      }
    }

    function toISO(dt) {
      return dt ? new Date(dt).toISOString() : undefined;
    }

    onMounted(() => {
      loadServices();
      onSearch();
      setTimer();
    });

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

