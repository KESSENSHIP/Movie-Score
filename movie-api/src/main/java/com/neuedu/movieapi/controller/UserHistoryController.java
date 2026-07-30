package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.UserHistory;
import com.neuedu.movieapi.service.UserHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-history")
@CrossOrigin(origins = "*")
public class UserHistoryController {

    @Autowired
    private UserHistoryService userHistoryService;

    @PostMapping("/view")
    public Result<String> addViewHistory(@RequestBody ViewHistoryRequest request) {
        return userHistoryService.addViewHistory(request.getUserMd5(), request.getMovieId());
    }

    @PostMapping("/review")
    public Result<String> addReviewHistory(@RequestBody ReviewHistoryRequest request) {
        return userHistoryService.addReviewHistory(request.getUserMd5(), request.getMovieId(), request.getRating(), request.getComment());
    }

    @GetMapping("/view")
    public PageResult<UserHistory> getViewHistory(
            @RequestParam String userMd5,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return userHistoryService.getViewHistory(userMd5, pageNum, pageSize);
    }

    @GetMapping("/review")
    public PageResult<UserHistory> getReviewHistory(
            @RequestParam String userMd5,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return userHistoryService.getReviewHistory(userMd5, pageNum, pageSize);
    }

    @PutMapping("/review")
    public Result<String> updateReviewHistory(@RequestBody ReviewHistoryRequest request) {
        UserHistory history = new UserHistory();
        history.setId(request.getId());
        history.setUserMd5(request.getUserMd5());
        history.setMovieId(request.getMovieId());
        history.setRating(request.getRating());
        history.setComment(request.getComment());
        return userHistoryService.updateReviewHistory(history);
    }

    @DeleteMapping("/view/{id}")
    public Result<String> deleteViewHistory(@PathVariable Long id) {
        return userHistoryService.deleteViewHistory(id);
    }

    @DeleteMapping("/review/{id}")
    public Result<String> deleteReviewHistory(@PathVariable Long id) {
        return userHistoryService.deleteReviewHistory(id);
    }

    static class ViewHistoryRequest {
        private String userMd5;
        private String movieId;

        public String getUserMd5() { return userMd5; }
        public void setUserMd5(String userMd5) { this.userMd5 = userMd5; }
        public String getMovieId() { return movieId; }
        public void setMovieId(String movieId) { this.movieId = movieId; }
    }

    static class ReviewHistoryRequest {
        private Long id;
        private String userMd5;
        private String movieId;
        private Integer rating;
        private String comment;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUserMd5() { return userMd5; }
        public void setUserMd5(String userMd5) { this.userMd5 = userMd5; }
        public String getMovieId() { return movieId; }
        public void setMovieId(String movieId) { this.movieId = movieId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
