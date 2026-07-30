package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT USER_MD5 as userMd5, USER_NICKNAME as nickname FROM user LIMIT #{pageSize} OFFSET #{offset}")
    List<User> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM user")
    Long count();

    @Select("SELECT USER_MD5 as userMd5, USER_NICKNAME as nickname FROM user WHERE USER_MD5 = #{userMd5}")
    User findById(@Param("userMd5") String userMd5);

    @Select("SELECT COUNT(*) FROM user WHERE USER_MD5 = #{userMd5}")
    Long countByUserMd5(@Param("userMd5") String userMd5);

    @Select("SELECT USER_MD5 as userMd5, USER_NICKNAME as nickname FROM user WHERE USER_NICKNAME LIKE CONCAT('%', #{keyword}, '%') OR USER_MD5 LIKE CONCAT('%', #{keyword}, '%') LIMIT #{pageSize} OFFSET #{offset}")
    List<User> searchByName(@Param("keyword") String keyword, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM user WHERE USER_NICKNAME LIKE CONCAT('%', #{keyword}, '%') OR USER_MD5 LIKE CONCAT('%', #{keyword}, '%')")
    Long countByName(@Param("keyword") String keyword);

    @Insert("INSERT INTO user(user_md5, user_nickname) VALUES (#{userMd5}, #{nickname})")
    int insert(User user);

    @Insert("INSERT IGNORE INTO user(user_md5, user_nickname) VALUES (#{userMd5}, #{nickname})")
    int insertIgnore(User user);

    @Update("UPDATE user SET user_nickname=#{nickname} WHERE user_md5=#{userMd5}")
    int update(User user);

    @Delete("DELETE FROM user WHERE user_md5=#{userMd5}")
    int deleteById(@Param("userMd5") String userMd5);

    @Select("<script>" +
            "SELECT USER_MD5 as userMd5, USER_NICKNAME as nickname FROM user WHERE USER_MD5 IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<User> findByIds(@Param("ids") List<String> ids);
}
