package com.neuedu.movieapi.entity;

import lombok.Data;

@Data
public class SentimentAnalysis {
    private Long id;
    private String commentId;
    private String movieId;
    private String sentiment;
    private Double confidence;
    private String analyzedAt;
}
