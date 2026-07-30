package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.Rating;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RatingMapper {

    @Select("SELECT RATING_ID as ratingId, USER_MD5 as userMd5, MOVIE_ID as movieId, RATING as rating, RATING_TIME as ratingTime " +
            "FROM rating LIMIT #{pageSize} OFFSET #{offset}")
    List<Rating> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM rating")
    Long count();

    @Select("SELECT RATING_ID as ratingId, USER_MD5 as userMd5, MOVIE_ID as movieId, RATING as rating, RATING_TIME as ratingTime " +
            "FROM rating WHERE RATING_ID = #{ratingId}")
    Rating findById(@Param("ratingId") String ratingId);

    @Select("SELECT RATING_ID as ratingId, USER_MD5 as userMd5, MOVIE_ID as movieId, RATING as rating, RATING_TIME as ratingTime " +
            "FROM rating WHERE MOVIE_ID = #{movieId} " +
            "ORDER BY RATING DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<Rating> findByMovieId(@Param("movieId") String movieId, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM rating WHERE MOVIE_ID = #{movieId}")
    Long countByMovieId(@Param("movieId") String movieId);

    @Select("SELECT COUNT(*) FROM rating WHERE USER_MD5 = #{userMd5}")
    Long countByUserMd5(@Param("userMd5") String userMd5);

    @Select("SELECT RATING_ID as ratingId, USER_MD5 as userMd5, MOVIE_ID as movieId, RATING as rating, RATING_TIME as ratingTime " +
            "FROM rating WHERE USER_MD5 = #{userMd5} LIMIT #{pageSize} OFFSET #{offset}")
    List<Rating> findByUserMd5(@Param("userMd5") String userMd5, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("<script>" +
            "SELECT RATING_ID as ratingId, USER_MD5 as userMd5, MOVIE_ID as movieId, RATING as rating, RATING_TIME as ratingTime " +
            "FROM rating WHERE MOVIE_ID IN " +
            "<foreach collection='movieIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<Rating> findByMovieIds(@Param("movieIds") List<String> movieIds, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM rating WHERE MOVIE_ID IN " +
            "<foreach collection='movieIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    Long countByMovieIds(@Param("movieIds") List<String> movieIds);

    @Insert("INSERT INTO rating(rating_id, user_md5, movie_id, rating, rating_time) " +
            "VALUES (#{ratingId}, #{userMd5}, #{movieId}, #{rating}, #{ratingTime})")
    int insert(Rating rating);

    @Update("UPDATE rating SET user_md5=#{userMd5}, movie_id=#{movieId}, " +
            "rating=#{rating}, rating_time=#{ratingTime} WHERE rating_id=#{ratingId}")
    int update(Rating rating);

    @Select("SELECT RATING_ID as ratingId, USER_MD5 as userMd5, MOVIE_ID as movieId, RATING, RATING_TIME as ratingTime " +
            "FROM rating " +
            "WHERE USER_MD5 LIKE CONCAT('%', #{keyword}, '%') OR MOVIE_ID LIKE CONCAT('%', #{keyword}, '%') " +
            "OR CAST(RATING AS CHAR) LIKE CONCAT('%', #{keyword}, '%') " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<Rating> search(@Param("keyword") String keyword, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM rating " +
            "WHERE USER_MD5 LIKE CONCAT('%', #{keyword}, '%') OR MOVIE_ID LIKE CONCAT('%', #{keyword}, '%') " +
            "OR CAST(RATING AS CHAR) LIKE CONCAT('%', #{keyword}, '%')")
    Long countSearch(@Param("keyword") String keyword);

    @Delete("DELETE FROM rating WHERE RATING_ID=#{ratingId}")
    int deleteById(@Param("ratingId") String ratingId);

    @Select("SELECT RATING_ID as ratingId FROM rating WHERE USER_MD5 = #{userMd5} AND MOVIE_ID = #{movieId} LIMIT 1")
    Rating findByUserMd5AndMovieId(@Param("userMd5") String userMd5, @Param("movieId") String movieId);

    @Delete("DELETE FROM rating WHERE USER_MD5 = #{userMd5} AND MOVIE_ID = #{movieId}")
    int deleteByUserMd5AndMovieId(@Param("userMd5") String userMd5, @Param("movieId") String movieId);

    @Delete("DELETE FROM rating WHERE USER_MD5 = #{userMd5}")
    int deleteByUserMd5(@Param("userMd5") String userMd5);

    @Select("SELECT " +
            "USER_MD5 as userMd5, " +
            "COUNT(*) as ratingCount, " +
            "AVG(RATING) as avgRating, " +
            "SUM(RATING * RATING) as sumRatingSq, " +
            "SUM(RATING) as sumRating, " +
            "MAX(RATING_TIME) as latestRatingTime " +
            "FROM rating GROUP BY USER_MD5 HAVING COUNT(*) >= 3")
    List<java.util.Map<String, Object>> findUserRatingFeatures();
}
