<template>
  <div class="wrap">
    <div class="card">
      <div class="brand">
        <span class="dot"></span>
        <h1>页面发生错误</h1>
      </div>
      <div class="desc">
        抱歉，当前页面无法正常显示。您可以返回日志中心或刷新页面重试。
      </div>
      <div class="actions">
        <router-link class="btn primary" to="/logs">返回日志中心</router-link>
        <button class="btn secondary" @click="reload">
          刷新
        </button>
      </div>
      <div class="footer">IM Logging · Powered by Lucky-cloud</div>
    </div>
  </div>
</template>

<script>
export default {
  setup() {
    function reload() {
      location.reload();
    }

    return {reload};
  }
};
</script>

<style lang="less" scoped>
:root {
  --bg: #0b1221;
  --card: #0f172a;
  --text: #e5e7eb;
  --muted: #94a3b8;
  --primary: #60a5fa;
  --danger: #f87171;
}

.wrap {
  min-height: 100vh;
  /* Changed from 100% to 100vh for scoped */
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(180deg, #0b1221 0%, #111827 100%);
  color: #e5e7eb;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
  "Helvetica Neue", Arial, sans-serif;
}

.card {
  width: 100%;
  max-width: 640px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(6px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.brand .dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f87171;
  box-shadow: 0 0 12px #f87171;
}

h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.desc {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 14px;
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.btn {
  appearance: none;
  border: 0;
  cursor: pointer;
  border-radius: 10px;
  padding: 10px 16px;
  font-weight: 600;
  text-decoration: none;
  display: inline-block;
}

.btn.primary {
  background: #1d4ed8;
  color: #fff;
}

.btn.secondary {
  background: #0b1221;
  color: #e5e7eb;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.footer {
  margin-top: 24px;
  color: #94a3b8;
  font-size: 12px;
  text-align: right;
}

a {
  color: #60a5fa;
  text-decoration: none;
}
</style>
