﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="comment-management">
    <div class="content-wrapper">
      <div class="toolbar">
        <el-autocomplete
          v-model="searchKeyword"
          :fetch-suggestions="querySearchSuggestions"
          placeholder="搜索评论ID/用户ID/电影ID/电影名/评论内容..."
          style="width: 420px"
          clearable
          @select="handleSuggestionSelect"
          @clear="handleSearchClear"
          @keyup.enter="searchComments"
          size="default"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="searchComments" class="search-btn">搜索</el-button>
          </template>
          <template #default="{ item }">
            <div class="suggestion-item">
              <span class="suggestion-name">{{ item.value }}</span>
              <span v-if="item.type" class="suggestion-type">{{ item.type }}</span>
            </div>
          </template>
        </el-autocomplete>
        <el-button type="primary" @click="showAddDialog">添加评论</el-button>
        <el-button @click="loadComments">刷新</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="comments" border style="width: 100%; height: 100%;" v-loading="loading" loading-text="加载中，请稍候..." :empty-text="loading ? '' : '暂无数据'">
      <el-table-column prop="commentId" label="评论ID" width="150"></el-table-column>
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
      <el-table-column prop="votes" label="点赞数" width="120">
        <template #default="scope">
          <span class="votes-value">
            <el-icon><Star /></el-icon>
            {{ scope.row.votes || 0 }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.content || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="commentTime" label="评论时间" width="180">
        <template #default="scope">
          {{ scope.row.commentTime || '——' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="showEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="deleteComment(scope.row.commentId)">删除</el-button>
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
      :title="isEdit ? '编辑评论' : '添加评论'"
      width="600px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="评论ID">
          <el-input v-model="form.commentId"></el-input>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="form.userMd5"></el-input>
        </el-form-item>
        <el-form-item label="电影ID">
          <el-input v-model="form.movieId"></el-input>
        </el-form-item>
        <el-form-item label="评论内容">
          <el-input v-model="form.content" type="textarea" :rows="4"></el-input>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="评分(1-5)">
              <el-rate v-model="form.rating" allow-half show-score text-color="#ff9900" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点赞数">
              <el-input-number v-model="form.votes" :min="0" style="width: 100%"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="评论时间">
          <el-date-picker
            v-model="form.commentTime"
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
        <el-button type="primary" @click="saveComment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from '../utils/axios'
import { Star, Search } from '@element-plus/icons-vue'

export default {
  name: 'CommentManagement',
  components: { Star, Search },
  data() {
    return {
      comments: [],
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
        commentId: '',
        userMd5: '',
        movieId: '',
        content: '',
        votes: 0,
        commentTime: '',
        rating: 3
      }
    }
  },
  mounted() {
    this.loadComments()
  },
  methods: {
    async loadComments() {
      this.loading = true
      try {
        const response = await axios.get('/comments', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize,
            keyword: this.searchKeyword
          }
        })
        this.comments = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
        await this.loadMovieNames()
      } catch (error) {
        console.error('加载评论失败:', error)
        if (this.currentPage > 1) {
          console.warn('检测到页码异常，自动回退到第1页')
          this.currentPage = 1
          this.loading = false
          return this.loadComments()
        }
        this.$message.error('加载评论失败')
      } finally {
        this.loading = false
      }
    },
    searchComments() {
      this.currentPage = 1
      this.loadComments()
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
          const userRes = await axios.get('/users/search', {
            params: { keyword: queryString, pageNum: 1, pageSize: 5 }
          })
          const suggestions = []
          const movies = movieRes.data?.data || []
          const users = userRes.data?.data || []
          movies.forEach(m => {
            suggestions.push({
              value: m.name,
              type: m.year ? `电影 · ${m.year}` : '电影',
              movie: m
            })
          })
          users.forEach(u => {
            suggestions.push({
              value: u.nickname || u.userMd5,
              type: `用户 · ${u.userMd5?.substring(0, 12)}...`,
              user: u
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
      this.searchComments()
    },
    handleSearchClear() {
      this.searchKeyword = ''
      this.loadComments()
    },
    async loadMovieNames() {
      const movieIds = [...new Set(this.comments.map(c => c.movieId))]
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
      this.loadComments()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.loadComments()
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
        commentId: '',
        userMd5: '',
        movieId: '',
        content: '',
        votes: 0,
        commentTime: '',
        rating: 3
      }
      this.dialogVisible = true
    },
    showEditDialog(comment) {
      this.isEdit = true
      this.form = { ...comment }
      this.dialogVisible = true
    },
    async saveComment() {
      try {
        let response
        if (this.isEdit) {
          response = await axios.put('/comments', this.form)
        } else {
          response = await axios.post('/comments', this.form)
        }
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.dialogVisible = false
          this.loadComments()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        console.error('保存评论失败:', error)
        this.$message.error('保存失败')
      }
    },
    async deleteComment(id) {
      try {
        await this.$confirm('确定要删除这个评论吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await axios.delete(`/comments/${id}`)
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.loadComments()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除评论失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.comment-management {
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

.votes-value {
  font-size: 16px;
  font-weight: bold;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 4px;
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
