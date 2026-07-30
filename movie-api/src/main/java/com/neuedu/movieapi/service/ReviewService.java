package com.neuedu.movieapi.service;

import com.neuedu.movieapi.entity.Comment;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Rating;
import com.neuedu.movieapi.entity.ReviewVO;
import com.neuedu.movieapi.mapper.CommentMapper;
import com.neuedu.movieapi.mapper.MovieMapper;
import com.neuedu.movieapi.mapper.RatingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MovieMapper movieMapper;

    public PageResult<ReviewVO> findByUserMd5(String userMd5, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        
        // 查询评分和评论（限制500条，避免内存溢出）
        List<Rating> ratings;
        List<Comment> comments;
        try {
            ratings = ratingMapper.findByUserMd5(userMd5, 500, 0);
        } catch (Exception e) {
            ratings = new ArrayList<>();
        }
        try {
            comments = commentMapper.findByUserMd5(userMd5, 500, 0);
        } catch (Exception e) {
            comments = new ArrayList<>();
        }
        
        // 按movieId分组评分和评论
        Map<String, List<Rating>> ratingMap = ratings.stream()
                .collect(Collectors.groupingBy(Rating::getMovieId));
        Map<String, List<Comment>> commentMap = comments.stream()
                .collect(Collectors.groupingBy(Comment::getMovieId));
        
        // 合并数据：同一电影的评分和评论合并到一行
        List<ReviewVO> reviews = new ArrayList<>();
        
        // 获取所有电影ID
        Set<String> allMovieIds = new HashSet<>();
        allMovieIds.addAll(ratingMap.keySet());
        allMovieIds.addAll(commentMap.keySet());
        
        for (String movieId : allMovieIds) {
            List<Rating> movieRatings = ratingMap.getOrDefault(movieId, Collections.emptyList());
            List<Comment> movieComments = commentMap.getOrDefault(movieId, Collections.emptyList());
            
            // 创建合并后的记录
            ReviewVO vo = new ReviewVO();
            vo.setMovieId(movieId);
            
            // 如果有评分，取第一个评分信息
            if (!movieRatings.isEmpty()) {
                Rating rating = movieRatings.get(0);
                vo.setRating(rating.getRating());
                vo.setRatingId(rating.getRatingId());
                vo.setRatingTime(formatDateTime(rating.getRatingTime()));
            }
            
            // 如果有评论，取第一个评论信息
            if (!movieComments.isEmpty()) {
                Comment comment = movieComments.get(0);
                vo.setCommentId(comment.getCommentId());
                vo.setContent(comment.getContent());
                vo.setVotes(comment.getVotes());
                vo.setCommentTime(formatDateTime(comment.getCommentTime()));
                // 如果评论有评分且评分字段为空，使用评论的评分
                if (vo.getRating() == null) {
                    vo.setRating(comment.getRating());
                }
            }
            
            // 设置类型：两者都有显示"评价"，只有评分显示"评分"，只有评论显示"评论"
            boolean hasRating = !movieRatings.isEmpty();
            boolean hasComment = !movieComments.isEmpty();
            if (hasRating && hasComment) {
                vo.setType("评价");
                vo.setId(vo.getCommentId() != null ? vo.getCommentId() : vo.getRatingId());
                vo.setTime(vo.getCommentTime() != null ? vo.getCommentTime() : vo.getRatingTime());
            } else if (hasRating) {
                vo.setType("评分");
                vo.setId(vo.getRatingId());
                vo.setTime(vo.getRatingTime());
            } else {
                vo.setType("评论");
                vo.setId(vo.getCommentId());
                vo.setTime(vo.getCommentTime());
            }
            
            reviews.add(vo);
        }
        
        // 按时间倒序排序
        reviews.sort((a, b) -> {
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return b.getTime().compareTo(a.getTime());
        });
        
        // 分页处理
        int totalSize = reviews.size();
        int fromIndex = offset;
        int toIndex = Math.min(offset + pageSize, totalSize);
        
        // 处理空列表情况
        List<ReviewVO> pageData;
        if (fromIndex >= totalSize) {
            pageData = new ArrayList<>();
        } else {
            pageData = reviews.subList(fromIndex, toIndex);
        }
        
        // 批量获取电影名称（代替N+1查询）
        Set<String> movieIds = pageData.stream()
                .map(ReviewVO::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        Map<String, String> movieNameMap = new HashMap<>();
        if (!movieIds.isEmpty()) {
            try {
                List<Map<String, String>> nameList = movieMapper.findNamesByIds(new java.util.ArrayList<>(movieIds));
                if (nameList != null) {
                    for (Map<String, String> entry : nameList) {
                        movieNameMap.put(entry.get("movieId"), entry.get("name"));
                    }
                }
            } catch (Exception e) {
                System.err.println("批量获取电影名称失败: " + e.getMessage());
            }
        }
        
        for (ReviewVO vo : pageData) {
            vo.setMovieName(movieNameMap.getOrDefault(vo.getMovieId(), "未知电影"));
        }
        
        return new PageResult<>(pageData, pageNum, pageSize, (long) totalSize);
    }
    
    private String formatDateTime(Object dateTime) {
        if (dateTime == null) return null;
        String str = dateTime.toString();
        if (str.contains("T")) {
            str = str.replace("T", " ");
        }
        if (str.contains(".") && str.length() > 19) {
            str = str.substring(0, 19);
        } else if (str.length() > 19) {
            str = str.substring(0, 19);
        }
        return str;
    }

    public PageResult<ReviewVO> findByMovieId(String movieId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        
        // 查询该电影的所有评分和评论
        List<Rating> ratings = ratingMapper.findByMovieId(movieId, 1000, 0);
        List<Comment> comments = commentMapper.findByMovieId(movieId, 1000, 0);
        
        // 合并数据
        List<ReviewVO> reviews = new ArrayList<>();
        
        // 添加所有评分
        for (Rating rating : ratings) {
            ReviewVO vo = new ReviewVO();
            vo.setId(rating.getRatingId());
            vo.setUserMd5(rating.getUserMd5());
            vo.setMovieId(rating.getMovieId());
            vo.setRating(rating.getRating());
            vo.setTime(formatDateTime(rating.getRatingTime()));
            vo.setType("评分");
            reviews.add(vo);
        }
        
        // 添加所有评论
        for (Comment comment : comments) {
            ReviewVO vo = new ReviewVO();
            vo.setId(comment.getCommentId());
            vo.setUserMd5(comment.getUserMd5());
            vo.setMovieId(comment.getMovieId());
            vo.setRating(comment.getRating());
            vo.setTime(formatDateTime(comment.getCommentTime()));
            vo.setType("评论");
            vo.setContent(comment.getContent());
            vo.setVotes(comment.getVotes());
            reviews.add(vo);
        }
        
        // 按时间倒序排序
        reviews.sort((a, b) -> {
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return b.getTime().compareTo(a.getTime());
        });
        
        // 分页处理
        int totalSize = reviews.size();
        int fromIndex = offset;
        int toIndex = Math.min(offset + pageSize, totalSize);
        
        List<ReviewVO> pageData;
        if (fromIndex >= totalSize) {
            pageData = new ArrayList<>();
        } else {
            pageData = reviews.subList(fromIndex, toIndex);
        }
        
        // 获取电影名称
        String movieName = "未知电影";
        try {
            movieName = movieMapper.findNameById(movieId);
            if (movieName == null) {
                movieName = "未知电影";
            }
        } catch (Exception e) {
            // ignore
        }
        
        for (ReviewVO vo : pageData) {
            vo.setMovieName(movieName);
        }
        
        return new PageResult<>(pageData, pageNum, pageSize, (long) totalSize);
    }
}