# Movie-Score
使用vue,java,mySql的综合系统
电影管理系统 - 项目文档
一、项目概述
1.1 项目名称
电影智能管理与分析系统（Movie Management System）

1.2 项目描述
本项目是一个基于 Spring Boot + Vue 3 构建的全栈电影管理与智能分析系统。系统集成了机器学习推荐算法（ALS 协同过滤）、K-Means 用户画像聚类、ARIMA 时间序列趋势预测、DeepSeek 大模型情感分析等核心功能，提供电影信息管理、评分评论、个性化推荐、数据可视化分析等完整能力，适合作为课程设计或毕业设计项目。

1.3 技术栈
分类	技术	版本	说明
后端框架	Spring Boot	3.x	Java Web 框架
数据库	MySQL	8.x	关系型数据库
ORM 框架	MyBatis	3.x	数据持久层
安全认证	Spring Security + JWT	-	认证与授权
密码加密	BCrypt	-	密码哈希加密
前端框架	Vue 3	3.4.x	渐进式 JavaScript 框架
UI 组件库	Element Plus	2.6.x	Vue 3 组件库
图表可视化	ECharts	6.1.x	数据可视化图表库
路由	Vue Router	4.3.x	前端路由管理
HTTP 客户端	Axios	1.6.x	HTTP 请求库
构建工具	Vite	5.1.x	前端构建工具
机器学习	Apache Commons Math3	-	K-Means++ 聚类
大数据分析	Apache Spark (PySpark)	-	数据预处理与分析
深度学习	DeepSeek API	-	大模型情感分析
时间序列	statsmodels (ARIMA)	-	趋势预测
数据处理	Pandas, NumPy, SciPy	-	数据分析与计算
Java 版本	JDK	17+	后端开发环境
Python 版本	Python	3.10	数据分析环境
1.4 项目结构
e:/movie/ ├── movie-api/                    # 后端 Spring Boot 项目 
│   ├── src/main/java/com/neuedu/movieapi/ 
│   │   ├── common/               # 通用类 
│   │   │   └── Result.java       # 统一响应封装 
│   │   ├── config/                # 配置类 
│   │   │   ├── CorsConfig.java 
│   │   │   ├── DatabaseInitConfig.java 
│   │   │   ├── PasswordEncoderConfig.java 
│   │   │   ├── SecurityConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controller/            # 控制器层（15个）
│   │   ├── entity/                # 实体层（14个）
│   │   ├── interceptor/           # 拦截器 
│   │   │   └── JwtInterceptor.java
│   │   ├── mapper/                # 数据访问层（12个） 
│   │   ├── service/               # 业务逻辑层（14个） 
│   │   ├── util/                  # 工具类 
│   │   │   └── JwtUtil.java 
│   │   └── MovieApiApplication.java 
│   ├── src/main/resources/ 
│   │   ├── als_model/             # ALS 推荐模型文件 
│   │   └── application.properties
│   └── pom.xml ├── movie-frontend/                # 前端 Vue 3 项目
│   ├── src/ 
│   │   ├── views/                 # 页面组件（19个）
│   │   ├── router/index.js        # 路由配置 
│   │   ├── utils/                 # 工具函数 
│   │   │   ├── axios.js 
│   │   │   └── md5.js 
│   │   ├── App.vue
│   │   └── main.js 
│   ├── public/                    # 静态资源
│   └── package.json └── movie_analysiz_01/            # Python 数据分析项目
├── test01/                    # 第一版分析脚本 
├── test02/                    # 第二版分析脚本 
└── .venv/                     # Python 虚拟环境

