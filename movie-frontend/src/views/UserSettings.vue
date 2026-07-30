<template>
    <div class="user-settings">
        <el-tabs v-model="activeTab" class="settings-tabs" :tab-position="'left'">
            <!-- 个人信息 -->
            <el-tab-pane label="个人信息" name="profile">
                <div class="settings-section">
                    <h3>个人信息</h3>
                    <el-form label-width="110px" class="settings-form">
                        <el-form-item label="用户名">
                            <el-input v-model="username" disabled style="width: 300px;" />
                        </el-form-item>
                        <el-form-item label="用户ID">
                            <div class="user-id-row">
                                <el-input v-model="userMd5" disabled style="width: 300px;" />
                                <el-button size="small" @click="copyUserId" class="copy-btn">复制</el-button>
                            </div>
                        </el-form-item>
                        <el-form-item label="当前昵称">
                            <el-input v-model="currentNickname" disabled style="width: 300px;" />
                        </el-form-item>
                        <el-form-item label="新昵称">
                            <el-input v-model="profileForm.nickname" placeholder="请输入新昵称" style="width: 300px;" />
                        </el-form-item>
                        <el-form-item label="当前密码">
                            <el-input 
                                v-model="profileForm.password" 
                                type="password" 
                                show-password
                                placeholder="输入当前密码以验证身份" 
                                style="width: 300px;" 
                            />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="saveProfile" :loading="profileSubmitting">
                                保存修改
                            </el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </el-tab-pane>

            <!-- 修改密码 -->
            <el-tab-pane label="修改密码" name="password">
                <div class="settings-section">
                    <h3>修改密码</h3>
                    <el-form label-width="120px" class="settings-form">
                        <el-form-item label="当前密码">
                            <el-input 
                                v-model="passwordForm.oldPassword" 
                                type="password" 
                                show-password
                                placeholder="请输入当前密码" 
                                style="width: 300px;" 
                            />
                        </el-form-item>
                        <el-form-item label="新密码">
                            <el-input 
                                v-model="passwordForm.newPassword" 
                                type="password" 
                                show-password
                                placeholder="请输入新密码（至少6位）" 
                                style="width: 300px;" 
                            />
                        </el-form-item>
                        <el-form-item label="确认新密码">
                            <el-input 
                                v-model="passwordForm.confirmPassword" 
                                type="password" 
                                show-password
                                placeholder="请再次输入新密码" 
                                style="width: 300px;" 
                            />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="savePassword" :loading="passwordSubmitting">
                                修改密码
                            </el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </el-tab-pane>

            <!-- 头像设置 -->
            <el-tab-pane label="头像设置" name="avatar">
                <div class="settings-section">
                    <h3>头像设置</h3>
                    <div class="avatar-upload-section">
                        <div class="avatar-preview">
                            <div v-if="avatarPreview" class="avatar-img" :style="{ backgroundImage: `url(${avatarPreview})` }"></div>
                            <div v-else class="avatar-placeholder">
                                <el-icon :size="48" color="#ccc"><User /></el-icon>
                            </div>
                        </div>
                        <div class="avatar-upload-actions">
                            <el-upload
                                ref="uploadRef"
                                :show-file-list="false"
                                @change="onAvatarFileChange"
                                :auto-upload="false"
                                accept="image/jpeg,image/png,image/gif,image/webp"
                            >
                                <template #trigger>
                                    <el-button type="primary">选择图片</el-button>
                                </template>
                            </el-upload>
                            <p class="upload-tip">支持 JPG/PNG/GIF/WebP，建议 200x200 以内</p>
                            <div v-if="avatarPreview" class="avatar-actions">
                                <el-input 
                                    v-model="avatarPassword" 
                                    type="password" 
                                    show-password
                                    placeholder="输入当前密码以验证身份" 
                                    style="width: 220px;" 
                                />
                                <div class="avatar-btn-row">
                                    <el-button type="primary" @click="saveAvatar" :loading="avatarSubmitting">保存头像</el-button>
                                    <el-button @click="clearAvatar">取消</el-button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/axios'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import md5 from '../utils/md5'

