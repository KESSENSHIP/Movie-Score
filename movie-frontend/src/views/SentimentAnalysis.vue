<template>
  <div class="sentiment-analysis">
    <div class="page-header">
      <h2>评论情感分析</h2>
      <p class="page-desc">基于 DeepSeek 大模型对电影评论进行智能情感分析</p>
    </div>

    <!-- 搜索电影 -->
    <div class="search-section">
      <el-input
        v-model="searchKeyword"
        placeholder="输入电影名称进行搜索"
        style="max-width: 400px;"
        @keyup.enter="searchMovies"
        clearable
      >
        <template #append>
          <el-button type="primary" @click="searchMovies">搜索</el-button>
        </template>
      </el-input>
    </div>

    <!-- 电影选择结果 -->
    <div v-if="searchResults.length > 0 && !analyzing && !selectedMovie" class="movie-select">
      <h3>选择要分析的电影</h3>
      <div class="movie-list">
        <div
          v-for="movie in searchResults"
          :key="movie.movieId"
          class="movie-item"
          @click="selectMovie(movie)"
        >
          <div class="movie-poster" :style="{ backgroundImage: movie.cover ? `url(${movie.cover})` : 'none' }">
            <el-icon v-if="!movie.cover" :size="40" color="#ccc"><Film /></el-icon>
          </div>
          <div class="movie-info">
            <h4>{{ movie.name }}</h4>
            <div class="movie-meta">
              <el-tag v-if="movie.year" size="small" type="warning">{{ movie.year }}</el-tag>
              <el-tag v-if="movie.genres" size="small" type="info">{{ movie.genres.split('|')[0] }}</el-tag>
              <span v-if="movie.doubanScore" class="score">评分: {{ movie.doubanScore }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分析配置 -->
    <div v-if="selectedMovie && !analyzing" class="analyze-config">
      <div class="selected-movie">
        <div class="poster" :style="{ backgroundImage: selectedMovie.cover ? `url(${selectedMovie.cover})` : 'none' }">
          <el-icon v-if="!selectedMovie.cover" :size="48" color="#ccc"><Film /></el-icon>
        </div>
        <div class="info">
          <h3>{{ selectedMovie.name }}</h3>
          <div class="meta">
            <el-tag v-if="selectedMovie.year" type="warning">{{ selectedMovie.year }}</el-tag>
            <el-tag v-if="selectedMovie.genres" type="info">{{ selectedMovie.genres }}</el-tag>
            <el-tag v-if="selectedMovie.language" type="success">{{ selectedMovie.language }}</el-tag>
          </div>
          <div class="comment-info">
            <el-icon><ChatDotRound /></el-icon>
            <span>评论数: {{ totalComments }}</span>
          </div>
        </div>
      </div>

      <div class="config-panel">
        <h4>分析配置</h4>
        <el-form :model="config" label-width="100px">
          <el-form-item label="采样数量">
            <el-slider
              v-model="config.sampleSize"
              :min="5"
              :max="50"
              :step="5"
              show-input
              style="max-width: 400px;"
            />
            <div class="config-hint">选择要分析的评论数量</div>
          </el-form-item>
          <el-form-item label="分析模式">
            <el-radio-group v-model="config.mode">
              <el-radio value="auto">智能采样</el-radio>
              <el-radio value="random">随机采样</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="重新分析">
            <el-switch v-model="config.forceReanalyze" />
            <span class="config-hint" style="margin-left: 10px;">开启后将覆盖已有的分析结果</span>
          </el-form-item>
        </el-form>

        <!-- 成本预估 -->
        <div class="cost-estimate">
          <h4>成本预估</h4>
          <div class="cost-info">
            <div class="cost-item">
              <span class="cost-label">预计 API 调用次数</span>
              <span class="cost-value">{{ estimatedBatches }} 次</span>
            </div>
            <div class="cost-item">
              <span class="cost-label">批量节省</span>
              <span class="cost-value cost-saving">节省 {{ Math.max(0, config.sampleSize - estimatedBatches) }} 次调用</span>
            </div>
            <div class="cost-item">
              <span class="cost-label">预估成本</span>
              <span class="cost-value">约 ¥{{ estimatedCost.toFixed(3) }}</span>
            </div>
          </div>
          <div class="cost-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>已分析过的评论会自动从缓存读取，不消耗 API 成本</span>
          </div>
        </div>

        <div class="action-buttons">
          <el-button type="primary" size="large" :loading="analyzing" @click="startAnalysis">
            <el-icon><MagicStick /></el-icon>
            开始情感分析
          </el-button>
          <el-button size="large" @click="resetSelection">重新选择</el-button>
        </div>
      </div>
    </div>

    <!-- 分析进行中 -->
    <div v-if="analyzing" class="analyzing-state">
      <div class="spinner">
        <el-icon :size="64" color="#409eff" class="rotating"><Loading /></el-icon>
      </div>
      <h3>正在分析中...</h3>
      <p>正在调用 AI 模型分析评论情感，请耐心等待</p>
      <p class="progress">{{ analyzedCount }} / {{ config.sampleSize }}</p>
    </div>

    <!-- 分析结果 -->
    <div v-if="analysisResult && !analyzing" class="analysis-result">
      <div class="result-header">
        <h3>{{ selectedMovie.name }} - 情感分析结果</h3>
        <div class="result-summary">
          <el-tag type="success" effect="dark" size="large">积极 {{ distribution?.counts?.positive || 0 }}</el-tag>
          <el-tag type="info" effect="dark" size="large">中立 {{ distribution?.counts?.neutral || 0 }}</el-tag>
          <el-tag type="danger" effect="dark" size="large">消极 {{ distribution?.counts?.negative || 0 }}</el-tag>
        </div>
      </div>

      <div class="result-stats">
        <div class="stat-card">
          <div class="stat-value">{{ analysisResult.analyzedCount || 0 }}</div>
          <div class="stat-label">本次分析</div>
        </div>
        <div class="stat-card stat-positive">
          <div class="stat-value">{{ distribution?.percentages?.positive?.toFixed(1) || '0.0' }}%</div>
          <div class="stat-label">好评率</div>
        </div>
        <div class="stat-card stat-negative">
          <div class="stat-value">{{ distribution?.percentages?.negative?.toFixed(1) || '0.0' }}%</div>
          <div class="stat-label">差评率</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ (analysisResult.totalAnalyzed || 0) }}</div>
          <div class="stat-label">累计分析</div>
        </div>
      </div>

      <!-- 饼图 -->
      <div class="chart-section">
        <h4>情感分布</h4>
        <div ref="pieChartRef" class="pie-chart"></div>
      </div>

      <!-- 评论列表 -->
      <div class="analyses-list">
        <h4>分析详情</h4>
        <el-table :data="analysisResult.analyses || []" stripe style="width: 100%" max-height="400">
          <el-table-column label="评论内容" min-width="250">
            <template #default="scope">
              <span class="comment-content">{{ scope.row.content }}</span>
            </template>
          </el-table-column>
          <el-table-column label="情感" width="100" align="center">
            <template #default="scope">
              <el-tag :type="sentimentTagType(scope.row.sentiment)" effect="dark" size="small">
                {{ sentimentLabel(scope.row.sentiment) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="置信度" width="100" align="center">
            <template #default="scope">
              <span>{{ (scope.row.confidence * 100).toFixed(0) }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="80" align="center">
            <template #default="scope">
              <span v-if="scope.row.rating">{{ scope.row.rating }}★</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="分析时间" width="160">
            <template #default="scope">
              <span class="time">{{ scope.row.analyzedAt }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, nextTick, onBeforeUnmount } from 'vue'
import axios from '../utils/axios'
import { Film, ChatDotRound, MagicStick, Loading, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

export default {
  name: 'SentimentAnalysis',
  components: { Film, ChatDotRound, MagicStick, Loading, InfoFilled },
  setup() {
    const searchKeyword = ref('')
    const searchResults = ref([])
    const selectedMovie = ref(null)
    const totalComments = ref(0)
    const analyzing = ref(false)
    const analyzedCount = ref(0)
    const analysisResult = ref(null)
    const distribution = ref(null)
    const pieChartRef = ref(null)

    // 每批处理的评论数量（与后端 BATCH_SIZE 一致）
    const BATCH_SIZE = 5
    // deepseek-chat 约 ¥2/百万tokens，单次批量约消耗 2000 tokens
    const COST_PER_BATCH = 0.004

    const config = reactive({
      sampleSize: 20,
      mode: 'auto',
      forceReanalyze: false
    })

    // 计算预计的批次数
    const estimatedBatches = computed(() => {
      return Math.ceil(config.sampleSize / BATCH_SIZE)
    })

    // 计算预估成本
    const estimatedCost = computed(() => {
      return estimatedBatches.value * COST_PER_BATCH
    })

    let pieChartInstance = null

    async function searchMovies() {
      if (!searchKeyword.value.trim()) return
      try {
        const response = await axios.get('/movies/search', {
          params: {
            keyword: searchKeyword.value,
            pageSize: 10,
            pageNum: 1,
            sortBy: 'score',
            sortOrder: 'desc'
          }
        })
        // PageResult returns directly: { data: [...], totalCount, ... }
        const pageData = response.data
        if (pageData && pageData.data && pageData.data.length > 0) {
          searchResults.value = pageData.data
          ElMessage.success(`找到 ${pageData.totalCount} 部电影`)
        } else {
          searchResults.value = []
          ElMessage.warning('没有找到匹配的电影')
        }
      } catch (error) {
        console.error('搜索电影失败:', error)
        ElMessage.error('搜索电影失败: ' + (error.message || '未知错误'))
      }
    }

    async function selectMovie(movie) {
      selectedMovie.value = movie
      searchResults.value = []
      try {
        const response = await axios.get(`/comments/movie/${movie.movieId}`, {
          params: { pageNum: 1, pageSize: 1 }
        })
        // PageResult returns directly
        const pageData = response.data
        totalComments.value = pageData?.totalCount || 0
      } catch (error) {
        console.error('获取评论数量失败:', error)
        totalComments.value = 0
      }
    }

    function resetSelection() {
      selectedMovie.value = null
      analysisResult.value = null
      distribution.value = null
      analyzedCount.value = 0
    }

    async function startAnalysis() {
      if (!selectedMovie.value) return
      analyzing.value = true
      analyzedCount.value = 0

      try {
        const response = await axios.post(
          `/sentiment/analyze/${selectedMovie.value.movieId}`,
          null,
          {
            params: {
              sampleSize: config.sampleSize,
              forceReanalyze: config.forceReanalyze
            }
          }
        )

        if (response.data.code === '200') {
          const result = response.data.data
          analysisResult.value = result
          distribution.value = result.distribution
          analyzedCount.value = result.analyzedCount || 0

          await nextTick()
          setTimeout(() => renderPieChart(), 100)
        }
      } catch (error) {
        console.error('情感分析失败:', error)
        ElMessage.error('分析失败: ' + (error.response?.data?.message || error.message))
      } finally {
        analyzing.value = false
      }
    }

    function renderPieChart() {
      if (!pieChartRef.value || !distribution.value) return

      if (pieChartInstance) {
        pieChartInstance.dispose()
      }
      pieChartInstance = echarts.init(pieChartRef.value)

      const counts = distribution.value.counts || {}
      const data = [
        { name: '积极', value: counts.positive || 0, itemStyle: { color: '#67c23a' } },
        { name: '中立', value: counts.neutral || 0, itemStyle: { color: '#909399' } },
        { name: '消极', value: counts.negative || 0, itemStyle: { color: '#f56c6c' } }
      ]

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          bottom: 10,
          left: 'center'
        },
        series: [{
          name: '情感分布',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}\n{c} ({d}%)'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 16,
              fontWeight: 'bold'
            }
          },
          data: data
        }]
      }

      pieChartInstance.setOption(option)
      pieChartInstance.resize()
    }

    function sentimentTagType(sentiment) {
      switch (sentiment) {
        case 'positive': return 'success'
        case 'negative': return 'danger'
        default: return 'info'
      }
    }

    function sentimentLabel(sentiment) {
      switch (sentiment) {
        case 'positive': return '积极'
        case 'negative': return '消极'
        default: return '中立'
      }
    }

    onBeforeUnmount(() => {
      if (pieChartInstance) {
        pieChartInstance.dispose()
        pieChartInstance = null
      }
    })

    return {
      searchKeyword,
      searchResults,
      selectedMovie,
      totalComments,
      analyzing,
      analyzedCount,
      analysisResult,
      distribution,
      config,
      pieChartRef,
      estimatedBatches,
      estimatedCost,
      searchMovies,
      selectMovie,
      resetSelection,
      startAnalysis,
      sentimentTagType,
      sentimentLabel
    }
  }
}
</script>

