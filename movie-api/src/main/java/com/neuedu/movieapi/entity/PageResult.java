package com.neuedu.movieapi.entity;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> data;
    private Integer currentPage;
    private Integer pageSize;
    private Long totalCount;
    private Integer totalPages;

    public PageResult(List<T> data, Integer currentPage, Integer pageSize, Long totalCount) {
        this.data = data;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = (int) Math.ceil((double) totalCount / pageSize);
    }
}
