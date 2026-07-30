package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.ReviewVO;
import com.neuedu.movieapi.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/user/{userMd5}")
    public PageResult<ReviewVO> getByUserMd5(@PathVariable String userMd5,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return reviewService.findByUserMd5(userMd5, pageNum, pageSize);
    }

    @GetMapping("/movie/{movieId}")
    public PageResult<ReviewVO> getByMovieId(@PathVariable String movieId,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return reviewService.findByMovieId(movieId, pageNum, pageSize);
    }
}