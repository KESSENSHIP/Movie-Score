<template>
  <div class="user-recommendations">
    <div v-if="loading" class="loading-container">
      <div class="skeleton-grid">
        <div v-for="n in 8" :key="n" class="skeleton-card">
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-line skeleton-title"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line skeleton-short"></div>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="loadError" class="empty-state">
      <el-icon :size="64" color="#f56c6c"><WarningFilled /></el-icon>
      <p>加载推荐失败，请检查网络连接</p>
      <el-button type="primary" @click="loadRecommendations">重新加载</el-button>
    </div>
    <div v-else-if="recommendations.length === 0" class="empty-state">
      <el-icon :size="64" color="#ccc"><Star /></el-icon>
      <p>暂无推荐内容</p>
      <p class="empty-hint">多给电影评分，系统会为你生成更精准的推荐</p>
      <el-button type="primary" @click="switchToMovies">去评分</el-button>
    </div>
    <template v-else>
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            :loading="refreshing"
            @click="refreshBatch"
            class="refresh-btn"
          >
            <el-icon><Refresh /></el-icon>
            换一批
          </el-button>
          <el-button
            :type="onlyHigh ? 'success' : 'default'"
            @click="toggleHighScore"
            class="filter-btn"
          >
            <el-icon><Star /></el-icon>
            仅高分
          </el-button>
          <el-select
            v-model="filterGenre"
            placeholder="选择类型"
            clearable
            size="default"
            class="genre-select"
            @change="onGenreChange"
          >
            <el-option label="剧情" value="剧情" />
            <el-option label="喜剧" value="喜剧" />
            <el-option label="动作" value="动作" />
            <el-option label="爱情" value="爱情" />
            <el-option label="科幻" value="科幻" />
            <el-option label="悬疑" value="悬疑" />
            <el-option label="犯罪" value="犯罪" />
            <el-option label="动画" value="动画" />
            <el-option label="纪录片" value="纪录片" />
            <el-option label="战争" value="战争" />
            <el-option label="奇幻" value="奇幻" />
            <el-option label="冒险" value="冒险" />
          </el-select>
        </div>
        <span class="rec-count">
          共 {{ totalCount }} 条推荐
          <span v-if="shuffleCount > 0" class="shuffle-tip">· 已换 {{ shuffleCount }} 次</span>
        </span>
      </div>

      <transition-group name="card-fade" tag="div" class="recommendation-grid">
        <div
          v-for="rec in recommendations"
          :key="rec.movieId"
          class="recommendation-card"
          :style="{ transitionDelay: getDelay(rec.rank) }"
          @click="showMovieDetail(rec)"
        >
          <div class="rec-cover-wrap">
            <img
              v-if="rec.movieCover && !coverError[rec.movieId]"
              :src="rec.movieCover"
              :alt="rec.movieName"
              class="rec-poster"
              @error="onCoverError(rec.movieId)"
            />
            <div v-else class="rec-cover-placeholder">
              <el-icon :size="48" color="#bbb"><Film /></el-icon>
            </div>
            <div class="rec-rank">
              <el-icon :size="12" color="#ffd700"><Star /></el-icon>
              #{{ rec.rank }}
            </div>
            <div class="rec-score" :class="scoreClass(rec.predictedRating)">
              {{ rec.predictedRating.toFixed(1) }}
            </div>
          </div>
          <div class="rec-info">
            <h3 class="rec-title" :title="rec.movieName">{{ rec.movieName }}</h3>
            <div class="rec-tags">
              <el-tag
                v-if="rec.genres"
                size="small"
                type="info"
                effect="plain"
                class="rec-tag"
              >{{ rec.genres.split('|')[0] || rec.genres }}</el-tag>
              <el-tag
                v-if="rec.language"
                size="small"
                type="success"
                effect="plain"
                class="rec-tag"
              >{{ rec.language }}</el-tag>
              <el-tag
                v-if="rec.year"
                size="small"
                type="warning"
                effect="plain"
                class="rec-tag"
              >{{ rec.year }}</el-tag>
            </div>
            <p class="rec-directors" v-if="rec.directors">
              <el-icon :size="12" color="#909399"><User /></el-icon>
              {{ rec.directors }}</p>
          </div>
        </div>
      </transition-group>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalCount"
          :page-sizes="[10, 20, 50]"
          layout="prev, pager, next, sizes"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </template>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import axios from '../utils/axios'
import { WarningFilled, Star, Film, Refresh, User } from '@element-plus/icons-vue'
import md5 from '../utils/md5'

