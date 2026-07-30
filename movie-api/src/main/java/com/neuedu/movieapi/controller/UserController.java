package com.neuedu.movieapi.controller;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.User;
import com.neuedu.movieapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    public PageResult<User> list(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.findAll(pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable String id) {
        return userService.findById(id);
    }

    @GetMapping("/search")
    public PageResult<User> search(@RequestParam String keyword, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return userService.search(keyword, pageNum, pageSize);
    }

    @PostMapping
    public Result<String> add(@RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping
    public Result<String> update(@RequestBody User user) {
        return userService.update(user);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        return userService.delete(id);
    }
}
