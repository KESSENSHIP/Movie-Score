package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.UserClusterResult;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserClusterMapper {

    @Insert("INSERT INTO user_cluster(user_md5, nickname, cluster_id, avg_rating, rating_count, rating_stddev, days_since_last_rating) " +
            "VALUES(#{userMd5}, #{nickname}, #{clusterId}, #{avgRating}, #{ratingCount}, #{ratingStddev}, #{daysSinceLastRating})")
    void insert(UserClusterResult result);

    @Select("SELECT uc.*, u.USER_NICKNAME as nickname FROM user_cluster uc " +
            "LEFT JOIN `user` u ON uc.user_md5 = u.USER_MD5 " +
            "ORDER BY uc.cluster_id, uc.rating_count DESC")
    List<UserClusterResult> findAllWithNickname();

    @Select("SELECT " +
            "cluster_id as clusterId, " +
            "COUNT(*) as userCount, " +
            "AVG(rating_count) as avgRatingCount, " +
            "AVG(avg_rating) as avgRating, " +
            "AVG(rating_stddev) as avgRatingStddev, " +
            "AVG(days_since_last_rating) as avgDaysSinceLast, " +
            "MAX(rating_count) as maxRatingCount, " +
            "MIN(rating_count) as minRatingCount " +
            "FROM user_cluster GROUP BY cluster_id ORDER BY cluster_id")
    List<ClusterSummary> findClusterSummary();

    @Delete("DELETE FROM user_cluster")
    void deleteAll();
    
    @Select("SELECT COUNT(*) FROM user_cluster")
    int count();

    @Insert("<script>" +
            "INSERT INTO user_cluster(user_md5, nickname, cluster_id, avg_rating, rating_count, rating_stddev, days_since_last_rating) VALUES " +
            "<foreach collection='list' item='r' separator=','>" +
            "(#{r.userMd5}, #{r.nickname}, #{r.clusterId}, #{r.avgRating}, #{r.ratingCount}, #{r.ratingStddev}, #{r.daysSinceLastRating})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<UserClusterResult> results);

    @Select("SELECT user_md5 as userMd5, nickname, cluster_id as clusterId, avg_rating as avgRating, " +
            "rating_count as ratingCount, rating_stddev as ratingStddev, days_since_last_rating as daysSinceLastRating " +
            "FROM user_cluster WHERE cluster_id = #{clusterId} ORDER BY rating_count DESC LIMIT #{limit} OFFSET #{offset}")
    List<UserClusterResult> findByClusterId(@Param("clusterId") int clusterId, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM user_cluster WHERE cluster_id = #{clusterId}")
    long countByClusterId(@Param("clusterId") int clusterId);

    class ClusterSummary {
        public Integer clusterId;
        public Long userCount;
        public Double avgRatingCount;
        public Double avgRating;
        public Double avgRatingStddev;
        public Double avgDaysSinceLast;
        public Long maxRatingCount;
        public Long minRatingCount;
    }
}