export default {
  name: 'UserRecommendations',
  components: { WarningFilled, Star, Film, Refresh, User },
  emits: ['show-movie-detail', 'switch-to-movies'],
  setup(props, { emit }) {
    const recommendations = ref([])
    const loading = ref(false)
    const refreshing = ref(false)
    const loadError = ref(false)
    const currentPage = ref(1)
    const pageSize = ref(20)
    const totalCount = ref(0)
    const coverError = reactive({})
    const shuffleCount = ref(0)
    const onlyHigh = ref(false)
    const filterGenre = ref('')

    function getUserMd5() {
      const username = sessionStorage.getItem('username')
      return username ? md5(username) : null
    }

    function onCoverError(movieId) {
      coverError[movieId] = true
    }

    function getDelay(rank) {
      const idx = ((rank || 1) - 1) % 20
      return `${idx * 30}ms`
    }

    async function loadRecommendations() {
      const userMd5 = getUserMd5()
      if (!userMd5) return

      loading.value = true
      loadError.value = false
      try {
        const params = {
          pageNum: currentPage.value,
          pageSize: pageSize.value,
          shuffle: false,
          minScore: onlyHigh.value ? 4 : 0
        }
        if (filterGenre.value) {
          params.genres = filterGenre.value
        }
        const response = await axios.get(`/recommendations/user/${userMd5}`, { params })
        if (response.data.code === '200') {
          recommendations.value = response.data.data?.data || []
          totalCount.value = response.data.data?.totalCount || 0
        } else {
          recommendations.value = []
          totalCount.value = 0
        }
      } catch (error) {
        console.error('加载推荐失败:', error)
        loadError.value = true
        recommendations.value = []
        totalCount.value = 0
      } finally {
        loading.value = false
      }
    }

    async function refreshBatch() {
      refreshing.value = true
      try {
        const userMd5 = getUserMd5()
        if (!userMd5) return

        const params = {
          pageNum: 1,
          pageSize: pageSize.value,
          shuffle: true,
          minScore: onlyHigh.value ? 4 : 0
        }
        if (filterGenre.value) {
          params.genres = filterGenre.value
        }

        const response = await axios.get(`/recommendations/user/${userMd5}`, { params })
        if (response.data.code === '200') {
          recommendations.value = response.data.data?.data || []
          totalCount.value = response.data.data?.totalCount || 0
          currentPage.value = 1
          shuffleCount.value += 1
        }
      } catch (error) {
        console.error('换一批失败:', error)
      } finally {
        refreshing.value = false
      }
    }

    function toggleHighScore() {
      onlyHigh.value = !onlyHigh.value
      currentPage.value = 1
      shuffleCount.value = 0
      loadRecommendations()
    }

    function onGenreChange() {
      currentPage.value = 1
      shuffleCount.value = 0
      loadRecommendations()
    }

    function handlePageChange(page) {
      currentPage.value = page
      loadRecommendations()
    }

    function handleSizeChange(size) {
      pageSize.value = size
      currentPage.value = 1
      loadRecommendations()
    }

    function showMovieDetail(rec) {
      emit('show-movie-detail', rec)
    }

    function switchToMovies() {
      emit('switch-to-movies')
    }

    onMounted(() => {
      loadRecommendations()
    })

    function scoreClass(score) {
      if (score >= 4) return 'score-high'
      if (score >= 3) return 'score-mid'
      return 'score-low'
    }

    return {
      recommendations,
      loading,
      refreshing,
      loadError,
      currentPage,
      pageSize,
      totalCount,
      coverError,
      shuffleCount,
      onlyHigh,
      filterGenre,
      loadRecommendations,
      refreshBatch,
      toggleHighScore,
      onGenreChange,
      handlePageChange,
      handleSizeChange,
      showMovieDetail,
      switchToMovies,
      onCoverError,
      getDelay,
      scoreClass
    }
  }
}
</script>

<style scoped>
.user-recommendations {
  min-height: 300px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #f5f7fa 100%);
  border-radius: 10px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
}

.genre-select {
  width: 130px;
}

.rec-count {
  font-size: 13px;
  color: #606266;
}

.shuffle-tip {
  color: #909399;
  margin-left: 4px;
}

.loading-container {
  padding: 20px 0;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.skeleton-cover {
  width: 100%;
  height: 260px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info {
  padding: 12px;
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 8px;
  border-radius: 4px;
}

.skeleton-title {
  height: 18px;
  width: 90%;
}

.skeleton-short {
  width: 60%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
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

.empty-hint {
  font-size: 13px;
  color: #bbb;
  margin: 0;
}

.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.recommendation-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.recommendation-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

/* 卡片淡入动画 */
.card-fade-enter-active {
  transition: all 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.card-fade-leave-active {
  transition: all 0.2s ease-out;
}
.card-fade-enter-from {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}
.card-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.rec-cover-wrap {
  width: 100%;
  height: 260px;
  position: relative;
  background-color: #f5f5f5;
  overflow: hidden;
}

.rec-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
}

.recommendation-card:hover .rec-poster {
  transform: scale(1.05);
}

.rec-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e8e8e8, #f5f5f5);
}

.rec-rank {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.75);
  color: #ffd700;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 3px;
  backdrop-filter: blur(4px);
}

.rec-score {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 3px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  background: rgba(0, 0, 0, 0.75);
  color: white;
  backdrop-filter: blur(4px);
}

.rec-score.score-high {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.rec-score.score-mid {
  background: linear-gradient(135deg, #e6a23c, #f0b852);
}

.rec-score.score-low {
  background: linear-gradient(135deg, #f56c6c, #f78989);
}

.rec-info {
  padding: 12px;
}

.rec-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}

.rec-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.rec-tag {
  font-size: 11px;
  height: 20px;
  padding: 0 6px;
  line-height: 18px;
}

.rec-directors {
  font-size: 12px;
  color: #909399;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 3px;
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
