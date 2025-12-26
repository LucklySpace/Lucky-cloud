<template>
  <div class="page">
    <div class="toolbar">
      <div class="title">概览</div>
      <el-button size="large" @click="loadOverview">刷新</el-button>
    </div>
    <div class="cards">
      <el-card v-for="l in levelList" :key="l" class="card">
        <div class="card-title">{{ l }}</div>
        <div class="card-value">{{ overview.levels[l] || 0 }}</div>
      </el-card>
    </div>
    <div class="section">
      <div class="title">小时序列</div>
      <div class="row">
        <el-select v-model="hourly.level" placeholder="级别" size="large" style="width: 120px">
          <el-option v-for="l in levelList" :key="l" :label="l" :value="l"></el-option>
        </el-select>
        <el-input-number v-model="hourly.hours" :max="72" :min="1" size="large"/>
        <el-button size="large" type="primary" @click="loadHourly">刷新</el-button>
      </div>
      <div class="bars">
        <div v-for="(v, k) in hourlySeries" :key="k" class="bar">
          <div class="bar-label">{{ k }}</div>
          <div class="bar-track">
            <div :style="{ width: barWidth(v, hourlyMax) }" class="bar-fill"></div>
          </div>
          <div class="bar-value">{{ v }}</div>
        </div>
      </div>
    </div>
    <div class="section">
      <div class="title">区间直方</div>
      <div class="row">
        <el-select v-model="hist.interval" size="large" style="width: 120px">
          <el-option label="hour" value="hour"></el-option>
          <el-option label="minute" value="minute"></el-option>
        </el-select>
        <el-select v-model="hist.level" clearable placeholder="级别" size="large" style="width: 120px">
          <el-option v-for="l in levelList" :key="l" :label="l" :value="l"></el-option>
        </el-select>
        <el-input v-model="hist.module" placeholder="模块" size="large" style="width: 160px"/>
        <el-input v-model="hist.service" placeholder="服务" size="large" style="width: 160px"/>
        <el-input v-model="hist.env" placeholder="环境" size="large" style="width: 120px"/>
        <el-input v-model="hist.keyword" placeholder="关键字" size="large" style="width: 180px"/>
        <el-date-picker v-model="hist.range" end-placeholder="结束" range-separator="至" size="large"
                        start-placeholder="开始" type="datetimerange"/>
        <el-button size="large" type="primary" @click="loadHistogram">刷新</el-button>
      </div>
      <div class="bars">
        <div v-for="(v, k) in histogram" :key="k" class="bar">
          <div class="bar-label">{{ k }}</div>
          <div class="bar-track">
            <div :style="{ width: barWidth(v, histMax) }" class="bar-fill"></div>
          </div>
          <div class="bar-value">{{ v }}</div>
        </div>
      </div>
    </div>
    <div class="section">
      <div class="title">聚合</div>
      <div class="row">
        <el-date-picker v-model="aggs.range" end-placeholder="结束" range-separator="至" size="large"
                        start-placeholder="开始" type="datetimerange"/>
        <el-input-number v-model="aggs.limit" :max="50" :min="1" size="large"/>
        <el-button size="large" type="primary" @click="loadAggs">刷新</el-button>
      </div>
      <div class="aggs">
        <el-card class="agg">
          <div class="agg-title">Top Services</div>
          <div v-for="item in topServices" :key="item.name" class="agg-row">
            <div class="agg-name">{{ item.name }}</div>
            <div class="agg-track">
              <div :style="{ width: barWidth(item.count, topServicesMax) }" class="agg-fill"></div>
            </div>
            <div class="agg-value">{{ item.count }}</div>
          </div>
        </el-card>
        <el-card class="agg">
          <div class="agg-title">Top Addresses</div>
          <div v-for="item in topAddresses" :key="item.name" class="agg-row">
            <div class="agg-name">{{ item.name }}</div>
            <div class="agg-track">
              <div :style="{ width: barWidth(item.count, topAddressesMax) }" class="agg-fill"></div>
            </div>
            <div class="agg-value">{{ item.count }}</div>
          </div>
        </el-card>
        <el-card class="agg">
          <div class="agg-title">Top Error Types</div>
          <div v-for="item in topErrors" :key="item.name" class="agg-row">
            <div class="agg-name">{{ item.name }}</div>
            <div class="agg-track">
              <div :style="{ width: barWidth(item.count, topErrorsMax) }" class="agg-fill"></div>
            </div>
            <div class="agg-value">{{ item.count }}</div>
          </div>
        </el-card>
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

.title {
  font-weight: 700;
  font-size: 18px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  padding: 12px 16px;
}

.card {
}

.card-title {
  color: var(--muted);
}

.card-value {
  font-size: 24px;
  font-weight: 700;
}

.section {
  padding: 12px 16px;
}

.row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar {
  display: grid;
  grid-template-columns: 140px 1fr 80px;
  gap: 8px;
  align-items: center;
}

.bar-label {
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.bar-track {
  height: 12px;
  background: #eef2f7;
  border-radius: 6px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), #8b5cf6);
}

.bar-value {
  text-align: right;
}

.aggs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.agg {
}

.agg-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.agg-row {
  display: grid;
  grid-template-columns: 140px 1fr 80px;
  gap: 8px;
  align-items: center;
  margin-bottom: 6px;
}

.agg-name {
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agg-track {
  height: 10px;
  background: #eef2f7;
  border-radius: 6px;
  overflow: hidden;
}

.agg-fill {
  height: 100%;
  background: #059669;
}

.agg-value {
  text-align: right;
}

@media (max-width: 1024px) {
  .cards {
    grid-template-columns: repeat(3, 1fr);
  }

  .aggs {
    grid-template-columns: 1fr;
  }
}
</style>
