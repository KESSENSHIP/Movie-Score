package com.neuedu.movieapi.service;

import com.neuedu.movieapi.entity.MovieStats;
import com.neuedu.movieapi.mapper.MovieStatsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieStatsService {

    @Autowired
    private MovieStatsMapper movieStatsMapper;

    /**
     * 根据统计类型获取统计数据
     */
    public List<MovieStats> findByStatType(String statType) {
        return movieStatsMapper.findByStatType(statType);
    }

    /**
     * 根据统计类型获取统计数据（带限制）
     */
    public List<MovieStats> findByStatTypeWithLimit(String statType, Integer limit) {
        return movieStatsMapper.findByStatTypeWithLimit(statType, limit);
    }

    /**
     * 获取所有统计类型
     */
    public List<String> findAllStatTypes() {
        return movieStatsMapper.findAllStatTypes();
    }

    /**
     * 根据统计类型删除数据
     */
    public int deleteByStatType(String statType) {
        return movieStatsMapper.deleteByStatType(statType);
    }
}
