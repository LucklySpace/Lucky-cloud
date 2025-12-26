<template>
  <el-container style="height: 100vh">
    <el-header class="header">
      <div class="brand">
        <!-- 菜单切换按钮 (Optional: Toggle Outer Sidebar) -->
        <el-button circle icon="Menu" text @click="isCollapse = !isCollapse"></el-button>

        <!-- LOGO/名称 -->
        <div>
          <div class="brand-title">Lucky IM 日志中心</div>
          <div style="font-size: 12px; color: var(--muted)">
            可视化日志查询
          </div>
        </div>
      </div>

      <!-- 右侧状态与操作 -->
      <div style="display: flex; align-items: center; gap: 12px">
        <div id="header-actions"></div>
      </div>
    </el-header>

    <el-container style="overflow: hidden">
      <el-aside style="background: #fff; border-right: 1px solid #e6edf3" width="200px">
        <el-menu :default-active="activePath" router style="border-right: none">
          <el-menu-item index="/logs">
            <el-icon>
              <Document/>
            </el-icon>
            <span>实时日志查看</span>
          </el-menu-item>
          <el-menu-item index="/manage">
            <el-icon>
              <Setting/>
            </el-icon>
            <span>日志管理</span>
          </el-menu-item>
          <el-menu-item index="/analysis">
            <el-icon>
              <DataAnalysis/>
            </el-icon>
            <span>日志分析</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main style="
          padding: 0;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        ">
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
export default {
  setup() {
    const {computed} = Vue;
    const route = VueRouter.useRoute();
    const activePath = computed(() => route.path);

    return {
      activePath,
    };
  },
};
</script>

<style lang="less" scoped>
/* Header */
.header {
  height: 56px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  gap: 12px;
  box-shadow: var(--shadow-sm);
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-title {
  font-weight: 700;
  font-size: 18px;
  background: linear-gradient(90deg, var(--primary), #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
</style>
