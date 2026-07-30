<template>
    <div class="login-container">
        <div class="login-box">
            <div class="logo-section">
                <el-icon :size="48" color="#409eff"><Film /></el-icon>
                <h1>电影管理系统</h1>
                <p>欢迎登录</p>
            </div>

            <el-tabs v-model="activeTab" class="login-tabs">
                <el-tab-pane label="登录" name="login">
                    <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" label-width="80px">
                        <el-form-item label="用户名" prop="username">
                            <el-input v-model="loginForm.username" placeholder="请输入用户名">
                                <template #prefix>
                                    <el-icon><User /></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item label="密码" prop="password">
                            <el-input v-model="loginForm.password" :type="showPasswords.login ? 'text' : 'password'" placeholder="请输入密码" @keyup.enter="handleLogin">
                                <template #prefix>
                                    <el-icon><Lock /></el-icon>
                                </template>
                                <template #suffix>
                                    <span @click="togglePassword('login')" style="cursor: pointer; padding: 4px;">👁</span>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loginLoading">登录</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>

                <el-tab-pane label="注册" name="register">
                    <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-width="80px">
                        <el-form-item label="用户名" prop="username">
                            <el-input v-model="registerForm.username" placeholder="请输入用户名">
                                <template #prefix>
                                    <el-icon><User /></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item label="密码" prop="password">
                            <el-input v-model="registerForm.password" :type="showPasswords.register ? 'text' : 'password'" placeholder="请输入密码">
                                <template #prefix>
                                    <el-icon><Lock /></el-icon>
                                </template>
                                <template #suffix>
                                    <span @click="togglePassword('register')" style="cursor: pointer; padding: 4px;">👁</span>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item label="确认密码" prop="confirmPassword">
                            <el-input v-model="registerForm.confirmPassword" :type="showPasswords.confirm ? 'text' : 'password'" placeholder="请再次输入密码">
                                <template #prefix>
                                    <el-icon><Lock /></el-icon>
                                </template>
                                <template #suffix>
                                    <span @click="togglePassword('confirm')" style="cursor: pointer; padding: 4px;">👁</span>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item label="昵称" prop="nickname">
                            <el-input v-model="registerForm.nickname" placeholder="请输入昵称">
                                <template #prefix>
                                    <el-icon><Avatar /></el-icon>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" class="login-btn" @click="handleRegister" :loading="registerLoading">注册</el-button>
                        </el-form-item>
                    </el-form>
                </el-tab-pane>
            </el-tabs>
        </div>
    </div>
</template>

<script>
import axios from '../utils/axios'
import { Film, User, Lock, Avatar } from '@element-plus/icons-vue'

export default {
    name: 'LoginRegister',
    components: { Film, User, Lock, Avatar },
    data() {
        return {
            activeTab: 'login',
            loginLoading: false,
            registerLoading: false,
            showPasswords: {
                login: false,
                register: false,
                confirm: false
            },
            loginForm: {
                username: '',
                password: ''
            },
            registerForm: {
                username: '',
                password: '',
                confirmPassword: '',
                nickname: ''
            },
            loginRules: {
                username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            },
            registerRules: {
                username: [
                    { required: true, message: '请输入用户名', trigger: 'blur' },
                    { min: 3, max: 20, message: '用户名长度在3-20之间', trigger: 'blur' }
                ],
                password: [
                    { required: true, message: '请输入密码', trigger: 'blur' },
                    { min: 6, max: 20, message: '密码长度在6-20之间', trigger: 'blur' }
                ],
                confirmPassword: [
                    { required: true, message: '请确认密码', trigger: 'blur' },
                    { validator: this.validateConfirmPassword, trigger: 'blur' }
                ],
                nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
            }
        }
    },
    methods: {
        togglePassword(type) {
            this.showPasswords[type] = !this.showPasswords[type]
        },
        validateConfirmPassword(rule, value, callback) {
            if (value !== this.registerForm.password) {
                callback(new Error('两次输入的密码不一致'))
            } else {
                callback()
            }
        },
        async handleLogin() {
            const valid = await this.$refs.loginFormRef.validate()
            if (!valid) return
            
            this.loginLoading = true
            try {
                const response = await axios.post('/auth/login', {
                    username: this.loginForm.username,
                    password: this.loginForm.password
                })
                
                if (response.data.code === '200' || response.data.success) {
                    const { token, username, nickname, role, hasAvatar } = response.data.data
                    sessionStorage.setItem('token', token)
                    sessionStorage.setItem('username', username)
                    sessionStorage.setItem('nickname', nickname)
                    sessionStorage.setItem('role', role)
                    sessionStorage.setItem('hasAvatar', hasAvatar === 'true' ? 'true' : 'false')
                    
                    this.$message.success('登录成功')
                    this.redirectByRole(role)
                } else {
                    this.$message.error(response.data.message || '登录失败')
                }
            } catch (error) {
                console.error('登录失败:', error)
                this.$message.error('登录失败，请检查用户名和密码')
            } finally {
                this.loginLoading = false
            }
        },
        async handleRegister() {
            const valid = await this.$refs.registerFormRef.validate()
            if (!valid) return
            
            this.registerLoading = true
            try {
                const response = await axios.post('/auth/register', {
                    username: this.registerForm.username,
                    password: this.registerForm.password,
                    nickname: this.registerForm.nickname
                })
                
                if (response.data.code === '200') {
                    this.$message.success('注册成功，请登录')
                    this.switchToLogin()
                } else {
                    this.$message.error(response.data.message || '注册失败')
                }
            } catch (error) {
                console.error('注册失败:', error)
                this.$message.error('注册失败')
            } finally {
                this.registerLoading = false
            }
        },
        redirectByRole(role) {
            setTimeout(() => {
                this.$router.push(role === 'ADMIN' ? '/admin/movies' : '/user')
            }, 300)
        },
        switchToLogin() {
            this.activeTab = 'login'
            this.registerForm = { username: '', password: '', confirmPassword: '', nickname: '' }
            this.showPasswords.register = false
            this.showPasswords.confirm = false
        }
    }
}
</script>

<style scoped>
.login-container {
    min-height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
    width: 450px;
    background: white;
    border-radius: 20px;
    padding: 40px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.logo-section {
    text-align: center;
    margin-bottom: 30px;
}

.logo-section h1 {
    margin: 15px 0 5px 0;
    font-size: 28px;
    color: #303133;
}

.logo-section p {
    color: #909399;
    font-size: 14px;
}

.login-tabs {
    :deep(.el-tabs__header) { margin-bottom: 25px; }
    :deep(.el-tabs__item) { font-size: 16px; }
}

.login-btn {
    width: 100%;
    height: 44px;
    font-size: 16px;
    border-radius: 8px;
    transition: all 0.3s ease;
}

.login-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
</style>
