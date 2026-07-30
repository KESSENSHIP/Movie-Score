﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="rating-management">
    <div class="content-wrapper">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户ID/电影ID/电影名称/评分"
          style="width: 300px"
          clearable
          @clear="loadRatings"
          @keyup.enter="searchRatings"
        >
          <template #append>
            <el-button type="primary" @click="searchRatings">搜索</el-button>
          </template>
        </el-input>
        <el-button type="primary" @click="showAddDialog">添加评分</el-button>
        <el-button @click="loadRatings">刷新</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="ratings" border style="width: 100%; height: 100%;" v-loading="loading" loading-text="加载中，请稍候..." :empty-text="loading ? '' : '暂无数据'">
      <el-table-column prop="ratingId" label="评分ID" width="150"></el-table-column>
      <el-table-column prop="userMd5" label="用户ID" min-width="180"></el-table-column>
      <el-table-column label="用户昵称" width="150">
        <template #default="scope">
          {{ scope.row.nickname || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="movieId" label="电影ID" width="120"></el-table-column>
      <el-table-column label="电影名称" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ getMovieName(scope.row.movieId) || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="120">
        <template #default="scope">
          <span class="rating-value" :class="getRatingClass(scope.row.rating)">
            {{ scope.row.rating || '——' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="ratingTime" label="评分时间" width="180">
        <template #default="scope">
          {{ scope.row.ratingTime || '——' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="showEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteRating(scope.row.ratingId)">删除</el-button>
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
      :title="isEdit ? '编辑评分' : '添加评分'"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="评分ID">
          <el-input v-model="form.ratingId"></el-input>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="form.userMd5"></el-input>
        </el-form-item>
        <el-form-item label="电影ID">
          <el-input v-model="form.movieId"></el-input>
        </el-form-item>
        <el-form-item label="评分(1-5)">
          <el-rate v-model="form.rating" allow-half show-score text-color="#ff9900" />
        </el-form-item>
        <el-form-item label="评分时间">
          <el-date-picker
            v-model="form.ratingTime"
            type="datetime"
            placeholder="选择日期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%">
          </el-date-picker>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRating">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from '../utils/axios'

export default {
  name: 'RatingManagement',
  data() {
    return {
      ratings: [],
      dialogVisible: false,
      isEdit: false,
      currentPage: 1,
      pageSize: 10,
      totalCount: 0,
      totalPages: 0,
      loading: true,
      movieMap: {},
      searchKeyword: '',
      form: {
        ratingId: '',
        userMd5: '',
        movieId: '',
        rating: 3,
        ratingTime: ''
      }
    }
  },
  mounted() {
    this.loadRatings()
  },
  methods: {
    async loadRatings() {
      this.loading = true
      try {
        const response = await axios.get('/ratings', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            keyword: this.searchKeyword
          }
        })
        this.ratings = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
        await this.loadMovieNames()
      } catch (error) {
        console.error('加载评分失败:', error)
        this.$message.error('加载评分失败')
      } finally {
        this.loading = false
      }
    },
    searchRatings() {
      this.currentPage = 1
      this.loadRatings()
    },
    async loadMovieNames() {
      const movieIds = [...new Set(this.ratings.map(r => r.movieId))]
      for (const movieId of movieIds) {
        if (!this.movieMap[movieId]) {
          try {
            const response = await axios.get(`/movies/${movieId}`)
            this.movieMap[movieId] = response.data.name || '未知电影'
          } catch (error) {
            this.movieMap[movieId] = '未知电影'
          }
        }
      }
    },
    getMovieName(movieId) {
      return this.movieMap[movieId]
    },
    handlePageChange(page) {
      this.currentPage = page
      this.loadRatings()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.loadRatings()
    },
    getRatingClass(rating) {
      if (!rating) return ''
      const r = parseFloat(rating)
      if (r >= 4.5) return 'rating-high'
      if (r >= 3) return 'rating-medium'
      return 'rating-low'
    },
    showAddDialog() {
      this.isEdit = false
      this.form = {
        ratingId: '',
        userMd5: '',
        movieId: '',
        rating: 3,
        ratingTime: ''
      }
      this.dialogVisible = true
    },
    showEditDialog(rating) {
      this.isEdit = true
      this.form = { ...rating }
      this.dialogVisible = true
    },
    async saveRating() {
      try {
        let response
        if (this.isEdit) {
          response = await axios.put('/ratings', this.form)
        } else {
          response = await axios.post('/ratings', this.form)
        }
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.dialogVisible = false
          this.loadRatings()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        console.error('保存评分失败:', error)
        this.$message.error('保存失败')
      }
    },
    async deleteRating(id) {
      try {
        await this.$confirm('确定要删除这个评分吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await axios.delete(`/ratings/${id}`)
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.loadRatings()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除评分失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.rating-management {
  padding: 0;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-wrapper {
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

.table-wrapper {
  flex: 1;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.rating-value {
  font-size: 20px;
  font-weight: bold;
  display: inline-block;
  padding: 4px 12px;
  border-radius: 6px;
}

.rating-high {
  color: #67c23a;
  background-color: #f0f9eb;
}

.rating-medium {
  color: #e6a23c;
  background-color: #fdf6ec;
}

.rating-low {
  color: #f56c6c;
  background-color: #fef0f0;
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
  </style>
