package com.neuedu.movieapi.entity;
import lombok.Data;

@Data
public class User {
    private String userMd5;
    private String nickname;
    private String createdAt;
}
