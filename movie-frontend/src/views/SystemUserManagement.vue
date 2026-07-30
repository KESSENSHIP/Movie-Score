﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<template>
    <div class="page-container">
        <div class="content-wrapper">
            <div class="search-bar">
                <el-autocomplete
                    v-model="searchKeyword"
                    :fetch-suggestions="querySearchSuggestions"
                    placeholder="搜索用户名/昵称/邮箱..."
                    style="width: 360px"
                    clearable
                    @select="handleSuggestionSelect"
                    @clear="handleSearchClear"
                    @keyup.enter="handleSearch"
                    size="default"
                >
                    <template #prefix>
                        <el-icon><Search /></el-icon>
                    </template>
                    <template #append>
                        <el-button type="primary" @click="handleSearch">搜索</el-button>
                    </template>
                    <template #default="{ item }">
                        <div class="suggestion-item">
                            <span class="suggestion-name">{{ item.value }}</span>
                            <span v-if="item.type" class="suggestion-type">{{ item.type }}</span>
                        </div>
                    </template>
                </el-autocomplete>
                <el-button type="primary" @click="showAddDialog">添加用户</el-button>
                <el-button @click="loadUsers">刷新</el-button>
            </div>

            <div class="table-wrapper">
                <el-table :data="users" border style="width: 100%; height: 100%;" :empty-text="loading ? '' : '暂无数据'" v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="150" />
            <el-table-column label="用户ID" width="200">
                <template #default="scope">
                    <span class="user-md5-text">{{ getUserMd5(scope.row.username) }}</span>
                </template>
            </el-table-column>
            <el-table-column prop="nickname" label="昵称" width="150" />
            <el-table-column prop="email" label="邮箱" width="200" />
            <el-table-column prop="role" label="角色" width="120">
                <template #default="scope">
                    <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'info'">
                        {{ scope.row.role === 'ADMIN' ? '系统管理员' : '用户' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                        {{ scope.row.status === 1 ? '启用' : '禁用' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="scope">
                    {{ formatTime(scope.row.createdAt) }}
                </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="180">
                <template #default="scope">
                    {{ formatTime(scope.row.updatedAt) }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="scope">
                    <el-button type="primary" size="small" @click="showEditDialog(scope.row)">编辑</el-button>
                    <el-button type="danger" size="small" @click="deleteUser(scope.row)">删除</el-button>
                </template>
            </el-table-column>
                </el-table>
            </div>
        </div>

        <div class="pagination-container">
            <el-pagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="totalCount"
                layout="prev, pager, next, jumper"
                @current-change="handlePageChange"
            />
        </div>

        <!-- 添加/编辑用户对话框 -->
        <el-dialog
            v-model="dialogVisible"
            :title="isEdit ? '编辑用户' : '添加用户'"
            width="500px"
        >
            <el-form :model="form" label-width="100px">
                <el-form-item label="用户名" required>
                    <el-input v-model="form.username" :disabled="isEdit"></el-input>
                </el-form-item>
                <el-form-item label="密码" :required="!isEdit">
                    <el-input v-model="form.password" type="password" :placeholder="isEdit ? '不填则保持原密码' : '请输入密码'"></el-input>
                </el-form-item>
                <el-form-item label="昵称">
                    <el-input v-model="form.nickname"></el-input>
                </el-form-item>
                <el-form-item label="邮箱">
                    <el-input v-model="form.email"></el-input>
                </el-form-item>
                <el-form-item label="角色" required>
                    <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
                        <el-option label="用户" value="USER"></el-option>
                        <el-option label="系统管理员" value="ADMIN"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="状态">
                    <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                        <el-option label="启用" :value="1"></el-option>
                        <el-option label="禁用" :value="0"></el-option>
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveUser">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import axios from '../utils/axios'
import md5 from '../utils/md5'
import { Search } from '@element-plus/icons-vue'

export default {
    name: 'SystemUserManagement',
    components: { Search },
    data() {
        return {
            users: [],
            currentPage: 1,
            pageSize: 8,
            totalCount: 0,
            searchKeyword: '',
            loading: true,
            // 对话框相关
            dialogVisible: false,
            isEdit: false,
            form: {
                id: null,
                username: '',
                password: '',
                nickname: '',
                email: '',
                role: 'USER',
                status: 1
            }
        }
    },
    mounted() {
        this.loadUsers()
    },
    methods: {
        getUserMd5(username) {
            if (!username) return '——'
            // 导入的用户名本身就是 userMd5（32位16进制字符串）
            if (/^[0-9a-f]{32}$/i.test(username)) {
                return username
            }
            // 注册用户计算 md5(username)
            return md5(username)
        },
        async loadUsers() {
            this.loading = true
            try {
                const response = await axios.get('/sys-users', {
                    params: {
                        pageNum: this.currentPage,
                        pageSize: this.pageSize,
                        keyword: this.searchKeyword || undefined
                    }
                })
                this.users = response.data.data
                this.totalCount = response.data.totalCount
            } catch (error) {
                console.error('加载系统用户失败:', error)
            } finally {
                this.loading = false
            }
        },
        handleSearch() {
            this.currentPage = 1
            this.loadUsers()
        },
        handleSearchClear() {
            this.searchKeyword = ''
            this.handleSearch()
        },
        async querySearchSuggestions(queryString, cb) {
            if (!queryString) {
                cb([])
                return
            }
            clearTimeout(this._searchTimer)
            this._searchTimer = setTimeout(async () => {
                try {
                    const res = await axios.get('/sys-users', {
                        params: { keyword: queryString, pageNum: 1, pageSize: 8 }
                    })
                    const suggestions = []
                    const users = res.data?.data || []
                    users.forEach(u => {
                        suggestions.push({
                            value: u.username,
                            type: u.nickname ? `昵称: ${u.nickname}` : u.role === 'ADMIN' ? '系统管理员' : '用户',
                            user: u
                        })
                    })
                    cb(suggestions)
                } catch (e) {
                    cb([])
                }
            }, 200)
        },
        handleSuggestionSelect(item) {
            this.searchKeyword = item.value
            this.handleSearch()
        },
        handlePageChange(page) {
            this.currentPage = page
            this.loadUsers()
        },
        formatTime(time) {
            if (!time) return '-'
            const date = new Date(time)
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            })
        },
        showAddDialog() {
            this.isEdit = false
            this.form = {
                id: null,
                username: '',
                password: '',
                nickname: '',
                email: '',
                role: 'USER',
                status: 1
            }
            this.dialogVisible = true
        },
        showEditDialog(user) {
            this.isEdit = true
            this.form = {
                id: user.id,
                username: user.username,
                password: '',
                nickname: user.nickname,
                email: user.email,
                role: user.role,
                status: user.status
            }
            this.dialogVisible = true
        },
        async saveUser() {
            if (!this.form.username) {
                this.$message.error('用户名不能为空')
                return
            }
            if (!this.form.role) {
                this.$message.error('角色不能为空')
                return
            }
            if (!this.isEdit && !this.form.password) {
                this.$message.error('密码不能为空')
                return
            }
            try {
                let response
                if (this.isEdit) {
                    response = await axios.put('/sys-users', this.form)
                } else {
                    response = await axios.post('/sys-users', this.form)
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
        async deleteUser(user) {
            try {
                await this.$confirm('确定要删除这个用户吗?', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                })
                const response = await axios.delete(`/sys-users/${user.id}`)
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
        }
    }
}
</script>

<style scoped>
.page-container {
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

.search-bar {
    margin-bottom: 16px;
    display: flex;
    gap: 10px;
    align-items: center;
    flex-shrink: 0;
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

.user-md5-text {
    display: inline-block;
    word-break: break-all;
    white-space: normal;
    font-size: 13px;
    color: #303133;
    line-height: 1.5;
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
