<template>
  <div class="flex flex-col h-full bg-slate-50 overflow-auto scroll-smooth">
    <!-- Header -->
    <div
        class="bg-white border-b border-slate-200 px-6 py-4 flex justify-between items-center shadow-sm sticky top-0 z-30">
      <div class="flex items-center gap-3">
        <div class="p-2 bg-indigo-100 text-indigo-600 rounded-lg">
          <el-icon size="20">
            <DataLine/>
          </el-icon>
        </div>
        <h1 class="text-lg font-bold text-slate-800">日志分析概览</h1>
      </div>
      <el-button circle icon="Refresh" @click="loadOverview"></el-button>
    </div>

    <div class="p-6 space-y-6 max-w-7xl mx-auto w-full">
      <!-- Overview Cards -->
      <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div v-for="l in levelList" :key="l"
             class="bg-white rounded-xl shadow-sm border border-slate-200 p-4 flex flex-col items-center justify-center hover:shadow-md transition-all duration-300 group cursor-default">
          <div
              class="text-xs font-bold tracking-wider text-slate-400 mb-1 group-hover:text-indigo-500 transition-colors">
            {{ l }}
          </div>
          <div class="text-2xl font-black text-slate-800 tabular-nums">{{ overview.levels[l] || 0 }}</div>
        </div>
      </div>

      <!-- Hourly Series -->
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div
            class="px-6 py-4 border-b border-slate-100 flex flex-wrap gap-4 justify-between items-center bg-slate-50/50">
          <div class="font-bold text-slate-700 flex items-center gap-2">
            <el-icon class="text-indigo-500">
              <Timer/>
            </el-icon>
            小时趋势
          </div>
          <div class="flex gap-2 items-center">
            <el-select v-model="hourly.level" class="w-32" placeholder="级别" size="default">
              <el-option v-for="l in levelList" :key="l" :label="l" :value="l"></el-option>
            </el-select>
            <el-input-number v-model="hourly.hours" :max="72" :min="1" class="w-32" size="default"/>
            <el-button icon="Refresh" plain size="default" type="primary" @click="loadHourly"></el-button>
          </div>
        </div>
        <div class="p-6">
          <div class="space-y-3">
            <div v-for="(v, k) in hourlySeries" :key="k" class="flex items-center gap-4 text-sm">
              <div class="w-32 text-right font-mono text-slate-500 text-xs">{{ k }}</div>
              <div class="flex-1 h-3 bg-slate-100 rounded-full overflow-hidden">
                <div :style="{ width: barWidth(v, hourlyMax) }"
                     class="h-full bg-indigo-500 rounded-full transition-all duration-500 ease-out"></div>
              </div>
              <div class="w-16 font-mono font-bold text-slate-700 text-right">{{ v }}</div>
            </div>
            <div v-if="Object.keys(hourlySeries).length === 0" class="text-center text-slate-400 py-8 italic">
              暂无数据
            </div>
          </div>
        </div>
      </div>

      <!-- Histogram -->
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div
            class="px-6 py-4 border-b border-slate-100 flex flex-col md:flex-row gap-4 justify-between items-start md:items-center bg-slate-50/50">
          <div class="font-bold text-slate-700 flex items-center gap-2">
            <el-icon class="text-emerald-500">
              <Histogram/>
            </el-icon>
            区间分布
          </div>
          <div class="flex flex-wrap gap-2 items-center w-full md:w-auto">
            <el-select v-model="hist.interval" class="w-24" size="default">
              <el-option label="Hour" value="hour"></el-option>
              <el-option label="Minute" value="minute"></el-option>
            </el-select>
            <el-select v-model="hist.level" class="w-24" clearable placeholder="级别" size="default">
              <el-option v-for="l in levelList" :key="l" :label="l" :value="l"></el-option>
            </el-select>
            <el-input v-model="hist.module" class="w-32" placeholder="模块" size="default"/>
            <el-input v-model="hist.service" class="w-32" placeholder="服务" size="default"/>
            <el-input v-model="hist.keyword" class="w-40" placeholder="关键字" size="default"/>
            <el-date-picker v-model="hist.range" class="!w-64" end-placeholder="End" range-separator="-"
                            size="default" start-placeholder="Start" type="datetimerange"/>
            <el-button icon="Refresh" plain size="default" type="primary" @click="loadHistogram"></el-button>
          </div>
        </div>
        <div class="p-6">
          <div class="space-y-2 max-h-96 overflow-y-auto pr-2 scrollbar-thin">
            <div v-for="(v, k) in histogram" :key="k"
                 class="flex items-center gap-4 text-sm group hover:bg-slate-50 p-1 rounded">
              <div class="w-36 text-right font-mono text-slate-500 text-xs">{{ k }}</div>
              <div class="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                    :style="{ width: barWidth(v, histMax) }"
                    class="h-full bg-gradient-to-r from-emerald-400 to-teal-500 rounded-full transition-all duration-500 ease-out"></div>
              </div>
              <div class="w-16 font-mono font-bold text-slate-700 text-right">{{ v }}</div>
            </div>
            <div v-if="Object.keys(histogram).length === 0" class="text-center text-slate-400 py-8 italic">
              暂无数据
            </div>
          </div>
        </div>
      </div>

      <!-- Aggregations -->
      <div class="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div
            class="px-6 py-4 border-b border-slate-100 flex flex-wrap gap-4 justify-between items-center bg-slate-50/50">
          <div class="font-bold text-slate-700 flex items-center gap-2">
            <el-icon class="text-amber-500">
              <PieChart/>
            </el-icon>
            聚合统计
          </div>
          <div class="flex gap-2 items-center">
            <el-date-picker v-model="aggs.range" class="!w-64" end-placeholder="End" range-separator="-"
                            size="default" start-placeholder="Start" type="datetimerange"/>
            <el-input-number v-model="aggs.limit" :max="50" :min="1" class="w-24" size="default"/>
            <el-button icon="Refresh" plain size="default" type="primary" @click="loadAggs"></el-button>
          </div>
        </div>

        <div class="p-6 grid grid-cols-1 lg:grid-cols-3 gap-8">
          <!-- Top Services -->
          <div class="bg-slate-50 rounded-lg p-4 border border-slate-100">
            <h3 class="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-2">
              Top Services
            </h3>
            <div class="space-y-3">
              <div v-for="item in topServices" :key="item.name" class="relative">
                <div class="flex justify-between text-xs mb-1">
                  <span :title="item.name" class="font-medium text-slate-700 truncate w-2/3">{{ item.name }}</span>
                  <span class="font-mono text-slate-500">{{ item.count }}</span>
                </div>
                <div class="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div :style="{ width: barWidth(item.count, topServicesMax) }"
                       class="h-full bg-blue-500 rounded-full"></div>
                </div>
              </div>
              <div v-if="topServices.length === 0" class="text-center text-slate-400 py-4 text-xs">No data</div>
            </div>
          </div>

          <!-- Top Addresses -->
          <div class="bg-slate-50 rounded-lg p-4 border border-slate-100">
            <h3 class="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-2">
              Top Addresses
            </h3>
            <div class="space-y-3">
              <div v-for="item in topAddresses" :key="item.name" class="relative">
                <div class="flex justify-between text-xs mb-1">
                  <span :title="item.name" class="font-medium text-slate-700 truncate w-2/3">{{ item.name }}</span>
                  <span class="font-mono text-slate-500">{{ item.count }}</span>
                </div>
                <div class="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div :style="{ width: barWidth(item.count, topAddressesMax) }"
                       class="h-full bg-purple-500 rounded-full"></div>
                </div>
              </div>
              <div v-if="topAddresses.length === 0" class="text-center text-slate-400 py-4 text-xs">No data</div>
            </div>
          </div>

          <!-- Top Errors -->
          <div class="bg-slate-50 rounded-lg p-4 border border-slate-100">
            <h3 class="text-sm font-bold text-slate-500 uppercase tracking-wider mb-4 flex items-center gap-2">
              Top Error Types
            </h3>
            <div class="space-y-3">
              <div v-for="item in topErrors" :key="item.name" class="relative">
                <div class="flex justify-between text-xs mb-1">
                  <span :title="item.name" class="font-medium text-slate-700 truncate w-2/3">{{ item.name }}</span>
                  <span class="font-mono text-slate-500">{{ item.count }}</span>
                </div>
                <div class="h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <div :style="{ width: barWidth(item.count, topErrorsMax) }"
                       class="h-full bg-red-500 rounded-full"></div>
                </div>
              </div>
              <div v-if="topErrors.length === 0" class="text-center text-slate-400 py-4 text-xs">No data</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  getAggTopAddresses,
  getAggTopErrors,
  getAggTopServices,
  getHistogram,
  getHourly,
  getOverview
} from '../api/logs.js';