<style scoped>
.sentiment-analysis {
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: #303133;
}

.page-desc {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.search-section {
  margin-bottom: 24px;
}

.movie-select h3,
.analyze-config h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.movie-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.movie-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #fff;
}

.movie-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.movie-poster {
  width: 60px;
  height: 80px;
  border-radius: 6px;
  background: #f5f5f5 center/cover no-repeat;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.movie-info {
  flex: 1;
  min-width: 0;
}

.movie-info h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.movie-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}

.score {
  color: #e6a23c;
  font-weight: 600;
}

.analyze-config {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.selected-movie {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.selected-movie .poster {
  width: 80px;
  height: 110px;
  border-radius: 8px;
  background: #f5f5f5 center/cover no-repeat;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.selected-movie .info {
  flex: 1;
}

.selected-movie h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
}

.selected-movie .meta {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.comment-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
  font-size: 14px;
}

.config-panel h4 {
  margin: 0 0 16px 0;
}

.config-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.cost-estimate {
  margin-top: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  border-radius: 10px;
  border: 1px solid #bae7ff;
}

.cost-estimate h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #1890ff;
}

.cost-info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.cost-item {
  display: flex;
  flex-direction: column;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
}

.cost-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.cost-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.cost-saving {
  color: #67c23a;
}

.cost-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
  padding: 8px 12px;
  border-radius: 6px;
}

.cost-tip .el-icon {
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.analyzing-state {
  text-align: center;
  padding: 60px 0;
}

.spinner {
  margin-bottom: 20px;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.analyzing-state h3 {
  margin: 0 0 10px 0;
}

.analyzing-state p {
  color: #909399;
  margin: 0 0 8px 0;
}

.progress {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

.result-header {
  margin-bottom: 24px;
}

.result-header h3 {
  margin: 0 0 12px 0;
  font-size: 20px;
}

.result-summary {
  display: flex;
  gap: 12px;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-positive .stat-value {
  color: #67c23a;
}

.stat-negative .stat-value {
  color: #f56c6c;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.chart-section {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chart-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.pie-chart {
  height: 350px;
}

.analyses-list {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.analyses-list h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.comment-content {
  color: #303133;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.time {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 768px) {
  .result-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .cost-info {
    grid-template-columns: 1fr;
  }
}
</style>
