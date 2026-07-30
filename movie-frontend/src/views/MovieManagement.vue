﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="movie-management">
    <div class="page-content">
      <div class="toolbar">
        <el-autocomplete
          v-model="searchKeyword"
          :fetch-suggestions="querySearchSuggestions"
          placeholder="搜索电影名称/导演/主演..."
          style="width: 360px"
          clearable
          @select="handleSuggestionSelect"
          @clear="handleSearchClear"
          @keyup.enter="searchMovies"
          size="default"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="searchMovies" class="search-btn">搜索</el-button>
          </template>
          <template #default="{ item }">
            <div class="suggestion-item">
              <span class="suggestion-name">{{ item.value }}</span>
              <span v-if="item.type" class="suggestion-type">{{ item.type }}</span>
            </div>
          </template>
        </el-autocomplete>
        <el-button type="primary" @click="showAddDialog">添加电影</el-button>
        <el-button @click="loadMovies">刷新</el-button>
        <div class="sort-buttons">
          <el-button :type="sortScore ? 'primary' : 'default'" @click="toggleSortScore">
            <el-icon><Star /></el-icon>
            按评分排序
            <span v-if="sortScore">{{ sortScoreOrder === 'desc' ? '↓' : '↑' }}</span>
          </el-button>
          <el-button :type="sortTime ? 'primary' : 'default'" @click="toggleSortTime">
            <el-icon><Clock /></el-icon>
            按上映时间排序
            <span v-if="sortTime">{{ sortTimeOrder === 'desc' ? '↓' : '↑' }}</span>
          </el-button>
        </div>
      </div>

      <div class="table-container">
        <el-table :data="movies" border style="width: 100%; height: 100%;" :empty-text="loading ? '' : '暂无数据'" v-loading="loading" loading-text="加载中，请稍候...">
      <el-table-column prop="movieId" label="电影ID" width="100"></el-table-column>
      <el-table-column label="海报" width="80">
        <template #default="scope">
          <img v-if="scope.row.cover" :src="scope.row.cover" class="cover-img" />
          <span v-else class="no-cover">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="电影名称" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.name || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="year" label="年份" width="80">
        <template #default="scope">
          {{ scope.row.year || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="genres" label="类型" width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.genres || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="directors" label="导演" width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.directors || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="actors" label="主演" width="180" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.actors || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="region" label="地区" width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.region || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="language" label="语言" width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.language || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="doubanScore" label="豆瓣评分" width="100">
        <template #default="scope">
          <span :class="getScoreClass(scope.row.doubanScore)">{{ scope.row.doubanScore || '——' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="doubanVotes" label="投票数" width="100">
        <template #default="scope">
          {{ scope.row.doubanVotes || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="mins" label="片长" width="80">
        <template #default="scope">
          {{ scope.row.mins ? scope.row.mins + '分钟' : '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="releaseDate" label="上映日期" width="120">
        <template #default="scope">
          {{ scope.row.releaseDate || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="storyline" label="剧情简介" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.storyline || '——' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="showEditDialog(scope.row)">编辑</el-button>
          <el-button type="success" size="small" @click="showMovieReviews(scope.row)">评价</el-button>
          <el-button type="danger" size="small" @click="deleteMovie(scope.row.movieId)">删除</el-button>
        </template>
      </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="totalCount"
        :page-sizes="[10, 20, 50, 100]"
        layout="prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑电影' : '添加电影'"
      width="700px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="电影ID">
          <el-input v-model="form.movieId"></el-input>
        </el-form-item>
        <el-form-item label="电影名称">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="电影海报">
          <div v-if="form.cover" class="cover-preview">
            <img :src="form.cover" class="preview-img" />
            <el-button size="small" type="danger" @click="form.cover = ''">删除图片</el-button>
          </div>
          <el-upload
            v-else
            class="upload-demo"
            :show-file-list="false"
            :before-upload="beforeCoverUpload"
            :http-request="customUpload"
            accept="image/*"
          >
            <el-button size="small" type="primary">点击上传海报</el-button>
          </el-upload>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年份">
              <el-input-number v-model="form.year" :min="1900" :max="2100" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="片长(分钟)">
              <el-input-number v-model="form.mins" :min="1" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="豆瓣评分">
              <el-input-number v-model="form.doubanScore" :min="0" :max="10" :step="0.1" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="投票数">
              <el-input-number v-model="form.doubanVotes" :min="0" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="类型">
          <el-input v-model="form.genres" placeholder="多个类型用/分隔"></el-input>
        </el-form-item>
        <el-form-item label="导演">
          <el-input v-model="form.directors"></el-input>
        </el-form-item>
        <el-form-item label="主演">
          <el-input v-model="form.actors"></el-input>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="地区">
              <el-input v-model="form.region"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="语言">
              <el-input v-model="form.language"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="上映日期">
          <el-date-picker
            v-model="form.releaseDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="剧情简介">
          <el-input v-model="form.storyline" type="textarea" :rows="4"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMovie">保存</el-button>
      </template>
    </el-dialog>

    <!-- 电影评价对话框 -->
    <el-dialog
      v-model="reviewsDialogVisible"
      :title="`电影 ${currentMovie?.name} 的评价记录`"
      width="1000px"
    >
      <el-table :data="movieReviews" border style="width: 100%;" v-loading="reviewsLoading" loading-text="加载中，请稍候..." :empty-text="reviewsLoading ? '' : '暂无数据'">
        <el-table-column prop="type" label="类型" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.type === '评论' ? 'primary' : 'success'" size="small">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userMd5" label="用户ID" min-width="180"></el-table-column>
        <el-table-column prop="rating" label="评分" width="80">
          <template #default="scope">
            <span :class="getScoreClass(scope.row.rating)">{{ scope.row.rating || '——' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.content || '——' }}
          </template>
        </el-table-column>
        <el-table-column prop="votes" label="点赞数" width="80">
          <template #default="scope">
            <span v-if="scope.row.votes !== undefined">
              <el-icon><Star /></el-icon> {{ scope.row.votes }}
            </span>
            <span v-else>——</span>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="时间" width="180"></el-table-column>
      </el-table>
      <div v-if="movieReviews.length === 0 && !reviewsLoading" style="text-align: center; padding: 20px;">
        暂无评价记录
      </div>
      <div v-if="movieReviews.length > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="reviewsCurrentPage"
          v-model:page-size="reviewsPageSize"
          :total="reviewsTotalCount"
          layout="prev, pager, next, jumper"
          @current-change="loadMovieReviews"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from '../utils/axios'
import { Star, Clock, Search } from '@element-plus/icons-vue'

export default {
  name: 'MovieManagement',
  components: { Star, Clock, Search },
  data() {
    return {
      movies: [],
      loading: true,
      searchKeyword: '',
      dialogVisible: false,
      isEdit: false,
      currentPage: 1,
      pageSize: 10,
      totalCount: 0,
      totalPages: 0,
      sortBy: '',
      sortOrder: 'desc',
      sortScore: false,
      sortScoreOrder: 'desc',
      sortTime: false,
      sortTimeOrder: 'desc',
      form: {
        movieId: '',
        name: '',
        cover: '',
        genres: '',
        directors: '',
        actors: '',
        doubanScore: 0,
        doubanVotes: 0,
        year: 2024,
        releaseDate: '',
        language: '',
        region: '',
        storyline: '',
        mins: null
      },
      // 评价相关
      reviewsDialogVisible: false,
      currentMovie: null,
      movieReviews: [],
      reviewsLoading: true,
      reviewsCurrentPage: 1,
      reviewsPageSize: 10,
      reviewsTotalCount: 0
    }
  },
  mounted() {
    this.loadMovies()
  },
  methods: {
    toggleSortScore() {
      if (!this.sortScore) {
        this.sortScore = true
        this.sortScoreOrder = 'desc'
      } else if (this.sortScoreOrder === 'desc') {
        this.sortScoreOrder = 'asc'
      } else {
        this.sortScore = false
        this.sortScoreOrder = 'desc'
      }
      this.currentPage = 1
      if (this.searchKeyword) {
        this.searchMovies()
      } else {
        this.loadMovies()
      }
    },
    toggleSortTime() {
      if (!this.sortTime) {
        this.sortTime = true
        this.sortTimeOrder = 'desc'
      } else if (this.sortTimeOrder === 'desc') {
        this.sortTimeOrder = 'asc'
      } else {
        this.sortTime = false
        this.sortTimeOrder = 'desc'
      }
      this.currentPage = 1
      if (this.searchKeyword) {
        this.searchMovies()
      } else {
        this.loadMovies()
      }
    },
    getSortParam() {
      if (this.sortScore && this.sortTime) {
        return { sortBy: 'both', scoreOrder: this.sortScoreOrder, timeOrder: this.sortTimeOrder }
      } else if (this.sortScore) {
        return { sortBy: 'score', sortOrder: this.sortScoreOrder }
      } else if (this.sortTime) {
        return { sortBy: 'time', sortOrder: this.sortTimeOrder }
      }
      return { sortBy: '', sortOrder: 'desc' }
    },
    async querySearchSuggestions(queryString, cb) {
      if (!queryString) {
        cb([])
        return
      }
      clearTimeout(this._searchTimer)
      this._searchTimer = setTimeout(async () => {
        try {
          const movieRes = await axios.get('/movies/search', {
            params: { keyword: queryString, pageNum: 1, pageSize: 5 }
          })
          const personRes = await axios.get('/persons/search', {
            params: { keyword: queryString, pageNum: 1, pageSize: 5 }
          })
          const suggestions = []
          const movies = movieRes.data?.data || []
          const persons = personRes.data?.data || []
          movies.forEach(m => {
            suggestions.push({
              value: m.name,
              type: m.year ? `电影 · ${m.year}` : '电影',
              movie: m
            })
          })
          persons.forEach(p => {
            suggestions.push({
              value: p.name,
              type: p.profession ? `${p.profession}` : '人物',
              person: p
            })
          })
          cb(suggestions.slice(0, 10))
        } catch (e) {
          cb([])
        }
      }, 200)
    },
    handleSuggestionSelect(item) {
      this.searchKeyword = item.value
      this.searchMovies()
    },
    handleSearchClear() {
      this.searchKeyword = ''
      this.loadMovies()
    },
    async loadMovies() {
      this.loading = true
      try {
        const sortParam = this.getSortParam()
        const response = await axios.get('/movies', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            sortBy: sortParam.sortBy,
            sortOrder: sortParam.sortOrder,
            scoreOrder: sortParam.scoreOrder,
            timeOrder: sortParam.timeOrder
          }
        })
        this.movies = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
      } catch (error) {
        console.error('加载电影失败:', error)
        this.$message.error('加载电影失败')
      } finally {
        this.loading = false
      }
    },
    async searchMovies() {
      this.currentPage = 1
      if (!this.searchKeyword) {
        this.loadMovies()
        return
      }
      try {
        const sortParam = this.getSortParam()
        const response = await axios.get('/movies/search', {
          params: {
            keyword: this.searchKeyword,
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            sortBy: sortParam.sortBy,
            sortOrder: sortParam.sortOrder,
            scoreOrder: sortParam.scoreOrder,
            timeOrder: sortParam.timeOrder
          }
        })
        this.movies = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
      } catch (error) {
        console.error('搜索电影失败:', error)
        this.$message.error('搜索电影失败')
      }
    },
    beforeCoverUpload(file) {
      const isImage = file.type.startsWith('image/')
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isImage) {
        this.$message.error('只能上传图片文件!')
        return false
      }
      if (!isLt5M) {
        this.$message.error('图片大小不能超过5MB!')
        return false
      }
      return true
    },
    customUpload(options) {
      const file = options.file
      const reader = new FileReader()
      reader.onload = (e) => {
        const img = new Image()
        img.onload = () => {
          // 压缩图片：最大宽度500px，质量0.7
          const canvas = document.createElement('canvas')
          const ctx = canvas.getContext('2d')
          const maxWidth = 500
          let width = img.width
          let height = img.height
          
          if (width > maxWidth) {
            height = (height * maxWidth) / width
            width = maxWidth
          }
          
          canvas.width = width
          canvas.height = height
          ctx.drawImage(img, 0, 0, width, height)
          
          // 转换为base64，质量0.7
          const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.7)
          this.form.cover = compressedDataUrl
          this.$message.success('海报上传成功')
        }
        img.src = e.target.result
      }
      reader.readAsDataURL(file)
    },
    handlePageChange(page) {
      this.currentPage = page
      if (this.searchKeyword) {
        this.searchMovies()
      } else {
        this.loadMovies()
      }
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      if (this.searchKeyword) {
        this.searchMovies()
      } else {
        this.loadMovies()
      }
    },
    getScoreClass(score) {
      if (!score) return ''
      if (score >= 8) return 'score-high'
      if (score >= 6) return 'score-medium'
      return 'score-low'
    },
    showMovieReviews(movie) {
      this.currentMovie = movie
      this.reviewsCurrentPage = 1
      this.reviewsDialogVisible = true
      this.loadMovieReviews()
    },
    async loadMovieReviews() {
      if (!this.currentMovie) return
      this.reviewsLoading = true
      try {
        const response = await axios.get(`/reviews/movie/${this.currentMovie.movieId}`, {
          params: {
            pageNum: this.reviewsCurrentPage,
            pageSize: this.reviewsPageSize
          }
        })
        this.movieReviews = response.data.data
        this.reviewsTotalCount = response.data.totalCount
      } catch (error) {
        console.error('加载电影评价失败:', error)
        this.$message.error('加载电影评价失败')
      } finally {
        this.reviewsLoading = false
      }
    },
    showAddDialog() {
      this.isEdit = false
      this.form = {
        movieId: '',
        name: '',
        cover: '',
        genres: '',
        directors: '',
        actors: '',
        doubanScore: 0,
        doubanVotes: 0,
        year: 2024,
        releaseDate: '',
        language: '',
        region: '',
        storyline: '',
        mins: null
      }
      this.dialogVisible = true
    },
    showEditDialog(movie) {
      this.isEdit = true
      this.form = { ...movie }
      this.dialogVisible = true
    },
    async saveMovie() {
      try {
        let response
        if (this.isEdit) {
          response = await axios.put('/movies', this.form)
        } else {
          response = await axios.post('/movies', this.form)
        }
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.dialogVisible = false
          this.loadMovies()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        console.error('保存电影失败:', error)
        this.$message.error('保存失败')
      }
    },
    async deleteMovie(id) {
      try {
        await this.$confirm('确定要删除这部电影吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await axios.delete(`/movies/${id}`)
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.loadMovies()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除电影失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.movie-management {
  padding: 0;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  padding-bottom: 90px;
  overflow: hidden;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-shrink: 0;
  margin-bottom: 16px;
}

.table-container {
  flex: 1;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.pagination-container {
  padding: 15px;
  background: #ffffff;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 主页面页码栏固定定位 */
.movie-management > .pagination-container {
  position: fixed;
  bottom: 20px;
  left: max(20px, calc((100vw - 1400px) / 2));
  right: max(20px, calc((100vw - 1400px) / 2));
  z-index: 10;
}

.el-dialog .pagination-container {
  margin-top: 20px;
}

.pagination-container :deep(.el-pagination) {
  font-size: 16px;
}

.pagination-container :deep(.el-pagination__total) {
  font-weight: 600;
  color: #666;
}

.pagination-container :deep(.el-pager li) {
  width: 40px;
  height: 40px;
  line-height: 40px;
  font-size: 16px;
  margin: 0 3px;
  border-radius: 6px;
}

.pagination-container :deep(.el-pager li.is-active) {
  background-color: #409eff;
  color: #fff;
}

.pagination-container :deep(.btn-prev),
.pagination-container :deep(.btn-next) {
  width: 40px;
  height: 40px;
  line-height: 40px;
  font-size: 18px;
  border-radius: 6px;
  margin: 0 5px;
}

.pagination-container :deep(.el-pagination__jump) {
  margin-left: 15px;
}

.pagination-container :deep(.el-pagination__jump .el-input__inner) {
  width: 80px;
  height: 40px;
  font-size: 16px;
}

.score-high {
  color: #67c23a;
  font-weight: bold;
}

.score-medium {
  color: #e6a23c;
  font-weight: bold;
}

.score-low {
  color: #f56c6c;
  font-weight: bold;
}

:deep(.el-input-group__append .el-button) {
  transition: all 0.3s ease;
}

:deep(.el-input-group__append .el-button:hover) {
  background-color: #66b1ff;
  border-color: #66b1ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

:deep(.el-input-group__append .el-button:active) {
  background-color: #3a8ee6;
  border-color: #3a8ee6;
  transform: translateY(0);
  box-shadow: none;
}

.suggestion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.suggestion-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.suggestion-type {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 8px;
}
</style>
