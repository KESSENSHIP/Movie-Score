<template>
  <div class="user-dashboard">
    <!-- 顶部导航栏 -->
    <nav class="nav-bar">
      <div class="nav-left">
        <div class="logo"></div>
        <ul class="nav-menu">
          <li
            v-for="item in navItems"
            :key="item.name"
            :class="['nav-item', { active: activeTab === item.name }]"
            @click="switchTab(item.name)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </li>
        </ul>
      </div>
      <div class="nav-right">
        <div class="search-wrapper">
          <el-autocomplete
            v-model="searchKeyword"
            :fetch-suggestions="querySearchSuggestions"
            placeholder="搜索电影/导演/演员..."
            style="width: 320px;"
            clearable
            @select="handleSuggestionSelect"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
            size="default"
            :trigger-on-focus="false"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #default="{ item }">
              <div class="suggestion-item">
                <span class="suggestion-name">{{ item.value }}</span>
                <span class="suggestion-meta">
                  <span v-if="item.year" class="suggestion-year">{{ item.year }}</span>
                  <span v-if="item.genres" class="suggestion-genre">{{ item.genres?.split('|')[0] }}</span>
                </span>
              </div>
            </template>
          </el-autocomplete>
        </div>
      </div>
    </nav>

    <!-- 内容区域 -->
    <div class="content-area">
      <!-- 首页：猜你喜欢 + 电影浏览 -->
      <template v-if="activeTab === 'home'">
        <div class="home-layout">
          <!-- 左侧：猜你喜欢（ALS推荐滑动海报） -->
          <div class="recommend-section">
            <div class="section-header">
              <h2 class="section-title">
                <el-icon><MagicStick /></el-icon>
                猜你喜欢
              </h2>
              <el-button
                text
                @click="refreshRecommendations"
                :loading="recLoading"
              >
                <el-icon><Refresh /></el-icon>
                换一批
              </el-button>
            </div>

            <!-- 海报轮播 -->
            <div v-if="recLoading" class="rec-skeleton">
              <div v-for="n in 5" :key="n" class="rec-skeleton-item">
                <div class="sk-cover"></div>
                <div class="sk-title"></div>
              </div>
            </div>
            <div v-else-if="recommendations.length === 0" class="rec-empty">
              <el-icon :size="48" color="#ccc"><Star /></el-icon>
              <p>暂无推荐</p>
              <p class="hint">多评分电影可获得更好的推荐</p>
            </div>
            <el-carousel
              v-else
              height="520px"
              :interval="5000"
              indicator-position="outside"
              arrow="always"
              class="rec-carousel"
            >
              <el-carousel-item v-for="rec in recommendations" :key="rec.movieId">
                <div class="rec-card" @click="showMovieDetail(rec)">
                  <div class="rec-cover">
                    <img
                      v-if="rec.movieCover"
                      :src="rec.movieCover"
                      :alt="rec.movieName"
                      class="rec-poster"
                      @error="handlePosterError($event)"
                    />
                    <div v-else class="no-cover">
                      <el-icon :size="60" color="#ddd"><Film /></el-icon>
                    </div>
                    <div class="rec-score-badge" v-if="rec.score">
                      <el-icon><StarFilled /></el-icon>
                      {{ rec.score?.toFixed(1) || '--' }}
                    </div>
                    <div class="rec-index-badge" v-if="rec.index">
                      <el-icon><TrendCharts /></el-icon>
                      {{ rec.index?.toFixed(2) }}
                    </div>
                    <div class="rec-info">
                      <h3 class="rec-name">{{ rec.movieName }}</h3>
                      <div class="rec-tags">
                        <el-tag v-if="rec.genres" size="small" effect="dark">{{ rec.genres?.split('|')[0] }}</el-tag>
                        <el-tag v-if="rec.year" size="small" type="warning" effect="dark">{{ rec.year }}</el-tag>
                        <el-tag v-if="rec.language" size="small" type="success" effect="dark">{{ rec.language }}</el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>

            <!-- 随机评论 -->
            <div class="random-review">
              <div class="review-header">
                <h3 class="review-title">
                  <el-icon><ChatDotRound /></el-icon>
                  精彩影评
                </h3>
                <el-button text size="small" @click="loadRandomComment" :loading="reviewLoading">
                  <el-icon><Refresh /></el-icon>
                  换一条
                </el-button>
              </div>
              <div v-if="reviewInitialLoading && !randomReview" class="review-skeleton">
                <div class="sk-line sk-title-line"></div>
                <div class="sk-line"></div>
                <div class="sk-line sk-short"></div>
              </div>
              <div v-else-if="randomReview" class="review-card" :class="{ 'is-loading': reviewLoading }">
                <div class="review-card-header" @click="showMovieDetail(randomReview)">
                  <span class="review-movie-name">{{ randomReview.movieName }}</span>
                  <span class="review-movie-year" v-if="randomReview.year">{{ randomReview.year }}</span>
                </div>
                <p class="review-card-content">{{ randomReview.content }}</p>
              </div>
              <div v-else class="review-empty">
                <el-icon :size="32" color="#ddd"><ChatDotRound /></el-icon>
                <p>暂无评论</p>
              </div>
            </div>
          </div>

          <!-- 右侧：电影浏览 -->
          <div class="browse-section">
            <div class="section-header">
              <h2 class="section-title">
                <el-icon><Film /></el-icon>
                电影浏览
              </h2>
            </div>

            <!-- 筛选栏 -->
            <div class="filter-bar">
              <el-select v-model="filterGenre" placeholder="类型" clearable size="small" @change="handleFilterChange" class="filter-item">
                <el-option label="全部" value="" />
                <el-option label="剧情" value="剧情" />
                <el-option label="喜剧" value="喜剧" />
                <el-option label="动作" value="动作" />
                <el-option label="爱情" value="爱情" />
                <el-option label="科幻" value="科幻" />
                <el-option label="悬疑" value="悬疑" />
                <el-option label="恐怖" value="恐怖" />
                <el-option label="犯罪" value="犯罪" />
                <el-option label="动画" value="动画" />
                <el-option label="纪录片" value="纪录片" />
              </el-select>
              <el-select v-model="filterYear" placeholder="年份" clearable filterable size="small" @change="handleFilterChange" class="filter-item">
                <el-option label="全部" value="" />
                <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
              </el-select>
              <el-select v-model="filterRegion" placeholder="地区" clearable filterable size="small" @change="handleFilterChange" class="filter-item">
                <el-option label="全部" value="" />
                <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
              </el-select>
              <el-button v-if="hasActiveFilter" size="small" @click="clearFilters">清除</el-button>
            </div>

            <!-- 电影网格 -->
            <div v-if="loading" class="movie-grid">
              <div v-for="i in 8" :key="i" class="movie-card skeleton-card">
                <div class="movie-cover skeleton-cover"></div>
                <div class="movie-info">
                  <h3 class="movie-title skeleton-title"></h3>
                  <div class="movie-meta">
                    <span class="skeleton-score"></span>
                    <span class="skeleton-year"></span>
                  </div>
                </div>
              </div>
            </div>
            <div v-else-if="loadError" class="empty-state">
              <p>加载失败</p>
              <el-button type="primary" size="small" @click="loadMovies">重试</el-button>
            </div>
            <div v-else class="movie-grid">
              <div
                v-for="movie in movies"
                :key="movie.movieId"
                class="movie-card"
                @click="showMovieDetail(movie)"
              >
                <div class="movie-cover" :style="{ backgroundImage: `url(${movie.cover})` }">
                  <div v-if="!movie.cover" class="no-cover">
                    <el-icon :size="40" color="#ccc"><Film /></el-icon>
                  </div>
                  <span class="movie-score" v-if="movie.doubanScore">{{ movie.doubanScore }}</span>
                </div>
                <div class="movie-info">
                  <h3 class="movie-title">{{ movie.name }}</h3>
                  <div class="movie-meta">
                    <span class="year">{{ movie.year }}</span>
                    <span class="genres">{{ movie.genres?.split('|')[0] }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="totalCount"
                layout="prev, pager, next"
                small
                @current-change="handlePageChange"
              />
            </div>
          </div>
        </div>
      </template>

      <!-- 历史记录 -->
      <template v-else-if="activeTab === 'history'">
        <div class="sub-view">
          <el-tabs v-model="historyTab" @tab-change="handleHistoryTabChange">
            <el-tab-pane label="浏览记录" name="view">
              <div v-if="viewHistoryLoading" class="empty-history">
                <el-icon :size="32" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
                <p>加载中...</p>
              </div>
              <div v-else-if="viewHistory.length === 0" class="empty-history">
                <el-icon :size="64" color="#ccc"><Clock /></el-icon>
                <p>暂无浏览记录</p>
              </div>
              <div v-else class="history-list">
                <div v-for="item in viewHistory" :key="item.id" class="history-item" @click="showMovieDetailFromHistory(item)">
                  <div class="history-cover" :style="{ backgroundImage: `url(${item.movieCover})` }">
                    <div v-if="!item.movieCover" class="no-cover">
                      <el-icon :size="32" color="#ccc"><Film /></el-icon>
                    </div>
                  </div>
                  <div class="history-info">
                    <h4>{{ item.movieName }}</h4>
                    <p class="history-time">{{ formatTime(item.viewTime) }}</p>
                  </div>
                  <div class="history-action">
                    <el-button type="text" @click.stop="removeViewHistory(item.id)">删除</el-button>
                  </div>
                </div>
              </div>
              <div v-if="!viewHistoryLoading && viewHistory.length > 0" class="pagination-container">
                <el-pagination
                  v-model:current-page="viewHistoryPage"
                  :page-size="historyPageSize"
                  :total="viewHistoryTotal"
                  layout="prev, pager, next"
                  small
                  @current-change="handleViewHistoryPageChange"
                />
              </div>
            </el-tab-pane>
            <el-tab-pane label="评价记录" name="review">
              <div v-if="reviewHistoryLoading" class="empty-history">
                <el-icon :size="32" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
                <p>加载中...</p>
              </div>
              <div v-else-if="reviewHistory.length === 0" class="empty-history">
                <el-icon :size="64" color="#ccc"><Star /></el-icon>
                <p>暂无评价记录</p>
              </div>
              <div v-else class="history-list">
                <div v-for="item in reviewHistory" :key="item.id" class="history-item">
                  <div class="history-cover" :style="{ backgroundImage: `url(${item.movieCover})` }">
                    <div v-if="!item.movieCover" class="no-cover">
                      <el-icon :size="32" color="#ccc"><Film /></el-icon>
                    </div>
                  </div>
                  <div class="history-info" @click="showMovieDetailFromHistory(item)">
                    <h4>{{ item.movieName }}</h4>
                    <div class="review-stars">
                      <el-icon v-for="i in 5" :key="i" :color="i <= item.rating ? '#e6a23c' : '#ccc'"><Star /></el-icon>
                      <span class="rating-value">{{ item.rating }}分</span>
                    </div>
                    <p v-if="item.comment" class="review-content">{{ item.comment }}</p>
                    <p class="history-time">{{ formatTime(item.reviewTime) }}</p>
                  </div>
                  <div class="history-action">
                    <el-button type="text" @click.stop="startEditReview(item)">编辑</el-button>
                    <el-button type="text" style="color: #f56c6c;" @click.stop="removeReviewHistory(item)">删除</el-button>
                  </div>
                </div>
              </div>
              <div v-if="!reviewHistoryLoading && reviewHistory.length > 0" class="pagination-container">
                <el-pagination
                  v-model:current-page="reviewHistoryPage"
                  :page-size="historyPageSize"
                  :total="reviewHistoryTotal"
                  layout="prev, pager, next"
                  small
                  @current-change="handleReviewHistoryPageChange"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>

      <!-- 数据分析 -->
      <template v-else-if="activeTab === 'stats'">
        <div class="sub-view"><StatsAnalysis /></div>
      </template>

      <!-- 预测评分 -->
      <template v-else-if="activeTab === 'predictions'">
        <div class="sub-view"><UserPrediction /></div>
      </template>

      <!-- 为你推荐 -->
      <template v-else-if="activeTab === 'recommendations'">
        <div class="sub-view">
          <UserRecommendations
            @show-movie-detail="handleRecMovieDetail"
            @switch-to-movies="switchToHome"
          />
        </div>
      </template>

      <!-- 趋势预测 -->
      <template v-else-if="activeTab === 'trend'">
        <div class="sub-view"><UserTrendForecast /></div>
      </template>

      <!-- 情感分析 -->
      <template v-else-if="activeTab === 'sentiment'">
        <div class="sub-view"><SentimentAnalysis /></div>
      </template>
    </div>

    <!-- 电影详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="selectedMovie?.name" width="800px">
      <div v-if="selectedMovie" class="movie-detail">
        <div class="detail-cover" :style="{ backgroundImage: `url(${selectedMovie.cover})` }">
          <div v-if="!selectedMovie.cover" class="no-cover">
            <el-icon :size="64" color="#ccc"><Film /></el-icon>
          </div>
        </div>
        <div class="detail-info">
          <div class="detail-row"><span class="label">评分:</span><span class="value score">{{ selectedMovie.doubanScore || '--' }}</span></div>
          <div class="detail-row"><span class="label">年份:</span><span class="value">{{ selectedMovie.year }}</span></div>
          <div class="detail-row"><span class="label">类型:</span><span class="value">{{ selectedMovie.genres }}</span></div>
          <div class="detail-row"><span class="label">导演:</span><span class="value">{{ selectedMovie.directors }}</span></div>
          <div class="detail-row"><span class="label">演员:</span><span class="value">{{ selectedMovie.actors }}</span></div>
          <div class="detail-row"><span class="label">语言:</span><span class="value">{{ selectedMovie.language }}</span></div>
          <div class="detail-row"><span class="label">地区:</span><span class="value">{{ selectedMovie.region }}</span></div>
          <div class="detail-row"><span class="label">片长:</span><span class="value">{{ selectedMovie.mins }}分钟</span></div>
          <div class="detail-row"><span class="label">上映日期:</span><span class="value">{{ selectedMovie.releaseDate }}</span></div>
          <div class="detail-row full"><span class="label">简介:</span><span class="value">{{ selectedMovie.storyline || '--' }}</span></div>
        </div>
      </div>

      <div v-if="selectedMovie" class="review-form">
        <h3>我要评价</h3>
        <el-form :model="reviewForm" label-width="80px">
          <el-form-item label="评分">
            <div class="star-rating">
              <el-icon v-for="i in 5" :key="i" :size="32" :color="i <= reviewForm.rating ? '#e6a23c' : '#ccc'" @click="setRating(i)" class="star-icon"><Star /></el-icon>
              <span class="rating-text">{{ reviewForm.rating || 0 }}分</span>
            </div>
          </el-form-item>
          <el-form-item label="评论">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" placeholder="写下你的评价..." />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitReview" :loading="submitting">提交评价</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-if="selectedMovie" class="comments-section">
        <h3>电影评论 ({{ commentTotal }})</h3>
        <div v-if="commentsLoading" class="comments-loading">
          <el-icon :size="24" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
          <span>加载评论中...</span>
        </div>
        <div v-else-if="movieComments.length === 0" class="comments-empty">
          <el-icon :size="40" color="#ccc"><ChatDotRound /></el-icon>
          <p>暂无评论，来写第一条评论吧</p>
        </div>
        <div v-else class="comment-list">
          <div v-for="comment in movieComments" :key="comment.commentId" class="comment-item">
            <div class="comment-header">
              <span class="comment-user">{{ comment.nickname || '匿名用户' }}</span>
              <span class="comment-stars">
                <el-icon v-for="i in 5" :key="i" :size="14" :color="i <= (comment.rating || 0) ? '#e6a23c' : '#ccc'"><Star /></el-icon>
                <span class="comment-rating-text">{{ comment.rating || '--' }}分</span>
              </span>
              <span class="comment-time">{{ formatTime(comment.commentTime) }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
            <div v-if="comment.userMd5 === currentUserMd5" class="comment-actions">
              <el-button type="text" size="small" @click="startEditComment(comment)">编辑</el-button>
              <el-button type="text" size="small" style="color: #f56c6c;" @click="deleteUserComment(comment.commentId)">删除</el-button>
            </div>
          </div>
        </div>
        <div v-if="commentTotal > commentPageSize" class="comment-pagination">
          <el-pagination
            v-model:current-page="commentPage"
            :page-size="commentPageSize"
            :total="commentTotal"
            layout="prev, pager, next, jumper"
            small
            @current-change="handleCommentPageChange"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import axios from '../utils/axios'
import {
  Film, Star, Clock, Loading, ChatDotRound, Search, MagicStick,
  Refresh, StarFilled, TrendCharts, DataAnalysis, EditPen,
  HomeFilled, ChatLineRound, UserFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatsAnalysis from './StatsAnalysis.vue'
import UserPrediction from './UserPrediction.vue'
import UserRecommendations from './UserRecommendations.vue'
import UserTrendForecast from './UserTrendForecast.vue'
import SentimentAnalysis from './SentimentAnalysis.vue'
import md5 from '../utils/md5'

const navItems = [
  { name: 'home', label: '首页', icon: 'HomeFilled' },
  { name: 'history', label: '历史记录', icon: 'Clock' },
  { name: 'stats', label: '数据分析', icon: 'DataAnalysis' },
  { name: 'predictions', label: '预测评分', icon: 'EditPen' },
  { name: 'recommendations', label: '为你推荐', icon: 'MagicStick' },
  { name: 'trend', label: '趋势预测', icon: 'TrendCharts' },
  { name: 'sentiment', label: '情感分析', icon: 'ChatLineRound' }
]

const activeTab = ref('home')
const historyTab = ref('view')
const searchKeyword = ref('')

// ALS推荐数据
const recommendations = ref([])
const recLoading = ref(false)

// 随机评论
const randomReview = ref(null)
const reviewLoading = ref(false)
const reviewInitialLoading = ref(true)
let reviewTimer = null

// 电影列表
const movies = ref([])
const currentPage = ref(1)
const pageSize = ref(8)
const totalCount = ref(0)

// 筛选条件
const filterGenre = ref('')
const filterYear = ref('')
const filterRegion = ref('')

const currentYear = new Date().getFullYear()
const yearOptions = []
for (let y = currentYear; y >= 1900; y--) yearOptions.push(String(y))

const regionOptions = ['美国', '英国', '中国', '日本', '韩国', '法国', '德国', '意大利', '西班牙', '加拿大', '澳大利亚', '印度', '俄罗斯', '巴西']

const hasActiveFilter = computed(() => filterGenre.value || filterYear.value || filterRegion.value)

const loading = ref(false)
const loadError = ref(false)

const detailVisible = ref(false)
const selectedMovie = ref(null)
const reviewForm = ref({ rating: 0, content: '' })
const submitting = ref(false)

const movieComments = ref([])
const commentPage = ref(1)
const commentPageSize = ref(10)
const commentTotal = ref(0)
const commentsLoading = ref(false)
const currentUserMd5 = ref('')

const viewHistory = ref([])
const viewHistoryPage = ref(1)
const viewHistoryTotal = ref(0)
const viewHistoryLoading = ref(false)

const reviewHistory = ref([])
const reviewHistoryPage = ref(1)
const reviewHistoryTotal = ref(0)
const reviewHistoryLoading = ref(false)

const editReviewVisible = ref(false)
const editReviewItem = ref(null)
const editReviewForm = ref({ rating: 0, comment: '' })
const editReviewSubmitting = ref(false)
const historyPageSize = ref(20)

// 评论编辑
const editingCommentId = ref(null)
const editContent = ref('')
const editSubmitting = ref(false)

// 导航切换
function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'home') {
    loadMovies()
    loadRecommendations()
  } else if (tab === 'history') {
    if (historyTab.value === 'view') loadViewHistory()
    else loadReviewHistory()
  }
}

async function loadRecommendations() {
  recLoading.value = true
  try {
    const userMd5 = getUserMd5()
    if (!userMd5) {
      recommendations.value = []
      return
    }
    const response = await axios.get(`/recommendations/user/${userMd5}`, {
      params: { pageNum: 1, pageSize: 10, shuffle: true, minScore: 0 }
    })
    // Result<PageResult<Recommendation>> => response.data.data.data
    recommendations.value = response.data?.data?.data || []
  } catch (error) {
    console.error('加载推荐失败:', error)
    recommendations.value = []
  } finally {
    recLoading.value = false
  }
}

async function refreshRecommendations() {
  recLoading.value = true
  try {
    const userMd5 = getUserMd5()
    if (!userMd5) return
    const response = await axios.get(`/recommendations/user/${userMd5}`, {
      params: { pageNum: Math.floor(Math.random() * 5) + 1, pageSize: 10, shuffle: true, minScore: 0 }
    })
    recommendations.value = response.data?.data?.data || []
    ElMessage.success('已换一批')
  } catch (error) {
    console.error('刷新推荐失败:', error)
    ElMessage.error('刷新失败')
  } finally {
    recLoading.value = false
  }
}

async function loadMovies() {
  loading.value = true
  loadError.value = false
  try {
    const params = { pageNum: currentPage.value, pageSize: pageSize.value }
    const hasKeyword = searchKeyword.value && searchKeyword.value.trim()
    const hasGenre = filterGenre.value
    const hasYear = filterYear.value
    const hasRegion = filterRegion.value

    if (hasKeyword || hasGenre || hasYear || hasRegion) {
      if (hasKeyword) params.keyword = searchKeyword.value.trim()
      if (hasGenre) params.genre = filterGenre.value
      if (hasYear) params.year = filterYear.value
      if (hasRegion) params.region = filterRegion.value

      // 仅关键词搜索时使用/search接口（支持按演员、导演等搜索）
      if (hasKeyword && !hasGenre && !hasYear && !hasRegion) {
        const response = await axios.get('/movies/search', { params })
        movies.value = response.data.data
        totalCount.value = response.data.totalCount
      } else {
        const response = await axios.get('/movies/filter', { params })
        movies.value = response.data.data
        totalCount.value = response.data.totalCount
      }
    } else {
      const response = await axios.get('/movies', { params })
      movies.value = response.data.data
      totalCount.value = response.data.totalCount
    }
  } catch (error) {
    console.error('加载电影失败:', error)
    loadError.value = true
    movies.value = []
    totalCount.value = 0
    if (error.response && error.response.status !== 401) {
      ElMessage.error('加载电影失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  const switchedToHome = activeTab.value !== 'home'
  if (switchedToHome) {
    activeTab.value = 'home'
  }
  loadMovies()
  if (switchedToHome) {
    loadRecommendations()
    loadRandomComment()
  }
}

let suggestionTimer = null
function querySearchSuggestions(queryString, cb) {
  if (suggestionTimer) clearTimeout(suggestionTimer)
  if (!queryString) {
    cb([])
    return
  }
  suggestionTimer = setTimeout(async () => {
    try {
      const response = await axios.get('/movies/search', {
        params: { keyword: queryString, pageNum: 1, pageSize: 8 }
      })
      const list = response.data?.data || []
      const suggestions = list.map(m => ({
        value: m.name,
        movieId: m.movieId || m.id,
        year: m.year,
        genres: m.genres
      }))
      cb(suggestions)
    } catch (err) {
      console.error('搜索建议失败:', err)
      cb([])
    }
  }, 200)
}

function handleSuggestionSelect(item) {
  searchKeyword.value = item.value
  currentPage.value = 1
  const switchedToHome = activeTab.value !== 'home'
  if (switchedToHome) {
    activeTab.value = 'home'
  }
  loadMovies()
  if (switchedToHome) {
    loadRecommendations()
    loadRandomComment()
  }
}

async function loadRandomComment() {
  reviewLoading.value = true
  try {
    const pageNum = Math.floor(Math.random() * 50) + 1
    const response = await axios.get('/comments', {
      params: { pageNum: pageNum, pageSize: 1, sortBy: 'time' }
    })
    const list = response.data?.data
    if (list && list.length > 0) {
      const comment = list[0]
      let movieName = comment.movieId
      try {
        const movieRes = await axios.get('/movies/' + comment.movieId)
        if (movieRes.data) {
          movieName = movieRes.data.name
          comment.year = movieRes.data.year
        }
      } catch (e) {
        // 忽略
      }
      comment.movieName = movieName
      // 加载完成后再替换旧评论
      randomReview.value = comment
    } else {
      randomReview.value = null
    }
  } catch (error) {
    console.error('加载随机评论失败:', error)
    randomReview.value = null
  } finally {
    reviewLoading.value = false
    reviewInitialLoading.value = false
  }
}

function startReviewAutoSwitch() {
  if (reviewTimer) clearInterval(reviewTimer)
  reviewTimer = setInterval(() => {
    loadRandomComment()
  }, 60000)
}

function stopReviewAutoSwitch() {
  if (reviewTimer) {
    clearInterval(reviewTimer)
    reviewTimer = null
  }
}

function handlePosterError(event) {
  event.target.style.display = 'none'
}

function handleFilterChange() {
  currentPage.value = 1
  loadMovies()
}

function clearFilters() {
  filterGenre.value = ''
  filterYear.value = ''
  filterRegion.value = ''
  currentPage.value = 1
  loadMovies()
}

function handlePageChange(page) {
  currentPage.value = page
  loadMovies()
}

function handleHistoryTabChange(tab) {
  if (tab === 'view') loadViewHistory()
  else loadReviewHistory()
}

function showMovieDetail(movie) {
  // 兼容推荐数据（movieId -> movieId 映射）
  const movieId = movie.movieId || movie.movieId
  const movieName = movie.movieName || movie.name
  const movieCover = movie.movieCover || movie.cover

  selectedMovie.value = {
    movieId: movieId,
    name: movieName,
    cover: movieCover,
    year: movie.year,
    genres: movie.genres,
    directors: movie.directors,
    language: movie.language,
    doubanScore: movie.score ?? movie.doubanScore
  }
  detailVisible.value = true
  reviewForm.value = { rating: 0, content: '' }
  commentPage.value = 1
  if (movieId) loadMovieComments()
  addViewHistory({ movieId: movieId })
}

function showMovieDetailFromHistory(item) {
  axios.get('/movies/' + item.movieId).then(response => {
    selectedMovie.value = response.data
    detailVisible.value = true
    reviewForm.value = { rating: 0, content: '' }
    commentPage.value = 1
    loadMovieComments()
    addViewHistory({ movieId: item.movieId })
  }).catch(() => {
    selectedMovie.value = { movieId: item.movieId, name: item.movieName, cover: item.movieCover }
    detailVisible.value = true
    reviewForm.value = { rating: 0, content: '' }
    commentPage.value = 1
    loadMovieComments()
  })
}

function setRating(rating) {
  reviewForm.value.rating = rating
}

function getCurrentTimestamp() {
  const now = new Date()
  const offset = -now.getTimezoneOffset()
  const sign = offset >= 0 ? '+' : '-'
  const pad = (n) => String(n).padStart(2, '0')
  const hours = pad(Math.floor(Math.abs(offset) / 60))
  const minutes = pad(Math.abs(offset) % 60)
  return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) + 'T' +
    pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds()) + '.' +
    String(now.getMilliseconds()).padStart(3, '0') + sign + hours + ':' + minutes
}

async function submitReview() {
  if (!reviewForm.value.rating) {
    ElMessage.warning('请先选择评分')
    return
  }
  submitting.value = true
  try {
    const username = sessionStorage.getItem('username')
    const userMd5 = md5(username)
    const now = getCurrentTimestamp()

    await axios.post('/ratings', {
      userMd5: userMd5,
      movieId: selectedMovie.value.movieId,
      rating: reviewForm.value.rating,
      ratingTime: now
    })

    if (reviewForm.value.content.trim()) {
      await axios.post('/comments', {
        userMd5: userMd5,
        movieId: selectedMovie.value.movieId,
        content: reviewForm.value.content,
        rating: reviewForm.value.rating,
        votes: 0,
        commentTime: now
      })
    }

    await axios.post('/user-history/review', {
      userMd5: userMd5,
      movieId: selectedMovie.value.movieId,
      rating: reviewForm.value.rating,
      comment: reviewForm.value.content
    })

    ElMessage.success('评价成功')
    reviewForm.value = { rating: 0, content: '' }
    loadMovieComments()
    loadReviewHistory()
  } catch (error) {
    console.error('提交评价失败:', error)
    ElMessage.error('评价失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function addViewHistory(movie) {
  try {
    const username = sessionStorage.getItem('username')
    if (!username) return
    const userMd5 = md5(username)
    await axios.post('/user-history/view', { userMd5: userMd5, movieId: movie.movieId })
  } catch (error) {
    console.error('添加浏览记录失败:', error)
  }
}

function getUserMd5() {
  const username = sessionStorage.getItem('username')
  return username ? md5(username) : null
}

async function loadViewHistory() {
  viewHistoryLoading.value = true
  try {
    const userMd5 = getUserMd5()
    if (!userMd5) { viewHistory.value = []; viewHistoryTotal.value = 0; return }
    const response = await axios.get('/user-history/view', { params: { userMd5: userMd5, pageNum: viewHistoryPage.value, pageSize: historyPageSize.value } })
    viewHistory.value = response.data.data || []
    viewHistoryTotal.value = response.data.totalCount || 0
  } catch (error) {
    console.error('加载浏览记录失败:', error)
    viewHistory.value = []
    viewHistoryTotal.value = 0
  } finally {
    viewHistoryLoading.value = false
  }
}

async function loadReviewHistory() {
  reviewHistoryLoading.value = true
  try {
    const userMd5 = getUserMd5()
    if (!userMd5) { reviewHistory.value = []; reviewHistoryTotal.value = 0; return }
    const response = await axios.get('/user-history/review', { params: { userMd5: userMd5, pageNum: reviewHistoryPage.value, pageSize: historyPageSize.value } })
    reviewHistory.value = response.data.data || []
    reviewHistoryTotal.value = response.data.totalCount || 0
  } catch (error) {
    console.error('加载评价记录失败:', error)
    reviewHistory.value = []
    reviewHistoryTotal.value = 0
  } finally {
    reviewHistoryLoading.value = false
  }
}

async function removeViewHistory(id) {
  try {
    await axios.delete('/user-history/view/' + id)
    loadViewHistory()
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

async function removeReviewHistory(item) {
  try {
    if (item.id) await axios.delete('/user-history/review/' + item.id)
    loadReviewHistory()
    ElMessage.success('删除成功')
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

function startEditReview(item) {
  editReviewItem.value = item
  editReviewForm.value = { rating: item.rating || 0, comment: item.comment || '' }
  editReviewVisible.value = true
}

async function saveEditReview() {
  if (!editReviewForm.value.rating) { ElMessage.warning('请选择评分'); return }
  const item = editReviewItem.value
  if (!item || !item.id) { ElMessage.error('评价数据异常'); return }
  editReviewSubmitting.value = true
  try {
    const response = await axios.put('/user-history/review', { id: item.id, userMd5: item.userMd5, movieId: item.movieId, rating: editReviewForm.value.rating, comment: editReviewForm.value.comment })
    if (response.data.code === '200') {
      ElMessage.success('评价更新成功')
      editReviewVisible.value = false
      loadReviewHistory()
    } else {
      ElMessage.error(response.data.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('编辑评价失败')
  } finally {
    editReviewSubmitting.value = false
  }
}

async function loadMovieComments() {
  if (!selectedMovie.value) return
  commentsLoading.value = true
  try {
    const response = await axios.get('/comments/movie/' + selectedMovie.value.movieId, { params: { pageNum: commentPage.value, pageSize: commentPageSize.value } })
    movieComments.value = response.data.data || []
    commentTotal.value = response.data.totalCount || 0
  } catch (error) {
    movieComments.value = []
    commentTotal.value = 0
  } finally {
    commentsLoading.value = false
  }
}

function handleCommentPageChange(page) {
  commentPage.value = page
  loadMovieComments()
}

function startEditComment(comment) {
  editingCommentId.value = comment.commentId
  editContent.value = comment.content
}

async function saveEditComment(comment) {
  editSubmitting.value = true
  try {
    const response = await axios.put('/comments', { commentId: comment.commentId, userMd5: comment.userMd5, movieId: comment.movieId, content: editContent.value, rating: comment.rating, votes: comment.votes, commentTime: comment.commentTime })
    if (response.data.code === '200') {
      ElMessage.success('评论修改成功')
      editingCommentId.value = null
      editContent.value = ''
      loadMovieComments()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('修改评论失败')
  } finally {
    editSubmitting.value = false
  }
}

async function deleteUserComment(commentId) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗?', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const response = await axios.delete('/comments/' + commentId)
    if (response.data.code === '200') {
      ElMessage.success('评论删除成功')
      loadMovieComments()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

function handleViewHistoryPageChange(page) { viewHistoryPage.value = page; loadViewHistory() }
function handleReviewHistoryPageChange(page) { reviewHistoryPage.value = page; loadReviewHistory() }

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const diff = new Date() - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

function handleRecMovieDetail(rec) {
  axios.get('/movies/' + rec.movieId).then(response => {
    selectedMovie.value = response.data
    detailVisible.value = true
    reviewForm.value = { rating: 0, content: '' }
    commentPage.value = 1
    loadMovieComments()
  }).catch(() => {
    selectedMovie.value = { movieId: rec.movieId, name: rec.movieName, cover: rec.movieCover }
    detailVisible.value = true
    reviewForm.value = { rating: 0, content: '' }
    commentPage.value = 1
    loadMovieComments()
  })
}

function switchToHome() {
  activeTab.value = 'home'
}

onMounted(() => {
  currentUserMd5.value = getUserMd5()
  loadMovies()
  loadRecommendations()
  loadRandomComment()
  startReviewAutoSwitch()
})

onBeforeUnmount(() => {
  stopReviewAutoSwitch()
})
</script>

<style scoped>
.user-dashboard {
  min-height: 100vh;
  background: #f5f7fa;
}

/* 顶部导航 */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
}

.nav-menu {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.nav-item.active {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.nav-right {
  display: flex;
  align-items: center;
}

.search-wrapper {
  position: relative;
}

.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}

.suggestion-name {
  font-weight: 500;
  color: #303133;
}

.suggestion-meta {
  display: flex;
  gap: 6px;
  font-size: 12px;
}

.suggestion-year {
  color: #909399;
}

.suggestion-genre {
  color: #667eea;
  background: #ecf5ff;
  padding: 1px 6px;
  border-radius: 4px;
}

/* 内容区 */
.content-area {
  padding: 20px;
  min-height: calc(100vh - 64px);
}

.sub-view {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  min-height: calc(100vh - 124px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 首页布局 */
.home-layout {
  display: grid;
  grid-template-columns: 4fr 6fr;
  gap: 20px;
  min-height: calc(100vh - 124px);
}

.recommend-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
}

.browse-section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.section-title .el-icon {
  color: #667eea;
}

/* 推荐轮播 */
.rec-carousel {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 8px;
}

.rec-carousel :deep(.el-carousel__item) {
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
}

.rec-carousel :deep(.el-carousel__item > div) {
  height: 100%;
}

.rec-carousel :deep(.el-carousel__indicators--outside) {
  margin-top: 4px;
}

.rec-carousel :deep(.el-carousel__indicator .el-carousel__button) {
  background-color: #c0c4cc;
}

.rec-card {
  cursor: pointer;
  height: 100%;
  width: 100%;
  position: relative;
  border-radius: 12px;
  overflow: hidden;
}

.rec-cover {
  width: 100%;
  height: 100%;
  background-color: #f0f0f0;
  position: relative;
  overflow: hidden;
}

.rec-poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.rec-score-badge,
.rec-index-badge {
  position: absolute;
  top: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #fff;
}

.rec-score-badge {
  right: 12px;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.rec-index-badge {
  left: 12px;
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.rec-info {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 40px 16px 12px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.4) 60%, transparent 100%);
}

.rec-name {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.5);
}

.rec-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* 横向滚动海报列表 */
.rec-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-behavior: smooth;
}

.rec-scroll::-webkit-scrollbar {
  height: 4px;
}

.rec-scroll::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 2px;
}

.rec-thumb {
  flex-shrink: 0;
  width: 110px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.rec-thumb:hover {
  transform: translateY(-4px);
}

.thumb-cover {
  width: 110px;
  height: 150px;
  background-size: cover;
  background-position: center;
  background-color: #f5f5f5;
  border-radius: 8px;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.thumb-score {
  position: absolute;
  bottom: 6px;
  right: 6px;
  background: rgba(0, 0, 0, 0.7);
  color: #ffd700;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.thumb-name {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

/* 骨架屏 */
.rec-skeleton {
  display: flex;
  gap: 12px;
  padding: 16px 0;
}

.rec-skeleton-item {
  flex: 1;
}

.sk-cover {
  height: 200px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: sk-loading 1.5s infinite;
  border-radius: 8px;
}

.sk-title {
  height: 14px;
  margin-top: 10px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: sk-loading 1.5s infinite;
  border-radius: 4px;
}

@keyframes sk-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.rec-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #909399;
  min-height: 300px;
}

.rec-empty .hint {
  font-size: 13px;
  color: #c0c4cc;
}

/* 随机评论 */
.random-review {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #ebeef5;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.review-title .el-icon {
  color: #667eea;
}

.review-card {
  padding: 10px 12px;
  background: #f8f9fb;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  transition: opacity 0.3s;
}

.review-card.is-loading {
  opacity: 0.6;
}

.review-card.is-loading::after {
  content: '';
  position: absolute;
  bottom: 8px;
  right: 10px;
  width: 14px;
  height: 14px;
  border: 2px solid #c0c4cc;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: review-spin 0.8s linear infinite;
}

@keyframes review-spin {
  to { transform: rotate(360deg); }
}

.review-card-header {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
  cursor: pointer;
}

.review-card-header:hover .review-movie-name {
  color: #667eea;
}

.review-movie-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  transition: color 0.2s;
}

.review-movie-year {
  font-size: 12px;
  color: #909399;
}

.review-card-content {
  font-size: 13px;
  line-height: 1.5;
  color: #606266;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.review-skeleton {
  padding: 8px 0;
}

.sk-line {
  height: 14px;
  background: linear-gradient(90deg, #eee 25%, #ddd 50%, #eee 75%);
  background-size: 200% 100%;
  animation: sk-loading 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 10px;
}

.sk-line.sk-title-line {
  width: 40%;
  height: 16px;
}

.sk-line.sk-short {
  width: 60%;
}

.review-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #909399;
}

.review-empty p {
  margin-top: 8px;
  font-size: 13px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-bar .filter-item {
  width: 120px;
}

/* 电影网格 */
.movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  flex: 1;
  align-content: start;
  overflow-y: auto;
  max-height: calc(100vh - 280px);
  padding-right: 4px;
}

.movie-grid::-webkit-scrollbar {
  width: 6px;
}

.movie-grid::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.movie-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.25s ease;
}

.movie-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.movie-cover {
  width: 100%;
  height: 240px;
  background-size: cover;
  background-position: center;
  background-color: #f5f5f5;
  position: relative;
}

.movie-score {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.65);
  color: #ffd700;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.movie-info {
  padding: 10px 12px;
}

.movie-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #909399;
}

.empty-state,
.empty-history {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #909399;
  min-height: 300px;
}

.empty-history p {
  margin-top: 12px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  margin-top: 12px;
}

/* 电影详情 */
.movie-detail {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-cover {
  width: 200px;
  height: 280px;
  background-size: cover;
  background-position: center;
  background-color: #f5f5f5;
  border-radius: 8px;
  flex-shrink: 0;
}

.detail-info {
  flex: 1;
  overflow-y: auto;
  max-height: 300px;
}

.detail-row {
  display: flex;
  margin-bottom: 10px;
}

.detail-row.full { flex-direction: column; }

.label {
  width: 80px;
  font-weight: 500;
  color: #606266;
  flex-shrink: 0;
}

.value { color: #303133; flex: 1; }
.value.score { color: #e6a23c; font-size: 20px; font-weight: 700; }

.review-form { border-top: 1px solid #ebeef5; padding-top: 20px; }
.review-form h3 { font-size: 16px; font-weight: 600; margin-bottom: 16px; }

.star-rating { display: flex; align-items: center; gap: 8px; }
.star-icon { cursor: pointer; transition: transform 0.2s; }
.star-icon:hover { transform: scale(1.2); }
.rating-text { font-size: 16px; font-weight: 600; color: #e6a23c; margin-left: 10px; }

.no-cover {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
}

.no-cover-small {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
}

/* 历史记录列表 */
.history-list { background: #fff; border-radius: 12px; overflow: hidden; }
.history-item {
  display: flex; align-items: center; padding: 16px;
  border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.2s;
}
.history-item:last-child { border-bottom: none; }
.history-item:hover { background: #f5f7fa; }
.history-cover { width: 60px; height: 80px; background-size: cover; background-position: center; background-color: #f5f5f5; border-radius: 6px; flex-shrink: 0; }
.history-info { flex: 1; margin-left: 16px; overflow: hidden; }
.history-info h4 { font-size: 15px; font-weight: 600; margin: 0 0 8px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-time { font-size: 12px; color: #909399; margin: 0; }
.review-stars { display: flex; align-items: center; margin-bottom: 4px; }
.rating-value { margin-left: 8px; font-size: 14px; font-weight: 600; color: #e6a23c; }
.review-content { font-size: 13px; color: #606266; margin: 4px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-action { margin-left: 16px; }

/* 评论 */
.comments-section { border-top: 1px solid #ebeef5; padding-top: 20px; margin-top: 10px; }
.comments-section h3 { font-size: 16px; font-weight: 600; margin-bottom: 16px; }
.comments-loading, .comments-empty { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 30px 0; color: #909399; flex-direction: column; }
.comment-list { max-height: 500px; overflow-y: auto; }
.comment-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.comment-item:last-child { border-bottom: none; }
.comment-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.comment-user { font-size: 13px; font-weight: 600; color: #409eff; }
.comment-stars { display: flex; align-items: center; gap: 2px; }
.comment-rating-text { font-size: 12px; color: #e6a23c; font-weight: 600; margin-left: 4px; }
.comment-time { font-size: 12px; color: #909399; margin-left: auto; }
.comment-content { font-size: 14px; color: #303133; line-height: 1.6; margin: 0 0 8px 0; word-break: break-word; }
.comment-actions { display: flex; gap: 8px; }
.comment-pagination { display: flex; justify-content: center; padding: 12px 0; }

.skeleton-card { pointer-events: none; }
.skeleton-cover { background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: sk-loading 1.5s infinite; }
.skeleton-title { height: 14px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: sk-loading 1.5s infinite; border-radius: 4px; }
.skeleton-score { display: inline-block; width: 50px; height: 12px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: sk-loading 1.5s infinite; border-radius: 4px; }
.skeleton-year { display: inline-block; width: 30px; height: 12px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: sk-loading 1.5s infinite; border-radius: 4px; }

.loading-icon { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }

:deep(.el-input-group__append .el-button) { transition: all 0.3s; }

/* 响应式 */
@media (max-width: 1200px) {
  .home-layout { grid-template-columns: 1fr; }
  .movie-grid { max-height: none; }
  .movie-cover { height: 220px; }
}

@media (max-width: 768px) {
  .nav-menu { gap: 0; }
  .nav-item { padding: 6px 10px; font-size: 13px; }
  .logo { display: none; }
  .movie-grid { grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); }
  .movie-cover { height: 160px; }
}
</style>