二、后端控制器详细说明
2.1 AuthController
路径: controller/AuthController.java
基础路径: /api/auth
功能: 注册/登录/个人信息/头像管理
方法	端点	说明
POST	/api/auth/register	用户注册
POST	/api/auth/login	用户登录返回JWT
PUT	/api/auth/profile	更新昵称
PUT	/api/auth/password	修改密码
GET	/api/auth/avatar	获取头像
PUT	/api/auth/avatar	更新头像
POST	/api/auth/import-users	批量导入
核心: BCrypt加密, JWT(1小时), MD5用户主键		
2.2 MovieController
路径: controller/MovieController.java
基础路径: /api/movies
功能: 电影CRUD/搜索/筛选 | GET / | 分页列表 | GET /{id} | 详情 | | GET /search | 搜索 | GET /filter | 多条件筛选 | | POST / | 新增 | PUT / | 更新 | DELETE /{id} | 删除 |
2.3 CommentController
路径: controller/CommentController.java
基础路径: /api/comments
功能: 评论CRUD/按电影用户查询
2.4 RatingController
路径: controller/RatingController.java
基础路径: /api/ratings
功能: 评分CRUD(1-5分)/按电影用户查询
2.5 MoviePredictionController
路径: controller/MoviePredictionController.java
基础路径: /api/predictions
功能: 电影预测评分列表/筛选
2.6 RecommendationController
路径: controller/RecommendationController.java
基础路径: /api/recommendations
功能: ALS个性化推荐
2.7 SentimentAnalysisController
路径: controller/SentimentAnalysisController.java
基础路径: /api/sentiment
功能: DeepSeek情感分析(批量)
2.8 TrendForecastController
路径: controller/TrendForecastController.java
基础路径: /api/trend-forecast
功能: ARIMA趋势预测
2.9 UserHistoryController
路径: controller/UserHistoryController.java
基础路径: /api/user-history
功能: 浏览/评价历史管理
2.10 其他控制器
UserController(/api/users): 用户CRUD
PersonController(/api/persons): 人员CRUD
SysUserController(/api/sys-users): 系统用户
MovieStatsController(/api/stats): 统计数据
UserProfileController(/api/user-profile): K-Means++聚类
ReviewController(/api/reviews): 评分评论聚合
三、服务层详细说明
服务类	文件路径	职责说明
Auth相关(UserService)	service/UserService.java	用户注册/登录/MD5映射/密码校验
MovieService	service/MovieService.java	电影CRUD/搜索/多条件筛选/关联检查
CommentService	service/CommentService.java	评论CRUD/昵称填充/智能搜索
RatingService	service/RatingService.java	评分CRUD/昵称填充/性能优化搜索
RecommendationService	service/RecommendationService.java	ALS模型加载/内存计算推荐
SentimentAnalysisService	service/SentimentAnalysisService.java	DeepSeek情感分析/批量处理
TrendForecastService	service/TrendForecastService.java	ARIMA预测数据加载/统计
UserHistoryService	service/UserHistoryService.java	浏览评价历史CRUD/同步双表
UserProfileService	service/UserProfileService.java	K-Means++聚类/肘部法则/批量保存
SysUserService	service/SysUserService.java	系统用户CRUD/级联删除
MovieStatsService	service/MovieStatsService.java	统计数据按类型查询
PersonService	service/PersonService.java	电影人员CRUD
ReviewService	service/ReviewService.java	评分评论聚合/按电影用户合并
MoviePrediction相关(通过Mapper)	-	预测评分数据直接通过Mapper访问
四、Mapper层详细说明
Mapper类	职责说明
MovieMapper	电影数据访问/搜索/筛选/ID查询/详情批量加载
CommentMapper	评论CRUD/按电影用户查询/搜索/计数
RatingMapper	评分CRUD/按电影用户查询/搜索/特征聚合
UserMapper	用户CRUD/按ID查询/批量查询/搜索
SysUserMapper	系统用户CRUD/按用户名查询/搜索/管理员计数
PersonMapper	人员CRUD/搜索
MoviePredictionMapper	预测评分查询/筛选/计数
MovieStatsMapper	统计数据CRUD/按类型查询/类型列表
RecommendationMapper	推荐数据CRUD/按用户查询
SentimentAnalysisMapper	情感分析CRUD/按评论ID/电影ID查询/分组统计
UserHistoryMapper	用户历史CRUD/浏览/评价分别查询/计数
UserClusterMapper	聚类结果CRUD/按簇查询/汇总统计/批量插入
五、实体层详细说明
实体类	主要字段	说明
Movie	movieId, name, cover, genres, directors, actors, doubanScore, doubanVotes, year, releaseDate, language, region, storyline, mins	电影核心信息
User	userMd5, nickname, createdAt	评分用户(MK5主键)
SysUser	id, username, password, nickname, email, avatar, role, status, createdAt, updatedAt	系统用户(含角色)
Comment	commentId, userMd5, movieId, content, votes, commentTime, rating, nickname(填充)	评论信息
Rating	ratingId, userMd5, movieId, rating(1-5), ratingTime, nickname(填充)	评分记录
Person	personId, name, sex, nameEn, nameZh, birth, birthplace, profession, biography	电影人员
MoviePrediction	movieId, name, year, genres, region, predictedScore	预测评分结果
Recommendation	id, userMd5, movieId, predictedRating, rank + 前端展示字段	ALS推荐结果
SentimentAnalysis	id, commentId, movieId, sentiment, confidence, analyzedAt	情感分析结果
MovieStats	id, statType, statKey, statValue, statCount, statPercentage, extraData	统计数据
UserHistory	id, userMd5, movieId, movieName, movieCover, rating, comment, viewTime, reviewTime	用户历史
UserClusterResult	userMd5, nickname, clusterId, avgRating, ratingCount, ratingStddev, daysSinceLastRating	聚类结果
ReviewVO	id, userMd5, movieId, movieName, rating, time, type, content, votes	评论文档VO
PageResult	data, currentPage, pageSize, totalCount, totalPages	通用分页结果
六、配置与拦截器
6.1 配置类
配置类	文件路径	功能说明
CorsConfig	config/CorsConfig.java	跨域配置，允许所有来源访问API
SecurityConfig	config/SecurityConfig.java	Spring Security配置，放行所有请求，禁用CSRF
WebConfig	config/WebConfig.java	JWT拦截器注册，配置公开/受保护路径
PasswordEncoderConfig	config/PasswordEncoderConfig.java	BCrypt密码编码器Bean
DatabaseInitConfig	config/DatabaseInitConfig.java	应用启动时自动创建数据表和索引
6.2 拦截器
拦截器	文件路径	功能说明
JwtInterceptor	interceptor/JwtInterceptor.java	JWT Token验证，解析Authorization头，设置username属性
6.3 工具类
工具类	文件路径	功能说明
JwtUtil	util/JwtUtil.java	JWT生成/解析/验证，HS256签名
6.4 通用类
类	路径	说明
Result	common/Result.java	统一响应封装(code/message/data/timestamp)
6.5 数据库初始化
DatabaseInitConfig在应用启动时自动执行:

