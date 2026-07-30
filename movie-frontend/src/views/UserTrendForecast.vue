<template>
  <div class="trend-forecast">
    <div v-if="loading" class="loading-container">
      <div class="skeleton-cards">
        <div v-for="n in 3" :key="n" class="skeleton-card-item">
          <div class="skeleton-card-value"></div>
          <div class="skeleton-card-label"></div>
        </div>
      </div>
      <div class="skeleton-chart">
        <div class="skeleton-chart-title"></div>
        <div class="skeleton-chart-body"></div>
      </div>
    </div>
    <div v-else-if="loadError" class="empty-state">
      <el-icon :size="64" color="#ccc"><WarningFilled /></el-icon>
      <p>加载趋势预测失败</p>
      <el-button type="primary" @click="loadData">重试</el-button>
    </div>
    <div v-else>
      <!-- 统计概览 -->
      <div class="overview-cards">
        <div class="overview-card card-hist">
          <div class="card-icon">
            <el-icon :size="28"><Clock /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-value">{{ stats.totalHistoricalYears || 0 }}</div>
            <div class="card-label">历史年数</div>
          </div>
        </div>
        <div class="overview-card card-peak">
          <div class="card-icon">
            <el-icon :size="28"><TrendCharts /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-value">{{ stats.maxCount || 0 }}</div>
            <div class="card-label">年最高产量 (部)</div>
          </div>
        </div>
        <div class="overview-card card-forecast">
          <div class="card-icon">
            <el-icon :size="28"><DataLine /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-value">{{ stats.forecastYears || 0 }}</div>
            <div class="card-label">预测年数</div>
          </div>
        </div>
        <div class="overview-card card-growth">
          <div class="card-icon">
            <el-icon :size="28"><Odometer /></el-icon>
          </div>
          <div class="card-info">
            <div class="card-value">{{ growthRate }}</div>
            <div class="card-label">年均增长率</div>
          </div>
        </div>
      </div>

      <!-- 图表区域 -->
      <div class="chart-section">
        <div class="chart-header">
          <h3>电影发行数量趋势（ARIMA 时序预测）</h3>
          <div class="view-switch">
            <el-radio-group v-model="viewMode" size="small" @change="onViewChange">
              <el-radio-button value="all">全量视图</el-radio-button>
              <el-radio-button value="recent">近30年</el-radio-button>
              <el-radio-button value="forecast">仅预测</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div ref="chartRef" class="chart-container"></div>
        <div class="chart-note">
          <span class="note-item">
            <span class="legend-line line-hist"></span>
            历史真实数据
          </span>
          <span class="note-sep">·</span>
          <span class="note-item">
            <span class="legend-line line-forecast"></span>
            ARIMA 模型预测
          </span>
          <span class="note-sep">·</span>
          <span class="note-item">
            <span class="legend-line line-ci"></span>
            95% 置信区间
          </span>
          <span class="note-sep">·</span>
          <span class="note-item">
            <el-tag size="small" type="info" effect="plain">ARIMA(3,1,1)</el-tag>
          </span>
          <span class="note-sep" v-if="stats.peakForecast">·</span>
          <span class="note-item" v-if="stats.peakForecast">
            预测峰值 <strong>{{ stats.peakForecast }}</strong> 部 @ {{ stats.forecastEndYear }}
          </span>
        </div>
      </div>

      <!-- 预测数据表 -->
      <div class="forecast-table-section" v-if="forecastList.length > 0">
        <h3>未来预测数据</h3>
        <el-table :data="forecastList" border stripe style="width: 100%" :empty-text="'暂无预测数据'" highlight-current-row>
          <el-table-column label="年份" prop="year" align="center" width="120">
            <template #default="scope">
              <span class="year-cell">{{ scope.row.year }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预测电影数量" align="center" width="180">
            <template #default="scope">
              <span class="forecast-value">{{ scope.row.count }}</span>
              <span class="unit">部</span>
            </template>
          </el-table-column>
          <el-table-column label="置信区间 (95%)" align="center">
            <template #default="scope">
              <span class="forecast-range">
                <span class="range-low">{{ scope.row.lowerBound || '-' }}</span>
                <span class="range-tilde"> ~ </span>
                <span class="range-high">{{ scope.row.upperBound || '-' }}</span>
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import axios from '../utils/axios'
import { WarningFilled, Clock, TrendCharts, DataLine, Odometer } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

export default {
  name: 'UserTrendForecast',
  components: { WarningFilled, Clock, TrendCharts, DataLine, Odometer },
  setup() {
    const loading = ref(false)
    const loadError = ref(false)
    const chartRef = ref(null)
    const forecastData = ref(null)
    const stats = ref({})
    const forecastList = ref([])
    const viewMode = ref('all')

    let chartInstance = null

    const growthRate = computed(() => {
      const historical = forecastData.value?.historical || []
      if (historical.length < 5) return '—'
      const recentHistorical = historical.slice(-10)
      const first = recentHistorical[0].count
      const last = recentHistorical[recentHistorical.length - 1].count
      if (first === 0) return '—'
      const cagr = Math.pow(last / first, 1 / (recentHistorical.length - 1)) - 1
      const sign = cagr >= 0 ? '+' : ''
      return `${sign}${(cagr * 100).toFixed(2)}%`
    })

    async function loadData() {
      loading.value = true
      loadError.value = false
      try {
        const response = await axios.get('/trend-forecast')
        if (response.data.code === '200') {
          forecastData.value = response.data.data
          stats.value = response.data.data.stats || {}
          forecastList.value = response.data.data.forecast || []

          await nextTick()
          setTimeout(() => {
            renderChart(response.data.data)
          }, 80)
        } else {
          loadError.value = true
        }
      } catch (error) {
        console.error('加载趋势预测失败:', error)
        loadError.value = true
      } finally {
        loading.value = false
      }
    }

    function onViewChange() {
      if (forecastData.value) {
        renderChart(forecastData.value)
      }
    }

    function renderChart(data) {
      if (!chartRef.value) return

      if (chartInstance) {
        chartInstance.dispose()
      }
      chartInstance = echarts.init(chartRef.value)

      const historical = (data.historical || []).map(d => [Number(d.year), Number(d.count)])
      const forecastRaw = data.forecast || []
      const forecast = forecastRaw.map(d => [Number(d.year), Number(d.count)])
      const recent = (data.recent || []).map(d => [Number(d.year), Number(d.count)])

      const upperBounds = forecastRaw.map(d => d.upperBound != null ? [Number(d.year), Number(d.upperBound)] : null).filter(Boolean)
      const lowerBounds = forecastRaw.map(d => d.lowerBound != null ? [Number(d.year), Number(d.lowerBound)] : null).filter(Boolean)

      // 根据视图模式裁剪数据
      let displayHistorical = historical
      let displayRecentForecast = [...recent, ...forecast]

      if (viewMode.value === 'recent') {
        const recentStartYear = Math.max(historical[historical.length - 1][0] - 30, historical[0][0])
        displayHistorical = historical.filter(d => d[0] >= recentStartYear)
        displayRecentForecast = displayRecentForecast.filter(d => d[0] >= recentStartYear - 2)
      } else if (viewMode.value === 'forecast') {
        displayHistorical = historical.filter(d => d[0] >= historical[historical.length - 1][0] - 3)
      }

      const hasConfidence = upperBounds.length > 0 && lowerBounds.length > 0

      // 自动计算X轴标签间隔
      const yearRange = displayHistorical.length + displayRecentForecast.length
      const labelInterval = Math.max(1, Math.floor(yearRange / 15))

      // 找出峰值年份
      const peakPoint = historical.reduce((max, p) => p[1] > max[1] ? p : max, [0, 0])
      const forecastEndYear = forecast.length > 0 ? forecast[forecast.length - 1][0] : null

      const series = []

      // 置信区间（上界）
      if (hasConfidence && viewMode.value !== 'forecast') {
        const filteredUpper = viewMode.value === 'recent'
          ? upperBounds.filter(d => d[0] >= displayHistorical[0][0] - 1)
          : upperBounds
        const filteredLower = viewMode.value === 'recent'
          ? lowerBounds.filter(d => d[0] >= displayHistorical[0][0] - 1)
          : lowerBounds

        if (filteredUpper.length > 0) {
          series.push({
            name: '置信上界',
            type: 'line',
            data: filteredUpper,
            symbol: 'none',
            lineStyle: { width: 1, color: 'rgba(245, 108, 108, 0.5)', type: 'dashed' },
            tooltip: { show: false },
            stack: 'ci-stack'
          })
          series.push({
            name: '置信区间',
            type: 'line',
            data: filteredLower,
            symbol: 'none',
            lineStyle: { width: 1, color: 'rgba(245, 108, 108, 0.5)', type: 'dashed' },
            areaStyle: {
              color: 'rgba(245, 108, 108, 0.12)'
            },
            tooltip: { show: false }
          })
        }
      }

      // 历史数据主线
      series.push({
        name: '历史数据',
        type: 'line',
        data: displayHistorical,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        showSymbol: false,
        sampling: 'lttb',
        lineStyle: { width: 2.5, color: '#5470c6', shadowColor: 'rgba(84, 112, 198, 0.3)', shadowBlur: 6 },
        itemStyle: { color: '#5470c6', borderColor: '#fff', borderWidth: 1 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(84, 112, 198, 0.35)' },
              { offset: 0.5, color: 'rgba(84, 112, 198, 0.15)' },
              { offset: 1, color: 'rgba(84, 112, 198, 0.02)' }
            ]
          }
        },
        markPoint: {
          data: peakPoint[1] > 0 ? [{
            name: '历史峰值',
            coord: [peakPoint[0], peakPoint[1]],
            value: `峰值 ${peakPoint[1]} 部`,
            itemStyle: { color: '#ee6666' },
            label: { color: '#1a1a2e', fontSize: 12, fontWeight: 'bold' }
          }] : [],
          symbolSize: 60
        }
      })

      // 近期+预测趋势
      series.push({
        name: viewMode.value === 'forecast' ? 'ARIMA 预测' : '近期/预测趋势',
        type: 'line',
        data: displayRecentForecast,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        showSymbol: true,
        sampling: 'lttb',
        lineStyle: { width: 2.5, color: '#ee6666', type: 'dashed', shadowColor: 'rgba(238, 102, 102, 0.3)', shadowBlur: 6 },
        itemStyle: { color: '#ee6666', borderColor: '#fff', borderWidth: 2 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(238, 102, 102, 0.3)' },
              { offset: 0.5, color: 'rgba(238, 102, 102, 0.12)' },
              { offset: 1, color: 'rgba(238, 102, 102, 0.02)' }
            ]
          }
        },
        connectNulls: true,
        markPoint: forecastEndYear ? {
          data: [{
            name: '预测截止',
            coord: [forecastEndYear, forecast[forecast.length - 1][1]],
            value: `${forecastEndYear}年`,
            itemStyle: { color: '#91cc75' },
            label: { color: '#fff', fontSize: 10, fontWeight: 'bold' }
          }],
          symbolSize: 50
        } : undefined
      })

      // 预测分界点标记
      const lastHistoricalYear = historical[historical.length - 1][0]
      const firstForecastYear = forecast[0] ? forecast[0][0] : null

      const markLineData = []
      if (firstForecastYear && firstForecastYear - lastHistoricalYear <= 2) {
        markLineData.push({
          xAxis: lastHistoricalYear + 0.5,
          lineStyle: { color: '#91cc75', type: 'dashed', width: 1.5 },
          label: {
            formatter: '预测起点',
            position: 'end',
            color: '#91cc75',
            fontSize: 11
          }
        })
      }

      series[series.length - 1].markLine = {
        silent: false,
        symbol: 'none',
        lineStyle: { color: '#91cc75', type: 'dashed', width: 1.5 },
        data: markLineData
      }

      // 计算dataZoom范围
      const allYears = [...displayHistorical.map(d => d[0]), ...displayRecentForecast.map(d => d[0])]
      const minYear = Math.min(...allYears)
      const maxYear = Math.max(...allYears)
      const totalRange = maxYear - minYear
      const defaultRange = Math.min(40, totalRange)
      const defaultStartPct = ((totalRange - defaultRange) / totalRange) * 100

      const option = {
        animation: true,
        animationDuration: 800,
        animationEasing: 'cubicOut',
        color: ['#5470c6', '#ee6666'],
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          borderColor: '#e4e7ed',
          borderWidth: 1,
          padding: [12, 16],
          textStyle: { color: '#303133', fontSize: 13 },
          axisPointer: {
            type: 'cross',
            label: {
              show: true,
              backgroundColor: '#5470c6',
              color: '#fff',
              fontSize: 12,
              formatter: function(params) {
                return params.value + '年'
              }
            },
            lineStyle: { color: '#c0c4cc', type: 'dashed', width: 1 },
            crossStyle: { color: '#c0c4cc', type: 'dashed', width: 1 }
          },
          formatter: function(params) {
            const year = Math.round(params[0].value[0])
            let result = `<div style="font-weight:700;margin-bottom:8px;font-size:14px;">${year} 年</div>`
            params.forEach(p => {
              if (p.seriesName === '置信上界' || p.seriesName === '置信区间') return
              const value = p.value[1]
              const color = p.seriesName === '历史数据' ? '#5470c6' : '#ee6666'
              result += `<div style="display:flex;align-items:center;gap:8px;margin:4px 0;font-size:13px;">
                ${p.marker} <span style="color:#606266;">${p.seriesName}</span>:
                <strong style="color:${color};margin-left:auto;">${Math.round(value)} 部</strong>
              </div>`
            })
            if (hasConfidence) {
              const fc = forecastRaw.find(f => Number(f.year) === year)
              if (fc && fc.lowerBound != null) {
                result += `<div style="margin-top:8px;padding-top:8px;border-top:1px solid #ebeef5;font-size:12px;color:#909399;">
                  95%置信区间: <span style="color:#67c23a;font-weight:600;">${fc.lowerBound}</span> ~ <span style="color:#f56c6c;font-weight:600;">${fc.upperBound}</span>
                </div>`
              }
            }
            return result
          }
        },
        legend: {
          data: viewMode.value === 'forecast'
            ? ['ARIMA 预测']
            : ['历史数据', '近期/预测趋势'],
          top: 5,
          right: 20,
          itemWidth: 25,
          itemHeight: 12,
          itemGap: 20,
          textStyle: { fontSize: 13, color: '#606266', fontWeight: 500 }
        },
        grid: {
          left: '5%',
          right: '4%',
          bottom: '12%',
          top: '12%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '年份',
          nameLocation: 'middle',
          nameGap: 35,
          nameTextStyle: { fontSize: 13, color: '#606266', fontWeight: 600 },
          min: minYear,
          max: maxYear + 1,
          interval: labelInterval,
          axisLabel: {
            fontSize: 11,
            interval: 0,
            rotate: 0,
            color: '#909399',
            formatter: function(value) {
              if (value % labelInterval === 0 || value === maxYear || value === minYear) {
                return value
              }
              return ''
            }
          },
          axisLine: { lineStyle: { color: '#dcdfe6' } },
          axisTick: { show: true, length: 4, lineStyle: { color: '#dcdfe6' } },
          splitLine: { show: false }
        },
        yAxis: {
          type: 'value',
          name: '电影数量 (部)',
          nameLocation: 'middle',
          nameGap: 55,
          nameTextStyle: { fontSize: 13, color: '#606266', fontWeight: 600 },
          axisLabel: {
            fontSize: 11,
            color: '#909399',
            formatter: function(value) { return value >= 1000 ? (value / 1000).toFixed(1) + 'k' : value }
          },
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: {
            lineStyle: { color: '#ebeef5', type: 'dashed', width: 0.8 }
          }
        },
        series: series,
        dataZoom: [
          {
            type: 'inside',
            start: viewMode.value === 'all' ? defaultStartPct : 0,
            end: 100,
            zoomLock: false,
            moveOnMouseMove: true,
            moveOnMouseWheel: false,
            preventDefaultMouseMove: true
          },
          {
            type: 'slider',
            show: true,
            bottom: '2%',
            height: 18,
            start: viewMode.value === 'all' ? defaultStartPct : 0,
            end: 100,
            borderColor: '#dcdfe6',
            fillerColor: 'rgba(84, 112, 198, 0.2)',
            handleStyle: {
              color: '#5470c6',
              borderColor: '#5470c6'
            },
            moveHandleStyle: { color: '#5470c6' },
            textStyle: { color: '#909399', fontSize: 11 }
          }
        ],
        toolbox: {
          show: true,
          right: 20,
          top: 5,
          feature: {
            dataZoom: {
              yAxisIndex: 'none',
              title: { zoom: '区域缩放', back: '还原' }
            },
            restore: { title: '还原' }
          },
          iconStyle: { borderColor: '#909399' }
        }
      }

      chartInstance.setOption(option, true)
      chartInstance.resize()

      window.addEventListener('resize', () => {
        if (chartInstance) chartInstance.resize()
      })
    }

    onMounted(() => {
      loadData()
    })

    onBeforeUnmount(() => {
      if (chartInstance) {
        chartInstance.dispose()
        chartInstance = null
      }
      window.removeEventListener('resize', () => {
        if (chartInstance) chartInstance.resize()
      })
    })

    return {
      loading, loadError, chartRef, stats, forecastList,
      viewMode, growthRate,
      loadData, onViewChange
    }
  }
}
</script>

