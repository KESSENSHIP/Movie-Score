package com.neuedu.movieapi.entity;
import lombok.Data;

@Data
public class Comment {
    private String commentId;
    private String userMd5;
    private String movieId;
    private String content;
    private Integer votes;
    private String commentTime;
    private Integer rating;
    private String nickname; // 非数据库字段，由服务层填充
}