package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.MoviePrediction;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MoviePredictionMapper {

    @Select("SELECT MOVIE_ID as movieId, NAME as name, YEAR as year, GENRES as genres, REGION as region, " +
            "LEAST(MAX(predicted_score), 10.0) as predictedScore " +
            "FROM movie_prediction " +
            "GROUP BY MOVIE_ID, NAME, YEAR, GENRES, REGION " +
            "ORDER BY LEAST(MAX(predicted_score), 10.0) DESC, MOVIE_ID ASC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<MoviePrediction> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(DISTINCT MOVIE_ID) FROM movie_prediction")
    Long countAll();

    @Select("SELECT MOVIE_ID as movieId, NAME as name, YEAR as year, GENRES as genres, REGION as region, " +
            "LEAST(MAX(predicted_score), 10.0) as predictedScore " +
            "FROM movie_prediction WHERE NAME LIKE CONCAT('%', #{keyword}, '%') " +
            "GROUP BY MOVIE_ID, NAME, YEAR, GENRES, REGION " +
            "ORDER BY LEAST(MAX(predicted_score), 10.0) DESC, MOVIE_ID ASC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    List<MoviePrediction> search(@Param("keyword") String keyword,
                                 @Param("pageSize") Integer pageSize,
                                 @Param("offset") Integer offset);

    @Select("SELECT COUNT(DISTINCT MOVIE_ID) FROM movie_prediction WHERE NAME LIKE CONCAT('%', #{keyword}, '%')")
    Long countSearch(@Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT MOVIE_ID as movieId, NAME as name, YEAR as year, GENRES as genres, REGION as region, " +
            "LEAST(MAX(predicted_score), 10.0) as predictedScore " +
            "FROM movie_prediction WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND NAME LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "<if test='genre != null and genre != \"\"'> AND GENRES LIKE CONCAT('%', #{genre}, '%') </if>" +
            "<if test='year != null and year != \"\"'> AND YEAR = #{year} </if>" +
            "<if test='region != null and region != \"\"'> AND REGION LIKE CONCAT('%', #{region}, '%') </if>" +
            " GROUP BY MOVIE_ID, NAME, YEAR, GENRES, REGION " +
            " HAVING 1=1" +
            "<if test='minScore != null'> AND LEAST(MAX(predicted_score), 10.0) &gt;= #{minScore} </if>" +
            "<if test='maxScore != null'> AND LEAST(MAX(predicted_score), 10.0) &lt;= #{maxScore} </if>" +
            " ORDER BY LEAST(MAX(predicted_score), 10.0) DESC, MOVIE_ID ASC " +
            " LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<MoviePrediction> searchByFilters(@Param("keyword") String keyword,
                                          @Param("genre") String genre,
                                          @Param("year") String year,
                                          @Param("region") String region,
                                          @Param("minScore") Double minScore,
                                          @Param("maxScore") Double maxScore,
                                          @Param("pageSize") Integer pageSize,
                                          @Param("offset") Integer offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM (SELECT MOVIE_ID FROM movie_prediction WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND NAME LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "<if test='genre != null and genre != \"\"'> AND GENRES LIKE CONCAT('%', #{genre}, '%') </if>" +
            "<if test='year != null and year != \"\"'> AND YEAR = #{year} </if>" +
            "<if test='region != null and region != \"\"'> AND REGION LIKE CONCAT('%', #{region}, '%') </if>" +
            " GROUP BY MOVIE_ID " +
            " HAVING 1=1" +
            "<if test='minScore != null'> AND LEAST(MAX(predicted_score), 10.0) &gt;= #{minScore} </if>" +
            "<if test='maxScore != null'> AND LEAST(MAX(predicted_score), 10.0) &lt;= #{maxScore} </if>" +
            ") tmp</script>")
    Long countByFilters(@Param("keyword") String keyword,
                        @Param("genre") String genre,
                        @Param("year") String year,
                        @Param("region") String region,
                        @Param("minScore") Double minScore,
                        @Param("maxScore") Double maxScore);
}
