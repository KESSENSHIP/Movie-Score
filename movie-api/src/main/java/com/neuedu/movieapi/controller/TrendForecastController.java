package com.neuedu.movieapi.controller;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.service.TrendForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/trend-forecast")
@CrossOrigin(origins = "*")
public class TrendForecastController {

    @Autowired
    private TrendForecastService trendForecastService;

    @GetMapping
    public Result<Map<String, Object>> getTrendForecast() {
        return trendForecastService.getTrendForecast();
    }
}
