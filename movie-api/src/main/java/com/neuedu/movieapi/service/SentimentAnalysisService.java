package com.neuedu.movieapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.Comment;
import com.neuedu.movieapi.entity.SentimentAnalysis;
import com.neuedu.movieapi.mapper.CommentMapper;
import com.neuedu.movieapi.mapper.MovieMapper;
import com.neuedu.movieapi.mapper.SentimentAnalysisMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SentimentAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisService.class);

    // 每次批量分析的评论数量（降低API调用成本）
    private static final int BATCH_SIZE = 5;

    @Value("${deepseek.api.key:1111111111111111111}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String deepseekApiUrl;

    @Autowired
    private SentimentAnalysisMapper sentimentMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MovieMapper movieMapper;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "你是一位专业的中文电影评论情感分析专家。请对每条电影评论进行情感分类，" +
            "只能返回以下三种情感之一：positive（积极/正面）、neutral（中立/中性）、negative（消极/负面）。" +
            "同时给出0-1之间的置信度。请严格按照JSON格式返回。";

    public Map<String, Object> analyzeMovieSentiment(String movieId, int sampleSize, boolean forceReanalyze) {
        Map<String, Object> result = new HashMap<>();
        try {
            String movieName = movieMapper.findNameById(movieId);
            if (movieName == null) {
                result.put("success", false);
                result.put("message", "电影不存在");
                return result;
            }

            List<Comment> allComments = commentMapper.findByMovieId(movieId, sampleSize * 3, 0);
            if (allComments == null || allComments.isEmpty()) {
                result.put("success", false);
                result.put("message", "该电影暂无评论");
                result.put("movieName", movieName);
                return result;
            }

            // 过滤已分析的评论（缓存复用，节约成本）
            List<Comment> commentsToAnalyze = new ArrayList<>();
            if (forceReanalyze) {
                // 强制重新分析
                commentsToAnalyze = allComments.stream()
                        .limit(sampleSize)
                        .collect(Collectors.toList());
            } else {
                for (Comment c : allComments) {
                    if (commentsToAnalyze.size() >= sampleSize) break;
                    SentimentAnalysis existing = sentimentMapper.findByCommentId(c.getCommentId());
                    if (existing == null) {
                        commentsToAnalyze.add(c);
                    }
                }
            }

            int analyzedCount = 0;
            int failedCount = 0;
            int batchCount = 0;

            // 批量处理：每次BATCH_SIZE条评论
            List<List<Comment>> batches = partition(commentsToAnalyze, BATCH_SIZE);

            for (List<Comment> batch : batches) {
                try {
                    Map<String, Map<String, Object>> batchResults = callDeepSeekApiBatch(batch);
                    batchCount++;

                    for (Comment comment : batch) {
                        try {
                            Map<String, Object> llmResult = batchResults.get(comment.getCommentId());
                            if (llmResult != null && llmResult.containsKey("sentiment")) {
                                SentimentAnalysis analysis = new SentimentAnalysis();
                                analysis.setCommentId(comment.getCommentId());
                                analysis.setMovieId(movieId);
                                analysis.setSentiment((String) llmResult.get("sentiment"));
                                analysis.setConfidence((Double) llmResult.get("confidence"));
                                analysis.setAnalyzedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                                if (sentimentMapper.existsByCommentId(comment.getCommentId())) {
                                    sentimentMapper.updateByCommentId(analysis);
                                } else {
                                    sentimentMapper.insert(analysis);
                                }
                                analyzedCount++;
                            } else {
                                failedCount++;
                            }
                        } catch (Exception e) {
                            log.warn("保存情感分析结果失败: commentId={}", comment.getCommentId());
                            failedCount++;
                        }
                    }

                    // 批量间稍作间隔，避免限流
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("批量情感分析失败: batch size={}", batch.size(), e);
                    failedCount += batch.size();
                }
            }

            long totalAnalyzed = sentimentMapper.countByMovieId(movieId);
            Map<String, Object> existingDistribution = getDistribution(movieId);

            result.put("success", true);
            result.put("movieId", movieId);
            result.put("movieName", movieName);
            result.put("totalComments", allComments.size());
            result.put("analyzedCount", analyzedCount);
            result.put("failedCount", failedCount);
            result.put("batchCount", batchCount);
            result.put("totalAnalyzed", totalAnalyzed);
            result.put("distribution", existingDistribution);
            result.put("analyses", getRecentAnalyses(movieId, 20));

        } catch (Exception e) {
            log.error("情感分析异常: movieId={}", movieId, e);
            result.put("success", false);
            result.put("message", "情感分析失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 批量调用DeepSeek API，一次分析多条评论
     */
    private Map<String, Map<String, Object>> callDeepSeekApiBatch(List<Comment> comments) {
        Map<String, Map<String, Object>> results = new HashMap<>();

        try {
            // 构建批量分析的提示
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("请对以下 ").append(comments.size()).append(" 条电影评论逐一进行情感分析。\n");
            userPrompt.append("请严格按照JSON数组格式返回，格式为：\n");
            userPrompt.append("[{\"id\": 1, \"sentiment\": \"positive|neutral|negative\", \"confidence\": 0.XX}, ...]\n\n");

            for (int i = 0; i < comments.size(); i++) {
                Comment c = comments.get(i);
                String content = c.getContent() != null ? c.getContent().replace("\"", "'") : "";
                // 限制单条评论长度，避免token浪费
                if (content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }
                userPrompt.append(i + 1).append(". \"").append(content).append("\"\n");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt.toString());
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.3);

            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            httpHeaders.set("Authorization", "Bearer " + deepseekApiKey);
            httpHeaders.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(requestBody, httpHeaders);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                    deepseekApiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String text = choices.get(0).get("message").get("content").asText();
                    return parseBatchResults(text, comments);
                }
            }
        } catch (Exception e) {
            log.error("批量调用DeepSeek API失败", e);
        }

        // 失败时返回空结果
        for (Comment c : comments) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("sentiment", "neutral");
            fallback.put("confidence", 0.5);
            results.put(c.getCommentId(), fallback);
        }
        return results;
    }

    /**
     * 解析批量分析结果
     */
    private Map<String, Map<String, Object>> parseBatchResults(String text, List<Comment> comments) {
        Map<String, Map<String, Object>> results = new HashMap<>();

        // 初始化默认值
        for (Comment c : comments) {
            Map<String, Object> defaultResult = new HashMap<>();
            defaultResult.put("sentiment", "neutral");
            defaultResult.put("confidence", 0.5);
            results.put(c.getCommentId(), defaultResult);
        }

        try {
            text = text.trim();
            int jsonStart = text.indexOf('[');
            int jsonEnd = text.lastIndexOf(']');

            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = text.substring(jsonStart, jsonEnd + 1);
                JsonNode arrayNode = objectMapper.readTree(jsonStr);

                if (arrayNode.isArray()) {
                    int index = 0;
                    for (JsonNode item : arrayNode) {
                        if (index < comments.size()) {
                            Comment comment = comments.get(index);
                            String sentiment = item.has("sentiment") ? item.get("sentiment").asText() : "neutral";
                            double confidence = item.has("confidence") ? item.get("confidence").asDouble() : 0.5;

                            Map<String, Object> parsed = new HashMap<>();
                            parsed.put("sentiment", mapSentiment(sentiment));
                            parsed.put("confidence", Math.min(1.0, Math.max(0.0, confidence)));
                            results.put(comment.getCommentId(), parsed);
                            index++;
                        }
                    }
                }
            } else {
                // 尝试解析单个JSON对象（有些模型可能返回单个对象）
                jsonStart = text.indexOf('{');
                jsonEnd = text.lastIndexOf('}');
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = text.substring(jsonStart, jsonEnd + 1);
                    JsonNode node = objectMapper.readTree(jsonStr);

                    // 尝试提取sentiment和confidence
                    if (node.has("sentiment") && comments.size() == 1) {
                        Map<String, Object> parsed = new HashMap<>();
                        parsed.put("sentiment", mapSentiment(node.get("sentiment").asText()));
                        parsed.put("confidence", node.has("confidence") ? node.get("confidence").asDouble() : 0.5);
                        results.put(comments.get(0).getCommentId(), parsed);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析批量情感结果失败: {}", text, e);
        }

        return results;
    }

    /**
     * 将列表分割为指定大小的批次
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String mapSentiment(String raw) {
        if (raw == null) return "neutral";
        String lower = raw.toLowerCase().trim();
        if (lower.contains("pos") || lower.contains("积极") || lower.contains("正面") || lower.equals("positive")) {
            return "positive";
        }
        if (lower.contains("neg") || lower.contains("消极") || lower.contains("负面") || lower.equals("negative")) {
            return "negative";
        }
        return "neutral";
    }

    public Map<String, Object> getDistribution(String movieId) {
        long total = sentimentMapper.countByMovieId(movieId);
        List<Map<String, Object>> grouped = sentimentMapper.countByMovieIdGrouped(movieId);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);

        Map<String, Long> counts = new HashMap<>();
        counts.put("positive", 0L);
        counts.put("neutral", 0L);
        counts.put("negative", 0L);

        for (Map<String, Object> row : grouped) {
            String sentiment = (String) row.get("sentiment");
            Long count = ((Number) row.get("cnt")).longValue();
            counts.put(sentiment, count);
        }

        Map<String, Double> percentages = new HashMap<>();
        if (total > 0) {
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                percentages.put(entry.getKey(), (entry.getValue() * 100.0) / total);
            }
        } else {
            percentages.put("positive", 0.0);
            percentages.put("neutral", 0.0);
            percentages.put("negative", 0.0);
        }

        result.put("counts", counts);
        result.put("percentages", percentages);
        return result;
    }

    public List<Map<String, Object>> getRecentAnalyses(String movieId, int limit) {
        List<SentimentAnalysis> analyses = sentimentMapper.findByMovieId(movieId);
        if (analyses == null) return Collections.emptyList();

        Set<String> commentIds = analyses.stream()
                .map(SentimentAnalysis::getCommentId)
                .collect(Collectors.toSet());

        Map<String, Comment> commentMap = new HashMap<>();
        if (!commentIds.isEmpty()) {
            for (String cid : commentIds) {
                Comment c = commentMapper.findById(cid);
                if (c != null) commentMap.put(cid, c);
            }
        }

        return analyses.stream()
                .limit(limit)
                .map(a -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("commentId", a.getCommentId());
                    item.put("movieId", a.getMovieId());
                    item.put("sentiment", a.getSentiment());
                    item.put("confidence", a.getConfidence());
                    item.put("analyzedAt", a.getAnalyzedAt());
                    Comment c = commentMap.get(a.getCommentId());
                    if (c != null) {
                        item.put("content", c.getContent());
                        item.put("rating", c.getRating());
                        item.put("votes", c.getVotes());
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    public Result<Map<String, Object>> analyzeComment(String commentId) {
        try {
            Comment comment = commentMapper.findById(commentId);
            if (comment == null) {
                return Result.error(500, "评论不存在");
            }

            // 检查是否已分析过（缓存复用）
            SentimentAnalysis existing = sentimentMapper.findByCommentId(commentId);
            if (existing != null) {
                Map<String, Object> result = new HashMap<>();
                result.put("commentId", existing.getCommentId());
                result.put("sentiment", existing.getSentiment());
                result.put("confidence", existing.getConfidence());
                result.put("analyzedAt", existing.getAnalyzedAt());
                result.put("fromCache", true);
                return Result.success(result);
            }

            // 单条调用（用于API接口）
            Map<String, Map<String, Object>> batchResults = callDeepSeekApiBatch(Collections.singletonList(comment));
            Map<String, Object> llmResult = batchResults.get(commentId);

            if (llmResult == null) {
                return Result.error(500, "分析失败");
            }

            SentimentAnalysis analysis = new SentimentAnalysis();
            analysis.setCommentId(comment.getCommentId());
            analysis.setMovieId(comment.getMovieId());
            analysis.setSentiment((String) llmResult.get("sentiment"));
            analysis.setConfidence((Double) llmResult.get("confidence"));
            analysis.setAnalyzedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            if (sentimentMapper.existsByCommentId(comment.getCommentId())) {
                sentimentMapper.updateByCommentId(analysis);
            } else {
                sentimentMapper.insert(analysis);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("commentId", analysis.getCommentId());
            result.put("sentiment", analysis.getSentiment());
            result.put("confidence", analysis.getConfidence());
            result.put("analyzedAt", analysis.getAnalyzedAt());
            return Result.success(result);
        } catch (Exception e) {
            log.error("单条评论情感分析失败", e);
            return Result.error(500, "分析失败: " + e.getMessage());
        }
    }
}
