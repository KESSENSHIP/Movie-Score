<template>
    <div class="stats-analysis">
        <el-tabs v-model="activeStatTab" @tab-change="handleStatTabChange">
            <!-- Tab 1: 地区电影发行量Top50 -->
            <el-tab-pane label=" 地区电影发行量Top50" name="region_top50">
                <div class="stats-header">
                    <div>
                        <h2>各国家/地区电影发行量 Top50</h2>
                        <p class="stats-desc">统计各国家/地区的电影发行数量，分析电影产业的地域分布情况</p>
                    </div>
                </div>
                
                <div v-if="loading" class="stats-loading">
                    <el-icon :size="32" class="loading-icon"><Loading /></el-icon>
                    <p>加载数据中...</p>
                </div>
                
                <div v-else-if="statsData.length > 0" class="stats-content">
                    <div class="stats-unified-card">
                        <div class="card-tab-bar">
                            <div
                                :class="['card-tab', { active: regionViewMode === 'table' }]"
                                @click="regionViewMode = 'table'"
                            >
                                <el-icon><List /></el-icon>
                                列表视图
                            </div>
                            <div
                                :class="['card-tab', { active: regionViewMode === 'chart' }]"
                                @click="regionViewMode = 'chart'"
                            >
                                <el-icon><DataAnalysis /></el-icon>
                                柱状图视图
                            </div>
                        </div>

                        <div v-if="regionViewMode === 'table'" class="card-body">
                            <el-table 
                                :data="pagedData" 
                                stripe 
                                style="width: 100%"
                                :header-cell-style="{ background: '#e8f0fe', color: '#1a1a2e', fontWeight: '700', fontSize: '15px' }"
                            >
                                <el-table-column type="index" label="排名" width="80" align="center">
                                    <template #default="scope">
                                        <span :class="'rank-badge rank-' + (scope.$index + 1 > 3 ? 'normal' : scope.$index + 1)">
                                            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
                                        </span>
                                    </template>
                                </el-table-column>
                                <el-table-column prop="statKey" label="国家/地区" min-width="150">
                                    <template #default="scope">
                                        <span class="region-name">{{ scope.row.statKey }}</span>
                                    </template>
                                </el-table-column>
                                <el-table-column prop="statCount" label="电影数量" width="150" align="center">
                                    <template #default="scope">
                                        <span class="count-value">{{ scope.row.statCount }}</span>
                                    </template>
                                </el-table-column>
                                <el-table-column label="占比" width="260" align="center">
                                    <template #default="scope">
                                        <div class="percentage-bar-wrapper">
                                            <el-progress 
                                                class="percentage-bar"
                                                :percentage="calculatePercentage(scope.row.statCount)" 
                                                :color="getBarColor(scope.$index + (currentPage - 1) * pageSize)"
                                                :stroke-width="16"
                                                :text-inside="false"
                                            />
                                            <span class="percentage-text">
                                                {{ calculatePercentage(scope.row.statCount) }}%
                                            </span>
                                        </div>
                                    </template>
                                </el-table-column>
                            </el-table>
                            <div class="pagination-container">
                                <el-pagination
                                    v-model:current-page="currentPage"
                                    :page-size="pageSize"
                                    :total="statsData.length"
                                    layout="prev, pager, next, jumper"
                                    @current-change="() => {}"
                                />
                            </div>
                        </div>

                        <div v-else class="card-body">
                            <div class="chart-wrapper" ref="chartRef"></div>
                        </div>
                    </div>
                </div>
                
                <div v-else class="stats-empty">
                    <el-icon :size="64" color="#ccc"><DataAnalysis /></el-icon>
                    <p>暂无统计数据</p>
                    <p class="empty-hint">请先运行数据分析脚本生成数据</p>
                    <el-button type="primary" @click="loadStats">刷新数据</el-button>
                </div>
            </el-tab-pane>

            <!-- Tab 2: 年度地区对比 -->
            <el-tab-pane label="年度地区发行量对比" name="year_region_comparison">
                <div class="stats-header">
                    <div class="header-row">
                        <div>
                            <h2>Top10 地区年度电影发行量对比</h2>
                            <p class="stats-desc">按年份统计各地区电影发行量，分析电影产业的时间演变趋势</p>
                        </div>
                        <el-button
                            v-if="yearRegionData.length > 0"
                            type="primary"
                            link
                            @click="showLineChart = !showLineChart"
                            class="chart-toggle-btn"
                        >
                            <el-icon style="margin-right: 4px;"><TrendCharts /></el-icon>
                            {{ showLineChart ? '隐藏折线图' : '显示折线图' }}
                        </el-button>
                    </div>
                </div>

                <div v-if="yearLoading" class="stats-loading">
                    <el-icon :size="32" class="loading-icon"><Loading /></el-icon>
                    <p>加载数据中...</p>
                </div>

                <div v-else-if="yearRegionData.length > 0" class="stats-content">
                    <div class="year-summary-container">
                        <div class="year-summary-card" v-for="(item, index) in yearRegionData" :key="item.statKey" @click="openRegionDialog(item.statKey)" style="cursor:pointer;">
                            <div class="card-rank">{{ index + 1 }}</div>
                            <div class="card-info">
                                <div class="card-region">{{ item.statKey }}</div>
                                <div class="card-count">{{ Number(item.statCount).toLocaleString() }} 部</div>
                            </div>
                        </div>
                    </div>

                    <transition name="fade">
                        <div v-if="showLineChart" class="chart-container">
                            <div class="chart-header">
                                <h3>Top10 地区年度电影发行量折线图</h3>
                                <span class="chart-subtitle">点击图例可切换显示/隐藏对应地区</span>
                            </div>
                            <div ref="lineChartRef" class="chart-wrapper"></div>
                        </div>
                    </transition>
                </div>

                <div v-else class="stats-empty">
                    <el-icon :size="64" color="#ccc"><TrendCharts /></el-icon>
                    <p>暂无统计数据</p>
                    <p class="empty-hint">请先运行年度对比分析脚本生成数据</p>
                    <el-button type="primary" @click="loadYearRegionData">刷新数据</el-button>
                </div>
            </el-tab-pane>

            <!-- Tab 3: 词云分析 -->
            <el-tab-pane label="词云分析" name="wordcloud">
                <div class="stats-header">
                    <h2>电影类型与标签词云</h2>
                    <p class="stats-desc">统计电影类型和标签的出现频率，生成词云图展示最受欢迎的类型和标签</p>
                </div>

                <div v-if="wordcloudLoading" class="stats-loading">
                    <el-icon :size="32" class="loading-icon"><Loading /></el-icon>
                    <p>加载数据中...</p>
                </div>

                <div v-else class="wordcloud-content">
                    <div class="wordcloud-sub-tabs">
                        <el-radio-group v-model="wordcloudTab" size="large">
                            <el-radio-button value="genre" label="类型词云" />
                            <el-radio-button value="tag" label="标签词云" />
                        </el-radio-group>
                    </div>

                    <div class="wordcloud-image-wrapper">
                        <img
                            v-if="wordcloudTab === 'genre'"
                            :src="genreWordcloudSrc"
                            alt="类型词云"
                            class="wordcloud-image"
                            @error="onGenreImageError"
                        />
                        <img
                            v-if="wordcloudTab === 'tag'"
                            :src="tagWordcloudSrc"
                            alt="标签词云"
                            class="wordcloud-image"
                            @error="onTagImageError"
                        />
                        <div v-if="wordcloudImageError" class="wordcloud-error">
                            <el-icon :size="48" color="#ccc"><PictureFilled /></el-icon>
                            <p>词云图片加载失败</p>
                            <el-button type="primary" @click="reloadWordcloudImages">重新加载</el-button>
                        </div>
                    </div>

                    <div class="wordcloud-stats">
                        <div class="wordcloud-stats-card">
                            <div class="stats-title">类型统计</div>
                            <div v-if="genreStats.length > 0" class="stats-list">
                                <div class="stats-row" v-for="(item, index) in genreStats" :key="item.statKey">
                                    <span class="stats-rank">{{ index + 1 }}</span>
                                    <span class="stats-label">{{ item.statKey }}</span>
                                    <span class="stats-count">{{ Number(item.statCount).toLocaleString() }}</span>
                                </div>
                            </div>
                            <div v-else class="stats-empty-small">暂无数据</div>
                        </div>
                        <div class="wordcloud-stats-card">
                            <div class="stats-title">标签统计</div>
                            <div v-if="tagStats.length > 0" class="stats-list">
                                <div class="stats-row" v-for="(item, index) in tagStats" :key="item.statKey">
                                    <span class="stats-rank">{{ index + 1 }}</span>
                                    <span class="stats-label">{{ item.statKey }}</span>
                                    <span class="stats-count">{{ Number(item.statCount).toLocaleString() }}</span>
                                </div>
                            </div>
                            <div v-else class="stats-empty-small">暂无数据</div>
                        </div>
                    </div>
                </div>
            </el-tab-pane>

            <!-- Tab 4: 评分投票Top20 -->
            <el-tab-pane label="评分投票Top20" name="top_rated_votes">
                <div class="stats-header">
                    <h2>评分最高与投票最多电影Top20</h2>
                    <p class="stats-desc">展示最受欢迎和最具争议的作品</p>
                </div>

                <div v-if="topLoading" class="stats-loading">
                    <el-icon :size="32" class="loading-icon"><Loading /></el-icon>
                    <p>加载数据中...</p>
                </div>

                <div v-else class="top-content">
                    <div class="top-sub-tabs">
                        <el-radio-group v-model="topTab" size="large">
                            <el-radio-button value="rated" label="评分Top20" />
                            <el-radio-button value="votes" label="投票Top20" />
                        </el-radio-group>
                    </div>

                    <div v-if="topTab === 'rated' && topRatedData.length > 0" class="top-section">
                        <el-table :data="topRatedData" stripe style="width: 100%" :header-cell-style="{ background: '#e8f0fe', color: '#1a1a2e', fontWeight: '700', fontSize: '15px' }">
                            <el-table-column type="index" label="排名" width="70" align="center">
                                <template #default="scope">
                                    <span :class="'rank-badge rank-' + (scope.$index + 1 > 3 ? 'normal' : scope.$index + 1)">{{ scope.$index + 1 }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="电影名称" min-width="280">
                                <template #default="scope">
                                    <span class="movie-clickable" @click="openMovieDetail(scope.row)">{{ scope.row.statKey }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="豆瓣评分" width="120" align="center">
                                <template #default="scope">
                                    <span class="score-display rated-color">{{ scope.row.statValue }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="投票数" width="140" align="center">
                                <template #default="scope">
                                    <span class="count-value">{{ Number(scope.row.statCount).toLocaleString() }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="100" align="center">
                                <template #default="scope">
                                    <el-button type="primary" link size="small" @click="openMovieComments(scope.row)">查看评论</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                        <div class="top-chart-toggle">
                            <el-button type="primary" link @click="showTopRatedChart = !showTopRatedChart">
                                {{ showTopRatedChart ? '隐藏图表' : '显示图表' }}
                            </el-button>
                        </div>
                        <div v-if="showTopRatedChart" class="top-chart-wrapper">
                            <img :src="topRatedChartSrc" alt="评分Top20图表" class="top-chart-image" @error="topRatedChartError = true" />
                            <div v-if="topRatedChartError" class="chart-error">图表加载失败</div>
                        </div>
                    </div>

                    <div v-if="topTab === 'votes' && topVotesData.length > 0" class="top-section">
                        <el-table :data="topVotesData" stripe style="width: 100%" :header-cell-style="{ background: '#e8f0fe', color: '#1a1a2e', fontWeight: '700', fontSize: '15px' }">
                            <el-table-column type="index" label="排名" width="70" align="center">
                                <template #default="scope">
                                    <span :class="'rank-badge rank-' + (scope.$index + 1 > 3 ? 'normal' : scope.$index + 1)">{{ scope.$index + 1 }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="电影名称" min-width="280">
                                <template #default="scope">
                                    <span class="movie-clickable" @click="openMovieDetail(scope.row)">{{ scope.row.statKey }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="投票数" width="140" align="center">
                                <template #default="scope">
                                    <span class="count-value">{{ Number(scope.row.statCount).toLocaleString() }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="豆瓣评分" width="120" align="center">
                                <template #default="scope">
                                    <span class="score-display">{{ getVoteScore(scope.row) }}</span>
                                </template>
                            </el-table-column>
                            <el-table-column label="操作" width="100" align="center">
                                <template #default="scope">
                                    <el-button type="primary" link size="small" @click="openMovieComments(scope.row)">查看评论</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                        <div class="top-chart-toggle">
                            <el-button type="primary" link @click="showTopVotesChart = !showTopVotesChart">
                                {{ showTopVotesChart ? '隐藏图表' : '显示图表' }}
                            </el-button>
                        </div>
                        <div v-if="showTopVotesChart" class="top-chart-wrapper">
                            <img :src="topVotesChartSrc" alt="投票Top20图表" class="top-chart-image" @error="topVotesChartError = true" />
                            <div v-if="topVotesChartError" class="chart-error">图表加载失败</div>
                        </div>
                    </div>

                    <div v-if="topTab === 'rated' && topRatedData.length === 0 && !topLoading" class="stats-empty">
                        <el-icon :size="64" color="#ccc"><Star /></el-icon>
                        <p>暂无评分Top20数据</p>
                        <el-button type="primary" @click="loadTopRatedVotes">刷新数据</el-button>
                    </div>
                    <div v-if="topTab === 'votes' && topVotesData.length === 0 && !topLoading" class="stats-empty">
                        <el-icon :size="64" color="#ccc"><Star /></el-icon>
                        <p>暂无投票Top20数据</p>
                        <el-button type="primary" @click="loadTopRatedVotes">刷新数据</el-button>
                    </div>
                </div>
            </el-tab-pane>
        </el-tabs>

        <!-- 地区电影列表弹窗 -->
        <el-dialog
            v-model="regionDialogVisible"
            :title="'『' + selectedRegion + '』地区发行的电影'"
            width="80%"
            :top="'5vh'"
            class="region-dialog"
            destroy-on-close
        >
            <div v-if="regionMoviesLoading" class="dialog-loading">
                <el-icon :size="24" class="loading-icon"><Loading /></el-icon>
                <p>加载电影列表...</p>
            </div>
            <div v-else>
                <div class="dialog-hint">共找到 <b>{{ regionMoviesTotal }}</b> 部电影</div>
                <el-table :data="regionMovies" stripe style="width: 100%" height="420" size="small">
                    <el-table-column type="index" label="序号" width="60" align="center" />
                    <el-table-column prop="name" label="电影名称" min-width="200">
                        <template #default="scope">
                            <span class="movie-name-link">{{ scope.row.name }}</span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="year" label="年份" width="80" align="center" />
                    <el-table-column prop="genres" label="类型" min-width="160" show-overflow-tooltip />
                    <el-table-column prop="directors" label="导演" min-width="140" show-overflow-tooltip />
                    <el-table-column prop="doubanScore" label="评分" width="80" align="center">
                        <template #default="scope">
                            <span :class="'score-tag score-' + (scope.row.doubanScore >= 8 ? 'high' : scope.row.doubanScore >= 6 ? 'mid' : 'low')">
                                {{ scope.row.doubanScore ? scope.row.doubanScore.toFixed(1) : '-' }}
                            </span>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="dialog-pagination">
                    <el-pagination
                        v-model:current-page="regionMoviesPage"
                        :page-size="regionMoviesPageSize"
                        :total="regionMoviesTotal"
                        layout="prev, pager, next, total, jumper"
                        background
                        @current-change="loadRegionMovies"
                    />
                </div>
            </div>
        </el-dialog>

        <!-- 电影详情弹窗 -->
        <el-dialog
            v-model="detailDialogVisible"
            title="电影详情"
            width="60%"
            :top="'5vh'"
            class="detail-dialog"
            destroy-on-close
        >
            <div v-if="detailLoading" class="dialog-loading">
                <el-icon :size="24" class="loading-icon"><Loading /></el-icon>
                <p>加载电影详情...</p>
            </div>
            <div v-else-if="detailMovie" class="detail-content">
                <div class="detail-body">
                    <div class="detail-poster">
                        <img v-if="detailMovie.cover && !detailCoverError" :src="detailMovie.cover" alt="海报" class="poster-img" @error="detailCoverError = true" />
                        <div v-else class="poster-placeholder">
                            <el-icon :size="48"><PictureFilled /></el-icon>
                            <span>暂无海报</span>
                        </div>
                    </div>
                    <div class="detail-info">
                        <div class="detail-header">
                            <h2 class="detail-title">{{ detailMovie.name }}</h2>
                            <el-tag v-if="detailMovie.doubanScore && detailMovie.doubanScore > 0" :type="detailMovie.doubanScore >= 8 ? 'danger' : detailMovie.doubanScore >= 6 ? 'warning' : 'info'" size="large" class="detail-score-tag">
                                {{ detailMovie.doubanScore.toFixed(1) }} 分
                            </el-tag>
                        </div>
                        <el-descriptions :column="2" border class="detail-descriptions">
                            <el-descriptions-item label="年份" :span="1">
                                <span class="detail-value">{{ detailMovie.year || '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="类型" :span="1">
                                <span class="detail-value">{{ detailMovie.genres || '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="导演" :span="2">
                                <span class="detail-value">{{ detailMovie.directors || '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="主演" :span="2">
                                <span class="detail-value">{{ detailMovie.actors || '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="地区" :span="1">
                                <span class="detail-value">{{ detailMovie.region || '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="语言" :span="1">
                                <span class="detail-value">{{ detailMovie.language || '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="片长" :span="1">
                                <span class="detail-value">{{ detailMovie.mins ? detailMovie.mins + ' 分钟' : '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="投票数" :span="1">
                                <span class="detail-value">{{ detailMovie.doubanVotes ? Number(detailMovie.doubanVotes).toLocaleString() : '未知' }}</span>
                            </el-descriptions-item>
                            <el-descriptions-item label="剧情简介" :span="2">
                                <span class="detail-storyline">{{ detailMovie.storyline || '暂无简介' }}</span>
                            </el-descriptions-item>
                        </el-descriptions>
                        <div class="detail-actions">
                            <el-button type="primary" @click="openMovieCommentsFromDetail">查看该电影评论</el-button>
                        </div>
                    </div>
                </div>
            </div>
            <div v-else class="dialog-loading">
                <p>未找到电影信息</p>
            </div>
        </el-dialog>

        <!-- 电影评论弹窗 -->
        <el-dialog
            v-model="commentDialogVisible"
            :title="'『' + commentMovieName + '』的评论'"
            width="75%"
            :top="'5vh'"
            class="comment-dialog"
            destroy-on-close
        >
            <div v-if="commentsLoading" class="dialog-loading">
                <el-icon :size="24" class="loading-icon"><Loading /></el-icon>
                <p>加载评论列表...</p>
            </div>
            <div v-else>
                <div class="dialog-hint">共找到 <b>{{ commentsTotal }}</b> 条评论</div>
                <el-table :data="commentsData" stripe style="width: 100%" height="420" size="small">
                    <el-table-column type="index" label="序号" width="60" align="center" />
                    <el-table-column prop="content" label="评论内容" min-width="350" show-overflow-tooltip />
                    <el-table-column prop="rating" label="评分" width="80" align="center">
                        <template #default="scope">
                            <span class="score-tag" :class="'score-' + (scope.row.rating >= 4 ? 'high' : scope.row.rating >= 2 ? 'mid' : 'low')">
                                {{ scope.row.rating || '-' }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="votes" label="点赞数" width="80" align="center" />
                    <el-table-column label="时间" width="120" align="center">
                        <template #default="scope">
                            <span style="color:#868e96;font-size:12px">{{ formatTime(scope.row.commentTime) }}</span>
                        </template>
                    </el-table-column>
                </el-table>
                <div class="dialog-pagination">
                    <el-pagination
                        v-model:current-page="commentsPage"
                        :page-size="commentsPageSize"
                        :total="commentsTotal"
                        layout="prev, pager, next, total, jumper"
                        background
                        @current-change="loadComments"
                    />
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount, watch } from 'vue'
import axios from '../utils/axios'
import { Loading, DataAnalysis, TrendCharts, PictureFilled, Star, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

// ========== 通用状态 ==========
const activeStatTab = ref('region_top50')

// ========== Tab1: 地区电影发行量 ==========
const statsData = ref([])
const loading = ref(false)
const chartRef = ref(null)
let chartInstance = null
let resizeHandler = null
const showChart = ref(true)
const regionViewMode = ref('table')
const currentPage = ref(1)
const pageSize = ref(10)
const totalCount = ref(0)

const pagedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return statsData.value.slice(start, start + pageSize.value)
})

function calculatePercentage(count) {
    if (totalCount.value === 0) return 0
    return Math.round((count / totalCount.value) * 1000) / 10
}

function getBarColor(index) {
    if (index === 0) return '#f56c6c'
    if (index === 1) return '#e6a23c'
    if (index === 2) return '#409eff'
    return '#67c23a'
}

async function loadStats() {
    loading.value = true
    try {
        const response = await axios.get('/stats/region_top50')
        if (response.data.code === '200') {
            statsData.value = response.data.data || []
            totalCount.value = statsData.value.reduce((sum, item) => sum + item.statCount, 0)
            await nextTick()
            drawChart()
        } else {
            ElMessage.error(response.data.message || '加载数据失败')
        }
    } catch (error) {
        console.error('加载统计数据失败:', error)
        ElMessage.error('加载统计数据失败')
        statsData.value = []
    } finally {
        loading.value = false
    }
}

function drawChart() {
    if (!chartRef.value || statsData.value.length === 0) return
    
    if (chartInstance) {
        chartInstance.dispose()
    }
    
    chartInstance = echarts.init(chartRef.value)
    
    const top20Data = statsData.value.slice(0, 20)
    const regions = top20Data.map(item => item.statKey).reverse()
    const counts = top20Data.map(item => item.statCount).reverse()
    
    const option = {
        title: {
            text: 'Top20 地区电影发行量',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 18, fontWeight: 'bold', color: '#1a1a2e' }
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' },
            formatter: function(params) {
                const data = params[0]
                return `<div style="font-weight:bold;font-size:14px;color:#1a1a2e">${data.name}</div>
                        <div style="color:#409eff;font-size:13px">电影数量: <b>${data.value}</b></div>`
            }
        },
        grid: { left: '3%', right: '10%', bottom: '3%', top: '60px', containLabel: true },
        xAxis: {
            type: 'value',
            name: '电影数量',
            nameTextStyle: { color: '#1a1a2e', fontSize: 13, fontWeight: '600' },
            axisLabel: { color: '#1a1a2e', fontSize: 12 },
            axisLine: { lineStyle: { color: '#dcdfe6' } },
            splitLine: { lineStyle: { color: '#ebeef5', type: 'dashed' } }
        },
        yAxis: {
            type: 'category',
            data: regions,
            axisLabel: { color: '#1a1a2e', fontSize: 13, fontWeight: '500' },
            axisLine: { lineStyle: { color: '#dcdfe6' } },
            axisTick: { show: false }
        },
        series: [{
            name: '电影数量',
            type: 'bar',
            data: counts,
            barWidth: '65%',
            itemStyle: {
                color: function(params) {
                    const colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399']
                    const reversedIndex = top20Data.length - 1 - params.dataIndex
                    return reversedIndex < 3 ? colors[reversedIndex] : colors[3]
                },
                borderRadius: [0, 6, 6, 0]
            },
            emphasis: {
                itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.25)' }
            },
            label: { show: true, position: 'right', fontSize: 13, fontWeight: '600', color: '#1a1a2e' }
        }]
    }
    
    chartInstance.setOption(option)
    resizeHandler = () => { chartInstance && chartInstance.resize() }
    window.addEventListener('resize', resizeHandler)
}

// ========== Tab2: 年度地区对比 ==========
const yearRegionData = ref([])
const yearLoading = ref(false)
const lineChartRef = ref(null)
let lineChartInstance = null
let lineResizeHandler = null
const showLineChart = ref(true)

const LINE_COLORS = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399', '#9b59b6', '#1abc9c', '#e74c3c', '#3498db', '#f39c12']

async function loadYearRegionData() {
    yearLoading.value = true
    try {
        const response = await axios.get('/stats/year_region_comparison')
        if (response.data.code === '200') {
            yearRegionData.value = response.data.data || []
            await nextTick()
            drawLineChart()
        } else {
            ElMessage.error(response.data.message || '加载数据失败')
        }
    } catch (error) {
        console.error('加载年度对比数据失败:', error)
        ElMessage.error('加载年度对比数据失败')
        yearRegionData.value = []
    } finally {
        yearLoading.value = false
    }
}

function drawLineChart() {
    if (!lineChartRef.value || yearRegionData.value.length === 0) return

    if (lineChartInstance) {
        lineChartInstance.dispose()
    }

    lineChartInstance = echarts.init(lineChartRef.value)

    // 解析extra_data获取每年的数据
    const series = []
    let allYears = new Set()

    yearRegionData.value.forEach((item, index) => {
        let yearData = {}
        try {
            yearData = JSON.parse(item.extraData || '{}')
        } catch (e) {
            yearData = {}
        }

        Object.keys(yearData).forEach(y => allYears.add(parseInt(y)))

        const years = Object.keys(yearData).map(Number).sort((a, b) => a - b)
        const values = years.map(y => yearData[String(y)])

        series.push({
            name: item.statKey,
            type: 'line',
            data: years.map((y, i) => [y, values[i]]),
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 2, color: LINE_COLORS[index % LINE_COLORS.length] },
            itemStyle: { color: LINE_COLORS[index % LINE_COLORS.length] },
            emphasis: {
                focus: 'series',
                itemStyle: { borderWidth: 2, borderColor: '#fff', shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.3)' }
            }
        })
    })

    const sortedYears = Array.from(allYears).sort((a, b) => a - b)

    const option = {
        title: {
            text: 'Top10 地区年度电影发行量趋势',
            left: 'center',
            top: 10,
            textStyle: { fontSize: 18, fontWeight: 'bold', color: '#1a1a2e' }
        },
        tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'cross', label: { backgroundColor: '#6a7985' } },
            formatter: function(params) {
                let html = `<div style="font-weight:bold;font-size:14px;color:#1a1a2e;margin-bottom:6px">${params[0].value[0]}年</div>`
                params.forEach(p => {
                    html += `<div style="display:flex;align-items:center;gap:6px;font-size:13px">
                        <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color}"></span>
                        <span>${p.seriesName}</span>
                        <span style="font-weight:bold;margin-left:auto">${p.value[1]}</span>
                    </div>`
                })
                return html
            }
        },
        legend: {
            type: 'scroll',
            bottom: 10,
            itemWidth: 15,
            itemHeight: 8,
            textStyle: { color: '#1a1a2e', fontSize: 13 },
            data: yearRegionData.value.map(item => item.statKey)
        },
        grid: { left: '3%', right: '4%', bottom: '60px', top: '60px', containLabel: true },
        xAxis: {
            type: 'value',
            name: '年份',
            nameTextStyle: { color: '#1a1a2e', fontSize: 13, fontWeight: '600' },
            min: sortedYears.length > 0 ? sortedYears[0] : 'dataMin',
            max: sortedYears.length > 0 ? sortedYears[sortedYears.length - 1] : 'dataMax',
            interval: 10,
            axisLabel: { color: '#1a1a2e', fontSize: 12, formatter: '{value}年' },
            axisLine: { lineStyle: { color: '#dcdfe6' } },
            splitLine: { lineStyle: { color: '#ebeef5', type: 'dashed' } }
        },
        yAxis: {
            type: 'value',
            name: '电影数量',
            nameTextStyle: { color: '#1a1a2e', fontSize: 13, fontWeight: '600' },
            axisLabel: { color: '#1a1a2e', fontSize: 12 },
            axisLine: { lineStyle: { color: '#dcdfe6' } },
            splitLine: { lineStyle: { color: '#ebeef5', type: 'dashed' } }
        },
        series: series
    }

    lineChartInstance.setOption(option)
    lineResizeHandler = () => { lineChartInstance && lineChartInstance.resize() }
    window.addEventListener('resize', lineResizeHandler)
}

// ========== Tab3: 词云分析 ==========
const wordcloudLoading = ref(false)
const wordcloudTab = ref('genre')
const genreStats = ref([])
const tagStats = ref([])
const wordcloudImageError = ref(false)

const genreWordcloudSrc = computed(() => {
    return `/genre_wordcloud.png?t=${Date.now()}`
})

const tagWordcloudSrc = computed(() => {
    return `/tag_wordcloud.png?t=${Date.now()}`
})

async function loadWordcloudData() {
    wordcloudLoading.value = true
    wordcloudImageError.value = false
    try {
        const [genreRes, tagRes] = await Promise.all([
            axios.get('/stats/genre_cloud'),
            axios.get('/stats/tag_cloud/limit', { params: { limit: 50 } })
        ])
        if (genreRes.data.code === '200') {
            genreStats.value = (genreRes.data.data || []).slice(0, 20)
        } else {
            console.error('加载类型词云数据失败:', genreRes.data.message)
        }
        if (tagRes.data.code === '200') {
            tagStats.value = (tagRes.data.data || []).filter(item => item.statKey !== 'Unknown').slice(0, 20)
        } else {
            console.error('加载标签词云数据失败:', tagRes.data.message)
        }
    } catch (error) {
        console.error('加载词云数据失败:', error)
    } finally {
        wordcloudLoading.value = false
    }
}

function onGenreImageError() {
    wordcloudImageError.value = true
}

function onTagImageError() {
    wordcloudImageError.value = true
}

function reloadWordcloudImages() {
    wordcloudImageError.value = false
    // Force re-render by toggling tab
    const currentTab = wordcloudTab.value
    wordcloudTab.value = 'tag'
    setTimeout(() => {
        wordcloudTab.value = currentTab
    }, 50)
}

// ========== Tab4: 评分投票Top20 ==========
const topLoading = ref(false)
const topTab = ref('rated')
const topRatedData = ref([])
const topVotesData = ref([])
const showTopRatedChart = ref(false)
const showTopVotesChart = ref(false)
const topRatedChartError = ref(false)
const topVotesChartError = ref(false)

const topRatedChartSrc = computed(() => `/top_rated.png?t=${Date.now()}`)
const topVotesChartSrc = computed(() => `/top_votes.png?t=${Date.now()}`)

function getVoteScore(row) {
    try {
        const extra = JSON.parse(row.extraData || '{}')
        const score = extra.score
        return score && score > 0 ? score.toFixed(1) : '-'
    } catch {
        return '-'
    }
}

async function loadTopRatedVotes() {
    topLoading.value = true
    try {
        const [ratedRes, votesRes] = await Promise.all([
            axios.get('/stats/top_rated'),
            axios.get('/stats/top_votes')
        ])
        if (ratedRes.data.code === '200') {
            topRatedData.value = ratedRes.data.data || []
        }
        if (votesRes.data.code === '200') {
            topVotesData.value = votesRes.data.data || []
        }
    } catch (error) {
        console.error('加载评分投票Top20数据失败:', error)
        ElMessage.error('加载数据失败')
    } finally {
        topLoading.value = false
    }
}

// ========== 电影详情弹窗 ==========
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailMovie = ref(null)
const detailCoverError = ref(false)
let detailRowCache = null

async function openMovieDetail(row) {
    detailRowCache = row
    detailDialogVisible.value = true
    detailLoading.value = true
    detailMovie.value = null
    detailCoverError.value = false
    try {
        const extra = JSON.parse(row.extraData || '{}')
        const movieId = extra.movieId || ''
        if (!movieId) {
            detailLoading.value = false
            return
        }
        const response = await axios.get(`/movies/${movieId}`)
        if (response.data) {
            detailMovie.value = response.data
        }
    } catch (error) {
        console.error('加载电影详情失败:', error)
        ElMessage.error('加载电影详情失败')
    } finally {
        detailLoading.value = false
    }
}

function openMovieCommentsFromDetail() {
    detailDialogVisible.value = false
    if (detailRowCache) {
        openMovieComments(detailRowCache)
    }
}

// ========== 电影评论弹窗 ==========
const commentDialogVisible = ref(false)
const commentMovieName = ref('')
const commentMovieId = ref('')
const commentsData = ref([])
const commentsLoading = ref(false)
const commentsPage = ref(1)
const commentsPageSize = ref(10)
const commentsTotal = ref(0)

function formatTime(time) {
    if (!time) return ''
    return time.substring(0, 10)
}

async function openMovieComments(row) {
    commentMovieName.value = row.statKey
    try {
        const extra = JSON.parse(row.extraData || '{}')
        commentMovieId.value = extra.movieId || ''
        console.log('评论弹窗: movieId =', commentMovieId.value, 'extraData =', row.extraData)
    } catch (e) {
        console.error('解析extraData失败:', e, 'row.extraData =', row.extraData)
        commentMovieId.value = ''
    }
    if (!commentMovieId.value) {
        ElMessage.warning('无法获取该电影的ID')
    }
    commentDialogVisible.value = true
    commentsPage.value = 1
    await loadComments()
}

async function loadComments() {
    if (!commentMovieId.value) {
        commentsData.value = []
        commentsTotal.value = 0
        commentsLoading.value = false
        return
    }
    commentsLoading.value = true
    try {
        const response = await axios.get(`/comments/movie/${commentMovieId.value}`, {
            params: { pageNum: commentsPage.value, pageSize: commentsPageSize.value },
            timeout: 30000
        })
        if (response.data) {
            commentsData.value = response.data.data || []
            commentsTotal.value = response.data.totalCount || 0
            console.log('评论加载成功:', commentsData.value.length, '条, 总计', commentsTotal.value)
        } else {
            commentsData.value = []
            commentsTotal.value = 0
            ElMessage.error('评论数据返回为空')
        }
    } catch (error) {
        console.error('加载评论失败:', error)
        ElMessage.error('加载评论失败: ' + (error.message || '未知错误'))
        commentsData.value = []
        commentsTotal.value = 0
    } finally {
        commentsLoading.value = false
    }
}

// ========== 标签页切换 ==========
function handleStatTabChange(tab) {
    if (tab === 'year_region_comparison' && yearRegionData.value.length === 0) {
        loadYearRegionData()
    }
    if (tab === 'wordcloud' && genreStats.value.length === 0) {
        loadWordcloudData()
    }
    if (tab === 'top_rated_votes' && topRatedData.value.length === 0) {
        loadTopRatedVotes()
    }
}

// ========== 地区电影弹窗 ==========
const regionDialogVisible = ref(false)
const selectedRegion = ref('')
const regionMovies = ref([])
const regionMoviesLoading = ref(false)
const regionMoviesPage = ref(1)
const regionMoviesPageSize = ref(10)
const regionMoviesTotal = ref(0)

async function openRegionDialog(region) {
    selectedRegion.value = region
    regionDialogVisible.value = true
    regionMoviesPage.value = 1
    await loadRegionMovies()
}

async function loadRegionMovies() {
    regionMoviesLoading.value = true
    try {
        const response = await axios.get('/movies/byRegion', {
            params: {
                region: selectedRegion.value,
                pageNum: regionMoviesPage.value,
                pageSize: regionMoviesPageSize.value
            }
        })
        if (response.data) {
            regionMovies.value = response.data.data || []
            regionMoviesTotal.value = response.data.totalCount || 0
        }
    } catch (error) {
        console.error('加载地区电影失败:', error)
        regionMovies.value = []
        regionMoviesTotal.value = 0
    } finally {
        regionMoviesLoading.value = false
    }
}

// ========== 监听器 ==========
watch(showChart, async (newVal) => {
    if (newVal) { await nextTick(); drawChart() }
})

watch(regionViewMode, async (newVal) => {
    if (newVal === 'chart') { await nextTick(); drawChart() }
})

watch(showLineChart, async (newVal) => {
    if (newVal) { await nextTick(); drawLineChart() }
})

watch(activeStatTab, (newTab) => {
    if (newTab === 'year_region_comparison' && yearRegionData.value.length === 0) {
        loadYearRegionData()
    }
})

// ========== 生命周期 ==========
onMounted(() => {
    loadStats()
})

onBeforeUnmount(() => {
    if (resizeHandler) window.removeEventListener('resize', resizeHandler)
    if (lineResizeHandler) window.removeEventListener('resize', lineResizeHandler)
    if (chartInstance) chartInstance.dispose()
    if (lineChartInstance) lineChartInstance.dispose()
})
</script>

<style scoped>
.stats-analysis { padding: 20px; }

.stats-header { margin-bottom: 20px; }

.header-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
}

.stats-header h2 {
    font-size: 22px;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0 0 6px 0;
}

.stats-desc {
    font-size: 14px;
    color: #495057;
    margin: 0;
    font-weight: 400;
}

.chart-toggle-btn {
    display: none;
}

.chart-toggle-btn:hover { font-size: 15px; }

.stats-loading {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 60px 0;
    color: #495057;
}

.loading-icon {
    animation: spin 1s linear infinite;
    color: #409eff;
}

@keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
}

.stats-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.stats-table-container {
    display: none;
}

.rank-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    font-weight: 700;
    font-size: 14px;
}

.rank-1 {
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.4);
}

.rank-2 {
    background: linear-gradient(135deg, #ffa94d 0%, #ff922b 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(230, 162, 60, 0.4);
}

.rank-3 {
    background: linear-gradient(135deg, #74c0fc 0%, #4dabf7 100%);
    color: white;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
}

.rank-normal {
    background: #f1f3f5;
    color: #495057;
}

.region-name {
    font-weight: 600;
    color: #1a1a2e;
    font-size: 15px;
}

.count-value {
    font-weight: 700;
    color: #f56c6c;
    font-size: 18px;
}

.percentage-bar-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    justify-content: center;
}

.percentage-bar {
    width: 160px;
    flex-shrink: 0;
}

.percentage-text {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a2e;
    min-width: 52px;
    text-align: left;
    font-variant-numeric: tabular-nums;
}

.pagination-container {
    display: flex;
    justify-content: center;
    padding: 16px 0 0;
    background: transparent;
    border-radius: 8px;
}

.stats-unified-card {
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    overflow: hidden;
}

.card-tab-bar {
    display: flex;
    gap: 0;
    border-bottom: 1px solid #ebeef5;
    background: #fafbfc;
}

.card-tab {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 12px 24px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    color: #606266;
    border-bottom: 3px solid transparent;
    transition: all 0.25s ease;
    user-select: none;
}

.card-tab:hover {
    color: #409eff;
    background: #f0f7ff;
}

.card-tab.active {
    color: #fff;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-bottom-color: transparent;
    font-weight: 600;
}

.card-body {
    padding: 20px;
}

.chart-container {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chart-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
}

.chart-header h3 {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0;
}

.chart-subtitle {
    font-size: 12px;
    color: #868e96;
    font-weight: 400;
}

.chart-wrapper {
    width: 100%;
    height: 460px;
}

.fade-enter-active, .fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
    opacity: 0;
}

.stats-empty {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 60px 0;
    color: #495057;
}

.stats-empty p { margin: 12px 0; font-size: 14px; }

.empty-hint {
    font-size: 12px !important;
    color: #868e96 !important;
}

/* Tab active bar fix - full background coverage */
.stats-analysis :deep(.el-tabs__header) {
  margin: 0 0 20px 0;
  background: #f8f9fb;
  border-radius: 8px;
  padding: 4px;
  border: none !important;
}

.stats-analysis :deep(.el-tabs__nav-wrap) {
  border: none !important;
  background: transparent;
}

.stats-analysis :deep(.el-tabs__nav-wrap)::after {
  display: none !important;
}

.stats-analysis :deep(.el-tabs__nav) {
  border: none !important;
  gap: 8px;
  padding: 0;
}

.stats-analysis :deep(.el-tabs__active-bar) {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
}

.stats-analysis :deep(.el-tabs__item) {
  height: 40px !important;
  line-height: 40px !important;
  font-size: 15px;
  font-weight: 500;
  color: #606266;
  transition: all 0.25s ease;
  padding: 0 28px !important;
  border-radius: 6px !important;
  margin: 0 !important;
  border: none !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.stats-analysis :deep(.el-tabs__item:hover) {
  color: #409eff;
  background: #e8f0fe;
}

.stats-analysis :deep(.el-tabs__item.is-active) {
  color: #fff !important;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.35);
  border: none !important;
}

:deep(.el-progress-bar__outer) {
    background-color: #f0f2f5;
    border-radius: 8px;
}

:deep(.el-progress-bar__inner) {
    border-radius: 8px;
    transition: width 0.3s ease;
}

:deep(.el-table th.el-table__cell) {
    background-color: #e8f0fe !important;
}

:deep(.el-table td.el-table__cell) {
    background-color: #ffffff !important;
}

:deep(.el-table .cell) {
    color: #1a1a2e;
}

/* 年度地区对比卡片 */
.year-summary-container {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 16px;
}

.year-summary-card {
    display: flex;
    align-items: center;
    gap: 14px;
    background: white;
    border-radius: 12px;
    padding: 16px 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.year-summary-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-rank {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: 50%;
    font-weight: 700;
    font-size: 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    flex-shrink: 0;
}

.year-summary-card:nth-child(1) .card-rank {
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
    box-shadow: 0 2px 8px rgba(245, 108, 108, 0.4);
}

.year-summary-card:nth-child(2) .card-rank {
    background: linear-gradient(135deg, #ffa94d 0%, #ff922b 100%);
    box-shadow: 0 2px 8px rgba(230, 162, 60, 0.4);
}

.year-summary-card:nth-child(3) .card-rank {
    background: linear-gradient(135deg, #74c0fc 0%, #4dabf7 100%);
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
}

.card-info { flex: 1; }

.card-region {
    font-size: 16px;
    font-weight: 700;
    color: #1a1a2e;
    margin-bottom: 4px;
}

.card-count {
    font-size: 14px;
    font-weight: 600;
    color: #67c23a;
}

/* 地区电影弹窗 */
.dialog-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 40px 0;
    color: #495057;
}

.dialog-hint {
    font-size: 14px;
    color: #495057;
    margin-bottom: 12px;
}

.dialog-hint b {
    color: #409eff;
    font-size: 16px;
}

.dialog-pagination {
    margin-top: 12px;
    display: flex;
    justify-content: center;
}

.movie-name-link {
    font-weight: 600;
    color: #1a1a2e;
    cursor: pointer;
}

.movie-name-link:hover {
    color: #409eff;
}

.score-tag {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 4px;
    font-weight: 700;
    font-size: 13px;
}

.score-high {
    background: #f0f9eb;
    color: #67c23a;
}

.score-mid {
    background: #fdf6ec;
    color: #e6a23c;
}

.score-low {
    background: #fef0f0;
    color: #f56c6c;
}

:deep(.region-dialog .el-dialog__body) {
    padding: 16px 24px;
}

/* 词云分析 */
.wordcloud-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.wordcloud-sub-tabs {
    display: flex;
    justify-content: center;
}

.wordcloud-image-wrapper {
    background: white;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
}

.wordcloud-image {
    max-width: 100%;
    max-height: 500px;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.wordcloud-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 40px;
    color: #868e96;
}

.wordcloud-error p {
    margin: 0;
    font-size: 14px;
}

.wordcloud-stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

.wordcloud-stats-card {
    background: white;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    max-height: 480px;
    overflow-y: auto;
}

.wordcloud-stats-card .stats-title {
    font-size: 18px;
    font-weight: 700;
    color: #1a1a2e;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 2px solid #e8f0fe;
}

.stats-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.stats-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 6px 8px;
    border-radius: 8px;
    transition: background 0.2s;
}

.stats-row:hover {
    background: #f8f9fa;
}

.stats-rank {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    font-size: 12px;
    font-weight: 700;
    color: white;
    background: #409eff;
    flex-shrink: 0;
}

.stats-row:nth-child(1) .stats-rank {
    background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
}

.stats-row:nth-child(2) .stats-rank {
    background: linear-gradient(135deg, #ffa94d 0%, #ff922b 100%);
}

.stats-row:nth-child(3) .stats-rank {
    background: linear-gradient(135deg, #74c0fc 0%, #4dabf7 100%);
}

.stats-label {
    flex: 1;
    font-size: 14px;
    font-weight: 500;
    color: #1a1a2e;
}

.stats-count {
    font-size: 14px;
    font-weight: 700;
    color: #f56c6c;
}

.stats-empty-small {
    text-align: center;
    padding: 40px 0;
    color: #868e96;
    font-size: 14px;
}

/* 评分投票Top20 */
.top-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.top-sub-tabs {
    display: flex;
    justify-content: center;
}

.top-section {
    background: white;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.movie-clickable {
    font-weight: 600;
    color: #1a1a2e;
    cursor: pointer;
    transition: color 0.2s;
}

.movie-clickable:hover {
    color: #409eff;
    text-decoration: underline;
}

.score-display {
    font-weight: 700;
    font-size: 16px;
    color: #f56c6c;
}

.score-display.rated-color {
    color: #e6a23c;
    font-size: 18px;
}

.top-chart-toggle {
    margin-top: 16px;
    text-align: center;
}

.top-chart-wrapper {
    margin-top: 12px;
    display: flex;
    justify-content: center;
    padding: 16px;
    background: #fafafa;
    border-radius: 12px;
}

.top-chart-image {
    max-width: 100%;
    max-height: 600px;
    border-radius: 8px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.chart-error {
    padding: 40px;
    color: #868e96;
    font-size: 14px;
}

/* 评论弹窗 */
:deep(.comment-dialog .el-dialog__body) {
    padding: 16px 24px;
}

/* 电影详情弹窗 */
:deep(.detail-dialog .el-dialog__body) {
    padding: 20px 28px;
}

.detail-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.detail-body {
    display: flex;
    gap: 24px;
    align-items: flex-start;
}

.detail-poster {
    flex-shrink: 0;
    width: 200px;
    min-height: 280px;
    border-radius: 8px;
    overflow: hidden;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
}

.poster-img {
    width: 100%;
    display: block;
    border-radius: 8px;
}

.poster-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #c0c4cc;
    font-size: 13px;
}

.detail-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.detail-header {
    display: flex;
    align-items: center;
    gap: 16px;
    padding-bottom: 16px;
    border-bottom: 2px solid #e8f0fe;
}

.detail-title {
    font-size: 24px;
    font-weight: 700;
    color: #1a1a2e;
    margin: 0;
    flex: 1;
}

.detail-score-tag {
    font-size: 18px !important;
    font-weight: 700 !important;
    padding: 8px 16px !important;
    flex-shrink: 0;
}

.detail-descriptions {
    margin-top: 4px;
}

.detail-value {
    font-size: 14px;
    color: #1a1a2e;
    font-weight: 500;
}

.detail-storyline {
    font-size: 14px;
    color: #495057;
    line-height: 1.7;
    display: block;
    max-height: 120px;
    overflow-y: auto;
    padding: 4px 0;
}

.detail-actions {
    display: flex;
    justify-content: center;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
}
</style>
