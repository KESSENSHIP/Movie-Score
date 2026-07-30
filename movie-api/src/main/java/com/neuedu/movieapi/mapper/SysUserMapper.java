package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {
    @Select("SELECT id, username, nickname, email, avatar, role, status, created_at as createdAt, updated_at as updatedAt FROM sys_user LIMIT #{pageSize} OFFSET #{offset}")
    List<SysUser> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM sys_user")
    Long countAll();

    @Select("SELECT id, username, nickname, email, avatar, role, status, created_at as createdAt, updated_at as updatedAt FROM sys_user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%') LIMIT #{pageSize} OFFSET #{offset}")
    List<SysUser> search(@Param("keyword") String keyword, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM sys_user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')")
    Long countSearch(@Param("keyword") String keyword);

    @Select("SELECT id, username, password, nickname, email, avatar, role, status, created_at as createdAt, updated_at as updatedAt FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, nickname, email, avatar, role, status, created_at as createdAt, updated_at as updatedAt FROM sys_user WHERE id = #{id}")
    SysUser findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM sys_user WHERE role = 'ADMIN'")
    Long countAdmins();

    @Insert("INSERT INTO sys_user(username, password, nickname, email, avatar, role, status, created_at, updated_at) VALUES (#{username}, #{password}, #{nickname}, #{email}, #{avatar}, #{role}, #{status}, #{createdAt}, #{updatedAt})")
    int insert(SysUser sysUser);

    @Update("UPDATE sys_user SET username=#{username}, password=#{password}, nickname=#{nickname}, email=#{email}, avatar=#{avatar}, role=#{role}, status=#{status}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(SysUser sysUser);

    @Delete("DELETE FROM sys_user WHERE id=#{id}")
    int deleteById(@Param("id") Long id);
}