<style scoped>
.trend-forecast {
  min-height: 300px;
}

.loading-container {
  padding: 0;
}

.skeleton-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.skeleton-card-item {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 20px 16px;
  height: 80px;
}

.skeleton-card-value {
  width: 60%;
  height: 32px;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 6px;
  margin-bottom: 8px;
  animation: shimmer 1.5s infinite;
}

.skeleton-card-label {
  width: 40%;
  height: 14px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  animation: shimmer 1.5s infinite;
}

.skeleton-chart {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #f0f0f0;
}

.skeleton-chart-title {
  width: 30%;
  height: 18px;
  background: #e8ebef;
  border-radius: 4px;
  margin-bottom: 16px;
  animation: shimmer 1.5s infinite;
}

.skeleton-chart-body {
  width: 100%;
  height: 380px;
  background: linear-gradient(90deg, #f5f5f5 25%, #e8ebef 50%, #f5f5f5 75%);
  background-size: 200% 100%;
  border-radius: 8px;
  animation: shimmer 1.5s infinite;
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

/* 概览卡片 */
.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card {
  border-radius: 14px;
  padding: 20px;
  text-align: left;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.overview-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15);
}

.card-hist {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card-peak {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card-forecast {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.card-growth {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.card-label {
  font-size: 12px;
  margin-top: 6px;
  opacity: 0.9;
  font-weight: 500;
}

/* 图表区域 */
.chart-section {
  margin-bottom: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chart-header h3 {
  font-size: 16px;
  color: #303133;
  margin: 0;
  padding-left: 10px;
  border-left: 3px solid #5470c6;
  font-weight: 600;
}

.view-switch {
  flex-shrink: 0;
}

.chart-container {
  width: 100%;
  height: 500px;
  background: white;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 14px 20px 36px 20px;
  border: 1px solid #f0f0f0;
}

.chart-note {
  font-size: 12px;
  color: #606266;
  text-align: center;
  margin: 12px 0 0 0;
  padding: 10px 16px;
  background: #f8f9fb;
  border-radius: 8px;
  line-height: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 2px;
}

.note-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.note-item strong {
  color: #ee6666;
  font-weight: 600;
}

.note-sep {
  color: #dcdfe6;
  margin: 0 6px;
}

.legend-line {
  display: inline-block;
  width: 20px;
  height: 3px;
  border-radius: 2px;
}

.legend-line.line-hist {
  background: #5470c6;
}

.legend-line.line-forecast {
  background: repeating-linear-gradient(90deg, #ee6666 0, #ee6666 6px, transparent 6px, transparent 10px);
}

.legend-line.line-ci {
  background: rgba(245, 108, 108, 0.25);
  height: 10px;
  width: 20px;
  border-radius: 2px;
}

/* 预测表 */
.forecast-table-section {
  margin-bottom: 24px;
}

.forecast-table-section h3 {
  font-size: 16px;
  color: #303133;
  margin: 0 0 12px 0;
  padding-left: 10px;
  border-left: 3px solid #ee6666;
  font-weight: 600;
}

.year-cell {
  font-weight: 600;
  color: #303133;
}

.forecast-value {
  font-size: 18px;
  font-weight: 700;
  color: #ee6666;
}

.forecast-value .unit {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 4px;
}

.forecast-range {
  font-family: 'SF Mono', 'Consolas', 'Monaco', monospace;
  font-size: 14px;
}

.range-low {
  color: #67c23a;
  font-weight: 600;
}

.range-high {
  color: #f56c6c;
  font-weight: 600;
}

.range-tilde {
  color: #c0c4cc;
  margin: 0 4px;
}

@media (max-width: 768px) {
  .overview-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-container {
    height: 380px;
  }
  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
