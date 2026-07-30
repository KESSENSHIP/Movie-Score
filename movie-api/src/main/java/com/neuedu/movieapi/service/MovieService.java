package com.neuedu.movieapi.service;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Movie;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.mapper.MovieMapper;
import com.neuedu.movieapi.mapper.RatingMapper;
import com.neuedu.movieapi.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MovieService {
    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private CommentMapper commentMapper;
    
    public PageResult<Movie> findAll(Integer pageNum, Integer pageSize, String sortBy, String sortOrder, String scoreOrder, String timeOrder) {
        int offset = (pageNum - 1) * pageSize;
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "desc";
        }
        if (scoreOrder == null || scoreOrder.isEmpty()) {
            scoreOrder = "desc";
        }
        if (timeOrder == null || timeOrder.isEmpty()) {
            timeOrder = "desc";
        }
        List<Movie> data = movieMapper.findAll(pageSize, offset, sortBy, sortOrder, scoreOrder, timeOrder);
        Long totalCount = movieMapper.count();
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }
    
    public Movie findById(String movieId) {
        return movieMapper.findById(movieId);
    }
    
    public PageResult<Movie> search(String keyword, Integer pageNum, Integer pageSize, String sortBy, String sortOrder, String scoreOrder, String timeOrder) {
        int offset = (pageNum - 1) * pageSize;
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "desc";
        }
        if (scoreOrder == null || scoreOrder.isEmpty()) {
            scoreOrder = "desc";
        }
        if (timeOrder == null || timeOrder.isEmpty()) {
            timeOrder = "desc";
        }
        List<Movie> data = movieMapper.searchByName(keyword, pageSize, offset, sortBy, sortOrder, scoreOrder, timeOrder);
        Long totalCount = movieMapper.countByName(keyword);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }
    
    public PageResult<Movie> findByRegion(String region, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Movie> data = movieMapper.findByRegion(region, pageSize, offset);
        Long totalCount = movieMapper.countByRegion(region);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public PageResult<Movie> searchByFilters(String keyword, String genre, String year, String region, Integer pageNum, Integer pageSize, String sortBy, String sortOrder, String scoreOrder, String timeOrder) {
        int offset = (pageNum - 1) * pageSize;
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "";
        }
        if (sortOrder == null || sortOrder.isEmpty()) {
            sortOrder = "desc";
        }
        if (scoreOrder == null || scoreOrder.isEmpty()) {
            scoreOrder = "desc";
        }
        if (timeOrder == null || timeOrder.isEmpty()) {
            timeOrder = "desc";
        }
        // 如果没有任何筛选项，回退到 findAll
        if ((keyword == null || keyword.isEmpty()) && (genre == null || genre.isEmpty()) && (year == null || year.isEmpty()) && (region == null || region.isEmpty())) {
            return findAll(pageNum, pageSize, sortBy, sortOrder, scoreOrder, timeOrder);
        }
        // 清理空字符串为 null，避免 mybatis if 判断误判
        if (keyword != null && keyword.trim().isEmpty()) keyword = null;
        if (genre != null && genre.trim().isEmpty()) genre = null;
        if (year != null && year.trim().isEmpty()) year = null;
        if (region != null && region.trim().isEmpty()) region = null;
        List<Movie> data = movieMapper.searchByFilters(keyword, genre, year, region, pageSize, offset, sortBy, sortOrder, scoreOrder, timeOrder);
        Long totalCount = movieMapper.countByFilters(keyword, genre, year, region);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }
    
    public Result<String> save(Movie movie) {
        // 验证必填字段
        if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
            return Result.error("电影ID不能为空");
        }
        if (movie.getName() == null || movie.getName().isEmpty()) {
            return Result.error("电影名称不能为空");
        }
        
        // 检查是否已存在
        Movie existing = movieMapper.findById(movie.getMovieId());
        if (existing != null) {
            return Result.error("电影ID已存在");
        }
        
        int result = movieMapper.insert(movie);
        return result > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }
    
    public Result<String> update(Movie movie) {
        // 验证必填字段
        if (movie.getMovieId() == null || movie.getMovieId().isEmpty()) {
            return Result.error("电影ID不能为空");
        }
        if (movie.getName() == null || movie.getName().isEmpty()) {
            return Result.error("电影名称不能为空");
        }
        
        // 检查是否存在
        Movie existing = movieMapper.findById(movie.getMovieId());
        if (existing == null) {
            return Result.error("电影不存在");
        }
        
        int result = movieMapper.update(movie);
        return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }
    
    public Result<String> delete(String movieId) {
        // 检查是否存在
        Movie movie = movieMapper.findById(movieId);
        if (movie == null) {
            return Result.error("电影不存在");
        }
        
        // 检查是否有关联数据
        Long ratingCount = ratingMapper.countByMovieId(movieId);
        Long commentCount = commentMapper.countByMovieId(movieId);
        
        if (ratingCount > 0 || commentCount > 0) {
            return Result.error("该电影存在关联数据（评分：" + ratingCount + "条，评论：" + commentCount + "条），无法删除");
        }
        
        int result = movieMapper.deleteById(movieId);
        return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}