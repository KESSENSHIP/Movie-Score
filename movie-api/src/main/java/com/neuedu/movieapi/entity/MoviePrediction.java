package com.neuedu.movieapi.entity;

public class MoviePrediction {
    private String movieId;
    private String name;
    private Integer year;
    private String genres;
    private String region;
    private Double predictedScore;

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public Double getPredictedScore() { return predictedScore; }
    public void setPredictedScore(Double predictedScore) { this.predictedScore = predictedScore; }
}
