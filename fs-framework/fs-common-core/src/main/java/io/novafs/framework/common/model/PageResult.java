package io.novafs.framework.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页返回结果
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    @Schema(description = "当前页码")
    private Integer page;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "总条数")
    private Long total;

    @Schema(description = "总页数")
    private Long pages;

    @Schema(description = "数据列表")
    private List<T> records;

    public static <T> PageResult<T> of(Integer page, Integer pageSize, Long total, List<T> records) {
        long pages = (total + pageSize - 1) / pageSize;
        return new PageResult<>(page, pageSize, total, pages, records);
    }
}
