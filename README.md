# Movie-Score
基于 Vue3 + SpringBoot3 + MySQL 的全栈电影智能分析综合系统

## 一、项目概述
### 1.1 项目名称
电影智能管理与分析系统（Movie Management System）

### 1.2 项目描述
本项目是基于 Spring Boot + Vue 3 构建的全栈电影管理与智能分析系统。集成四大机器学习能力：ALS协同过滤个性化推荐、K-Means++用户画像聚类、ARIMA时间序列产量预测、DeepSeek大模型评论情感分析。
完整实现电影信息管理、评分评论、个性化推荐、多维数据可视化分析，适用于课程设计、毕业设计。

### 1.3 技术栈
| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 后端框架 | Spring Boot | 3.x | Java Web后端 |
| 数据库 | MySQL | 8.x | 关系型数据库 |
| ORM | MyBatis | 3.x | 数据持久层 |
| 权限认证 | Spring Security + JWT | - | 登录鉴权 |
| 密码加密 | BCrypt | - | 用户密码哈希存储 |
| 前端框架 | Vue 3 | 3.4.x | 前端主体框架 |
| UI组件 | Element Plus | 2.6.x | 后台管理组件库 |
| 可视化 | ECharts | 6.1.x | 图表展示 |
| 路由 | Vue Router | 4.3.x | 前端路由控制 |
| HTTP请求 | Axios | 1.6.x | 前后端接口交互 |
| 构建工具 | Vite | 5.1.x | 前端打包 |
| 聚类算法 | Apache Commons Math3 | - | K-Means++用户分群 |
| 大数据处理 | PySpark | - | 数据集预处理 |
| 大模型 | DeepSeek API | - | 评论情感分析 |
| 时序预测 | statsmodels ARIMA | - | 电影产量趋势预测 |
| 数据计算 | Pandas / NumPy / SciPy | - | 数值分析 |
| 后端运行环境 | JDK | 17+ | Java运行依赖 |
| 数据分析环境 | Python | 3.10 | 算法脚本运行 |
Movie-Score/
├─ movie-api/ # SpringBoot 后端项目
├─ movie-frontend/ # Vue3 前端项目
├─ python_analysis/ # Python 算法脚本、图表、notebook
├─ .gitignore # Git 忽略配置
└─ README.md # 项目说明文档
### 1.4 项目整体目录结构

#### 后端 movie-api 结构

#### 前端 movie-frontend 结构

#### python_analysis 数据分析目录
存放全部Python算法脚本、可视化图表、Jupyter笔记、数据库驱动jar包：
- ALS协同过滤训练脚本
- ARIMA时序预测脚本
- 数据校验、数据预处理脚本
- 情感分析调用脚本
- 各类分析结果图片

## 二、后端控制器功能说明
### 2.1 AuthController 登录注册模块
基础路径：`/api/auth`
| 请求方式 | 接口地址 | 功能 |
| ---- | ---- | ---- |
| POST | /register | 用户注册 |
| POST | /login | 用户登录，返回JWT令牌 |
| PUT | /profile | 修改用户昵称 |
| PUT | /password | 修改登录密码 |
| GET | /avatar | 获取用户头像 |
| PUT | /avatar | 上传更新头像 |
| POST | /import-users | 批量导入测试用户 |
核心特性：BCrypt密码加密、JWT有效期1小时、用户名MD5匿名化存储

### 2.2 MovieController 电影管理模块
基础路径：`/api/movies`
提供电影新增、删除、编辑、分页查询、关键词搜索、多条件筛选接口。

### 2.3 CommentController 评论模块
基础路径：`/api/comments`
电影评论增删改查、按电影/用户筛选评论。

### 2.4 RatingController 评分模块
基础路径：`/api/ratings`
用户电影1-5星评分记录管理、按用户/电影筛选评分数据。

### 2.5 智能算法相关控制器
1. MoviePredictionController：电影评分预测列表筛选
2. RecommendationController：ALS协同过滤个性化推荐接口
3. SentimentAnalysisController：DeepSeek批量评论情感分析
4. TrendForecastController：ARIMA电影产量趋势预测
5. UserProfileController：K-Means++用户画像聚类计算

### 2.6 其他控制器
- UserController：普通评分用户管理
- PersonController：导演、演员影人信息管理
- SysUserController：后台管理员账号管理
- MovieStatsController：电影数据统计接口
- UserHistoryController：用户浏览、评价历史记录

## 三、四层业务架构（Controller-Service-Mapper-Entity）
### Service业务层
封装所有业务逻辑：用户登录校验、电影检索、ALS模型加载、聚类计算、大模型批量请求、历史记录维护。

### Mapper数据访问层
MyBatis映射，提供所有数据表CRUD、聚合统计、多条件分页查询。

### Entity实体层
包含电影、用户、管理员、评论、评分、影人、推荐结果、聚类结果、情感分析记录等14个数据表实体，配套通用分页返回对象`PageResult<T>`。

## 四、全局配置与工具类
1. **CorsConfig**：全局跨域放行
2. **SecurityConfig**：Spring安全配置，关闭CSRF
3. **WebConfig**：注册JWT拦截器，区分公开/需登录接口
4. **DatabaseInitConfig**：项目启动自动建表、创建索引、统一数据库字符集utf8mb4
5. **JwtUtil**：JWT令牌生成、校验、解析工具
6. **Result<T>**：全局统一接口返回格式（状态码、消息、数据、时间戳）

