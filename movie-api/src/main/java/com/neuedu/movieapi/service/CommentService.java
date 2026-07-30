package com.neuedu.movieapi.service;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Comment;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.User;
import com.neuedu.movieapi.mapper.CommentMapper;
import com.neuedu.movieapi.mapper.MovieMapper;
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
public class CommentService {
    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MovieMapper movieMapper;

    public PageResult<Comment> findAll(Integer pageNum, Integer pageSize, String sortBy) {
        return findAll(pageNum, pageSize, sortBy, null);
    }

    public PageResult<Comment> findAll(Integer pageNum, Integer pageSize, String sortBy, String keyword) {
        pageNum = Math.min(pageNum, 100000);
        int offset = (pageNum - 1) * pageSize;
        List<Comment> data;
        Long totalCount;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            if (kw.length() <= 12) {
                Long movieIdCount = movieMapper.countById(kw);
                if (movieIdCount != null && movieIdCount > 0) {
                    data = commentMapper.findByMovieId(kw, pageSize, offset);
                    totalCount = commentMapper.countByMovieId(kw);
                    fillNicknames(data);
                    return new PageResult<>(data, pageNum, pageSize, totalCount);
                }
                Long userIdCount = userMapper.countByUserMd5(kw);
                if (userIdCount != null && userIdCount > 0) {
                    data = commentMapper.findByUserMd5(kw, pageSize, offset);
                    totalCount = commentMapper.countByUserMd5(kw);
                    fillNicknames(data);
                    return new PageResult<>(data, pageNum, pageSize, totalCount);
                }
            }
            List<String> movieIds = movieMapper.findIdsByName(kw);
            if (!movieIds.isEmpty()) {
                data = commentMapper.findByMovieIds(movieIds, pageSize, offset);
                totalCount = commentMapper.countByMovieIds(movieIds);
                fillNicknames(data);
                return new PageResult<>(data, pageNum, pageSize, totalCount);
            }
            data = commentMapper.search(kw, pageSize, offset);
            totalCount = commentMapper.countSearch(kw);
        } else {
            data = commentMapper.findAll(pageSize, offset);
            totalCount = commentMapper.count();
        }
        fillNicknames(data);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Comment findById(String commentId) {
        Comment comment = commentMapper.findById(commentId);
        if (comment != null && comment.getUserMd5() != null) {
            User user = userMapper.findById(comment.getUserMd5());
            comment.setNickname(user != null ? user.getNickname() : "——");
        }
        return comment;
    }

    public PageResult<Comment> findByMovieId(String movieId, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Comment> data = commentMapper.findByMovieId(movieId, pageSize, offset);
        fillNicknames(data);
        Long totalCount = commentMapper.countByMovieId(movieId);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public PageResult<Comment> findByUserMd5(String userMd5, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Comment> data = commentMapper.findByUserMd5(userMd5, pageSize, offset);
        fillNicknames(data);
        Long totalCount = commentMapper.countByUserMd5(userMd5);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Result<String> save(Comment comment) {
        try {
            // 验证必填字段
            if (comment.getUserMd5() == null || comment.getUserMd5().isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            if (comment.getMovieId() == null || comment.getMovieId().isEmpty()) {
                return Result.error("电影ID不能为空");
            }
            if (comment.getContent() == null || comment.getContent().isEmpty()) {
                return Result.error("评论内容不能为空");
            }
            if (comment.getRating() != null && (comment.getRating() < 1 || comment.getRating() > 5)) {
                return Result.error("评分必须在1-5之间");
            }
            if (comment.getVotes() != null && comment.getVotes() < 0) {
                return Result.error("点赞数不能为负数");
            }
            
            // 自动生成评论ID
            if (comment.getCommentId() == null || comment.getCommentId().isEmpty()) {
                comment.setCommentId(UUID.randomUUID().toString().replace("-", ""));
            }
            
            // 允许同一用户对同一电影多次评价，不检查重复
            int result = commentMapper.insert(comment);
            
            // 确保 user 表有该用户的映射记录（如不存在则创建）
            try {
                User existingUser = userMapper.findById(comment.getUserMd5());
                if (existingUser == null) {
                    User newUser = new User();
                    newUser.setUserMd5(comment.getUserMd5());
                    newUser.setNickname("用户" + comment.getUserMd5().substring(0, 8));
                    userMapper.insertIgnore(newUser);
                }
            } catch (Exception e) {
                // 少量数据忽略异常
            }
            
            return result > 0 ? Result.success("添加成功") : Result.error("添加失败");
        } catch (Exception e) {
            log.error("保存评论失败: commentId={}, error={}", comment != null ? comment.getCommentId() : null, e.getMessage(), e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    public Result<String> update(Comment comment) {
        try {
            // 验证必填字段
            if (comment.getCommentId() == null || comment.getCommentId().isEmpty()) {
                return Result.error("评论ID不能为空");
            }
            if (comment.getUserMd5() == null || comment.getUserMd5().isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            if (comment.getMovieId() == null || comment.getMovieId().isEmpty()) {
                return Result.error("电影ID不能为空");
            }
            if (comment.getContent() == null || comment.getContent().isEmpty()) {
                return Result.error("评论内容不能为空");
            }
            if (comment.getRating() != null && (comment.getRating() < 1 || comment.getRating() > 5)) {
                return Result.error("评分必须在1-5之间");
            }
            if (comment.getVotes() != null && comment.getVotes() < 0) {
                return Result.error("点赞数不能为负数");
            }
            
            // 检查是否存在
            Comment existing = commentMapper.findById(comment.getCommentId());
            if (existing == null) {
                return Result.error("评论不存在");
            }
            
            int result = commentMapper.update(comment);
            return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新评论失败: commentId={}, error={}", comment != null ? comment.getCommentId() : null, e.getMessage(), e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    public Result<String> delete(String commentId) {
        try {
            // 检查是否存在
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                return Result.error("评论不存在");
            }
            
            int result = commentMapper.deleteById(commentId);
            return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除评论失败: commentId={}, error={}", commentId, e.getMessage(), e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    public Result<String> deleteByUserMd5AndMovieId(String userMd5, String movieId) {
        try {
            commentMapper.deleteByUserMd5AndMovieId(userMd5, movieId);
            return Result.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败");
        }
    }

    /** 批量填充评论列表中每个 userMd5 对应的用户昵称 */
    private void fillNicknames(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;
        List<String> ids = comments.stream()
                .map(Comment::getUserMd5)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        List<User> users = userMapper.findByIds(ids);
        Map<String, String> nicknameMap = users.stream()
                .collect(Collectors.toMap(User::getUserMd5, u -> u.getNickname() != null ? u.getNickname() : "——", (a, b) -> a));
        for (Comment c : comments) {
            if (c.getUserMd5() != null && nicknameMap.containsKey(c.getUserMd5())) {
                c.setNickname(nicknameMap.get(c.getUserMd5()));
            } else {
                c.setNickname("——");
            }
        }
    }
}