package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.SentimentAnalysis;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface SentimentAnalysisMapper {

    @Select("SELECT id as id, comment_id as commentId, movie_id as movieId, sentiment, confidence, analyzed_at as analyzedAt " +
            "FROM sentiment_analysis WHERE comment_id = #{commentId}")
    SentimentAnalysis findByCommentId(@Param("commentId") String commentId);

    @Select("SELECT id as id, comment_id as commentId, movie_id as movieId, sentiment, confidence, analyzed_at as analyzedAt " +
            "FROM sentiment_analysis WHERE movie_id = #{movieId} ORDER BY analyzed_at DESC")
    List<SentimentAnalysis> findByMovieId(@Param("movieId") String movieId);

    @Select("SELECT id as id, comment_id as commentId, movie_id as movieId, sentiment, confidence, analyzed_at as analyzedAt " +
            "FROM sentiment_analysis WHERE movie_id = #{movieId} LIMIT #{pageSize} OFFSET #{offset}")
    List<SentimentAnalysis> findByMovieIdPaged(@Param("movieId") String movieId, @Param("pageSize") int pageSize, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM sentiment_analysis WHERE movie_id = #{movieId}")
    Long countByMovieId(@Param("movieId") String movieId);

    @Select("SELECT sentiment, COUNT(*) as cnt FROM sentiment_analysis WHERE movie_id = #{movieId} GROUP BY sentiment")
    List<Map<String, Object>> countByMovieIdGrouped(@Param("movieId") String movieId);

    @Insert("INSERT INTO sentiment_analysis(comment_id, movie_id, sentiment, confidence, analyzed_at) " +
            "VALUES(#{commentId}, #{movieId}, #{sentiment}, #{confidence}, #{analyzedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SentimentAnalysis analysis);

    @Update("UPDATE sentiment_analysis SET sentiment=#{sentiment}, confidence=#{confidence}, analyzed_at=#{analyzedAt} " +
            "WHERE comment_id=#{commentId}")
    int updateByCommentId(SentimentAnalysis analysis);

    @Select("SELECT COUNT(*) FROM sentiment_analysis WHERE comment_id = #{commentId}")
    boolean existsByCommentId(@Param("commentId") String commentId);

    @Delete("DELETE FROM sentiment_analysis WHERE movie_id = #{movieId}")
    int deleteByMovieId(@Param("movieId") String movieId);
}
