package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.entity.SysUser;
import com.neuedu.movieapi.service.SysUserService;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sys-users")
public class SysUserController {
    @Autowired
    private SysUserService sysUserService;

    @GetMapping
    public PageResult<SysUser> findAll(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return sysUserService.search(keyword, pageNum, pageSize);
        }
        return sysUserService.findAll(pageNum, pageSize);
    }

    @PostMapping
    public Result<String> create(@RequestBody SysUser sysUser) {
        return sysUserService.create(sysUser);
    }

    @PutMapping
    public Result<String> update(@RequestBody SysUser sysUser) {
        return sysUserService.update(sysUser);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        return sysUserService.delete(id);
    }
}