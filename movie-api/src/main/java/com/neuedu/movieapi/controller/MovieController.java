package com.neuedu.movieapi.controller;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Movie;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "*")
public class MovieController {
    @Autowired
    private MovieService movieService;
    
    @GetMapping
// http://localhost:8888/api/movies?pageNum=1&pageSize=10&sortBy=score&sortOrder=desc&scoreOrder=desc&timeOrder=desc
    public PageResult<Movie> list(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(defaultValue = "") String sortBy, @RequestParam(defaultValue = "desc") String sortOrder, @RequestParam(defaultValue = "desc") String scoreOrder, @RequestParam(defaultValue = "desc") String timeOrder) {
        return movieService.findAll(pageNum, pageSize, sortBy, sortOrder, scoreOrder, timeOrder);
    }

    @GetMapping("/{id}")
// http://localhost:8888/api/movies/5107534
    public Movie getById(@PathVariable String id) {
        return movieService.findById(id);
    }
    
    @GetMapping("/search")
// http://localhost:8888/api/movies/search?keyword=Interstellar&pageNum=1&pageSize=10&sortBy=score&sortOrder=desc&scoreOrder=desc&timeOrder=desc
    public PageResult<Movie> search(@RequestParam String keyword, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(defaultValue = "") String sortBy, @RequestParam(defaultValue = "desc") String sortOrder, @RequestParam(defaultValue = "desc") String scoreOrder, @RequestParam(defaultValue = "desc") String timeOrder) {
        return movieService.search(keyword, pageNum, pageSize, sortBy, sortOrder, scoreOrder, timeOrder);
    }

    @GetMapping("/byRegion")
// http://localhost:8888/api/movies/byRegion?region=美国&pageNum=1&pageSize=10
    public PageResult<Movie> listByRegion(@RequestParam String region, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return movieService.findByRegion(region, pageNum, pageSize);
    }

    @GetMapping("/filter")
// http://localhost:8888/api/movies/filter?keyword=星际&genre=科幻&year=2020&region=美国&pageNum=1&pageSize=10
    public PageResult<Movie> filter(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String genre,
                                    @RequestParam(required = false) String year,
                                    @RequestParam(required = false) String region,
                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(defaultValue = "") String sortBy,
                                    @RequestParam(defaultValue = "desc") String sortOrder,
                                    @RequestParam(defaultValue = "desc") String scoreOrder,
                                    @RequestParam(defaultValue = "desc") String timeOrder) {
        return movieService.searchByFilters(keyword, genre, year, region, pageNum, pageSize, sortBy, sortOrder, scoreOrder, timeOrder);
    }

    @PostMapping
// http://localhost:8888/api/movies
// {"name":"邋遢大王历险记","year":"2026","rating":"7.5","genre":"动作"}
    public Result<String> add(@RequestBody Movie movie) {
        return movieService.save(movie);
    }
    
    @PutMapping
// http://localhost:8888/api/movies
// {"name":"邋遢大王历险记2","year":"2022","rating":"7.2","genre":"动作2","id":4}
    public Result<String> update(@RequestBody Movie movie) {
        return movieService.update(movie);
    }
    
    @DeleteMapping("/{id}")
// http://localhost:8888/api/movies/5107534
    public Result<String> delete(@PathVariable String id) {
        return movieService.delete(id);
    }
}