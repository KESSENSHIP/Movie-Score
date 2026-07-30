import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000, // 超时时间30秒
  retry: 2, // 重试次数
  retryDelay: 1000 // 重试延迟1秒
})

// 请求拦截器 - 添加token
instance.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理token过期和超时重试
instance.interceptors.response.use(
  response => {
    return response
  },
  error => {
    const config = error.config
    
    // 处理token过期
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('username')
      sessionStorage.removeItem('nickname')
      sessionStorage.removeItem('role')
      sessionStorage.removeItem('avatar')
      sessionStorage.removeItem('hasAvatar')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    
    // 超时重试逻辑
    if (!config || !config.retry) {
      return Promise.reject(error)
    }
    
    // 设置重试次数
    config.__retryCount = config.__retryCount || 0
    
    // 如果超过重试次数，放弃重试
    if (config.__retryCount >= config.retry) {
      return Promise.reject(error)
    }
    
    // 增加重试次数
    config.__retryCount += 1
    
    // 创建新的Promise来处理重试
    const backoff = new Promise(resolve => {
      setTimeout(() => {
        resolve()
      }, config.retryDelay || 1000)
    })
    
    // 返回重试请求
    return backoff.then(() => {
      return instance(config)
    })
  }
)

export default instance