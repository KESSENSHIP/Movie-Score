<template>
  <div class="admin-recommendations">
    <h2>用户推荐管理</h2>
    
    <!-- 用户搜索 -->
    <div class="search-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户ID (MD5)"
        style="width: 300px;"
        clearable
        @keyup.enter="searchUsers"
      />
      <el-button type="primary" @click="searchUsers" :loading="searching">搜索</el-button>
      <el-button type="success" @click="showRecommendationsDirectly" :disabled="!searchKeyword.trim()">
        直接查看推荐
      </el-button>
    </div>

    <!-- 用户列表 -->
    <div v-if="users.length > 0" class="user-list">
      <el-table :data="users" border style="width: 100%" :empty-text="''" v-loading="searching">
        <el-table-column prop="userMd5" label="用户ID" min-width="280" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" width="200" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.nickname || '——' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="scope">
            <el-button type="primary" size="small" @click="showRecommendations(scope.row)">
              查看推荐
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 推荐结果 -->
    <div v-if="recommendations.length > 0" class="rec-section">
      <h3>
        推荐结果 - 用户: {{ selectedUserMd5 }}
        <el-tag type="info" style="margin-left: 10px;">共 {{ totalRecs }} 部推荐</el-tag>
      </h3>
      
      <div class="rec-grid">
        <div v-for="rec in recommendations" :key="rec.movieId" class="rec-card">
          <div class="rec-cover" :style="rec.movieCover ? { backgroundImage: `url(${rec.movieCover})` } : {}">
            <div v-if="!rec.movieCover" class="no-cover">
              <el-icon :size="32" color="#ccc"><Film /></el-icon>
            </div>
            <div class="rec-badge">#{{ rec.rank }}</div>
          </div>
          <div class="rec-info">
            <h4 class="rec-title">{{ rec.movieName || '未知电影' }}</h4>
            <div class="rec-score">
              推荐指数:
              <el-progress
                :percentage="Math.round(rec.predictedRating / 5 * 100)"
                :stroke-width="14"
                :text-inside="true"
                :color="scoreColor(rec.predictedRating)"
              >
                {{ rec.predictedRating.toFixed(2) }}
              </el-progress>
            </div>
            <p class="rec-meta">{{ rec.genres || '——' }} | {{ rec.directors || '——' }}</p>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="recPageNum"
          v-model:page-size="recPageSize"
          :total="totalRecs"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next, sizes"
          @current-change="loadRecommendations"
          @size-change="(s) => { recPageSize = s; recPageNum = 1; loadRecommendations() }"
        />
      </div>
    </div>

    <div v-if="noRecommendations" class="empty-state">
      <el-icon :size="64" color="#ccc"><WarningFilled /></el-icon>
      <p>该用户暂无推荐</p>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import axios from '../utils/axios'
import { Film, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'AdminRecommendations',
  components: { Film, WarningFilled },
  setup() {
    const searchKeyword = ref('')
    const searching = ref(false)
    const users = ref([])
    const selectedUserMd5 = ref('')
    const recommendations = ref([])
    const totalRecs = ref(0)
    const recPageNum = ref(1)
    const recPageSize = ref(20)
    const noRecommendations = ref(false)

    async function searchUsers() {
      if (!searchKeyword.value.trim()) return
      searching.value = true
      try {
        const response = await axios.get('/users/search', {
          params: { keyword: searchKeyword.value.trim(), pageNum: 1, pageSize: 20 }
        })
        // 兼容两种返回格式：带code的Result包装 或 直接返回PageResult
        const respData = response.data
        if (respData.code === '200') {
          users.value = respData.data?.data || []
        } else if (Array.isArray(respData.data)) {
          users.value = respData.data || []
        } else {
          users.value = []
        }
        recommendations.value = []
        noRecommendations.value = false
      } catch (error) {
        console.error('搜索用户失败:', error)
        ElMessage.error('搜索用户失败')
        users.value = []
      } finally {
        searching.value = false
      }
    }

    async function showRecommendations(user) {
      selectedUserMd5.value = user.userMd5
      recPageNum.value = 1
      recommendations.value = []
      noRecommendations.value = false
      loadRecommendations()
    }

    function showRecommendationsDirectly() {
      const userId = searchKeyword.value.trim()
      if (!userId) return
      selectedUserMd5.value = userId
      users.value = []
      recPageNum.value = 1
      recommendations.value = []
      noRecommendations.value = false
      loadRecommendations()
    }

    async function loadRecommendations() {
      if (!selectedUserMd5.value) return
      try {
        const response = await axios.get(`/recommendations/user/${selectedUserMd5.value}`, {
          params: { pageNum: recPageNum.value, pageSize: recPageSize.value }
        })
        // 兼容两种返回格式：带code的Result包装 或 直接返回PageResult
        const respData = response.data
        if (respData.code === '200') {
          recommendations.value = respData.data?.data || []
          totalRecs.value = respData.data?.totalCount || 0
        } else if (Array.isArray(respData.data)) {
          recommendations.value = respData.data || []
          totalRecs.value = respData.totalCount || 0
        } else {
          recommendations.value = []
          totalRecs.value = 0
        }
        noRecommendations.value = recommendations.value.length === 0
      } catch (error) {
        console.error('加载推荐失败:', error)
        ElMessage.error('加载推荐失败')
        recommendations.value = []
        totalRecs.value = 0
        noRecommendations.value = true
      }
    }

    function scoreColor(score) {
      if (score >= 4) return '#67c23a'
      if (score >= 3) return '#409eff'
      if (score >= 2) return '#e6a23c'
      return '#f56c6c'
    }

    return {
      searchKeyword, searching, users, selectedUserMd5,
      recommendations, totalRecs, recPageNum, recPageSize, noRecommendations,
      searchUsers, showRecommendations, showRecommendationsDirectly, loadRecommendations, scoreColor
    }
  }
}
</script>

<style scoped>
.admin-recommendations {
  padding: 20px;
  padding-bottom: 80px;
}

.admin-recommendations h2 {
  font-size: 22px;
  color: #303133;
  margin: 0 0 20px 0;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.user-list {
  margin-bottom: 24px;
}

.rec-section {
  margin-top: 24px;
}

.rec-section h3 {
  font-size: 18px;
  color: #303133;
  margin: 0 0 16px 0;
  padding-left: 12px;
  border-left: 4px solid #409eff;
}

.rec-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.rec-card {
  display: flex;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s;
}

.rec-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.rec-cover {
  width: 100px;
  min-height: 140px;
  background-size: cover;
  background-position: center;
  background-color: #f5f5f5;
  flex-shrink: 0;
  position: relative;
}

.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rec-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  background: rgba(0, 0, 0, 0.7);
  color: #ffd700;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.rec-info {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rec-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-score {
  font-size: 13px;
  color: #606266;
}

.rec-meta {
  margin: 0;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.pagination-container {
  padding: 15px;
  background: #ffffff;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;
  bottom: 20px;
  left: 20px;
  right: 20px;
  z-index: 10;
}
</style>