## 五、前端页面模块
### 用户端页面（普通观影用户）
1. UserDashboard：首页电影浏览、ALS猜你喜欢推荐、我的评分/评论
2. UserPrediction：电影预测评分列表筛选
3. UserRecommendations：个性化推荐展示，支持筛选高分、切换推荐
4. UserTrendForecast：ARIMA产量预测折线图表
5. SentimentAnalysis：电影评论情感分布饼图
6. UserSettings：个人昵称、密码、头像修改

### 管理员端页面（后台运营）
1. MovieManagement：电影全量CRUD管理
2. PersonManagement：影人导演演员管理
3. UserManagement：评分用户列表
4. RatingManagement：所有用户评分记录
5. CommentManagement：全量评论管理
6. SystemUserManagement：后台管理员账号角色管理
7. AdminStatsAnalysis：电影数据统计、用户聚类画像可视化
8. AdminRecommendations：查询任意用户的ALS推荐列表

## 六、四大核心算法实现
### 6.1 ALS 交替最小二乘法协同过滤推荐
1. 读取MySQL评分数据，过滤低频用户
2. 构建用户-电影稀疏评分矩阵训练30维隐因子模型
3. 保存用户、影片向量二进制文件，后端启动加载至内存
4. 在线向量点积计算预测评分，返回Top20个性化推荐

### 6.2 K-Means++ 用户画像聚类
提取用户多维特征：平均评分、评分数、评分波动标准差、活跃天数；
使用肘部法则自动计算最优聚类K值，对用户分群，批量存入数据库用于分层运营分析。

### 6.3 ARIMA 时间序列趋势预测
按年份统计历年电影产出，自动匹配最优ARIMA阶数，预测未来3年电影产量，前端折线图可视化展示历史+预测数据。

### 6.4 DeepSeek 大模型情感分析
批量传入电影评论，区分positive积极/neutral中立/negative消极情感，返回0~1置信度；批量缓存避免重复调用API，降低调用成本。

## 七、开发阶段规划
1. **第一阶段 基础架构**
搭建前后端项目骨架、设计数据库、JWT登录认证、跨域与安全配置。
2. **第二阶段 基础CRUD功能**
电影、影人、用户、评分、评论、管理员账号完整增删改查。
3. **第三阶段 ALS推荐算法**
Python训练推荐模型、后端加载模型、前端推荐页面开发。
4. **第四阶段 智能分析模块**
K-Means用户聚类、ARIMA产量预测、DeepSeek评论情感分析、数据可视化图表。
5. **第五阶段 优化完善**
用户浏览历史、头像上传、索引性能优化、UI美化、项目部署文档。

## 八、系统核心亮点
1. **个性化推荐**：ALS矩阵分解，内存高速计算，支持多维度筛选
2. **AI情感分析**：接入DeepSeek大模型，批量处理评论、结果缓存
3. **用户分群画像**：K-Means++自动最优聚类，多维用户特征可视化
4. **时序产量预测**：ARIMA模型预判电影市场产出趋势
5. **完整权限体系**：JWT登录、双角色区分普通用户/管理员、路由权限拦截
6. **安全机制**：BCrypt密码加密、用户名MD5匿名存储、跨域防护
7. **完整可视化**：ECharts折线、饼图、统计卡片，骨架屏加载优化交互

## 九、API接口总览
### 认证接口
用户注册、登录、修改昵称/密码、头像上传、批量导入用户

### 电影业务接口
电影分页、详情、关键词搜索、多条件筛选、新增编辑删除

### 评论&评分接口
评论/评分新增、删除、按电影/用户筛选查询

### 智能分析接口
ALS个性化推荐、电影评分预测、评论情感分析、ARIMA趋势预测、用户聚类计算

### 后台管理接口
影人、普通用户、管理员账号、数据统计、用户历史记录全量管理

## 十、部署说明
### 10.1 环境依赖
JDK17+、Node.js18+、MySQL8.0+、Python3.10、Maven3.8+

### 10.2 数据库初始化
执行SQL创建`movie_db`库（字符集utf8mb4），导入影片、评分、评论原始CSV数据集；运行Python预处理脚本清洗数据导入数据表。

### 10.3 后端部署
1. 修改`application.properties`数据库连接、DeepSeek密钥
2. 执行Python脚本训练ALS模型、生成ARIMA预测文件
3. Maven打包`mvn clean package`
4. Java -jar 启动服务，端口8888，自动创建数据表

### 10.4 前端部署
1. npm install 安装依赖
2. npm run dev 本地调试 / npm run build 生产打包
3. dist静态文件部署至Nginx

### 10.5 默认账号
管理员：admin / 123456
普通用户：页面自助注册；批量导入用户默认密码123456

### 10.6 部署注意事项
- ALS模型文件必须放置在`resources/als_model`目录
- ARIMA预测json文件预生成存放资源目录
- 大数据量410万评分查询已添加数据库索引优化速度
- 注释WebConfig拦截器配置可临时关闭登录校验

## 十一、项目总结
本电影智能管理系统是一套完整全栈毕业设计项目，整合Java后端、Vue前端、Python机器学习算法。
完整覆盖基础管理业务+四大主流数据分析算法，实现从数据采集、模型训练、在线接口调用、前端可视化展示全链路工程落地。
分层代码规范、接口统一封装、权限与安全完善，适合学习全栈开发与机器学习工程化落地。
