package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.UserHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserHistoryMapper {

    @Update("UPDATE user_history SET view_time = #{viewTime} WHERE user_md5 = #{userMd5} AND movie_id = #{movieId} AND view_time IS NOT NULL")
    int updateViewHistory(UserHistory history);

    @Insert("INSERT INTO user_history(user_md5, movie_id, view_time) VALUES(#{userMd5}, #{movieId}, #{viewTime})")
    int insertViewHistory(UserHistory history);

    @Insert("INSERT INTO user_history(user_md5, movie_id, rating, comment, review_time) VALUES(#{userMd5}, #{movieId}, #{rating}, #{comment}, #{reviewTime})")
    int insertReviewHistory(UserHistory history);

    @Select("SELECT h.id, h.user_md5 as userMd5, h.movie_id as movieId, h.view_time as viewTime FROM user_history h WHERE h.id IN (SELECT MAX(id) FROM user_history WHERE user_md5 = #{userMd5} AND view_time IS NOT NULL GROUP BY movie_id) AND h.user_md5 = #{userMd5} ORDER BY h.view_time DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<UserHistory> findViewHistoryByUserMd5(@Param("userMd5") String userMd5, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(DISTINCT movie_id) FROM user_history WHERE user_md5 = #{userMd5} AND view_time IS NOT NULL")
    int countViewHistory(@Param("userMd5") String userMd5);

    @Select("SELECT h.id, h.user_md5 as userMd5, h.movie_id as movieId, h.rating, h.comment, h.review_time as reviewTime FROM user_history h WHERE h.user_md5 = #{userMd5} AND h.review_time IS NOT NULL ORDER BY h.review_time DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<UserHistory> findReviewHistoryByUserMd5(@Param("userMd5") String userMd5, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM user_history WHERE user_md5 = #{userMd5} AND review_time IS NOT NULL")
    int countReviewHistory(@Param("userMd5") String userMd5);

    @Delete("DELETE FROM user_history WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM user_history WHERE user_md5 = #{userMd5} AND movie_id = #{movieId}")
    int deleteByUserMd5AndMovieId(@Param("userMd5") String userMd5, @Param("movieId") String movieId);

    @Update("UPDATE user_history SET rating = #{rating}, comment = #{comment}, review_time = #{reviewTime} WHERE id = #{id}")
    int updateReviewHistory(UserHistory history);
}
