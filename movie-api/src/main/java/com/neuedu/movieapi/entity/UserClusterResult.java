package com.neuedu.movieapi.entity;

import lombok.Data;

@Data
public class UserClusterResult {
    private String userMd5;
    private String nickname;
    private Integer clusterId;
    private Double avgRating;
    private Integer ratingCount;
    private Double ratingStddev;
    private Double daysSinceLastRating;
}
