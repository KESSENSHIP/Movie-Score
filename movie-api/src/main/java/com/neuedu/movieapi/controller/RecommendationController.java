package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Recommendation;
import com.neuedu.movieapi.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/user/{userMd5}")
    public Result<PageResult<Recommendation>> getRecommendations(
            @PathVariable String userMd5,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "false") boolean shuffle,
            @RequestParam(required = false) String genres,
            @RequestParam(defaultValue = "0") double minScore) {
        return recommendationService.getRecommendations(userMd5, pageNum, pageSize, shuffle, genres, minScore);
    }

    @GetMapping("/user/{userMd5}/top")
    public Result<List<Recommendation>> getTopRecommendations(@PathVariable String userMd5) {
        return recommendationService.getTopRecommendations(userMd5);
    }
}
