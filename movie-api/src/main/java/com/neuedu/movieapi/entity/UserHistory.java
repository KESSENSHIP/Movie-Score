package com.neuedu.movieapi.entity;

import lombok.Data;

@Data
public class UserHistory {
    private Long id;
    private String userMd5;
    private String movieId;
    private String movieName;
    private String movieCover;
    private Integer rating;
    private String comment;
    private String viewTime;
    private String reviewTime;
    private String commentId;
    private String ratingId;
}
