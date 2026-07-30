package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.Comment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT COMMENT_ID as commentId, USER_MD5 as userMd5, MOVIE_ID as movieId, CONTENT as content, VOTES as votes, COMMENT_TIME as commentTime, RATING as rating " +
            "FROM comment LIMIT #{pageSize} OFFSET #{offset}")
    List<Comment> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM comment")
    Long count();

    @Select("SELECT COMMENT_ID as commentId, USER_MD5 as userMd5, MOVIE_ID as movieId, CONTENT as content, VOTES as votes, COMMENT_TIME as commentTime, RATING as rating " +
            "FROM comment WHERE COMMENT_ID = #{commentId}")
    Comment findById(@Param("commentId") String commentId);

    @Select("SELECT COMMENT_ID as commentId, USER_MD5 as userMd5, MOVIE_ID as movieId, CONTENT as content, VOTES as votes, COMMENT_TIME as commentTime, RATING as rating " +
            "FROM comment WHERE MOVIE_ID = #{movieId} LIMIT #{pageSize} OFFSET #{offset}")
    List<Comment> findByMovieId(@Param("movieId") String movieId, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM comment WHERE MOVIE_ID = #{movieId}")
    Long countByMovieId(@Param("movieId") String movieId);

    @Select("SELECT COUNT(*) FROM comment WHERE USER_MD5 = #{userMd5}")
    Long countByUserMd5(@Param("userMd5") String userMd5);

    @Select("SELECT COMMENT_ID as commentId, USER_MD5 as userMd5, MOVIE_ID as movieId, CONTENT as content, VOTES as votes, COMMENT_TIME as commentTime, RATING as rating " +
            "FROM comment WHERE USER_MD5 = #{userMd5} LIMIT #{pageSize} OFFSET #{offset}")
    List<Comment> findByUserMd5(@Param("userMd5") String userMd5, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("<script>" +
            "SELECT COMMENT_ID as commentId, USER_MD5 as userMd5, MOVIE_ID as movieId, CONTENT as content, VOTES as votes, COMMENT_TIME as commentTime, RATING as rating " +
            "FROM comment WHERE MOVIE_ID IN " +
            "<foreach collection='movieIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<Comment> findByMovieIds(@Param("movieIds") List<String> movieIds, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM comment WHERE MOVIE_ID IN " +
            "<foreach collection='movieIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    Long countByMovieIds(@Param("movieIds") List<String> movieIds);

    @Insert("INSERT INTO comment(comment_id, user_md5, movie_id, content, votes, comment_time, rating) " +
            "VALUES (#{commentId}, #{userMd5}, #{movieId}, #{content}, #{votes}, #{commentTime}, #{rating})")
    int insert(Comment comment);

    @Update("UPDATE comment SET user_md5=#{userMd5}, movie_id=#{movieId}, " +
            "content=#{content}, votes=#{votes}, comment_time=#{commentTime}, rating=#{rating} WHERE comment_id=#{commentId}")
    int update(Comment comment);

    @Select("SELECT COMMENT_ID as commentId, USER_MD5 as userMd5, MOVIE_ID as movieId, CONTENT as content, VOTES as votes, COMMENT_TIME as commentTime, RATING as rating " +
            "FROM comment " +
            "WHERE USER_MD5 LIKE CONCAT('%', #{keyword}, '%') OR MOVIE_ID LIKE CONCAT('%', #{keyword}, '%') " +
            "OR CONTENT LIKE CONCAT('%', #{keyword}, '%') LIMIT #{pageSize} OFFSET #{offset}")
    List<Comment> search(@Param("keyword") String keyword, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM comment " +
            "WHERE USER_MD5 LIKE CONCAT('%', #{keyword}, '%') OR MOVIE_ID LIKE CONCAT('%', #{keyword}, '%') " +
            "OR CONTENT LIKE CONCAT('%', #{keyword}, '%')")
    Long countSearch(@Param("keyword") String keyword);

    @Delete("DELETE FROM comment WHERE COMMENT_ID=#{commentId}")
    int deleteById(@Param("commentId") String commentId);

    @Select("SELECT COMMENT_ID as commentId FROM comment WHERE USER_MD5 = #{userMd5} AND MOVIE_ID = #{movieId} LIMIT 1")
    Comment findByUserMd5AndMovieId(@Param("userMd5") String userMd5, @Param("movieId") String movieId);

    @Delete("DELETE FROM comment WHERE USER_MD5 = #{userMd5} AND MOVIE_ID = #{movieId}")
    int deleteByUserMd5AndMovieId(@Param("userMd5") String userMd5, @Param("movieId") String movieId);

    @Delete("DELETE FROM comment WHERE USER_MD5 = #{userMd5}")
    int deleteByUserMd5(@Param("userMd5") String userMd5);
}