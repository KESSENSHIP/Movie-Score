package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.SysUser;
import com.neuedu.movieapi.entity.User;
import com.neuedu.movieapi.mapper.SysUserMapper;
import com.neuedu.movieapi.mapper.UserMapper;
import com.neuedu.movieapi.service.UserService;
import com.neuedu.movieapi.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    //    http://localhost:8888/api/auth/register
//    {"username":"tom","password":"123456","nickname":"汤姆"}
    public Result<String> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String nickname = params.get("nickname");
        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }
        boolean success = userService.register(username, password, nickname);
        return success ? Result.success("注册成功") : Result.error("用户名已存在");
    }

    @PostMapping("/login")
    //    http://localhost:8888/api/auth/login
//    {"username":"admin","password":"123456"}
    public Result<Map<String, String>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        SysUser user = userService.findByUsername(username);
//        System.out.println("-----AuthController--user="+user);
        if (user == null || !userService.checkPassword(password, user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }
        String token = jwtUtil.generateToken(username);
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("username", username);
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        // 验证头像数据是否有效
        boolean avatarValid = false;
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            try {
                String avatarData = user.getAvatar();
                int commaIdx = avatarData.indexOf(',');
                String b64 = commaIdx >= 0 ? avatarData.substring(commaIdx + 1) : avatarData;
                Base64.getDecoder().decode(b64);
                avatarValid = true;
            } catch (Exception ignored) {
            }
        }
        result.put("hasAvatar", avatarValid ? "true" : "false");

        // 同步昵称到 user 表（确保管理员端显示正确的用户昵称）
        try {
            String userMd5 = md5Hex(username);
            User userRecord = userMapper.findById(userMd5);
            if (userRecord != null) {
                // 更新昵称（如果 sys_user 中的昵称不同）
                if (!java.util.Objects.equals(userRecord.getNickname(), user.getNickname())) {
                    userRecord.setNickname(user.getNickname());
                    userMapper.update(userRecord);
                }
            } else {
                // 创建 user 记录
                User newUser = new User();
                newUser.setUserMd5(userMd5);
                newUser.setNickname(user.getNickname());
                userMapper.insert(newUser);
            }
        } catch (Exception e) {
            System.out.println("同步昵称到 user 表时出错: " + e.getMessage());
        }

        return Result.success(result);
    }

    /** 从 Authorization header 提取用户名 */
    private String extractUsername(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            return jwtUtil.getUsernameFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /** MD5 哈希（与前端保持一致） */
    private String md5Hex(String str) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return str;
        }
    }

    @PutMapping("/profile")
    //    http://localhost:8888/api/auth/profile
//    {"nickname":"新昵称", "password":"当前密码"}  Authorization: Bearer xxx
    public Result<?> updateProfile(@RequestBody Map<String, String> params,
                                                     @RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return Result.error(401, "未登录或token失效");
        }

        String nickname = params.get("nickname");
        if (nickname == null || nickname.trim().isEmpty()) {
            return Result.error("昵称不能为空");
        }
        nickname = nickname.trim();

        String password = params.get("password");
        if (password == null || password.isEmpty()) {
            return Result.error("请输入当前密码进行验证");
        }

        SysUser sysUser = sysUserMapper.findByUsername(username);
        if (sysUser == null) {
            return Result.error("用户不存在");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, sysUser.getPassword())) {
            return Result.error("密码验证失败，当前密码不正确");
        }

        // 更新 sys_user 表的 nickname
        sysUser.setNickname(nickname);
        sysUser.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.update(sysUser);

        // 同步更新 user 表的 nickname
        String userMd5 = md5Hex(username);
        User user = userMapper.findById(userMd5);
        if (user != null) {
            user.setNickname(nickname);
            userMapper.update(user);
        }

        Map<String, String> result = new HashMap<>();
        result.put("nickname", nickname);
        return Result.success(result);
    }

    @PutMapping("/password")
    //    http://localhost:8888/api/auth/password
//    {"oldPassword":"旧密码","newPassword":"新密码"}  Authorization: Bearer xxx
    public Result<String> changePassword(@RequestBody Map<String, String> params,
                                         @RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return Result.error(401, "未登录或token失效");
        }

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("旧密码不能为空");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        SysUser sysUser = sysUserMapper.findByUsername(username);
        if (sysUser == null) {
            return Result.error("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, sysUser.getPassword())) {
            return Result.error("旧密码不正确");
        }

        // 更新密码
        sysUser.setPassword(passwordEncoder.encode(newPassword));
        sysUser.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.update(sysUser);

        return Result.success("密码修改成功");
    }

    @GetMapping("/avatar")
    //    http://localhost:8888/api/auth/avatar?username=xxx
    public void getAvatar(@RequestParam(value = "username", required = false) String usernameParam,
                          @RequestHeader(value = "Authorization", required = false) String authHeader,
                          HttpServletResponse response) {
        String username = extractUsername(authHeader);
        // 如果 header 中没有 token，则尝试使用 query parameter
        if (username == null) {
            username = usernameParam;
        }
        if (username == null) {
            response.setStatus(401);
            return;
        }

        SysUser sysUser = sysUserMapper.findByUsername(username);
        if (sysUser == null || sysUser.getAvatar() == null || sysUser.getAvatar().isEmpty()) {
            response.setStatus(204);
            return;
        }

        try {
            String dataUrl = sysUser.getAvatar();
            // data:image/jpeg;base64,/9j/4AAQ...
            int commaIndex = dataUrl.indexOf(',');

            String contentType = "image/png";
            String base64Data;

            if (commaIndex >= 0) {
                // 标准 data URL 格式
                String header = dataUrl.substring(0, commaIndex);
                base64Data = dataUrl.substring(commaIndex + 1);
                if (header.contains("image/jpeg") || header.contains("image/jpg")) {
                    contentType = "image/jpeg";
                } else if (header.contains("image/gif")) {
                    contentType = "image/gif";
                } else if (header.contains("image/webp")) {
                    contentType = "image/webp";
                }
            } else {
                // 无逗号，视为纯 base64 数据
                base64Data = dataUrl;
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            response.setContentType(contentType);
            response.setContentLength(imageBytes.length);
            response.setHeader("Cache-Control", "private, max-age=86400");
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();
        } catch (IllegalArgumentException e) {
            // base64 数据无效，视为无头像
            response.setStatus(204);
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    @PutMapping("/avatar")
    //    http://localhost:8888/api/auth/avatar
//    {"avatar":"base64编码数据", "password":"当前密码"}  Authorization: Bearer xxx
    public Result<String> updateAvatar(@RequestBody Map<String, String> params,
                                       @RequestHeader("Authorization") String authHeader) {
        String username = extractUsername(authHeader);
        if (username == null) {
            return Result.error(401, "未登录或token失效");
        }

        String avatar = params.get("avatar");
        if (avatar == null || avatar.isEmpty()) {
            return Result.error("头像数据不能为空");
        }

        String password = params.get("password");
        if (password == null || password.isEmpty()) {
            return Result.error("请输入当前密码进行验证");
        }

        SysUser sysUser = sysUserMapper.findByUsername(username);
        if (sysUser == null) {
            return Result.error("用户不存在");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, sysUser.getPassword())) {
            return Result.error("密码验证失败，当前密码不正确");
        }

        // 更新头像
        sysUser.setAvatar(avatar);
        sysUser.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.update(sysUser);

        return Result.success("头像更新成功");
    }

    @PostMapping("/import-users")
    public Result<Map<String, Object>> importUsers() {
        return userService.importUsersToSysUser();
    }
}