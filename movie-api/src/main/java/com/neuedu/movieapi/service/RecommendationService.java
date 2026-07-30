package com.neuedu.movieapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Recommendation;
import com.neuedu.movieapi.mapper.MovieMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    @Value("${als.model.path:classpath:als_model}")
    private String modelPath;

    @Autowired(required = false)
    private MovieMapper movieMapper;

    // ALS 模型数据 (float32)
    private float[][] userFactors;
    private float[][] itemFactors;
    private int nFactors;

    // 映射
    private String[] idxToUser;       // user_idx -> user_md5
    private String[] idxToMovie;      // movie_idx -> movie_id
    private Map<String, Integer> userToIdx; // user_md5 -> user_idx

    // 电影名称缓存 (movieId -> movieName)
    private final Map<String, String> movieNameCache = new HashMap<>();
    private final Map<String, String> movieCoverCache = new HashMap<>();
    private final Map<String, String> movieGenresCache = new HashMap<>();
    private final Map<String, String> movieDirectorsCache = new HashMap<>();
    private final Map<String, String> movieLanguageCache = new HashMap<>();
    private final Map<String, String> movieYearCache = new HashMap<>();

    private boolean modelLoaded = false;

    @PostConstruct
    public void init() {
        try {
            loadModel();
        } catch (Exception e) {
            log.error("Failed to load ALS model: {}", e.getMessage());
        }
    }

    private void loadModel() throws Exception {
        String basePath;
        if (modelPath.startsWith("classpath:")) {
            String resourcePath = modelPath.substring("classpath:".length());
            // 尝试从 classpath 获取，但更可靠的是从外部目录
            basePath = "e:/movie/movie-api/src/main/resources/" + resourcePath;
        } else {
            basePath = modelPath;
        }

        log.info("Loading ALS model from: {}", basePath);

        // 加载 params
        ObjectMapper mapper = new ObjectMapper();
        File paramsFile = new File(basePath, "params.json");
        if (!paramsFile.exists()) {
            log.warn("ALS model params not found at {}", paramsFile);
            return;
        }
        Map<String, Object> params = mapper.readValue(paramsFile, new TypeReference<>() {});
        nFactors = (Integer) params.get("n_factors");
        int nUsers = (Integer) params.get("n_users");
        int nItems = (Integer) params.get("n_items");
        log.info("ALS model: {} users, {} items, {} factors", nUsers, nItems, nFactors);

        // 加载用户因子矩阵
        File userBin = new File(basePath, "user_factors.bin");
        if (!userBin.exists()) {
            log.warn("user_factors.bin not found");
            return;
        }
        byte[] userBytes = Files.readAllBytes(userBin.toPath());
        ByteBuffer userBuf = ByteBuffer.wrap(userBytes).order(ByteOrder.LITTLE_ENDIAN);
        userFactors = new float[nUsers][nFactors];
        for (int i = 0; i < nUsers; i++) {
            for (int j = 0; j < nFactors; j++) {
                userFactors[i][j] = userBuf.getFloat();
            }
        }
        log.info("Loaded user factors: {} users × {} factors", nUsers, nFactors);

        // 加载物品因子矩阵
        File itemBin = new File(basePath, "item_factors.bin");
        byte[] itemBytes = Files.readAllBytes(itemBin.toPath());
        ByteBuffer itemBuf = ByteBuffer.wrap(itemBytes).order(ByteOrder.LITTLE_ENDIAN);
        itemFactors = new float[nItems][nFactors];
        for (int i = 0; i < nItems; i++) {
            for (int j = 0; j < nFactors; j++) {
                itemFactors[i][j] = itemBuf.getFloat();
            }
        }
        log.info("Loaded item factors: {} items × {} factors", nItems, nFactors);

        // 加载用户映射
        File userMappingFile = new File(basePath, "idx_to_user.json");
        Map<String, List<String>> userMapping = mapper.readValue(userMappingFile, new TypeReference<>() {});
        List<String> userList = userMapping.get("users");
        idxToUser = userList.toArray(new String[0]);
        log.info("Loaded user mapping: {} users", idxToUser.length);

        // 加载电影映射
        File movieMappingFile = new File(basePath, "idx_to_movie.json");
        Map<String, List<String>> movieMapping = mapper.readValue(movieMappingFile, new TypeReference<>() {});
        List<String> movieList = movieMapping.get("movies");
        idxToMovie = movieList.toArray(new String[0]);
        log.info("Loaded movie mapping: {} movies", idxToMovie.length);

        // 加载 user_md5 -> idx 映射
        File userToIdxFile = new File(basePath, "user_to_idx.json");
        userToIdx = mapper.readValue(userToIdxFile, new TypeReference<>() {});
        log.info("Loaded user_to_idx mapping: {} entries", userToIdx.size());

        modelLoaded = true;
        log.info("ALS model loaded successfully");
    }

    /**
     * 获取用户的 Top-N 推荐（分页，从内存模型计算）
     */
    public Result<PageResult<Recommendation>> getRecommendations(String userMd5, int pageNum, int pageSize,
                                                                 boolean shuffle, String genres, double minScore) {
        try {
            if (!modelLoaded) {
                return Result.error(500, "推荐模型未加载，请先运行 ALS 训练");
            }

            Integer userIdx = userToIdx.get(userMd5);
            if (userIdx == null) {
                return Result.success(new PageResult<>(List.of(), pageNum, pageSize, 0L));
            }

            // 计算推荐
            List<Recommendation> allRecs = computeRecommendations(userIdx, 200);

            // 过滤：最低评分
            if (minScore > 0) {
                double threshold = minScore;
                allRecs = allRecs.stream()
                        .filter(r -> r.getPredictedRating() != null && r.getPredictedRating() >= threshold)
                        .collect(Collectors.toList());
            }

            // 过滤：类型（包含任意一个关键字即匹配）
            if (genres != null && !genres.trim().isEmpty()) {
                final String gLower = genres.trim().toLowerCase();
                allRecs = allRecs.stream()
                        .filter(r -> {
                            String g = r.getGenres();
                            if (g == null) return false;
                            return g.toLowerCase().contains(gLower);
                        })
                        .collect(Collectors.toList());
            }

            // 随机打乱（重新计算排名序号）
            if (shuffle) {
                Collections.shuffle(allRecs, new Random());
                int rank = 1;
                for (Recommendation r : allRecs) {
                    r.setRank(rank++);
                }
            }

            // 分页
            int totalCount = allRecs.size();
            int fromIndex = Math.min((pageNum - 1) * pageSize, totalCount);
            int toIndex = Math.min(fromIndex + pageSize, totalCount);
            List<Recommendation> pageRecs = allRecs.subList(fromIndex, toIndex);

            return Result.success(new PageResult<>(pageRecs, pageNum, pageSize, (long) totalCount));
        } catch (Exception e) {
            log.error("获取用户推荐失败 userMd5={}", userMd5, e);
            return Result.error(500, "获取推荐失败: " + e.getMessage());
        }
    }

    public Result<PageResult<Recommendation>> getRecommendations(String userMd5, int pageNum, int pageSize) {
        return getRecommendations(userMd5, pageNum, pageSize, false, null, 0);
    }

    /**
     * 获取 Top-N 推荐（用于首页展示）
     */
    public Result<List<Recommendation>> getTopRecommendations(String userMd5) {
        try {
            if (!modelLoaded) {
                return Result.error(500, "推荐模型未加载");
            }

            Integer userIdx = userToIdx.get(userMd5);
            if (userIdx == null) {
                return Result.success(List.of());
            }

            List<Recommendation> recs = computeRecommendations(userIdx, 20);
            return Result.success(recs);
        } catch (Exception e) {
            log.error("获取用户Top推荐失败 userMd5={}", userMd5, e);
            return Result.error(500, "获取推荐失败: " + e.getMessage());
        }
    }

    /**
     * 为指定用户索引计算推荐电影
     */
    private List<Recommendation> computeRecommendations(int userIdx, int topN) {
        float[] userVec = userFactors[userIdx];

        // 计算所有电影的预测评分（向量 × 矩阵 = 点积）
        float[] scores = new float[itemFactors.length];
        for (int i = 0; i < itemFactors.length; i++) {
            float sum = 0;
            for (int j = 0; j < nFactors; j++) {
                sum += userVec[j] * itemFactors[i][j];
            }
            scores[i] = sum;
        }

        // 找出评分最高的 topN 部电影
        // 使用优先队列（最小堆）保持 top-N
        PriorityQueue<Integer> pq = new PriorityQueue<>(
                (a, b) -> Float.compare(scores[a], scores[b])
        );
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0) {
                pq.offer(i);
                if (pq.size() > topN) {
                    pq.poll();
                }
            }
        }

        // 从堆中取出结果（从小到大）
        List<Integer> topIndices = new ArrayList<>(pq);
        topIndices.sort((a, b) -> Float.compare(scores[b], scores[a]));

        // 构建推荐结果
        List<Recommendation> recs = new ArrayList<>();
        int rank = 1;
        for (int idx : topIndices) {
            String movieId = idxToMovie[idx];
            Recommendation rec = new Recommendation();
            rec.setMovieId(movieId);
            rec.setPredictedRating((double) scores[idx]);
            rec.setRank(rank++);
            rec.setMovieName(movieNameCache.getOrDefault(movieId, null));
            rec.setMovieCover(movieCoverCache.getOrDefault(movieId, null));
            rec.setGenres(movieGenresCache.getOrDefault(movieId, null));
            rec.setDirectors(movieDirectorsCache.getOrDefault(movieId, null));
            rec.setLanguage(movieLanguageCache.getOrDefault(movieId, null));
            rec.setYear(movieYearCache.getOrDefault(movieId, null));
            recs.add(rec);
        }

        // 异步加载缺失的电影名称
        fillMovieDetails(recs);

        return recs;
    }

    /**
     * 填充缺失的电影详情
     */
    private void fillMovieDetails(List<Recommendation> recs) {
        if (movieMapper == null) return;

        List<String> missingIds = recs.stream()
                .filter(r -> r.getMovieName() == null || r.getMovieCover() == null
                        || r.getGenres() == null || r.getDirectors() == null
                        || r.getLanguage() == null || r.getYear() == null)
                .map(Recommendation::getMovieId)
                .distinct()
                .collect(Collectors.toList());

        if (missingIds.isEmpty()) return;

        try {
            List<Map<String, String>> movieInfos = movieMapper.findDetailsByIds(missingIds);
            for (Map<String, String> info : movieInfos) {
                String movieId = info.get("movieId");
                if (movieId == null) continue;

                String name = info.get("name");
                String cover = info.get("cover");
                String genres = info.get("genres");
                String directors = info.get("directors");
                String language = info.get("language");
                String year = info.get("year");

                if (name != null) movieNameCache.put(movieId, name);
                if (cover != null) movieCoverCache.put(movieId, cover);
                if (genres != null) movieGenresCache.put(movieId, genres);
                if (directors != null) movieDirectorsCache.put(movieId, directors);
                if (language != null) movieLanguageCache.put(movieId, language);
                if (year != null) movieYearCache.put(movieId, year);
            }

            for (Recommendation rec : recs) {
                if (rec.getMovieName() == null) {
                    rec.setMovieName(movieNameCache.get(rec.getMovieId()));
                }
                if (rec.getMovieCover() == null) {
                    rec.setMovieCover(movieCoverCache.get(rec.getMovieId()));
                }
                if (rec.getGenres() == null) {
                    rec.setGenres(movieGenresCache.get(rec.getMovieId()));
                }
                if (rec.getDirectors() == null) {
                    rec.setDirectors(movieDirectorsCache.get(rec.getMovieId()));
                }
                if (rec.getLanguage() == null) {
                    rec.setLanguage(movieLanguageCache.get(rec.getMovieId()));
                }
                if (rec.getYear() == null) {
                    rec.setYear(movieYearCache.get(rec.getMovieId()));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load movie details for recommendations: {}", e.getMessage());
        }
    }
}
