package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Comment;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.mapper.CommentMapper;
import com.neuedu.movieapi.service.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@CrossOrigin(origins = "*")
public class SentimentAnalysisController {

    @Autowired
    private SentimentAnalysisService sentimentService;

    @Autowired
    private CommentMapper commentMapper;

    @PostMapping("/analyze/{movieId}")
    public Result<Map<String, Object>> analyzeMovie(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "20") int sampleSize,
            @RequestParam(defaultValue = "false") boolean forceReanalyze) {
        Map<String, Object> result = sentimentService.analyzeMovieSentiment(movieId, sampleSize, forceReanalyze);
        if ((boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.error(500, (String) result.get("message"));
        }
    }

    @GetMapping("/movie/{movieId}/distribution")
    public Result<Map<String, Object>> getDistribution(@PathVariable String movieId) {
        Map<String, Object> distribution = sentimentService.getDistribution(movieId);
        return Result.success(distribution);
    }

    @GetMapping("/movie/{movieId}/analyses")
    public Result<List<Map<String, Object>>> getAnalyses(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> analyses = sentimentService.getRecentAnalyses(movieId, limit);
        return Result.success(analyses);
    }

    @PostMapping("/comment/{commentId}")
    public Result<Map<String, Object>> analyzeSingleComment(@PathVariable String commentId) {
        return sentimentService.analyzeComment(commentId);
    }

    @GetMapping("/movie/{movieId}/comments")
    public Result<PageResult<Comment>> getComments(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Comment> data = commentMapper.findByMovieId(movieId, pageSize, offset);
        Long total = commentMapper.countByMovieId(movieId);
        return Result.success(new PageResult<>(data, pageNum, pageSize, total));
    }
}
