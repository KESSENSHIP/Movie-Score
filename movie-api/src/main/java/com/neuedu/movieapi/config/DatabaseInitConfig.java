package com.neuedu.movieapi.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        createSysUserTable();
        createUserHistoryTable();
        createMovieStatsTable();
        createUserClusterTable();
        createSentimentAnalysisTable();
    }
    
    private void createSysUserTable() {
        // 创建 sys_user 表（包含 avatar 字段）
        String sql = "CREATE TABLE IF NOT EXISTS sys_user (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(255) NOT NULL," +
                "password VARCHAR(255) NOT NULL," +
                "nickname VARCHAR(255)," +
                "email VARCHAR(255)," +
                "avatar MEDIUMTEXT," +
                "role VARCHAR(50) DEFAULT 'USER'," +
                "status INT DEFAULT 1," +
                "created_at DATETIME," +
                "updated_at DATETIME" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

        try {
            jdbcTemplate.execute(sql);
            System.out.println("表 sys_user 创建成功或已存在");
        } catch (Exception e) {
            System.err.println("创建表 sys_user 失败: " + e.getMessage());
        }

        // 如果表已存在但没有 avatar 列，则补充添加
        try {
            jdbcTemplate.execute("ALTER TABLE sys_user ADD COLUMN avatar MEDIUMTEXT");
            System.out.println("列 avatar 添加成功");
        } catch (Exception e) {
            // 列可能已存在，尝试修改类型
            try {
                jdbcTemplate.execute("ALTER TABLE sys_user MODIFY COLUMN avatar MEDIUMTEXT");
                System.out.println("列 avatar 类型已修改为 MEDIUMTEXT");
            } catch (Exception e2) {
                // 忽略，表可能已包含该列
            }
        }
    }
    
    private void createMovieStatsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS movie_stats (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "stat_type VARCHAR(100) NOT NULL," +
                "stat_key VARCHAR(500) NOT NULL," +
                "stat_value VARCHAR(500)," +
                "stat_count BIGINT," +
                "stat_percentage DECIMAL(10,2)," +
                "extra_data JSON," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_stat_type (stat_type)," +
                "INDEX idx_stat_count (stat_count)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("表 movie_stats 创建成功或已存在");
        } catch (Exception e) {
            System.err.println("创建表 movie_stats 失败: " + e.getMessage());
        }
    }

    private void createUserHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_history (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_md5 VARCHAR(32) NOT NULL," +
                "movie_id VARCHAR(50) NOT NULL," +
                "rating INT," +
                "comment TEXT," +
                "view_time DATETIME," +
                "review_time DATETIME," +
                "INDEX idx_user_md5 (user_md5)," +
                "INDEX idx_view_time (view_time)," +
                "INDEX idx_review_time (review_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("表 user_history 创建成功或已存在");
            
            // 删除冗余字段（如果存在）
            try {
                jdbcTemplate.execute("ALTER TABLE user_history DROP COLUMN IF EXISTS movie_name");
                System.out.println("字段 movie_name 删除成功或不存在");
            } catch (Exception e) {
                System.err.println("删除字段 movie_name 失败: " + e.getMessage());
            }
            
            try {
                jdbcTemplate.execute("ALTER TABLE user_history DROP COLUMN IF EXISTS movie_cover");
                System.out.println("字段 movie_cover 删除成功或不存在");
            } catch (Exception e) {
                System.err.println("删除字段 movie_cover 失败: " + e.getMessage());
            }
            
            // 修改表排序规则以匹配 movie 表
            try {
                jdbcTemplate.execute("ALTER TABLE user_history CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                System.out.println("表 user_history 排序规则修改成功");
            } catch (Exception e) {
                System.err.println("修改表排序规则失败: " + e.getMessage());
            }
            
            // 删除唯一键约束，允许同一用户对同一电影多次评价
            try {
                jdbcTemplate.execute("ALTER TABLE user_history DROP INDEX uk_user_movie");
                System.out.println("唯一键 uk_user_movie 删除成功");
            } catch (Exception e) {
                System.err.println("删除唯一键 uk_user_movie 失败（可能已不存在）: " + e.getMessage());
            }
            
            // 添加复合索引提升查询性能
            try {
                jdbcTemplate.execute("ALTER TABLE user_history ADD INDEX idx_user_view (user_md5, view_time)");
                System.out.println("复合索引 idx_user_view 创建成功");
            } catch (Exception e) {
                System.err.println("创建复合索引 idx_user_view 失败（可能已存在）: " + e.getMessage());
            }
            
            try {
                jdbcTemplate.execute("ALTER TABLE user_history ADD INDEX idx_user_review (user_md5, review_time)");
                System.out.println("复合索引 idx_user_review 创建成功");
            } catch (Exception e) {
                System.err.println("创建复合索引 idx_user_review 失败（可能已存在）: " + e.getMessage());
            }
            
            // 为 comment 和 rating 表添加索引
            createCommentRatingIndexes();
            
        } catch (Exception e) {
            System.err.println("创建表 user_history 失败: " + e.getMessage());
        }
    }
    
    private void createCommentRatingIndexes() {
        // 为 movie 表添加索引（用于搜索筛选）
        try {
            jdbcTemplate.execute("ALTER TABLE movie ADD INDEX idx_movie_name (name)");
            System.out.println("索引 idx_movie_name 创建成功");
        } catch (Exception e) {
            System.err.println("创建索引 idx_movie_name 失败（可能已存在）: " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE movie ADD INDEX idx_movie_year (year)");
            System.out.println("索引 idx_movie_year 创建成功");
        } catch (Exception e) {
            System.err.println("创建索引 idx_movie_year 失败（可能已存在）: " + e.getMessage());
        }

        // 为 comment 表添加索引
        try {
            jdbcTemplate.execute("ALTER TABLE comment ADD INDEX idx_comment_user_movie (user_md5, movie_id)");
            System.out.println("索引 idx_comment_user_movie 创建成功");
        } catch (Exception e) {
            System.err.println("创建索引 idx_comment_user_movie 失败（可能已存在）: " + e.getMessage());
        }
        
        // 为 rating 表添加索引
        try {
            jdbcTemplate.execute("ALTER TABLE rating ADD INDEX idx_rating_user_movie (user_md5, movie_id)");
            System.out.println("索引 idx_rating_user_movie 创建成功");
        } catch (Exception e) {
            System.err.println("创建索引 idx_rating_user_movie 失败（可能已存在）: " + e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE rating ADD INDEX idx_rating_user_md5 (user_md5)");
            System.out.println("索引 idx_rating_user_md5 创建成功");
        } catch (Exception e) {
            System.err.println("创建索引 idx_rating_user_md5 失败（可能已存在）: " + e.getMessage());
        }

        // 为 user 表添加唯一约束，确保每个账户只有一个 user_md5
        try {
            jdbcTemplate.execute("ALTER TABLE user ADD CONSTRAINT uk_user_md5 UNIQUE (user_md5)");
            System.out.println("唯一约束 uk_user_md5 创建成功");
        } catch (Exception e) {
            System.err.println("创建唯一约束 uk_user_md5 失败（可能已存在）: " + e.getMessage());
        }
    }

    private void createUserClusterTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_cluster (" +
                "user_md5 VARCHAR(32) PRIMARY KEY," +
                "nickname VARCHAR(255)," +
                "cluster_id INT NOT NULL," +
                "avg_rating DOUBLE," +
                "rating_count INT," +
                "rating_stddev DOUBLE," +
                "days_since_last_rating DOUBLE," +
                "INDEX idx_cluster_id (cluster_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try {
            jdbcTemplate.execute(sql);
            System.out.println("表 user_cluster 创建成功或已存在");
        } catch (Exception e) {
            System.err.println("创建表 user_cluster 失败: " + e.getMessage());
        }
    }

    private void createSentimentAnalysisTable() {
        String sql = "CREATE TABLE IF NOT EXISTS sentiment_analysis (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "comment_id VARCHAR(50) NOT NULL," +
                "movie_id VARCHAR(50) NOT NULL," +
                "sentiment VARCHAR(20) NOT NULL," +
                "confidence DOUBLE," +
                "analyzed_at VARCHAR(30)," +
                "UNIQUE KEY uk_comment_id (comment_id)," +
                "INDEX idx_movie_id (movie_id)," +
                "INDEX idx_sentiment (sentiment)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try {
            jdbcTemplate.execute(sql);
            System.out.println("表 sentiment_analysis 创建成功或已存在");
        } catch (Exception e) {
            System.err.println("创建表 sentiment_analysis 失败: " + e.getMessage());
        }
    }
}
