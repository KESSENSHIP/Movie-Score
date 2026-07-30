﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="user-management">
    <div class="toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户昵称或ID"
        style="width: 300px"
        clearable
        @clear="loadUsers"
      >
        <template #append>
          <el-button @click="searchUsers">搜索</el-button>
        </template>
      </el-input>
      <el-button type="primary" @click="showAddDialog">添加用户</el-button>
      <el-button @click="loadUsers">刷新</el-button>
    </div>

    <el-table :data="users" border style="width: 100%; margin-top: 20px; height: 550px;">
      <el-table-column prop="userMd5" label="用户ID" min-width="200"></el-table-column>
      <el-table-column prop="nickname" label="昵称" min-width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.nickname || '——' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="showEditDialog(scope.row)">编辑</el-button>
          <el-button type="success" size="small" @click="showUserReviews(scope.row)">评价</el-button>
          <el-button type="danger" size="small" @click="deleteUser(scope.row.userMd5)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="form.userMd5"></el-input>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户评价对话框 -->
    <el-dialog
      v-model="reviewsDialogVisible"
      :title="`用户 ${currentUser?.nickname} 的评价记录`"
      width="1000px"
    >
      <el-table :data="userReviews" border style="width: 100%;" v-loading="reviewsLoading" loading-text="加载中，请稍候..." :empty-text="reviewsLoading ? '' : '暂无数据'">
        <el-table-column prop="type" label="类型" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.type === '评论' ? 'primary' : 'success'" size="small">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="120"></el-table-column>
        <el-table-column prop="movieId" label="电影ID" width="100"></el-table-column>
        <el-table-column prop="movieName" label="电影名称" min-width="150" show-overflow-tooltip></el-table-column>
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
      <div v-if="userReviews.length === 0" style="text-align: center; padding: 20px;">
        暂无评价记录
      </div>
      <div v-if="userReviews.length > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="reviewsCurrentPage"
          v-model:page-size="pageSize"
          :total="reviewsTotalCount"
          layout="prev, pager, next, jumper"
          @current-change="loadUserReviews"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from '../utils/axios'
import { Star } from '@element-plus/icons-vue'

export default {
  name: 'UserManagement',
  components: { Star },
  data() {
    return {
      users: [],
      searchKeyword: '',
      dialogVisible: false,
      isEdit: false,
      currentPage: 1,
      pageSize: 10,
      totalCount: 0,
      totalPages: 0,
      form: {
        userMd5: '',
        nickname: ''
      },
      // 用户评价相关
      reviewsDialogVisible: false,
      userReviews: [],
      reviewsCurrentPage: 1,
      reviewsTotalCount: 0,
      reviewsLoading: true,
      // 当前用户
      currentUser: null,
    }
  },
  mounted() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      try {
        const response = await axios.get('/users', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        })
        this.users = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
      } catch (error) {
        console.error('加载用户失败:', error)
        this.$message.error('加载用户失败')
      }
    },
    async searchUsers() {
      this.currentPage = 1
      if (!this.searchKeyword) {
        this.loadUsers()
        return
      }
      try {
        const response = await axios.get('/users/search', {
          params: {
            keyword: this.searchKeyword,
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        })
        this.users = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
      } catch (error) {
        console.error('搜索用户失败:', error)
        this.$message.error('搜索用户失败')
      }
    },
    handlePageChange(page) {
      this.currentPage = page
      if (this.searchKeyword) {
        this.searchUsers()
      } else {
        this.loadUsers()
      }
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      if (this.searchKeyword) {
        this.searchUsers()
      } else {
        this.loadUsers()
      }
    },
    showAddDialog() {
      this.isEdit = false
      this.form = {
        userMd5: '',
        nickname: ''
      }
      this.dialogVisible = true
    },
    showEditDialog(user) {
      this.isEdit = true
      this.form = { ...user }
      this.dialogVisible = true
    },
    async saveUser() {
      try {
        let response
        if (this.isEdit) {
          response = await axios.put('/users', this.form)
        } else {
          response = await axios.post('/users', this.form)
        }
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.dialogVisible = false
          this.loadUsers()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        console.error('保存用户失败:', error)
        this.$message.error('保存失败')
      }
    },
    async deleteUser(id) {
      try {
        await this.$confirm('确定要删除这个用户吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await axios.delete(`/users/${id}`)
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.loadUsers()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除用户失败:', error)
          this.$message.error('删除失败')
        }
      }
    },
    showUserReviews(user) {
      this.currentUser = user
      this.reviewsCurrentPage = 1
      this.reviewsDialogVisible = true
      this.loadUserReviews()
    },
    async loadUserReviews() {
      this.reviewsLoading = true
      try {
        const response = await axios.get(`/reviews/user/${this.currentUser.userMd5}`, {
          params: {
            pageNum: this.reviewsCurrentPage,
            pageSize: this.pageSize
          }
        })
        this.userReviews = response.data.data
        this.reviewsTotalCount = response.data.totalCount
      } catch (error) {
        console.error('加载用户评价失败:', error)
        this.$message.error('加载评价失败')
      } finally {
        this.reviewsLoading = false
      }
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '——'
      return dateTime.replace('T', ' ').substring(0, 19)
    },
    getScoreClass(score) {
      if (!score) return ''
      if (score >= 8) return 'score-high'
      if (score >= 6) return 'score-medium'
      return 'score-low'
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 0;
  padding-bottom: 80px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
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
.user-management > .pagination-container {
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
</style>