创建sys_user表(含avatar MEDIUMTEXT字段)
创建user_history表(浏览/评价历史)
创建movie_stats表(统计数据)
创建user_cluster表(聚类结果)
创建sentiment_analysis表(情感分析)
添加索引(movie.name, movie.year, comment, rating等)
修复表排序规则为utf8mb4_unicode_ci
6.6 application.properties
服务端口: 8888
数据源: MySQL movie_db数据库
MyBatis配置: 驼峰映射/SQL日志输出
DeepSeek API: sk-开头的密钥配置
七、前端文件详细说明
7.1 入口与路由
main.js
路径: src/main.js
创建Vue应用，注册ElementPlus和VueRouter，挂载到#app
App.vue
路径: src/App.vue
根组件，包含router-view和全局样式
router/index.js
路径: src/router/index.js
路由配置表:
/login -> LoginRegister(登录注册页)
/user -> UserLayout(用户布局) -> UserDashboard, UserSettings
/admin -> AdminLayout(管理员布局) -> Movies, Persons, Users, Ratings, Comments, SysUsers, Stats, Recommendations
路由守卫: 未登录跳转登录页，角色权限检查
7.2 工具函数
utils/axios.js
创建axios实例(baseURL:/api, 超时30秒, 重试2次)
请求拦截器: 自动添加Authorization:Bearer Token
响应拦截器: 401自动登出+跳转登录页, 超时重试
utils/md5.js
纯JS实现的MD5哈希函数(与后端Java MD5保持一致)
用于将用户名转为userMd5作为user表主键
7.3 登录与注册
LoginRegister.vue
路径: views/LoginRegister.vue
功能: 登录/注册双Tab切换
登录: POST /auth/login, 保存token到sessionStorage, 按角色跳转
注册: POST /auth/register, 验证表单(密码一致性)
UI: 渐变背景(紫色), 圆角卡片, Element Plus表单验证
7.4 用户端页面
UserLayout.vue
路径: views/UserLayout.vue
用户端布局组件: 顶部导航栏(logo/用户信息/退出按钮)
显示用户头像, 昵称, 提供主页/设置/登出入口
UserDashboard.vue
路径: views/UserDashboard.vue
用户主面板: 搜索栏 + 电影浏览 + ALS推荐(猜你喜欢)
支持电影搜索、浏览、评分、评论
包含多个Tab: 首页/电影浏览/我的评分/我的评论
UserPrediction.vue
路径: views/UserPrediction.vue
电影预测评分浏览: 搜索+多条件筛选(类型/年份/地区/评分范围)
展示预测评分统计和电影卡片列表
UserRecommendations.vue
路径: views/UserRecommendations.vue
ALS推荐展示: 骨架屏加载、换一批、仅高分筛选、类型筛选
空状态引导用户去评分
UserTrendForecast.vue
路径: views/UserTrendForecast.vue
ARIMA趋势预测可视化: 统计概览卡片+ECharts折线图
展示历史数据+预测数据+增长率
SentimentAnalysis.vue
路径: views/SentimentAnalysis.vue
情感分析: 电影搜索→选择→配置→分析→结果展示
情感分布饼图+评论列表+置信度
UserSettings.vue
路径: views/UserSettings.vue
个人设置: 个人信息Tab(修改昵称) + 修改密码Tab + 头像上传Tab
需要密码验证才能修改
7.5 管理员端页面
AdminLayout.vue
路径: views/AdminLayout.vue
管理员布局: 侧边导航菜单+顶部欢迎+退出按钮
导航项: 电影管理/人员管理/评分用户/评分管理/评论管理/系统用户/数据分析/用户推荐
MovieManagement.vue
路径: views/MovieManagement.vue
电影CRUD管理: 搜索/新增/编辑/删除
使用el-autocomplete搜索, el-table展示, el-dialog编辑
PersonManagement.vue
路径: views/PersonManagement.vue
电影人员CRUD: 导演/演员信息管理
UserManagement.vue
路径: views/UserManagement.vue
评分用户管理: 查看评分用户列表, 搜索, 删除
RatingManagement.vue
路径: views/RatingManagement.vue
评分记录管理: 查看/搜索/删除评分记录
CommentManagement.vue
路径: views/CommentManagement.vue
评论管理: 查看/搜索/删除评论
SystemUserManagement.vue
路径: views/SystemUserManagement.vue
系统用户管理: 创建/编辑/删除系统用户, 角色管理
保护最后一个管理员不被删除
AdminStatsAnalysis.vue
路径: views/AdminStatsAnalysis.vue
数据分析入口: 电影数据统计 + 用户画像分析
包含StatsAnalysis(统计图表)和UserProfileAnalysis(K-Means聚类)
AdminRecommendations.vue
路径: views/AdminRecommendations.vue
用户推荐管理: 搜索用户MD5, 查看指定用户的ALS推荐
八、数据模型与算法
8.1 ALS协同过滤推荐
算法: Alternating Least Squares (交替最小二乘法) 实现位置: movie_analysiz_01/test01/output/als_recommendation.py 参数: n_factors=30, n_iterations=15, reg_param=0.1, top_n=20 流程:

