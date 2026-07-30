package com.neuedu.movieapi.entity;
import lombok.Data;

@Data
public class Person {
    private String personId;
    private String name;
    private String sex;
    private String nameEn;
    private String nameZh;
    private String birth;
    private String birthplace;
    private String profession;
    private String biography;
    private String createdAt;
}
