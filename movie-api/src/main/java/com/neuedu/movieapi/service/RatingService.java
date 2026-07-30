package com.neuedu.movieapi.service;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Rating;
import com.neuedu.movieapi.entity.User;
import com.neuedu.movieapi.mapper.MovieMapper;
import com.neuedu.movieapi.mapper.RatingMapper;
import com.neuedu.movieapi.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RatingService {
    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MovieMapper movieMapper;

    public PageResult<Rating> findAll(Integer pageNum, Integer pageSize, String sortBy) {
        return findAll(pageNum, pageSize, sortBy, null);
    }

    public PageResult<Rating> findAll(Integer pageNum, Integer pageSize, String sortBy, String keyword) {
        // 限制最大pageNum，防止超大数值导致性能问题
        pageNum = Math.min(pageNum, 100000);
        int offset = (pageNum - 1) * pageSize;
        List<Rating> data;
        Long totalCount;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // 先尝试通过电影名称匹配查找电影ID（利用索引，速度快）
            List<String> movieIds = movieMapper.findIdsByName(kw);
            if (!movieIds.isEmpty()) {
                // 按电影ID批量查询评分（MOVIE_ID有索引）
                data = ratingMapper.findByMovieIds(movieIds, pageSize, offset);
                totalCount = ratingMapper.countByMovieIds(movieIds);
                fillNicknames(data);
                return new PageResult<>(data, pageNum, pageSize, totalCount);
            } else {
                // 没有匹配的电影名称，直接返回空（LIKE %keyword% 在410万条记录上会超时）
                data = java.util.Collections.emptyList();
                totalCount = 0L;
            }
        } else {
            data = ratingMapper.findAll(pageSize, offset);
            totalCount = ratingMapper.count();
        }
        fillNicknames(data);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Rating findById(String ratingId) {
        Rating rating = ratingMapper.findById(ratingId);
        if (rating != null && rating.getUserMd5() != null) {
            User user = userMapper.findById(rating.getUserMd5());
            rating.setNickname(user != null ? user.getNickname() : "——");
        }
        return rating;
    }

    public PageResult<Rating> findByMovieId(String movieId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Rating> data = ratingMapper.findByMovieId(movieId, pageSize, offset);
        fillNicknames(data);
        Long totalCount = ratingMapper.countByMovieId(movieId);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public PageResult<Rating> findByUserMd5(String userMd5, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Rating> data = ratingMapper.findByUserMd5(userMd5, pageSize, offset);
        fillNicknames(data);
        Long totalCount = ratingMapper.countByUserMd5(userMd5);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Result<String> save(Rating rating) {
        try {
            // 验证必填字段
            if (rating.getUserMd5() == null || rating.getUserMd5().isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            if (rating.getMovieId() == null || rating.getMovieId().isEmpty()) {
                return Result.error("电影ID不能为空");
            }
            if (rating.getRating() == null || rating.getRating() < 1 || rating.getRating() > 5) {
                return Result.error("评分必须在1-5之间");
            }
            
            // 自动生成评分ID
            if (rating.getRatingId() == null || rating.getRatingId().isEmpty()) {
                rating.setRatingId(UUID.randomUUID().toString().replace("-", ""));
            }
            
            // 允许同一用户对同一电影多次评价，不检查重复
            int result = ratingMapper.insert(rating);
            
            // 确保 user 表有该用户的映射记录（如不存在则创建）
            try {
                User existingUser = userMapper.findById(rating.getUserMd5());
                if (existingUser == null) {
                    User newUser = new User();
                    newUser.setUserMd5(rating.getUserMd5());
                    newUser.setNickname("用户" + rating.getUserMd5().substring(0, 8));
                    userMapper.insertIgnore(newUser);
                }
            } catch (Exception e) {
                // 少量数据忽略异常
            }
            
            return result > 0 ? Result.success("添加成功") : Result.error("添加失败");
        } catch (Exception e) {
            log.error("保存评分失败: ratingId={}, error={}", rating != null ? rating.getRatingId() : null, e.getMessage(), e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    public Result<String> update(Rating rating) {
        try {
            // 验证必填字段
            if (rating.getRatingId() == null || rating.getRatingId().isEmpty()) {
                return Result.error("评分ID不能为空");
            }
            if (rating.getUserMd5() == null || rating.getUserMd5().isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            if (rating.getMovieId() == null || rating.getMovieId().isEmpty()) {
                return Result.error("电影ID不能为空");
            }
            if (rating.getRating() != null && (rating.getRating() < 1 || rating.getRating() > 5)) {
                return Result.error("评分必须在1-5之间");
            }
            
            // 检查是否存在
            Rating existing = ratingMapper.findById(rating.getRatingId());
            if (existing == null) {
                return Result.error("评分不存在");
            }
            
            int result = ratingMapper.update(rating);
            return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新评分失败: ratingId={}, error={}", rating != null ? rating.getRatingId() : null, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    public Result<String> delete(String ratingId) {
        try {
            // 检查是否存在
            Rating rating = ratingMapper.findById(ratingId);
            if (rating == null) {
                return Result.error("评分不存在");
            }
            
            int result = ratingMapper.deleteById(ratingId);
            return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除评分失败: ratingId={}, error={}", ratingId, e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    public Result<String> deleteByUserMd5AndMovieId(String userMd5, String movieId) {
        try {
            ratingMapper.deleteByUserMd5AndMovieId(userMd5, movieId);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }

    /** 批量填充评分列表中每个 userMd5 对应的用户昵称 */
    private void fillNicknames(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) return;
        // 收集不重复的 userMd5
        List<String> ids = ratings.stream()
                .map(Rating::getUserMd5)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        // 批量查询用户昵称
        List<User> users = userMapper.findByIds(ids);
        Map<String, String> nicknameMap = users.stream()
                .collect(Collectors.toMap(User::getUserMd5, u -> u.getNickname() != null ? u.getNickname() : "——", (a, b) -> a));
        // 设置昵称
        for (Rating r : ratings) {
            if (r.getUserMd5() != null && nicknameMap.containsKey(r.getUserMd5())) {
                r.setNickname(nicknameMap.get(r.getUserMd5()));
            } else {
                r.setNickname("——");
            }
        }
    }
}