从MySQL加载评分数据(过滤低频用户<5条)
构建用户-电影评分稀疏矩阵
ALS训练: 交替固定一项因子求解另一项因子
保存模型: user_factors.bin, item_factors.bin(flOat32二进制)
保存映射: user_to_idx.json, idx_to_user.json, idx_to_movie.json
为所有用户生成Top-N推荐
后端加载: RecommendationService在@PostConstruct中加载ALS模型到内存

从classpath:als_model读取模型文件
解析params.json获取因子数量
使用ByteBuffer(小端序)读取float32因子矩阵
在线计算: userVec * itemVec = 预测评分
使用优先队列(最小堆)获取Top-N推荐
8.2 K-Means++用户画像聚类
算法: K-Means++聚类(Spark MLlib + Apache Commons Math3双实现) 特征: avgRating(平均评分), ratingCount(评分数), ratingStddev(标准差), daysSinceLastRating(活跃天数) 流程:

提取用户评分特征(聚合rating表数据)
Z-Score标准化特征
快速肘部法则确定最优K(2-10范围)
K-Means++聚类(欧氏距离, 30次迭代)
分配用户到簇, 批量保存到user_cluster表
生成簇汇总统计(用户数/平均评分数/标准差)
后端实现: UserProfileService使用Apache Commons Math3

