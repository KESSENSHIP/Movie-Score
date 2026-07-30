<template>
    <div class="user-prediction">
        <!-- 搜索栏 -->
        <div class="search-bar">
            <el-input
                v-model="searchKeyword"
                placeholder="搜索电影名称"
                style="width: 300px;"
                clearable
                @clear="handleSearch"
                @keyup.enter="handleSearch"
            >
                <template #append>
                    <el-button type="primary" @click="handleSearch">搜索</el-button>
                </template>
            </el-input>
            <el-select v-model="filterGenre" placeholder="类型" clearable class="filter-item" @change="handleFilterChange">
                <el-option label="全部类型" value="" />
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
                <el-option label="战争" value="战争" />
                <el-option label="奇幻" value="奇幻" />
                <el-option label="冒险" value="冒险" />
            </el-select>
            <el-select v-model="filterYear" placeholder="年份" clearable filterable class="filter-item" @change="handleFilterChange">
                <el-option label="全部年份" value="" />
                <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
            </el-select>
            <el-select v-model="filterRegion" placeholder="地区" clearable filterable class="filter-item" @change="handleFilterChange">
                <el-option label="全部地区" value="" />
                <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
            </el-select>
            <div class="rating-range">
                <el-input-number v-model="filterRatingMin" :min="0" :max="10" :step="0.5" :precision="1" placeholder="最低评分" controls-position="right" class="rating-input" @change="handleFilterChange" />
                <span class="range-separator">—</span>
                <el-input-number v-model="filterRatingMax" :min="0" :max="10" :step="0.5" :precision="1" placeholder="最高评分" controls-position="right" class="rating-input" @change="handleFilterChange" />
            </div>
            <el-button v-if="hasActiveFilter" @click="clearFilters">清除筛选</el-button>
        </div>

        <!-- 统计信息 -->
        <div class="stats-bar">
            <span>共 <strong>{{ totalCount }}</strong> 部电影获得预测评分</span>
            <span class="stats-divider">|</span>
            <span>当前展示电影平均评分: <strong class="score-high">{{ avgScore }}</strong></span>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="loading-container">
            <el-icon :size="32" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
            <p>加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="loadError" class="empty-state">
            <el-icon :size="64" color="#ccc"><Warning /></el-icon>
            <p>加载失败，请重试</p>
            <el-button type="primary" @click="loadPredictions">重试</el-button>
        </div>

        <!-- 电影表格 -->
        <div v-else class="prediction-table-wrapper">
            <el-table :data="predictions" style="width: 100%" height="550" empty-text="暂无数据" stripe>
                <el-table-column type="index" label="序号" width="70" align="center" />
                <el-table-column prop="movieId" label="电影ID" width="90" align="center" />
                <el-table-column prop="name" label="电影名称" min-width="240" show-overflow-tooltip>
                    <template #default="scope">
                        <el-popover
                            placement="right"
                            :width="380"
                            trigger="hover"
                            :show-after="500"
                            :hide-after="100"
                            @show="fetchMovieDetail(scope.row.movieId)"
                        >
                            <template #reference>
                                <span class="movie-name-link">{{ scope.row.name }}</span>
                            </template>
                            <div class="movie-detail-card">
                                <div class="detail-title">{{ scope.row.name }}</div>
                                <div class="detail-divider"></div>
                                <template v-if="loadingMovieId === scope.row.movieId">
                                    <div class="detail-loading">
                                        <el-icon :size="20" color="#66b1ff" class="loading-icon"><Loading /></el-icon>
                                        <span>加载中...</span>
                                    </div>
                                </template>
                                <template v-else-if="movieCache[scope.row.movieId]">
                                    <div class="detail-row">
                                        <span class="detail-label">导演</span>
                                        <span class="detail-value">{{ movieCache[scope.row.movieId].directors || '——' }}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-label">主演</span>
                                        <span class="detail-value text-ellipsis-2">{{ movieCache[scope.row.movieId].actors || '——' }}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-label">类型</span>
                                        <span class="detail-value">{{ scope.row.genres || '——' }}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-label">地区</span>
                                        <span class="detail-value">{{ scope.row.region || '——' }}</span>
                                    </div>
                                    <div class="detail-row">
                                        <span class="detail-label">年份</span>
                                        <span class="detail-value">{{ scope.row.year || '——' }}</span>
                                    </div>
                                    <div class="detail-row" v-if="movieCache[scope.row.movieId].storyline">
                                        <span class="detail-label">简介</span>
                                        <span class="detail-value text-ellipsis-4">{{ movieCache[scope.row.movieId].storyline }}</span>
                                    </div>
                                    <div class="detail-divider"></div>
                                    <div class="detail-row">
                                        <span class="detail-label">预测评分</span>
                                        <span class="detail-value" :class="scoreClass(scope.row.predictedScore)">
                                            <el-rate
                                                :model-value="parseFloat(scope.row.predictedScore) || 0"
                                                :max="10"
                                                disabled
                                                show-score
                                                score-template="{value} 分"
                                                size="small"
                                                style="display: inline-flex;"
                                            />
                                        </span>
                                    </div>
                                </template>
                                <template v-else>
                                    <div class="detail-loading">暂无详细信息</div>
                                </template>
                            </div>
                        </el-popover>
                    </template>
                </el-table-column>
                <el-table-column prop="year" label="年份" width="80" align="center" />
                <el-table-column prop="genres" label="类型" width="180" show-overflow-tooltip />
                <el-table-column prop="region" label="地区" width="160" show-overflow-tooltip />
                <el-table-column label="预测评分" width="130" align="center">
                    <template #default="scope">
                        <span :class="scoreClass(scope.row.predictedScore)">
                            {{ scope.row.predictedScore || '--' }}
                        </span>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 分页 -->
        <div class="pagination-container">
            <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="totalCount"
                layout="prev, pager, next, jumper"
                @current-change="handlePageChange"
            />
        </div>
    </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import axios from '../utils/axios'