export default {
    name: 'UserSettings',
    components: { User },
    setup() {
        const router = useRouter()
        const activeTab = ref('profile')

        // 个人信息
        const username = ref('')
        const userMd5 = ref('')
        const currentNickname = ref('')
        const profileForm = ref({ nickname: '', password: '' })
        const profileSubmitting = ref(false)

        // 密码
        const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
        const passwordSubmitting = ref(false)

        // 头像
        const uploadRef = ref(null)
        const avatarPreview = ref('')
        const avatarBase64 = ref('')
        const avatarPassword = ref('')
        const avatarSubmitting = ref(false)

        onMounted(() => {
            username.value = sessionStorage.getItem('username') || ''
            userMd5.value = md5(username.value)
            currentNickname.value = sessionStorage.getItem('nickname') || ''
            const hasSavedAvatar = sessionStorage.getItem('hasAvatar') === 'true'
            if (hasSavedAvatar) {
                avatarPreview.value = `/api/auth/avatar?username=${sessionStorage.getItem('username')}&v=${Date.now()}`
            }
        })

        // 复制用户ID
        function copyUserId() {
            navigator.clipboard.writeText(userMd5.value).then(() => {
                ElMessage.success('用户ID已复制')
            }).catch(() => {
                ElMessage.error('复制失败')
            })
        }

        // 保存昵称
        async function saveProfile() {
            if (!profileForm.value.nickname.trim()) {
                ElMessage.warning('请输入新昵称')
                return
            }
            if (!profileForm.value.password) {
                ElMessage.warning('请输入当前密码以验证身份')
                return
            }
            profileSubmitting.value = true
            try {
                const response = await axios.put('/auth/profile', {
                    nickname: profileForm.value.nickname.trim(),
                    password: profileForm.value.password
                })
                if (response.data.code === '200') {
                    const newNickname = response.data.data.nickname
                    sessionStorage.setItem('nickname', newNickname)
                    currentNickname.value = newNickname
                    profileForm.value = { nickname: '', password: '' }
                    ElMessage.success('昵称修改成功')
                } else {
                    ElMessage.error(response.data.message)
                }
            } catch (error) {
                console.error('修改昵称失败:', error)
                ElMessage.error('修改失败')
            } finally {
                profileSubmitting.value = false
            }
        }

        // 修改密码
        async function savePassword() {
            const { oldPassword, newPassword, confirmPassword } = passwordForm.value
            if (!oldPassword) {
                ElMessage.warning('请输入当前密码')
                return
            }
            if (!newPassword || newPassword.length < 6) {
                ElMessage.warning('新密码长度不能少于6位')
                return
            }
            if (newPassword !== confirmPassword) {
                ElMessage.warning('两次输入的新密码不一致')
                return
            }
            passwordSubmitting.value = true
            try {
                const response = await axios.put('/auth/password', {
                    oldPassword,
                    newPassword
                })
                if (response.data.code === '200') {
                    ElMessage.success('密码修改成功，请重新登录')
                    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
                    // 跳转登录页
                    setTimeout(() => {
                        sessionStorage.clear()
                        router.push('/login')
                    }, 1500)
                } else {
                    ElMessage.error(response.data.message)
                }
            } catch (error) {
                console.error('修改密码失败:', error)
                ElMessage.error('修改失败')
            } finally {
                passwordSubmitting.value = false
            }
        }

        // 选择图片后处理
        function onAvatarFileChange(uploadFile) {
            const file = uploadFile.raw
            if (!file) return
            const isImage = file.type.startsWith('image/')
            const isLt2M = file.size / 1024 / 1024 < 2
            if (!isImage) {
                ElMessage.error('请选择图片文件')
                return
            }
            if (!isLt2M) {
                ElMessage.error('图片大小不能超过 2MB')
                return
            }
            // 读取文件为 base64 预览
            const reader = new FileReader()
            reader.onload = (e) => {
                avatarBase64.value = e.target.result
                avatarPreview.value = e.target.result
            }
            reader.readAsDataURL(file)
        }

        // 保存头像
        async function saveAvatar() {
            if (!avatarBase64.value) {
                ElMessage.warning('请先选择头像图片')
                return
            }
            if (!avatarPassword.value) {
                ElMessage.warning('请输入当前密码以验证身份')
                return
            }
            avatarSubmitting.value = true
            try {
                const response = await axios.put('/auth/avatar', {
                    avatar: avatarBase64.value,
                    password: avatarPassword.value
                })
                if (response.data.code === '200') {
                    sessionStorage.setItem('hasAvatar', 'true')
                    avatarPassword.value = ''
                    avatarBase64.value = ''
                    ElMessage.success('头像更新成功')
                    ElMessage.info('刷新页面即可看到新头像')
                } else {
                    ElMessage.error(response.data.message)
                }
            } catch (error) {
                console.error('上传头像失败:', error)
                ElMessage.error('上传失败')
            } finally {
                avatarSubmitting.value = false
            }
        }

        // 清除头像选择
        function clearAvatar() {
            avatarBase64.value = ''
            avatarPreview.value = ''
            if (uploadRef.value) {
                uploadRef.value.clearFiles()
            }
        }

        return {
            activeTab,
            username,
            userMd5,
            currentNickname,
            profileForm,
            profileSubmitting,
            passwordForm,
            passwordSubmitting,
            uploadRef,
            avatarPreview,
            avatarPassword,
            avatarSubmitting,
            saveProfile,
            savePassword,
            onAvatarFileChange,
            saveAvatar,
            clearAvatar,
            copyUserId
        }
    }
}
</script>

<style scoped>
.user-settings {
    max-width: 800px;
    margin: 0 auto;
    background: #fff;
    border-radius: 16px;
    padding: 30px;
    min-height: 500px;
}

.settings-tabs {
    min-height: 450px;
}

.settings-section {
    padding: 0 20px;
}

.settings-section h3 {
    font-size: 20px;
    color: #303133;
    margin-bottom: 30px;
    padding-bottom: 15px;
    border-bottom: 2px solid #ecf0f1;
}

.settings-form {
    max-width: 500px;
}

.avatar-upload-section {
    display: flex;
    gap: 40px;
    align-items: flex-start;
}

.avatar-preview {
    width: 150px;
    height: 150px;
    border-radius: 50%;
    overflow: hidden;
    border: 3px solid #e0e0e0;
    flex-shrink: 0;
}

.avatar-img {
    width: 100%;
    height: 100%;
    background-size: cover;
    background-position: center;
}

.avatar-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
}

.avatar-upload-actions {
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.upload-tip {
    font-size: 13px;
    color: #909399;
    margin: 0;
}

.avatar-actions {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.avatar-btn-row {
    display: flex;
    gap: 10px;
}

.user-id-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.copy-btn {
    flex-shrink: 0;
}
</style>
