package com.neuedu.movieapi.service;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.SysUser;
import com.neuedu.movieapi.entity.User;
import com.neuedu.movieapi.mapper.SysUserMapper;
import com.neuedu.movieapi.mapper.UserMapper;
import com.neuedu.movieapi.mapper.RatingMapper;
import com.neuedu.movieapi.mapper.CommentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PageResult<User> findAll(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<User> data = userMapper.findAll(pageSize, offset);
        Long totalCount = userMapper.count();
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public User findById(String userMd5) {
        return userMapper.findById(userMd5);
    }

    public PageResult<User> search(String keyword, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<User> data = userMapper.searchByName(keyword, pageSize, offset);
        Long totalCount = userMapper.countByName(keyword);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Result<String> save(User user) {
        // 验证必填字段
        if (user.getUserMd5() == null || user.getUserMd5().isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            return Result.error("用户昵称不能为空");
        }
        
        // 检查是否已存在
        User existing = userMapper.findById(user.getUserMd5());
        if (existing != null) {
            return Result.error("用户ID已存在");
        }
        
        int result = userMapper.insert(user);
        return result > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    public Result<String> update(User user) {
        // 验证必填字段
        if (user.getUserMd5() == null || user.getUserMd5().isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            return Result.error("用户昵称不能为空");
        }
        
        // 检查是否存在
        User existing = userMapper.findById(user.getUserMd5());
        if (existing == null) {
            return Result.error("用户不存在");
        }
        
        int result = userMapper.update(user);
        return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    public Result<String> delete(String userMd5) {
        // 检查是否存在
        User user = userMapper.findById(userMd5);
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        // 检查是否有关联数据（评分和评论）
        Long ratingCount = ratingMapper.countByUserMd5(userMd5);
        Long commentCount = commentMapper.countByUserMd5(userMd5);
        
        if (ratingCount > 0 || commentCount > 0) {
            return Result.error("该用户存在关联数据（评分：" + ratingCount + "条，评论：" + commentCount + "条），无法删除");
        }
        
        int result = userMapper.deleteById(userMd5);
        return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }

    // 认证相关方法
    public SysUser findByUsername(String username) {
        return sysUserMapper.findByUsername(username);
    }

    /**
     * 将 user 表中的所有用户导入到 sys_user 表（创建登录账户）
     * 默认密码: 123456，用户名使用 userMd5
     */
    public Result<Map<String, Object>> importUsersToSysUser() {
        List<User> allUsers = userMapper.findAll(10000, 0);
        int created = 0;
        int skipped = 0;
        String defaultPassword = passwordEncoder.encode("123456");

        for (User user : allUsers) {
            if (user.getUserMd5() == null || user.getUserMd5().isEmpty()) {
                skipped++;
                continue;
            }
            SysUser existing = sysUserMapper.findByUsername(user.getUserMd5());
            if (existing != null) {
                skipped++;
                continue;
            }
            SysUser newUser = new SysUser();
            newUser.setUsername(user.getUserMd5());
            newUser.setPassword(defaultPassword);
            newUser.setNickname(user.getNickname() != null ? user.getNickname() : "用户" + user.getUserMd5().substring(0, 8));
            newUser.setRole("USER");
            newUser.setStatus(1);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            newUser.setCreatedAt(now);
            newUser.setUpdatedAt(now);
            try {
                sysUserMapper.insert(newUser);
                created++;
            } catch (Exception e) {
                log.warn("导入用户 {} 失败: {}", user.getUserMd5(), e.getMessage());
                skipped++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("total", allUsers.size());
        return Result.success(result);
    }

    public boolean register(String username, String password, String nickname) {
        SysUser existingUser = sysUserMapper.findByUsername(username);
        if (existingUser != null) {
            return false;
        }
        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNickname(nickname != null ? nickname : username);
        newUser.setRole("USER"); // 默认角色为普通用户
        newUser.setStatus(1); // 默认状态为启用
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);
        sysUserMapper.insert(newUser);
        
        // 同时创建用户记录（user表），存储userMd5->nickname映射
        try {
            String userMd5 = DigestUtils.md5DigestAsHex(username.getBytes(StandardCharsets.UTF_8));
            User userRecord = new User();
            userRecord.setUserMd5(userMd5);
            userRecord.setNickname(nickname != null ? nickname : username);
            User existing = userMapper.findById(userMd5);
            if (existing == null) {
                userMapper.insert(userRecord);
                System.out.println("用户记录已创建: userMd5=" + userMd5 + ", nickname=" + userRecord.getNickname());
            }
        } catch (Exception e) {
            System.out.println("创建用户记录时出错（可能已存在）: " + e.getMessage());
        }
        
        return true;
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
