package com.neuedu.movieapi.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {
    private String id;
    private String userMd5;
    private String movieId;
    private String movieName;
    private Integer rating;
    private String time;
    private String type;
    private String content;
    private Integer votes;
    private String ratingId;
    private String commentId;
    private String ratingTime;
    private String commentTime;
}