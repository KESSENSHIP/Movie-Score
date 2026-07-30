<template>
  <div class="profile-analysis">
    <div class="page-header">
      <h2>用户画像分析 - K-Means 聚类</h2>
      <div class="header-actions">
        <el-button type="primary" @click="executeAnalysis" :loading="running" :disabled="running">
          {{ running ? '分析中...' : '执行聚类分析' }}
        </el-button>
        <el-button @click="loadResult" :disabled="running">刷新结果</el-button>
      </div>
    </div>

    <!-- 加载/错误/空状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon :size="32" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    <div v-else-if="loadError" class="empty-state">
      <p>{{ errorMessage || '加载失败' }}</p>
      <el-button type="primary" @click="loadResult">重试</el-button>
    </div>
    <div v-else-if="!result" class="empty-state">
      <el-icon :size="64" color="#ccc"><DataAnalysis /></el-icon>
      <p>尚未进行聚类分析，请点击上方按钮执行分析</p>
    </div>

    <!-- 分析结果 -->
    <template v-if="result">
      <!-- 概览信息 -->
      <div class="overview-cards">
        <div class="overview-card">
          <div class="card-value">{{ result.totalUsers }}</div>
          <div class="card-label">参与聚类用户数</div>
        </div>
        <div class="overview-card">
          <div class="card-value">{{ result.optimalK }}</div>
          <div class="card-label">最佳聚类数 (K)</div>
        </div>
      </div>

      <!-- 簇汇总表 -->
      <div class="section">
        <h3>聚类结果汇总</h3>
        <el-table :data="result.clusterSummary" border style="width: 100%" :empty-text="loading ? '' : '暂无数据'" v-loading="loading">
          <el-table-column label="簇编号" width="100" align="center">
            <template #default="scope">
              <el-tag :type="getClusterTag(scope.row.clusterId)">
                簇 {{ scope.row.clusterId }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userCount" label="用户数" width="120" align="center" />
          <el-table-column prop="avgRatingCount" label="平均评分次数" width="160" align="center" />
          <el-table-column prop="avgRating" label="平均评分" width="140" align="center">
            <template #default="scope">
              <span :class="ratingClass(parseFloat(scope.row.avgRating))">
                {{ scope.row.avgRating }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="avgRatingStddev" label="评分标准差" width="140" align="center" />
          <el-table-column prop="avgDaysSinceLast" label="距最近评分(天)" width="160" align="center" />
          <el-table-column label="用户画像解读" min-width="300">
            <template #default="scope">
              <span class="profile-desc">{{ getProfileDescription(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="scope">
              <el-button type="primary" size="small" @click="showClusterUsers(scope.row)">
                查看用户
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 簇用户列表对话框 -->
      <el-dialog
        v-model="userDialogVisible"
        :title="'簇 ' + userDialogClusterId + ' - 用户列表'"
        width="900px"
        top="5vh"
        :close-on-click-modal="false"
      >
        <div v-if="loadingUsers" class="dialog-loading">
          <el-icon :size="24" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="loadUsersError" class="dialog-loading">
          <p>加载失败</p>
          <el-button type="primary" size="small" @click="loadClusterUsers">重试</el-button>
        </div>
        <template v-else>
          <el-table :data="clusterUsers" border style="width: 100%; height: 450px;" v-loading="loadingUsers" :empty-text="''">
            <el-table-column type="index" label="序号" width="70" align="center" />
            <el-table-column prop="userMd5" label="用户ID" min-width="260" show-overflow-tooltip />
            <el-table-column prop="nickname" label="昵称" width="160" show-overflow-tooltip>
              <template #default="scope">
                {{ scope.row.nickname || '——' }}
              </template>
            </el-table-column>
            <el-table-column prop="ratingCount" label="评分次数" width="110" align="center" />
            <el-table-column prop="avgRating" label="平均评分" width="110" align="center">
              <template #default="scope">
                <span :class="ratingClass(scope.row.avgRating)">
                  {{ scope.row.avgRating ? scope.row.avgRating.toFixed(2) : '--' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="ratingStddev" label="评分标准差" width="110" align="center">
              <template #default="scope">
                {{ scope.row.ratingStddev ? scope.row.ratingStddev.toFixed(3) : '--' }}
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-container" style="position: static; margin-top: 16px;">
            <el-pagination
              v-model:current-page="userPageNum"
              v-model:page-size="userPageSize"
              :total="userTotalCount"
              :page-sizes="[10, 20, 50, 100]"
              layout="prev, pager, next, sizes"
              @current-change="loadClusterUsers"
              @size-change="loadClusterUsers"
            />
          </div>
        </template>
      </el-dialog>

      <!-- 业务解读 -->
      <div class="section insights-section">
        <h3>业务含义与建议</h3>
        <div class="insight-grid">
          <div v-for="insight in insights" :key="insight.title" class="insight-card">
            <div class="insight-title">{{ insight.title }}</div>
            <div class="insight-content">{{ insight.content }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { ref } from 'vue'
import axios from '../utils/axios'
import { Loading, DataAnalysis } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'UserProfileAnalysis',
  components: { Loading, DataAnalysis },
  setup() {
    const result = ref(null)
    const loading = ref(false)
    const loadError = ref(false)
    const errorMessage = ref('')
    const running = ref(false)

    // 簇用户列表相关
    const userDialogVisible = ref(false)
    const userDialogClusterId = ref(0)
    const clusterUsers = ref([])
    const loadingUsers = ref(false)
    const loadUsersError = ref(false)
    const userPageNum = ref(1)
    const userPageSize = ref(10)
    const userTotalCount = ref(0)

    async function executeAnalysis() {
      running.value = true
      loadError.value = false
      try {
        const response = await axios.post('/user-profile/cluster')
        if (response.data.code === '200') {
          result.value = response.data.data
          ElMessage.success('聚类分析完成')
        } else {
          ElMessage.error(response.data.message || '聚类分析失败')
          loadError.value = true
          errorMessage.value = response.data.message || '聚类分析失败'
        }
      } catch (error) {
        console.error('聚类分析失败:', error)
        ElMessage.error('聚类分析请求失败')
        loadError.value = true
        errorMessage.value = '聚类分析请求失败'
      } finally {
        running.value = false
      }
    }

    async function loadResult() {
      loading.value = true
      loadError.value = false
      try {
        const response = await axios.get('/user-profile/cluster-result')
        if (response.data.code === '200') {
          result.value = response.data.data
        } else {
          result.value = null
          if (response.data.message) {
            // 尚未分析的情况，不显示错误
          }
        }
      } catch (error) {
        console.error('加载聚类结果失败:', error)
        loadError.value = true
        errorMessage.value = '加载聚类结果失败'
      } finally {
        loading.value = false
      }
    }

    function getClusterTag(clusterId) {
      const tags = ['', 'success', 'warning', 'danger', 'info', 'primary']
      return tags[clusterId % tags.length] || ''
    }

    function ratingClass(val) {
      if (val >= 3) return 'score-high'
      if (val >= 2.5) return 'score-mid'
      return 'score-low'
    }

    function getProfileDescription(row) {
      const avgRating = parseFloat(row.avgRating)
      const avgCount = parseFloat(row.avgRatingCount)
      const stddev = parseFloat(row.avgRatingStddev)

      let desc = ''
      if (avgCount >= 50) {
        desc += '高活跃度'
      } else if (avgCount >= 15) {
        desc += '中等活跃度'
      } else {
        desc += '低活跃度'
      }

      if (avgRating >= 3) {
        desc += '，评分偏高'
      } else if (avgRating >= 2.3) {
        desc += '，评分中等'
      } else {
        desc += '，评分偏低'
      }

      if (stddev >= 1) {
        desc += '，评分波动大'
      } else {
        desc += '，评分稳定'
      }

      return desc
    }

    function showClusterUsers(row) {
      userDialogClusterId.value = row.clusterId
      userPageNum.value = 1
      userDialogVisible.value = true
      loadClusterUsers()
    }

    async function loadClusterUsers() {
      loadingUsers.value = true
      loadUsersError.value = false
      try {
        const response = await axios.get(
          `/user-profile/cluster/${userDialogClusterId.value}/users`,
          { params: { pageNum: userPageNum.value, pageSize: userPageSize.value } }
        )
        if (response.data.code === '200') {
          clusterUsers.value = response.data.data?.data || []
          userTotalCount.value = response.data.data?.totalCount || 0
        } else {
          clusterUsers.value = []
          userTotalCount.value = 0
        }
      } catch (error) {
        console.error('加载簇用户失败:', error)
        loadUsersError.value = true
      } finally {
        loadingUsers.value = false
      }
    }

    const insights = [
      {
        title: '核心用户群',
        content: '评分次数多、评分稳定或波动较大的用户是平台核心用户。对于评分高的用户（簇1），可推荐高质量内容、鼓励写影评；对于评分低的用户（簇2），需分析其评分对象类型，针对性改进或提供个性化推荐；对于评分波动大的用户（簇3），意见不稳定，可尝试用内容多样性留住他们。'
      },
      {
        title: '高活跃用户',
        content: '评分次数非常多但评分较低的用户，是平台核心数据贡献者，应给予奖励或特权，同时关注他们的低分趋势，排查是否存在系统性问题。'
      },
      {
        title: '活跃度与评分关联',
        content: '高评分并不与高活跃度正相关。用户行为模式多样，聚类有效区分了不同类型，可针对不同群体制定差异化的运营策略。'
      }
    ]

    // 页面加载时尝试获取已有结果
    loadResult()

    return {
      result,
      loading,
      loadError,
      errorMessage,
      running,
      executeAnalysis,
      loadResult,
      getClusterTag,
      ratingClass,
      getProfileDescription,
      insights,
      // 簇用户列表
      userDialogVisible,
      userDialogClusterId,
      clusterUsers,
      loadingUsers,
      loadUsersError,
      userPageNum,
      userPageSize,
      userTotalCount,
      showClusterUsers,
      loadClusterUsers
    }
  }
}
</script>

<style scoped>
.profile-analysis {
  padding: 20px;
  padding-bottom: 80px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #909399;
}

.loading-container .loading-icon {
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: #909399;
  gap: 16px;
}

.overview-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.overview-card {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  color: #fff;
}

.overview-card:nth-child(2) {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-value {
  font-size: 36px;
  font-weight: bold;
  line-height: 1.2;
}

.card-label {
  font-size: 14px;
  margin-top: 8px;
  opacity: 0.9;
}

.section {
  margin-bottom: 24px;
}

.section h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 16px 0;
  padding-left: 12px;
  border-left: 4px solid #409eff;
}

.profile-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.score-high { color: #67c23a; font-weight: bold; }
.score-mid { color: #e6a23c; font-weight: bold; }
.score-low { color: #f56c6c; font-weight: bold; }

.insights-section {
  margin-top: 32px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}

.insight-card {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  transition: box-shadow 0.3s;
}

.insight-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.insight-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
  display: inline-block;
}

.insight-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.dialog-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;
  gap: 12px;
}

.dialog-loading .loading-icon {
  animation: rotating 2s linear infinite;
}

.pagination-container {
  padding: 15px;
  background: #ffffff;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  bottom: 20px;
  left: max(20px, calc((100vw - 1400px) / 2));
  right: max(20px, calc((100vw - 1400px) / 2));
  z-index: 10;
}
</style>
