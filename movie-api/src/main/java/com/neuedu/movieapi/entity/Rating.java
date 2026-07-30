package com.neuedu.movieapi.entity;
import lombok.Data;

@Data
public class Rating {
    private String ratingId;
    private String userMd5;
    private String movieId;
    private Integer rating;
    private String ratingTime;
    private String nickname; // 非数据库字段，由服务层填充
}