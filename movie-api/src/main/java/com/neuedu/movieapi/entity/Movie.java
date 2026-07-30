package com.neuedu.movieapi.entity;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Movie {
    private String movieId;
    private String name;
    private String cover;
    private String genres;
    private String directors;
    private String actors;
    private Double doubanScore;
    private Integer doubanVotes;
    private Integer year;
    private LocalDate releaseDate;
    private String language;
    private String region;
    private String storyline;
    private Integer mins;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}