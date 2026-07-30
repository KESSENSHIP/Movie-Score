package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.MovieStats;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MovieStatsMapper {

    @Select("SELECT id, stat_type as statType, stat_key as statKey, stat_value as statValue, stat_count as statCount, stat_percentage as statPercentage, extra_data as extraData, created_at as createdAt FROM movie_stats WHERE stat_type = #{statType} ORDER BY stat_count DESC")
    List<MovieStats> findByStatType(@Param("statType") String statType);

    @Select("SELECT id, stat_type as statType, stat_key as statKey, stat_value as statValue, stat_count as statCount, stat_percentage as statPercentage, extra_data as extraData, created_at as createdAt FROM movie_stats WHERE stat_type = #{statType} ORDER BY stat_count DESC LIMIT #{limit}")
    List<MovieStats> findByStatTypeWithLimit(@Param("statType") String statType, @Param("limit") Integer limit);

    @Select("SELECT DISTINCT stat_type as statType FROM movie_stats")
    List<String> findAllStatTypes();

    @Delete("DELETE FROM movie_stats WHERE stat_type = #{statType}")
    int deleteByStatType(@Param("statType") String statType);
}
