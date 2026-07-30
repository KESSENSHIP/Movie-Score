package com.neuedu.movieapi.service;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Comment;
import com.neuedu.movieapi.entity.Movie;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Rating;
import com.neuedu.movieapi.entity.UserHistory;
import com.neuedu.movieapi.mapper.CommentMapper;
import com.neuedu.movieapi.mapper.MovieMapper;
import com.neuedu.movieapi.mapper.RatingMapper;
import com.neuedu.movieapi.mapper.UserHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserHistoryService {

    @Autowired
    private UserHistoryMapper userHistoryMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RatingMapper ratingMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Result<String> addViewHistory(String userMd5, String movieId) {
        try {
            UserHistory history = new UserHistory();
            history.setUserMd5(userMd5);
            history.setMovieId(movieId);
            history.setViewTime(LocalDateTime.now().format(FORMATTER));
            
            // 先尝试更新已存在的浏览记录（去重：同一用户对同一电影只保留一条）
            int updated = userHistoryMapper.updateViewHistory(history);
            if (updated == 0) {
                // 如果没有找到记录，插入新记录
                userHistoryMapper.insertViewHistory(history);
            }
            return Result.success("浏览记录添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("浏览记录添加失败");
        }
    }

    public Result<String> addReviewHistory(String userMd5, String movieId, Integer rating, String comment) {
        try {
            UserHistory history = new UserHistory();
            history.setUserMd5(userMd5);
            history.setMovieId(movieId);
            history.setRating(rating);
            history.setComment(comment);
            history.setReviewTime(LocalDateTime.now().format(FORMATTER));
            
            userHistoryMapper.insertReviewHistory(history);
            return Result.success("评价记录添加成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("评价记录添加失败");
        }
    }

    public PageResult<UserHistory> getViewHistory(String userMd5, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<UserHistory> list = userHistoryMapper.findViewHistoryByUserMd5(userMd5, pageSize, offset);
        int totalCount = userHistoryMapper.countViewHistory(userMd5);
        
        // 批量填充电影信息
        fillMovieInfo(list);
        
        return new PageResult<>(list, pageNum, pageSize, (long) totalCount);
    }

    public PageResult<UserHistory> getReviewHistory(String userMd5, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<UserHistory> list = userHistoryMapper.findReviewHistoryByUserMd5(userMd5, pageSize, offset);
        int totalCount = userHistoryMapper.countReviewHistory(userMd5);
        
        // 批量填充电影信息
        fillMovieInfo(list);
        
        return new PageResult<>(list, pageNum, pageSize, (long) totalCount);
    }

    // 批量填充电影信息
    private void fillMovieInfo(List<UserHistory> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return;
        }
        
        // 缓存电影信息，避免重复查询
        Map<String, Movie> movieCache = new HashMap<>();
        
        for (UserHistory history : historyList) {
            String movieId = history.getMovieId();
            if (movieId == null || movieId.isEmpty()) {
                continue;
            }
            
            Movie movie = movieCache.get(movieId);
            if (movie == null) {
                movie = movieMapper.findById(movieId);
                if (movie != null) {
                    movieCache.put(movieId, movie);
                }
            }
            
            if (movie != null) {
                history.setMovieName(movie.getName());
                history.setMovieCover(movie.getCover());
            }
        }
    }

    public Result<String> deleteViewHistory(Long id) {
        try {
            userHistoryMapper.deleteById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }

    public Result<String> deleteReviewHistory(Long id) {
        try {
            userHistoryMapper.deleteById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }

    public Result<String> updateReviewHistory(UserHistory history) {
        try {
            history.setReviewTime(LocalDateTime.now().format(FORMATTER));
            userHistoryMapper.updateReviewHistory(history);

            // 同步更新 comment 表
            try {
                Comment existingComment = commentMapper.findByUserMd5AndMovieId(history.getUserMd5(), history.getMovieId());
                if (existingComment != null) {
                    existingComment.setContent(history.getComment());
                    existingComment.setRating(history.getRating());
                    existingComment.setCommentTime(history.getReviewTime());
                    commentMapper.update(existingComment);
                }
            } catch (Exception e) {
                // 忽略 comment 表更新异常
            }

            // 同步更新 rating 表
            try {
                Rating existingRating = ratingMapper.findByUserMd5AndMovieId(history.getUserMd5(), history.getMovieId());
                if (existingRating != null) {
                    existingRating.setRating(history.getRating());
                    existingRating.setRatingTime(history.getReviewTime());
                    ratingMapper.update(existingRating);
                }
            } catch (Exception e) {
                // 忽略 rating 表更新异常
            }

            return Result.success("评价更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("评价更新失败");
        }
    }
}
