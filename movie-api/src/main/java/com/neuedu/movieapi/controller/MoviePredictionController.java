package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.entity.MoviePrediction;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.mapper.MoviePredictionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "*")
public class MoviePredictionController {

    @Autowired
    private MoviePredictionMapper predictionMapper;

    @GetMapping
    // http://localhost:8888/api/predictions?pageNum=1&pageSize=10
    public PageResult<MoviePrediction> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageNum > 100000) pageNum = 100000;
        int offset = (pageNum - 1) * pageSize;
        long total = predictionMapper.countAll();
        return new PageResult<>(predictionMapper.findAll(pageSize, offset), pageNum, pageSize, total);
    }

    @GetMapping("/filter")
    // http://localhost:8888/api/predictions/filter?keyword=星际&genre=科幻&year=2020&region=美国&minScore=6&maxScore=8&pageNum=1&pageSize=10
    public PageResult<MoviePrediction> filter(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String genre,
                                              @RequestParam(required = false) String year,
                                              @RequestParam(required = false) String region,
                                              @RequestParam(required = false) Double minScore,
                                              @RequestParam(required = false) Double maxScore,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        if (pageNum > 100000) pageNum = 100000;
        int offset = (pageNum - 1) * pageSize;

        if (keyword != null && !keyword.isEmpty() && genre == null && year == null && region == null && minScore == null && maxScore == null) {
            // 仅关键词搜索
            long total = predictionMapper.countSearch(keyword);
            return new PageResult<>(predictionMapper.search(keyword, pageSize, offset), pageNum, pageSize, total);
        }

        long total = predictionMapper.countByFilters(keyword, genre, year, region, minScore, maxScore);
        return new PageResult<>(predictionMapper.searchByFilters(keyword, genre, year, region, minScore, maxScore, pageSize, offset), pageNum, pageSize, total);
    }
}