import { Loading, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export default {
    name: 'UserPrediction',
    components: { Loading, Warning },
    setup() {
        const predictions = ref([])
        const currentPage = ref(1)
        const pageSize = ref(15)
        const totalCount = ref(0)
        const loading = ref(false)
        const loadError = ref(false)
        const searchKeyword = ref('')
        const filterGenre = ref('')
        const filterYear = ref('')
        const filterRegion = ref('')
        const filterRatingMin = ref(null)
        const filterRatingMax = ref(null)
        const movieCache = ref({})
        const loadingMovieId = ref(null)
        const regionOptions = ref([
            '美国', '中国', '日本', '英国', '法国', '韩国',
            '香港', '台湾', '德国', '印度', '意大利', '西班牙',
            '加拿大', '澳大利亚', '俄罗斯', '泰国', '荷兰',
            '瑞典', '比利时', '巴西', '墨西哥', '新西兰',
            '爱尔兰', '丹麦', '挪威', '芬兰', '阿根廷',
            '新加坡', '马来西亚', '越南', '菲律宾', '土耳其',
            '以色列', '伊朗', '南非'
        ])
        const currentYear = new Date().getFullYear()
        const yearOptions = computed(() => {
            const years = []
            for (let y = currentYear; y >= 1900; y--) {
                years.push(y)
            }
            return years
        })

        const hasActiveFilter = computed(() =>
            searchKeyword.value || filterGenre.value || filterYear.value || filterRegion.value
            || filterRatingMin.value != null || filterRatingMax.value != null
        )

        const avgScore = computed(() => {
            if (predictions.value.length === 0) return '--'
            const sum = predictions.value.reduce((s, p) => s + parseFloat(p.predictedScore || 0), 0)
            return (sum / predictions.value.length).toFixed(1)
        })

        function scoreClass(score) {
            const s = parseFloat(score)
            if (s >= 8) return 'score-high'
            if (s >= 6) return 'score-mid'
            if (s >= 4) return 'score-low'
            return 'score-very-low'
        }

        async function loadPredictions() {
            loading.value = true
            loadError.value = false
            try {
                let url
                if (hasActiveFilter.value) {
                    const params = new URLSearchParams()
                    params.append('pageNum', currentPage.value)
                    params.append('pageSize', pageSize.value)
                    if (searchKeyword.value) params.append('keyword', searchKeyword.value)
                    if (filterGenre.value) params.append('genre', filterGenre.value)
                    if (filterYear.value) params.append('year', String(filterYear.value))
                    if (filterRegion.value) params.append('region', filterRegion.value)
                    if (filterRatingMin.value != null) params.append('minScore', String(filterRatingMin.value))
                    if (filterRatingMax.value != null) params.append('maxScore', String(filterRatingMax.value))
                    url = `/predictions/filter?${params.toString()}`
                } else {
                    url = `/predictions?pageNum=${currentPage.value}&pageSize=${pageSize.value}`
                }
                const response = await axios.get(url)
                if (response.data) {
                    predictions.value = response.data.data || []
                    totalCount.value = response.data.totalCount || 0
                } else {
                    predictions.value = []
                    totalCount.value = 0
                }
            } catch (error) {
                console.error('加载预测评分失败:', error)
                loadError.value = true
                ElMessage.error('加载预测评分失败')
            } finally {
                loading.value = false
            }
        }

        function handleSearch() {
            currentPage.value = 1
            loadPredictions()
        }

        function handleFilterChange() {
            currentPage.value = 1
            loadPredictions()
        }

        function clearFilters() {
            searchKeyword.value = ''
            filterGenre.value = ''
            filterYear.value = ''
            filterRegion.value = ''
            filterRatingMin.value = null
            filterRatingMax.value = null
            currentPage.value = 1
            loadPredictions()
        }

        async function fetchMovieDetail(movieId) {
            if (movieCache.value[movieId] || loadingMovieId.value === movieId) return
            loadingMovieId.value = movieId
            try {
                const response = await axios.get(`/movies/${movieId}`)
                if (response.data) {
                    movieCache.value[movieId] = response.data
                }
            } catch (error) {
                console.error('加载电影详情失败:', movieId, error)
                movieCache.value[movieId] = null
            } finally {
                loadingMovieId.value = null
            }
        }

        function handlePageChange(page) {
            currentPage.value = page
            loadPredictions()
        }

        onMounted(() => {
            loadPredictions()
        })

        return {
            predictions,
            currentPage,
            pageSize,
            totalCount,
            loading,
            loadError,
            searchKeyword,
            filterGenre,
            filterYear,
            filterRegion,
            filterRatingMin,
            filterRatingMax,
            yearOptions,
            regionOptions,
            hasActiveFilter,
            avgScore,
            movieCache,
            loadingMovieId,
            scoreClass,
            loadPredictions,
            handleSearch,
            handleFilterChange,
            clearFilters,
            fetchMovieDetail,
            handlePageChange
        }
    }
}
</script>

<style scoped>
.user-prediction {
    padding: 20px 0;
    padding-bottom: 80px;
}

.search-bar {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 12px;
}

.filter-item {
    width: 140px;
}

.rating-range {
    display: flex;
    align-items: center;
    gap: 6px;
}

.rating-input {
    width: 140px;
}

.range-separator {
    color: #909399;
    font-size: 14px;
}

.stats-bar {
    margin-bottom: 16px;
    font-size: 14px;
    color: #606266;
}

.stats-divider {
    margin: 0 12px;
    color: #dcdfe6;
}

.score-high {
    color: #f56c6c;
    font-weight: 700;
    font-size: 15px;
}

.score-mid {
    color: #e6a23c;
    font-weight: 600;
    font-size: 15px;
}

.score-low {
    color: #409eff;
    font-weight: 600;
    font-size: 15px;
}

.score-very-low {
    color: #909399;
    font-weight: 500;
    font-size: 15px;
}

.prediction-table-wrapper {
    border-radius: 16px;
    overflow: hidden;
    border: 1px solid #ebeef5;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.loading-container {
    text-align: center;
    padding: 80px 0;
    color: #909399;
}

.empty-state {
    text-align: center;
    padding: 80px 0;
    color: #909399;
}

.empty-state p {
    margin: 16px 0;
}

.loading-icon {
    animation: rotating 1.5s linear infinite;
}

@keyframes rotating {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.pagination-container {
    display: flex;
    justify-content: center;
    padding: 16px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    position: fixed;
    bottom: 20px;
    left: 20px;
    right: 20px;
    z-index: 10;
}

.movie-name-link {
    cursor: pointer;
    color: #409eff;
    transition: color 0.2s;
}

.movie-name-link:hover {
    color: #66b1ff;
    text-decoration: underline;
}

.movie-detail-card {
    padding: 4px 0;
}

.detail-title {
    font-size: 16px;
    font-weight: 700;
    color: #303133;
    margin-bottom: 8px;
    line-height: 1.4;
}

.detail-divider {
    height: 1px;
    background: #ebeef5;
    margin-bottom: 10px;
}

.detail-row {
    display: flex;
    align-items: center;
    padding: 4px 0;
    font-size: 13px;
}

.detail-label {
    width: 70px;
    flex-shrink: 0;
    color: #909399;
}

.detail-value {
    flex: 1;
    color: #606266;
    word-break: break-all;
}

.detail-value .el-rate {
    vertical-align: middle;
}

:deep(.movie-detail-card .el-rate__text) {
    font-size: 13px;
    color: #f56c6c;
}

.detail-loading {
    text-align: center;
    padding: 30px 0;
    color: #909399;
    font-size: 13px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.text-ellipsis-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
}

.text-ellipsis-4 {
    display: -webkit-box;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.5;
}
</style>
