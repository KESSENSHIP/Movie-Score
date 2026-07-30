package com.neuedu.movieapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.movieapi.common.Result;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class TrendForecastService {

    private static final Logger log = LoggerFactory.getLogger(TrendForecastService.class);

    private Map<String, Object> forecastData;
    private boolean loaded = false;

    @PostConstruct
    public void init() {
        loadForecastData();
    }

    private void loadForecastData() {
        try {
            String basePath = "e:/movie/movie-api/src/main/resources/trend_forecast";
            File jsonFile = new File(basePath, "trend_forecast.json");
            if (!jsonFile.exists()) {
                log.warn("Trend forecast data not found at {}", jsonFile);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            forecastData = mapper.readValue(jsonFile, new TypeReference<>() {});
            loaded = true;

            List<Map<String, Object>> historical = (List<Map<String, Object>>) forecastData.get("historical");
            List<Map<String, Object>> forecast = (List<Map<String, Object>>) forecastData.get("forecast");
            log.info("Loaded trend forecast: {} historical years, {} forecast years",
                    historical != null ? historical.size() : 0,
                    forecast != null ? forecast.size() : 0);
        } catch (Exception e) {
            log.error("Failed to load trend forecast data: {}", e.getMessage());
        }
    }

    public Result<Map<String, Object>> getTrendForecast() {
        if (!loaded || forecastData == null) {
            return Result.error(500, "趋势预测数据未加载，请先运行 ARIMA 预测脚本");
        }

        Map<String, Object> result = new HashMap<>(forecastData);

        // 合并历史+预测的完整趋势数据（用于折线图）
        List<Map<String, Object>> historical = (List<Map<String, Object>>) forecastData.get("historical");
        List<Map<String, Object>> recent = (List<Map<String, Object>>) forecastData.get("recent");
        List<Map<String, Object>> forecast = (List<Map<String, Object>>) forecastData.get("forecast");

        // 构建完整时间序列
        List<Map<String, Object>> fullSeries = new ArrayList<>();
        if (historical != null) fullSeries.addAll(historical);
        if (recent != null) fullSeries.addAll(recent);
        if (forecast != null) fullSeries.addAll(forecast);

        result.put("fullSeries", fullSeries);

        // 添加统计数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalHistoricalYears", historical != null ? historical.size() : 0);
        stats.put("forecastYears", forecast != null ? forecast.size() : 0);

        if (historical != null && !historical.isEmpty()) {
            stats.put("maxYear", historical.get(historical.size() - 1).get("year"));
            stats.put("maxCount", historical.stream()
                    .mapToInt(m -> ((Number) m.get("count")).intValue())
                    .max().orElse(0));
        }

        if (forecast != null && !forecast.isEmpty()) {
            stats.put("peakForecast", forecast.stream()
                    .mapToDouble(m -> ((Number) m.get("count")).doubleValue())
                    .max().orElse(0));
            Map<String, Object> lastForecast = forecast.get(forecast.size() - 1);
            stats.put("forecastEndYear", lastForecast.get("year"));
        }

        result.put("stats", stats);
        return Result.success(result);
    }
}
