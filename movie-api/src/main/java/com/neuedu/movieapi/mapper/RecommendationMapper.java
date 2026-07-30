package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.Recommendation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecommendationMapper {

    @Select("SELECT r.id, r.user_md5 as userMd5, r.movie_id as movieId, " +
            "r.predicted_rating as predictedRating, r.`rank`, r.created_at as createdAt, " +
            "m.name as movieName, m.cover as movieCover, m.genres, m.directors " +
            "FROM recommendation r " +
            "LEFT JOIN movie m ON r.movie_id = m.movie_id " +
            "WHERE r.user_md5 = #{userMd5} " +
            "ORDER BY r.`rank` ASC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<Recommendation> findByUserMd5(@Param("userMd5") String userMd5,
                                       @Param("pageSize") Integer pageSize,
                                       @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM recommendation WHERE user_md5 = #{userMd5}")
    Long countByUserMd5(@Param("userMd5") String userMd5);

    @Select("SELECT r.id, r.user_md5 as userMd5, r.movie_id as movieId, " +
            "r.predicted_rating as predictedRating, r.`rank`, r.created_at as createdAt, " +
            "m.name as movieName, m.cover as movieCover, m.genres, m.directors " +
            "FROM recommendation r " +
            "LEFT JOIN movie m ON r.movie_id = m.movie_id " +
            "WHERE r.user_md5 = #{userMd5} " +
            "ORDER BY r.`rank` ASC LIMIT 50")
    List<Recommendation> findTopByUserMd5(@Param("userMd5") String userMd5);
}
