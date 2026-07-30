package com.neuedu.movieapi.controller;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Rating;
import com.neuedu.movieapi.service.RatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
@CrossOrigin(origins = "*")
public class RatingController {
    @Autowired
    private RatingService ratingService;

    @GetMapping
    public PageResult<Rating> list(@RequestParam(defaultValue = "1") Integer pageNum, 
                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                   @RequestParam(defaultValue = "time") String sortBy,
                                   @RequestParam(required = false) String keyword) {
        return ratingService.findAll(pageNum, pageSize, sortBy, keyword);
    }

    @GetMapping("/{id}")
    public Rating getById(@PathVariable String id) {
        return ratingService.findById(id);
    }

    @GetMapping("/movie/{movieId}")
    public PageResult<Rating> getByMovieId(@PathVariable String movieId, 
                                           @RequestParam(defaultValue = "1") Integer pageNum, 
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return ratingService.findByMovieId(movieId, pageNum, pageSize);
    }

    @GetMapping("/user/{userMd5}")
    public PageResult<Rating> getByUserMd5(@PathVariable String userMd5, 
                                           @RequestParam(defaultValue = "1") Integer pageNum, 
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        return ratingService.findByUserMd5(userMd5, pageNum, pageSize);
    }

    @PostMapping
    public Result<String> add(@RequestBody Rating rating) {
        return ratingService.save(rating);
    }

    @PutMapping
    public Result<String> update(@RequestBody Rating rating) {
        return ratingService.update(rating);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        return ratingService.delete(id);
    }

    @DeleteMapping("/user-movie")
    public Result<String> deleteByUserAndMovie(@RequestParam String userMd5, @RequestParam String movieId) {
        return ratingService.deleteByUserMd5AndMovieId(userMd5, movieId);
    }
}