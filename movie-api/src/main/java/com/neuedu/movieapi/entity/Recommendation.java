package com.neuedu.movieapi.entity;

import java.time.LocalDateTime;

public class Recommendation {
    private Integer id;
    private String userMd5;
    private String movieId;
    private Double predictedRating;
    private Integer rank;
    private LocalDateTime createdAt;

    // 前端展示用
    private String movieName;
    private String movieCover;
    private String genres;
    private String directors;
    private String language;
    private String year;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserMd5() { return userMd5; }
    public void setUserMd5(String userMd5) { this.userMd5 = userMd5; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public Double getPredictedRating() { return predictedRating; }
    public void setPredictedRating(Double predictedRating) { this.predictedRating = predictedRating; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMovieName() { return movieName; }
    public void setMovieName(String movieName) { this.movieName = movieName; }

    public String getMovieCover() { return movieCover; }
    public void setMovieCover(String movieCover) { this.movieCover = movieCover; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public String getDirectors() { return directors; }
    public void setDirectors(String directors) { this.directors = directors; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
}
