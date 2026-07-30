<template>
  <div class="admin-stats-analysis">
    <el-tabs v-model="activeTab" class="analysis-tabs" type="card">
      <el-tab-pane label="电影数据统计" name="stats">
        <StatsAnalysis />
      </el-tab-pane>
      <el-tab-pane label="用户画像分析" name="profile">
        <UserProfileAnalysis />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref } from 'vue'
import StatsAnalysis from './StatsAnalysis.vue'
import UserProfileAnalysis from './UserProfileAnalysis.vue'

export default {
  name: 'AdminStatsAnalysis',
  components: { StatsAnalysis, UserProfileAnalysis },
  setup() {
    const activeTab = ref('stats')
    return { activeTab }
  }
}
</script>

<style scoped>
.admin-stats-analysis {
  padding: 0;
}

.analysis-tabs {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.analysis-tabs :deep(.el-tabs__header) {
  margin: 0;
  background: #f8f9fb;
  border-bottom: 1px solid #ebeef5;
}

.analysis-tabs :deep(.el-tabs__nav) {
  border: none;
  padding: 4px;
}

.analysis-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  padding: 0 32px;
  height: 42px;
  line-height: 42px;
  border: none !important;
  border-radius: 6px;
  transition: all 0.25s ease;
  color: #606266;
}

.analysis-tabs :deep(.el-tabs__item:hover) {
  color: #409eff;
}

.analysis-tabs :deep(.el-tabs__item.is-active) {
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.35);
}

.analysis-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.analysis-tabs :deep(.el-tabs__content) {
  padding: 16px 0 0;
}
</style>
