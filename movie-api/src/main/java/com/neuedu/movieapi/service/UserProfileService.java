package com.neuedu.movieapi.service;

import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.UserClusterResult;
import com.neuedu.movieapi.mapper.RatingMapper;
import com.neuedu.movieapi.mapper.UserClusterMapper;
import com.neuedu.movieapi.mapper.UserMapper;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    @Autowired
    private UserClusterMapper userClusterMapper;

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private UserMapper userMapper;

    public Result<Map<String, Object>> performClustering() {
        try {
            log.info("=== 开始用户画像聚类分析 ===");

            // 1. 提取用户评分特征
            List<UserFeature> features = extractUserFeatures();
            log.info("提取到 {} 个用户特征", features.size());
            if (features.isEmpty()) {
                return Result.error(500, "没有评分数据，无法进行聚类分析");
            }

            // 2. 标准化特征
            List<double[]> normalized = normalizeFeatures(features);

            // 3. 快速肘部法则（减少迭代次数）
            ElbowResult elbow = quickElbow(normalized, 2, 10);
            int finalK = elbow.optimalK;
            log.info("最佳 K={}", finalK);

            // 4. 执行最终 K-Means++
            KMeansPlusPlusClusterer<DoublePoint> clusterer =
                    new KMeansPlusPlusClusterer<>(finalK, 30, new EuclideanDistance());
            List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(
                    normalized.stream().map(DoublePoint::new).collect(Collectors.toList())
            );

            // 5. 分配用户到各簇
            List<UserClusterResult> results = new ArrayList<>();
            for (int i = 0; i < features.size(); i++) {
                int clusterId = assignToCluster(normalized.get(i), clusters);
                UserClusterResult r = new UserClusterResult();
                r.setUserMd5(features.get(i).userMd5);
                r.setNickname(features.get(i).nickname);
                r.setClusterId(clusterId);
                r.setAvgRating(features.get(i).avgRating);
                r.setRatingCount(features.get(i).ratingCount);
                r.setRatingStddev(features.get(i).ratingStddev);
                r.setDaysSinceLastRating(features.get(i).daysSinceLastRating);
                results.add(r);
            }

            // 6. 批量保存到数据库
            userClusterMapper.deleteAll();
            batchInsert(results, 100);
            log.info("保存 {} 条聚类结果", results.size());

            // 7. 获取簇汇总
            List<UserClusterMapper.ClusterSummary> summaries = userClusterMapper.findClusterSummary();
            List<Map<String, Object>> clusterData = new ArrayList<>();
            for (UserClusterMapper.ClusterSummary s : summaries) {
                Map<String, Object> m = new HashMap<>();
                m.put("clusterId", s.clusterId);
                m.put("userCount", s.userCount);
                m.put("avgRatingCount", String.format("%.2f", s.avgRatingCount));
                m.put("avgRating", String.format("%.2f", s.avgRating));
                m.put("avgRatingStddev", String.format("%.3f", s.avgRatingStddev));
                m.put("avgDaysSinceLast", String.format("%.1f", s.avgDaysSinceLast));
                clusterData.add(m);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("optimalK", finalK);
            result.put("totalUsers", features.size());
            result.put("clusterSummary", clusterData);
            result.put("wcssValues", elbow.wcssValues);

            log.info("=== 用户画像聚类分析完成 ===");
            return Result.success(result);
        } catch (Exception e) {
            log.error("聚类分析失败", e);
            return Result.error(500, "聚类分析失败: " + e.getMessage());
        }
    }

    public Result<Map<String, Object>> getClusterResult() {
        try {
            List<UserClusterMapper.ClusterSummary> summaries = userClusterMapper.findClusterSummary();
            if (summaries.isEmpty()) {
                return Result.error(500, "尚未进行聚类分析");
            }
            List<Map<String, Object>> clusterData = new ArrayList<>();
            for (UserClusterMapper.ClusterSummary s : summaries) {
                Map<String, Object> m = new HashMap<>();
                m.put("clusterId", s.clusterId);
                m.put("userCount", s.userCount);
                m.put("avgRatingCount", String.format("%.2f", s.avgRatingCount));
                m.put("avgRating", String.format("%.2f", s.avgRating));
                m.put("avgRatingStddev", String.format("%.3f", s.avgRatingStddev));
                m.put("avgDaysSinceLast", String.format("%.1f", s.avgDaysSinceLast));
                clusterData.add(m);
            }
            int totalUsers = summaries.stream().mapToInt(s -> s.userCount.intValue()).sum();
            Map<String, Object> result = new HashMap<>();
            result.put("optimalK", summaries.size());
            result.put("totalUsers", totalUsers);
            result.put("clusterSummary", clusterData);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取聚类结果失败", e);
            return Result.error(500, "获取聚类结果失败: " + e.getMessage());
        }
    }

    public Result<PageResult<UserClusterResult>> getClusterUsers(int clusterId, int pageNum, int pageSize) {
        try {
            int offset = Math.max(0, (pageNum - 1) * pageSize);
            int limit = Math.min(pageSize, 100);
            List<UserClusterResult> users = userClusterMapper.findByClusterId(clusterId, limit, offset);
            long totalCount = userClusterMapper.countByClusterId(clusterId);
            return Result.success(new PageResult<>(users, pageNum, pageSize, totalCount));
        } catch (Exception e) {
            log.error("获取簇用户列表失败", e);
            return Result.error(500, "获取失败: " + e.getMessage());
        }
    }

    /** 批量插入 */
    private void batchInsert(List<UserClusterResult> results, int batchSize) {
        for (int i = 0; i < results.size(); i += batchSize) {
            int end = Math.min(i + batchSize, results.size());
            List<UserClusterResult> batch = results.subList(i, end);
            try {
                userClusterMapper.batchInsert(batch);
            } catch (Exception e) {
                log.warn("批量插入失败 ({}..{}): {}", i, end, e.getMessage());
            }
        }
    }

    private List<UserFeature> extractUserFeatures() {
        List<Map<String, Object>> rawData = ratingMapper.findUserRatingFeatures();
        LocalDateTime globalLatest = LocalDateTime.now();

        // 批量查询所有用户昵称
        List<String> userMd5s = rawData.stream()
                .map(row -> (String) row.get("userMd5"))
                .collect(Collectors.toList());
        List<com.neuedu.movieapi.entity.User> userList = userMapper.findByIds(userMd5s);
        Map<String, String> nicknameMap = new HashMap<>();
        if (userList != null) {
            for (com.neuedu.movieapi.entity.User u : userList) {
                if (u.getUserMd5() != null) nicknameMap.put(u.getUserMd5(), u.getNickname());
            }
        }

        List<UserFeature> features = new ArrayList<>();
        for (Map<String, Object> row : rawData) {
            UserFeature f = new UserFeature();
            f.userMd5 = (String) row.get("userMd5");
            f.ratingCount = ((Number) row.get("ratingCount")).intValue();
            f.avgRating = ((Number) row.get("avgRating")).doubleValue();

            // 从 sumRating 和 sumRatingSq 计算标准差
            Number sumRatingSq = (Number) row.get("sumRatingSq");
            Number sumRating = (Number) row.get("sumRating");
            if (sumRatingSq != null && sumRating != null && f.ratingCount > 1) {
                double sq = sumRatingSq.doubleValue();
                double s = sumRating.doubleValue();
                double n = f.ratingCount;
                f.ratingStddev = Math.sqrt((sq - (s * s) / n) / (n - 1));
            } else {
                f.ratingStddev = 0.0;
            }

            Object lastTime = row.get("latestRatingTime");
            if (lastTime != null) {
                try {
                    String timeStr = lastTime.toString().replace(" ", "T");
                    if (!timeStr.contains("T")) timeStr = timeStr.substring(0, 10) + "T00:00:00";
                    LocalDateTime lt = LocalDateTime.parse(timeStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                    f.daysSinceLastRating = Duration.between(lt, globalLatest).toDays();
                } catch (Exception e) {
                    f.daysSinceLastRating = 365.0;
                }
            } else {
                f.daysSinceLastRating = 365.0;
            }

            f.nickname = nicknameMap.get(f.userMd5);
            features.add(f);
        }
        return features;
    }

    private List<double[]> normalizeFeatures(List<UserFeature> features) {
        int dim = 4;
        double[] means = new double[dim];
        double[] stds = new double[dim];

        for (int j = 0; j < dim; j++) {
            final int idx = j;
            DescriptiveStatistics ds = new DescriptiveStatistics();
            for (UserFeature f : features) ds.addValue(f.getFeature(idx));
            means[j] = ds.getMean();
            stds[j] = ds.getStandardDeviation();
            if (stds[j] == 0) stds[j] = 1;
        }

        List<double[]> normalized = new ArrayList<>();
        for (UserFeature f : features) {
            double[] n = new double[dim];
            for (int j = 0; j < dim; j++) {
                n[j] = (f.getFeature(j) - means[j]) / stds[j];
            }
            normalized.add(n);
        }
        return normalized;
    }

    /** 快速肘部法则 */
    private ElbowResult quickElbow(List<double[]> data, int minK, int maxK) {
        List<Map<String, Object>> wcssValues = new ArrayList<>();
        List<Double> wcssList = new ArrayList<>();
        EuclideanDistance dist = new EuclideanDistance();

        for (int k = minK; k <= maxK; k++) {
            KMeansPlusPlusClusterer<DoublePoint> clusterer =
                    new KMeansPlusPlusClusterer<>(k, 10, dist);
            List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(
                    data.stream().map(DoublePoint::new).collect(Collectors.toList())
            );
            double wcss = 0;
            for (CentroidCluster<DoublePoint> c : clusters) {
                double[] center = c.getCenter().getPoint();
                for (DoublePoint p : c.getPoints()) {
                    wcss += Math.pow(dist.compute(center, p.getPoint()), 2);
                }
            }
            wcssList.add(wcss);
            Map<String, Object> entry = new HashMap<>();
            entry.put("k", k);
            entry.put("wcss", String.format("%.2f", wcss));
            wcssValues.add(entry);
        }

        int bestK = minK;
        if (wcssList.size() >= 3) {
            double maxSecondDiff = 0;
            for (int i = 1; i < wcssList.size() - 1; i++) {
                double diff1 = wcssList.get(i - 1) - wcssList.get(i);
                double diff2 = wcssList.get(i) - wcssList.get(i + 1);
                double secondDiff = diff1 - diff2;
                if (secondDiff > maxSecondDiff) {
                    maxSecondDiff = secondDiff;
                    bestK = minK + i;
                }
            }
        }
        bestK = Math.max(bestK, 3);

        ElbowResult result = new ElbowResult();
        result.optimalK = bestK;
        result.wcssValues = wcssValues;
        return result;
    }

    private int assignToCluster(double[] point, List<CentroidCluster<DoublePoint>> clusters) {
        EuclideanDistance dist = new EuclideanDistance();
        double minDist = Double.MAX_VALUE;
        int bestCluster = 0;
        for (int i = 0; i < clusters.size(); i++) {
            double d = dist.compute(point, clusters.get(i).getCenter().getPoint());
            if (d < minDist) {
                minDist = d;
                bestCluster = i;
            }
        }
        return bestCluster;
    }

    static class ElbowResult {
        int optimalK;
        List<Map<String, Object>> wcssValues;
    }

    static class UserFeature {
        String userMd5;
        String nickname;
        int ratingCount;
        double avgRating;
        double ratingStddev;
        double daysSinceLastRating;

        double getFeature(int index) {
            switch (index) {
                case 0: return avgRating;
                case 1: return ratingCount;
                case 2: return ratingStddev;
                case 3: return daysSinceLastRating;
                default: return 0;
            }
        }
    }
}
