package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.Movie;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface MovieMapper {

    @Select("SELECT movie_id as movieId, name, cover, genres, directors, actors, douban_score as doubanScore, douban_votes as doubanVotes, year, release_date as releaseDate, language, region, storyline, mins FROM movie ORDER BY CASE WHEN douban_score IS NOT NULL AND douban_score > 0 THEN 0 ELSE 1 END, CASE WHEN #{sortBy} = 'score' AND #{sortOrder} = 'desc' THEN douban_score END DESC, CASE WHEN #{sortBy} = 'score' AND #{sortOrder} = 'asc' THEN douban_score END ASC, CASE WHEN #{sortBy} = 'time' AND #{sortOrder} = 'desc' THEN release_date END DESC, CASE WHEN #{sortBy} = 'time' AND #{sortOrder} = 'asc' THEN release_date END ASC, CASE WHEN #{sortBy} = 'both' AND #{scoreOrder} = 'desc' THEN douban_score END DESC, CASE WHEN #{sortBy} = 'both' AND #{scoreOrder} = 'asc' THEN douban_score END ASC, CASE WHEN #{sortBy} = 'both' AND #{timeOrder} = 'desc' THEN release_date END DESC, CASE WHEN #{sortBy} = 'both' AND #{timeOrder} = 'asc' THEN release_date END ASC, movie_id ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<Movie> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset, @Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder, @Param("scoreOrder") String scoreOrder, @Param("timeOrder") String timeOrder);

    @Select("SELECT COUNT(*) FROM movie")
    Long count();

    @Select("SELECT movie_id as movieId, name, cover, genres, directors, actors, douban_score as doubanScore, douban_votes as doubanVotes, year, release_date as releaseDate, language, region, storyline, mins FROM movie WHERE movie_id = #{movieId}")
    Movie findById(@Param("movieId") String movieId);

    @Select("SELECT COUNT(*) FROM movie WHERE movie_id = #{movieId}")
    Long countById(@Param("movieId") String movieId);

    @Select("SELECT name FROM movie WHERE movie_id = #{movieId}")
    String findNameById(@Param("movieId") String movieId);

    @Select("<script>" +
            "SELECT movie_id as movieId, name FROM movie WHERE movie_id IN " +
            "<foreach collection='movieIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, String>> findNamesByIds(@Param("movieIds") List<String> movieIds);

    @Select("<script>" +
            "SELECT movie_id as movieId, name, cover, genres, directors, language, year FROM movie WHERE movie_id IN " +
            "<foreach collection='movieIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, String>> findDetailsByIds(@Param("movieIds") List<String> movieIds);

    @Select("SELECT movie_id as movieId, name, cover, genres, directors, actors, douban_score as doubanScore, douban_votes as doubanVotes, year, release_date as releaseDate, language, region, storyline, mins FROM movie WHERE name LIKE CONCAT('%', #{keyword}, '%') OR genres LIKE CONCAT('%', #{keyword}, '%') OR directors LIKE CONCAT('%', #{keyword}, '%') OR actors LIKE CONCAT('%', #{keyword}, '%') OR language LIKE CONCAT('%', #{keyword}, '%') OR region LIKE CONCAT('%', #{keyword}, '%') OR CAST(year AS CHAR) LIKE CONCAT('%', #{keyword}, '%') ORDER BY CASE WHEN douban_score IS NOT NULL AND douban_score > 0 THEN 0 ELSE 1 END, CASE WHEN #{sortBy} = 'score' AND #{sortOrder} = 'desc' THEN douban_score END DESC, CASE WHEN #{sortBy} = 'score' AND #{sortOrder} = 'asc' THEN douban_score END ASC, CASE WHEN #{sortBy} = 'time' AND #{sortOrder} = 'desc' THEN release_date END DESC, CASE WHEN #{sortBy} = 'time' AND #{sortOrder} = 'asc' THEN release_date END ASC, CASE WHEN #{sortBy} = 'both' AND #{scoreOrder} = 'desc' THEN douban_score END DESC, CASE WHEN #{sortBy} = 'both' AND #{scoreOrder} = 'asc' THEN douban_score END ASC, CASE WHEN #{sortBy} = 'both' AND #{timeOrder} = 'desc' THEN release_date END DESC, CASE WHEN #{sortBy} = 'both' AND #{timeOrder} = 'asc' THEN release_date END ASC, movie_id ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<Movie> searchByName(@Param("keyword") String keyword, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset, @Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder, @Param("scoreOrder") String scoreOrder, @Param("timeOrder") String timeOrder);

    @Select("SELECT COUNT(*) FROM movie WHERE name LIKE CONCAT('%', #{keyword}, '%') OR genres LIKE CONCAT('%', #{keyword}, '%') OR directors LIKE CONCAT('%', #{keyword}, '%') OR actors LIKE CONCAT('%', #{keyword}, '%') OR language LIKE CONCAT('%', #{keyword}, '%') OR region LIKE CONCAT('%', #{keyword}, '%') OR CAST(year AS CHAR) LIKE CONCAT('%', #{keyword}, '%')")
    Long countByName(@Param("keyword") String keyword);

    @Select("SELECT movie_id FROM movie WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<String> findIdsByName(@Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT movie_id as movieId, name, cover, genres, directors, actors, douban_score as doubanScore, douban_votes as doubanVotes, year, release_date as releaseDate, language, region, storyline, mins FROM movie WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND (name LIKE CONCAT('%', #{keyword}, '%') OR actors LIKE CONCAT('%', #{keyword}, '%') OR directors LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<if test='genre != null and genre != \"\"'> AND genres LIKE CONCAT('%', #{genre}, '%') </if>" +
            "<if test='year != null and year != \"\"'> AND year = #{year} </if>" +
            "<if test='region != null and region != \"\"'> AND region LIKE CONCAT('%', #{region}, '%') </if>" +
            " ORDER BY CASE WHEN douban_score IS NOT NULL AND douban_score &gt; 0 THEN 0 ELSE 1 END, " +
            "CASE WHEN #{sortBy} = 'score' AND #{sortOrder} = 'desc' THEN douban_score END DESC, " +
            "CASE WHEN #{sortBy} = 'score' AND #{sortOrder} = 'asc' THEN douban_score END ASC, " +
            "CASE WHEN #{sortBy} = 'time' AND #{sortOrder} = 'desc' THEN release_date END DESC, " +
            "CASE WHEN #{sortBy} = 'time' AND #{sortOrder} = 'asc' THEN release_date END ASC, " +
            "CASE WHEN #{sortBy} = 'both' AND #{scoreOrder} = 'desc' THEN douban_score END DESC, " +
            "CASE WHEN #{sortBy} = 'both' AND #{scoreOrder} = 'asc' THEN douban_score END ASC, " +
            "CASE WHEN #{sortBy} = 'both' AND #{timeOrder} = 'desc' THEN release_date END DESC, " +
            "CASE WHEN #{sortBy} = 'both' AND #{timeOrder} = 'asc' THEN release_date END ASC, " +
            "movie_id ASC LIMIT #{pageSize} OFFSET #{offset}" +
            "</script>")
    List<Movie> searchByFilters(@Param("keyword") String keyword, @Param("genre") String genre, @Param("year") String year, @Param("region") String region, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset, @Param("sortBy") String sortBy, @Param("sortOrder") String sortOrder, @Param("scoreOrder") String scoreOrder, @Param("timeOrder") String timeOrder);

    @Select("<script>" +
            "SELECT COUNT(*) FROM movie WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'> AND (name LIKE CONCAT('%', #{keyword}, '%') OR actors LIKE CONCAT('%', #{keyword}, '%') OR directors LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "<if test='genre != null and genre != \"\"'> AND genres LIKE CONCAT('%', #{genre}, '%') </if>" +
            "<if test='year != null and year != \"\"'> AND year = #{year} </if>" +
            "<if test='region != null and region != \"\"'> AND region LIKE CONCAT('%', #{region}, '%') </if>" +
            "</script>")
    Long countByFilters(@Param("keyword") String keyword, @Param("genre") String genre, @Param("year") String year, @Param("region") String region);

    @Select("SELECT movie_id as movieId, name, cover, genres, directors, actors, douban_score as doubanScore, douban_votes as doubanVotes, year, release_date as releaseDate, language, region, storyline, mins FROM movie WHERE region LIKE CONCAT('%', #{region}, '%') ORDER BY douban_score IS NULL, douban_score DESC, movie_id ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<Movie> findByRegion(@Param("region") String region, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM movie WHERE region LIKE CONCAT('%', #{region}, '%')")
    Long countByRegion(@Param("region") String region);

    @Insert("INSERT INTO movie(movie_id, name, cover, genres, directors, actors, douban_score, douban_votes, year, release_date, language, region, storyline, mins) " +
            "VALUES (#{movieId}, #{name}, #{cover}, #{genres}, #{directors}, #{actors}, #{doubanScore}, #{doubanVotes}, #{year}, #{releaseDate}, #{language}, #{region}, #{storyline}, #{mins})")
    int insert(Movie movie);

    @Update("UPDATE movie SET name=#{name}, cover=#{cover}, genres=#{genres}, directors=#{directors}, actors=#{actors}, " +
            "douban_score=#{doubanScore}, douban_votes=#{doubanVotes}, year=#{year}, release_date=#{releaseDate}, " +
            "language=#{language}, region=#{region}, storyline=#{storyline}, mins=#{mins} WHERE movie_id=#{movieId}")
    int update(Movie movie);

    @Delete("DELETE FROM movie WHERE movie_id=#{movieId}")
    int deleteById(@Param("movieId") String movieId);
}