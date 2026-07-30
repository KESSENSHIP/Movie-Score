package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.MovieStats;
import com.neuedu.movieapi.service.MovieStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class MovieStatsController {

    @Autowired
    private MovieStatsService movieStatsService;

    /**
     * 根据统计类型获取统计数据
     * 例如: /api/stats/region_top50
     */
    @GetMapping("/{statType}")
    public Result<List<MovieStats>> getByStatType(@PathVariable String statType) {
        try {
            List<MovieStats> data = movieStatsService.findByStatType(statType);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("500", "获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 根据统计类型获取统计数据（带限制）
     * 例如: /api/stats/region_top50?limit=20
     */
    @GetMapping("/{statType}/limit")
    public Result<List<MovieStats>> getByStatTypeWithLimit(
            @PathVariable String statType,
            @RequestParam(defaultValue = "50") Integer limit) {
        try {
            List<MovieStats> data = movieStatsService.findByStatTypeWithLimit(statType, limit);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("500", "获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有统计类型
     * 例如: /api/stats/types
     */
    @GetMapping("/types/list")
    public Result<List<String>> getAllStatTypes() {
        try {
            List<String> data = movieStatsService.findAllStatTypes();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("500", "获取统计类型失败: " + e.getMessage());
        }
    }

    /**
     * 根据统计类型删除数据
     * 例如: /api/stats/region_top50
     */
    @DeleteMapping("/{statType}")
    public Result<String> deleteByStatType(@PathVariable String statType) {
        try {
            int result = movieStatsService.deleteByStatType(statType);
            return Result.success("删除成功，共删除 " + result + " 条记录");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