export default {
  setup() {
    const {ref, reactive, onMounted, computed} = Vue;
    const levelList = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR'];
    const overview = reactive({levels: {}});
    const hourly = reactive({level: 'ERROR', hours: 24});
    const hourlySeries = ref({});
    const histogram = ref({});
    const hist = reactive({interval: 'hour', level: '', module: '', service: '', env: '', keyword: '', range: []});
    const aggs = reactive({range: [], limit: 10});
    const topServices = ref([]);
    const topAddresses = ref([]);
    const topErrors = ref([]);

    function toISO(dt) {
      return dt ? new Date(dt).toISOString() : undefined;
    }

    function barWidth(v, max) {
      const m = Math.max(max || 1, 1);
      const pct = Math.round((v / m) * 100);
      return pct + '%';
    }

    const hourlyMax = computed(() => Object.values(hourlySeries.value).reduce((a, b) => Math.max(a, b), 0));
    const histMax = computed(() => Object.values(histogram.value).reduce((a, b) => Math.max(a, b), 0));
    const topServicesMax = computed(() => topServices.value.reduce((a, b) => Math.max(a, b.count), 0));
    const topAddressesMax = computed(() => topAddresses.value.reduce((a, b) => Math.max(a, b.count), 0));
    const topErrorsMax = computed(() => topErrors.value.reduce((a, b) => Math.max(a, b.count), 0));

    async function loadOverview() {
      try {
        const res = await getOverview();
        const data = res?.data || res || {};
        overview.levels = data.levels || {};
      } catch {
        overview.levels = {};
      }
    }

    async function loadHourly() {
      try {
        const res = await getHourly({level: hourly.level, hours: hourly.hours});
        hourlySeries.value = res?.data || res || {};
      } catch {
        hourlySeries.value = {};
      }
    }

    async function loadHistogram() {
      const [start, end] = Array.isArray(hist.range) ? hist.range : [];
      const params = {
        module: hist.module || undefined,
        start: toISO(start),
        end: toISO(end),
        level: hist.level || undefined,
        service: hist.service || undefined,
        env: hist.env || undefined,
        keyword: hist.keyword || undefined,
        interval: hist.interval || 'hour'
      };
      try {
        const res = await getHistogram(params);
        histogram.value = res?.data || res || {};
      } catch {
        histogram.value = {};
      }
    }

    async function loadAggs() {
      const [start, end] = Array.isArray(aggs.range) ? aggs.range : [];
      const params = {start: toISO(start), end: toISO(end), limit: aggs.limit};
      try {
        const s = await getAggTopServices(params);
        topServices.value = s?.data || s || [];
      } catch {
        topServices.value = [];
      }
      try {
        const a = await getAggTopAddresses(params);
        topAddresses.value = a?.data || a || [];
      } catch {
        topAddresses.value = [];
      }
      try {
        const e = await getAggTopErrors(params);
        topErrors.value = e?.data || e || [];
      } catch {
        topErrors.value = [];
      }
    }

    onMounted(() => {
      loadOverview();
      loadHourly();
      loadHistogram();
      loadAggs();
    });
    return {
      levelList, overview,
      hourly, hourlySeries, hourlyMax,
      hist, histogram, histMax,
      aggs, topServices, topAddresses, topErrors, topServicesMax, topAddressesMax, topErrorsMax,
      loadOverview, loadHourly, loadHistogram, loadAggs, barWidth
    };
  }
}
</script>