KMeansPlusPlusClusterer进行聚类
自定义肘部法则实现(WCSS计算)
DescriptiveStatistics进行标准化
8.3 ARIMA时间序列趋势预测
算法: ARIMA(p,d,q)自回归积分滑动平均模型 实现位置: movie_analysiz_01/test01/output/trend_forecast.py 流程:

Spark读取电影数据, 按年份统计电影数量
根据数据量选择ARIMA阶数(数据点<10用(1,1,0), 否则(5,1,0))
statsmodels拟合ARIMA模型
预测未来3年电影数量
保存为trend_forecast.json供后端读取
8.4 DeepSeek大模型情感分析
API: DeepSeek Chat API (deepseek-chat模型) 实现位置: SentimentAnalysisService + sentiment_analysis.py 提示词策略:

System Prompt: 专业中文电影评论情感分析专家
输出格式: JSON数组 [{id, sentiment, confidence}]
情感类别: positive(积极)/neutral(中立)/negative(消极)
Temperature: 0.3(低随机性)
Max Tokens: 500
优化策略:

批量处理: 每5条评论为一批
缓存复用: 已分析评论不再重复调用
置信度: 0-1范围, 表示结果可靠度
单条评论限制300字符, 节省token
批量间100ms间隔防限流
九、开发计划（5个阶段）
第一阶段：基础架构搭建
搭建Spring Boot后端项目骨架
搭建Vue3+ElementPlus前端项目骨架
设计数据库表结构(movie, user, rating, comment, person, sys_user)
实现统一响应封装(Result)
实现JWT认证和BCrypt密码加密
实现跨域配置和基础安全配置
第二阶段：核心CRUD功能
实现电影管理模块(列表/搜索/筛选/增删改)
实现评分管理模块(评分/删除/按电影用户查询)
实现评论管理模块(评论/删除/搜索)
实现人员管理模块(导演演员CRUD)
实现系统用户管理(管理员/普通用户)
前端实现对应管理页面(表格/表单/对话框)
第三阶段：机器学习与推荐
使用PySpark/Pandas进行数据预处理
实现ALS协同过滤推荐模型
训练模型并保存为二进制文件
后端实现ALS模型加载与在线推荐计算
前端实现推荐页面(骨架屏/换一批/筛选)
实现电影评分预测(Random Forest回归)
后端暴露预测评分API
第四阶段：高级分析与智能功能
实现K-Means++用户画像聚类
实现肘部法则确定最优K值
后端集成Apache Commons Math3进行聚类计算
前端实现聚类可视化页面
实现ARIMA时间序列趋势预测
集成DeepSeek大模型情感分析
前端实现情感分析和趋势预测页面
第五阶段：优化与完善
实现用户历史追踪(浏览/评价)
实现头像上传与更新(base64)
性能优化(索引优化/批量查询/缓存)
UI/UX优化(渐变主题/骨架屏/响应式)
数据可视化完善(ECharts图表)
部署与文档编写
十、核心功能亮点
10.1 个性化推荐系统
基于ALS矩阵分解的协同过滤推荐
30维隐因子向量捕捉用户/电影特征
支持多种过滤(类型/最低评分/随机打乱)
内存计算, 响应迅速
10.2 深度学习情感分析
接入DeepSeek大模型API
批量处理降低API调用成本
三种情感分类+置信度评分
缓存复用, 避免重复计算
10.3 用户画像聚类
K-Means++聚类算法自动确定最优K值
多维度用户特征(活跃度/评分倾向/稳定性)
可视化聚类结果, 指导分层运营
10.4 趋势预测
ARIMA时间序列模型预测电影产量趋势
历史数据+预测数据结合展示
统计概览卡片快速了解整体情况
10.5 完整权限体系
JWT Token认证, 1小时有效期
双角色体系(ADMIN/USER)
路由守卫自动权限校验
受保护接口拦截验证
10.6 精美UI体验
紫色渐变主题设计
骨架屏加载效果
ECharts丰富图表可视化
响应式布局适配
10.7 数据安全
BCrypt密码加密存储
MD5用户名哈希(用户匿名化)
防止关联数据误删
CORS跨域安全配置
十一、API端点汇总表
认证模块
方法	端点	描述
POST	/api/auth/register	用户注册
POST	/api/auth/login	用户登录
PUT	/api/auth/profile	更新昵称
PUT	/api/auth/password	修改密码
GET	/api/auth/avatar	获取头像
PUT	/api/auth/avatar	更新头像
POST	/api/auth/import-users	批量导入
电影模块
方法	端点	描述
GET	/api/movies	分页列表
GET	/api/movies/{id}	电影详情
GET	/api/movies/search	关键词搜索
GET	/api/movies/byRegion	地区筛选
GET	/api/movies/filter	多条件筛选
POST	/api/movies	新增电影
PUT	/api/movies	更新电影
DELETE	/api/movies/{id}	删除电影
评论模块
方法	端点	描述
GET	/api/comments	评论列表
GET	/api/comments/{id}	评论详情
GET	/api/comments/movie/{movieId}	电影评论
GET	/api/comments/user/{userMd5}	用户评论
POST	/api/comments	新增评论
PUT	/api/comments	更新评论
DELETE	/api/comments/{id}	删除评论
DELETE	/api/comments/user-movie	批量删除
评分模块
方法	端点	描述
GET	/api/ratings	评分列表
GET	/api/ratings/movie/{movieId}	电影评分
GET	/api/ratings/user/{userMd5}	用户评分
POST	/api/ratings	新增评分
PUT	/api/ratings	更新评分
DELETE	/api/ratings/{id}	删除评分
推荐与分析模块
方法	端点	描述
GET	/api/recommendations/user/{userMd5}	ALS推荐列表
GET	/api/recommendations/user/{userMd5}/top	Top-N推荐
GET	/api/predictions	预测评分列表
GET	/api/predictions/filter	筛选预测
POST	/api/sentiment/analyze/{movieId}	情感分析
GET	/api/sentiment/movie/{movieId}/distribution	情感分布
GET	/api/trend-forecast	趋势预测
POST	/api/user-profile/cluster	触发K-Means聚类
GET	/api/user-profile/cluster-result	聚类结果
其他模块
方法	端点	描述
GET/POST/PUT/DELETE	/api/users	评分用户CRUD
GET/POST/PUT/DELETE	/api/persons	人员CRUD
GET/POST/PUT/DELETE	/api/sys-users	系统用户CRUD
GET	/api/stats/{statType}	统计数据
GET	/api/stats/types/list	统计类型列表
GET	/api/reviews/user/{userMd5}	用户评价汇总
GET	/api/reviews/movie/{movieId}	电影评价汇总
CRUD	/api/user-history	用户历史管理
十二、部署说明
12.1 环境要求
JDK 17+
Node.js 18+
MySQL 8.0+
Python 3.10+
Maven 3.8+
12.2 数据库准备
创建数据库: CREATE DATABASE movie_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
导入原始数据 movies.csv, users.csv, ratings.csv, comments.csv, person.csv
运行Python预处理脚本生成干净数据
将数据导入MySQL对应表
12.3 后端部署
配置数据源: 修改application.properties中的数据库连接
配置DeepSeek API Key
运行ALS训练脚本: python als_recommendation.py
运行ARIMA预测脚本: python trend_forecast.py
构建项目: mvn clean package
启动服务: java -jar movie-api-0.0.1-SNAPSHOT.jar
服务启动在8888端口, 自动创建所需数据表
12.4 前端部署
安装依赖: npm install
配置API地址: 修改vite.config.js中的proxy
开发模式: npm run dev
生产构建: npm run build
预览: npm run preview
构建产物在dist/目录, 可部署到Nginx等静态服务器
12.5 默认账户
管理员: admin / 123456 (通过import-users接口或直接SQL创建)
普通用户: 注册接口自助注册
导入用户默认密码: 123456
12.6 注意事项
ALS模型文件需预先训练好, 放在src/main/resources/als_model/目录
ARIMA预测结果需预先生成trend_forecast.json, 放在src/main/resources/trend_forecast/
DeepSeek API Key需正确配置, 情感分析功能依赖此Key
大数据量(410万评分)搜索已优化: 通过电影名称索引加速
JWT认证可通过注释WebConfig中的@Configuration临时关闭
十三、项目总结
本电影智能管理与分析系统是一个功能完整、架构合理的全栈Web应用，具有以下特点：

