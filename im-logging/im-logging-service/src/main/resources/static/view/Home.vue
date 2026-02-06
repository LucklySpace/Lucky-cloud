<template>
  <el-container class="h-screen w-full bg-slate-50">
    <!-- Sidebar -->
    <el-aside class="bg-white border-r border-slate-200 flex flex-col transition-all duration-300" width="240px">
      <!-- Brand -->
      <div class="h-16 flex items-center px-6 border-b border-slate-100 bg-white">
        <div class="flex items-center gap-3">
          <div
              class="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center text-white font-bold shadow-md shadow-indigo-200">
            L
          </div>
          <div>
            <h1 class="font-bold text-slate-800 text-lg tracking-tight">Lucky IM</h1>
            <p class="text-xs text-slate-400 font-medium -mt-1">日志中心</p>
          </div>
        </div>
      </div>

      <!-- Menu -->
      <el-menu
          :default-active="activePath"
          active-text-color="#4f46e5"
          background-color="#ffffff"
          class="flex-1 border-none py-4"
          router
          text-color="#64748b"
      >
        <el-menu-item class="my-1 mx-3 rounded-lg hover:bg-slate-50" index="/logs">
          <el-icon>
            <Document/>
          </el-icon>
          <span class="font-medium">实时日志</span>
        </el-menu-item>
        <el-menu-item class="my-1 mx-3 rounded-lg hover:bg-slate-50" index="/analysis">
          <el-icon>
            <DataAnalysis/>
          </el-icon>
          <span class="font-medium">日志分析</span>
        </el-menu-item>
        <el-menu-item class="my-1 mx-3 rounded-lg hover:bg-slate-50" index="/manage">
          <el-icon>
            <Setting/>
          </el-icon>
          <span class="font-medium">系统管理</span>
        </el-menu-item>
      </el-menu>

      <!-- Footer User Profile (Mock) -->
      <div class="p-4 border-t border-slate-100">
        <div class="flex items-center gap-3 p-2 rounded-lg hover:bg-slate-50 cursor-pointer transition-colors">
          <div class="w-8 h-8 rounded-full bg-slate-200 flex items-center justify-center text-slate-500">
            <el-icon>
              <User/>
            </el-icon>
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-slate-700 truncate">Admin User</p>
            <p class="text-xs text-slate-400 truncate">admin@lucky.com</p>
          </div>
        </div>
      </div>
    </el-aside>

    <!-- Main Content -->
    <el-container class="flex-1 flex flex-col overflow-hidden">
      <!-- Header -->
      <el-header class="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 shadow-sm z-10">
        <div class="flex items-center text-slate-500 text-sm">
          <span class="mr-2">当前环境:</span>
          <span
              class="px-2 py-1 bg-emerald-50 text-emerald-600 rounded text-xs font-semibold border border-emerald-100">DEV</span>
        </div>
        <div class="flex items-center gap-4">
          <el-button circle plain size="small">
            <el-icon class="text-slate-500">
              <Bell/>
            </el-icon>
          </el-button>
          <el-button circle plain size="small">
            <el-icon class="text-slate-500">
              <QuestionFilled/>
            </el-icon>
          </el-button>
        </div>
      </el-header>

      <!-- Content -->
      <el-main class="flex-1 p-0 overflow-hidden relative bg-slate-50">
        <router-view v-slot="{ Component }">
          <transition mode="out-in" name="fade">
            <component :is="Component"/>
          </transition>
        </router-view>
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

<style scoped>
/* Transition for router view */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Override Element Plus specific styles that can't be handled by Tailwind utilities easily */
:deep(.el-menu-item.is-active) {
  background-color: #eef2ff !important; /* indigo-50 */
  color: #4f46e5 !important; /* indigo-600 */
}
</style>
