﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
  <div class="person-management">
    <div class="content-wrapper">
      <div class="toolbar">
        <el-autocomplete
          v-model="searchKeyword"
          :fetch-suggestions="querySearchSuggestions"
          placeholder="搜索人员姓名/职业..."
          style="width: 360px"
          clearable
          @select="handleSuggestionSelect"
          @clear="handleSearchClear"
          @keyup.enter="searchPersons"
          size="default"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button type="primary" @click="searchPersons" class="search-btn">搜索</el-button>
          </template>
          <template #default="{ item }">
            <div class="suggestion-item">
              <span class="suggestion-name">{{ item.value }}</span>
              <span v-if="item.type" class="suggestion-type">{{ item.type }}</span>
            </div>
          </template>
        </el-autocomplete>
        <el-button type="primary" @click="showAddDialog">添加人员</el-button>
        <el-button @click="loadPersons">刷新</el-button>
      </div>

      <div class="table-wrapper">
        <el-table :data="persons" border style="width: 100%; height: 100%;" :empty-text="loading ? '' : '暂无数据'" v-loading="loading" loading-text="加载中，请稍候...">
      <el-table-column prop="personId" label="人员ID" width="120"></el-table-column>
      <el-table-column prop="name" label="姓名" width="120">
        <template #default="scope">
          {{ scope.row.name || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="nameZh" label="中文名" width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.nameZh || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="nameEn" label="别名" width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.nameEn || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.sex === '男' ? 'primary' : 'success'">{{ scope.row.sex || '——' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="birth" label="出生日期" width="120">
        <template #default="scope">
          {{ scope.row.birth || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="birthplace" label="出生地" width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.birthplace || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="profession" label="职业" width="150" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.profession || '——' }}
        </template>
      </el-table-column>
      <el-table-column prop="biography" label="简介" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.biography || '——' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" @click="showEditDialog(scope.row)">编辑</el-button>
          <el-button type="danger" size="small" @click="deletePerson(scope.row.personId)">删除</el-button>
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
      :title="isEdit ? '编辑人员' : '添加人员'"
      width="700px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="人员ID">
          <el-input v-model="form.personId"></el-input>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="姓名">
              <el-input v-model="form.name"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="别名">
              <el-input v-model="form.nameEn"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="中文名">
              <el-input v-model="form.nameZh"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.sex" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" value="男"></el-option>
                <el-option label="女" value="女"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birth"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出生地">
              <el-input v-model="form.birthplace"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="职业">
          <el-input v-model="form.profession"></el-input>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.biography" type="textarea" :rows="4"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerson">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import axios from '../utils/axios'
import { Search } from '@element-plus/icons-vue'

export default {
  name: 'PersonManagement',
  components: { Search },
  data() {
    return {
      persons: [],
      loading: true,
      searchKeyword: '',
      dialogVisible: false,
      isEdit: false,
      currentPage: 1,
      pageSize: 10,
      totalCount: 0,
      totalPages: 0,
      form: {
        personId: '',
        name: '',
        sex: '',
        nameEn: '',
        nameZh: '',
        birth: '',
        birthplace: '',
        profession: '',
        biography: ''
      }
    }
  },
  mounted() {
    this.loadPersons()
  },
  methods: {
    async loadPersons() {
      this.loading = true
      try {
        const response = await axios.get('/persons', {
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        })
        this.persons = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
      } catch (error) {
        console.error('加载人员失败:', error)
        this.$message.error('加载人员失败')
      } finally {
        this.loading = false
      }
    },
    async searchPersons() {
      this.currentPage = 1
      if (!this.searchKeyword) {
        this.loadPersons()
        return
      }
      try {
        const response = await axios.get('/persons/search', {
          params: {
            keyword: this.searchKeyword,
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        })
        this.persons = response.data.data
        this.totalCount = response.data.totalCount
        this.totalPages = response.data.totalPages
      } catch (error) {
        console.error('搜索人员失败:', error)
        this.$message.error('搜索人员失败')
      }
    },
    async querySearchSuggestions(queryString, cb) {
      if (!queryString) {
        cb([])
        return
      }
      clearTimeout(this._searchTimer)
      this._searchTimer = setTimeout(async () => {
        try {
          const personRes = await axios.get('/persons/search', {
            params: { keyword: queryString, pageNum: 1, pageSize: 8 }
          })
          const movieRes = await axios.get('/movies/search', {
            params: { keyword: queryString, pageNum: 1, pageSize: 5 }
          })
          const suggestions = []
          const persons = personRes.data?.data || []
          const movies = movieRes.data?.data || []
          persons.forEach(p => {
            suggestions.push({
              value: p.name,
              type: p.profession ? p.profession : '人物',
              person: p
            })
          })
          movies.forEach(m => {
            suggestions.push({
              value: m.name,
              type: m.year ? `电影 · ${m.year}` : '电影',
              movie: m
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
      this.searchPersons()
    },
    handleSearchClear() {
      this.searchKeyword = ''
      this.loadPersons()
    },
    handlePageChange(page) {
      this.currentPage = page
      if (this.searchKeyword) {
        this.searchPersons()
      } else {
        this.loadPersons()
      }
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      if (this.searchKeyword) {
        this.searchPersons()
      } else {
        this.loadPersons()
      }
    },
    showAddDialog() {
      this.isEdit = false
      this.form = {
        personId: '',
        name: '',
        sex: '',
        nameEn: '',
        nameZh: '',
        birth: '',
        birthplace: '',
        profession: '',
        biography: ''
      }
      this.dialogVisible = true
    },
    showEditDialog(person) {
      this.isEdit = true
      this.form = { ...person }
      this.dialogVisible = true
    },
    async savePerson() {
      try {
        let response
        if (this.isEdit) {
          response = await axios.put('/persons', this.form)
        } else {
          response = await axios.post('/persons', this.form)
        }
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.dialogVisible = false
          this.loadPersons()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        console.error('保存人员失败:', error)
        this.$message.error('保存失败')
      }
    },
    async deletePerson(id) {
      try {
        await this.$confirm('确定要删除这个人员吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        const response = await axios.delete(`/persons/${id}`)
        if (response.data.code === '200') {
          this.$message.success(response.data.message)
          this.loadPersons()
        } else {
          this.$message.error(response.data.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除人员失败:', error)
          this.$message.error('删除失败')
        }
      }
    }
  }
}
</script>

<style scoped>
.person-management {
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