技术架构
后端采用Spring Boot 3 + MyBatis + Spring Security + JWT的成熟技术栈
前端采用Vue 3 + Element Plus + ECharts的现代化技术栈
数据分析层集成了PySpark、Pandas、NumPy等大数据分析工具
机器学习算法覆盖了协同过滤、K-Means聚类、ARIMA时间序列、大模型API调用
功能完整性
覆盖了电影管理系统的所有核心业务：电影/人员/用户/评分/评论的CRUD
实现了三大智能分析能力：ALS推荐、K-Means画像、ARIMA预测
集成了DeepSeek大模型实现评论情感智能分析
提供了完善的用户体验：搜索/筛选/分页/排序/骨架屏/主题设计
工程质量
分层架构清晰：Controller-Service-Mapper-Entity四层架构
统一响应封装Result，规范API返回格式
完善的异常处理和数据校验
性能优化到位：索引添加、批量查询、缓存策略、分页限制
安全措施完善：BCrypt加密、JWT认证、角色权限、CORS配置
扩展性
ALS模型可重新训练以适应新数据
情感分析可更换不同的大模型API
K-Means聚类可调整特征维度和K值范围
架构支持水平扩展和微服务拆分
本项目不仅是一个完整的管理系统，更是一个集成了多种机器学习算法的智能分析平台， 展示了从数据采集、预处理、模型训练到在线服务的完整数据科学工程实践。 对于学习全栈开发、机器学习工程化应用具有很高的参考价值。
