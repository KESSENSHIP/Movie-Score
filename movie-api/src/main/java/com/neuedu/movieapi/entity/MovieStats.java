package com.neuedu.movieapi.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MovieStats {
    private Long id;
    private String statType;
    private String statKey;
    private String statValue;
    private Long statCount;
    private BigDecimal statPercentage;
    private String extraData;
    private String createdAt;
}
