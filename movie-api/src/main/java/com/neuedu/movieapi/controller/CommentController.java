package com.neuedu.movieapi.controller;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Comment;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @GetMapping
    public PageResult<Comment> list(@RequestParam(defaultValue = "1") Integer pageNum, 
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(defaultValue = "time") String sortBy,
                                    @RequestParam(required = false) String keyword) {
        return commentService.findAll(pageNum, pageSize, sortBy, keyword);
    }

    @GetMapping("/{id}")
    public Comment getById(@PathVariable String id) {
        return commentService.findById(id);
    }

    @GetMapping("/movie/{movieId}")
    public PageResult<Comment> getByMovieId(@PathVariable String movieId, 
                                            @RequestParam(defaultValue = "1") Integer pageNum, 
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return commentService.findByMovieId(movieId, pageNum, pageSize);
    }

    @GetMapping("/user/{userMd5}")
    public PageResult<Comment> getByUserMd5(@PathVariable String userMd5, 
                                            @RequestParam(defaultValue = "1") Integer pageNum, 
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return commentService.findByUserMd5(userMd5, pageNum, pageSize);
    }

    @PostMapping
    public Result<String> add(@RequestBody Comment comment) {
        return commentService.save(comment);
    }

    @PutMapping
    public Result<String> update(@RequestBody Comment comment) {
        return commentService.update(comment);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        return commentService.delete(id);
    }

    @DeleteMapping("/user-movie")
    public Result<String> deleteByUserAndMovie(@RequestParam String userMd5, @RequestParam String movieId) {
        return commentService.deleteByUserMd5AndMovieId(userMd5, movieId);
    }
}