package com.neuedu.movieapi.service;

import com.neuedu.movieapi.entity.SysUser;
import com.neuedu.movieapi.mapper.SysUserMapper;
import com.neuedu.movieapi.mapper.UserMapper;
import com.neuedu.movieapi.mapper.RatingMapper;
import com.neuedu.movieapi.mapper.CommentMapper;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public PageResult<SysUser> findAll(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<SysUser> data = sysUserMapper.findAll(pageSize, offset);
        Long totalCount = sysUserMapper.countAll();
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public PageResult<SysUser> search(String keyword, Integer pageNum, Integer pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageNum, pageSize);
        }
        int offset = (pageNum - 1) * pageSize;
        List<SysUser> data = sysUserMapper.search(keyword.trim(), pageSize, offset);
        Long totalCount = sysUserMapper.countSearch(keyword.trim());
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Result<String> create(SysUser sysUser) {
        // 检查用户名是否已存在
        if (sysUserMapper.findByUsername(sysUser.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        
        // 默认角色为用户
        if (sysUser.getRole() == null || sysUser.getRole().isEmpty()) {
            sysUser.setRole("USER");
        }
        
        // 默认状态为启用
        if (sysUser.getStatus() == null) {
            sysUser.setStatus(1);
        }
        
        // 加密密码
        sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        sysUser.setCreatedAt(now);
        sysUser.setUpdatedAt(now);
        
        int result = sysUserMapper.insert(sysUser);
        if (result > 0) {
            return Result.success("创建成功");
        }
        return Result.error("创建失败");
    }

    public Result<String> update(SysUser sysUser) {
        // 检查用户是否存在
        SysUser existingUser = sysUserMapper.findById(sysUser.getId());
        if (existingUser == null) {
            return Result.error("用户不存在");
        }
        
        // 如果密码有值，说明需要更新密码
        if (sysUser.getPassword() != null && !sysUser.getPassword().isEmpty()) {
            sysUser.setPassword(passwordEncoder.encode(sysUser.getPassword()));
        } else {
            // 保留原密码
            sysUser.setPassword(existingUser.getPassword());
        }
        
        // 设置更新时间
        sysUser.setUpdatedAt(LocalDateTime.now());
        
        int result = sysUserMapper.update(sysUser);
        if (result > 0) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    public Result<String> delete(Long id) {
        SysUser existingUser = sysUserMapper.findById(id);
        if (existingUser == null) {
            return Result.error("用户不存在");
        }
        
        if ("ADMIN".equals(existingUser.getRole())) {
            Long adminCount = sysUserMapper.countAdmins();
            if (adminCount <= 1) {
                return Result.error("不能删除最后一个系统管理员");
            }
        }

        String userMd5 = DigestUtils.md5DigestAsHex(existingUser.getUsername().getBytes(StandardCharsets.UTF_8));

        Long ratingCount = ratingMapper.countByUserMd5(userMd5);
        if (ratingCount != null && ratingCount > 0) {
            ratingMapper.deleteByUserMd5(userMd5);
        }

        Long commentCount = commentMapper.countByUserMd5(userMd5);
        if (commentCount != null && commentCount > 0) {
            commentMapper.deleteByUserMd5(userMd5);
        }

        userMapper.deleteById(userMd5);
        
        int result = sysUserMapper.deleteById(id);
        if (result > 0) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
}