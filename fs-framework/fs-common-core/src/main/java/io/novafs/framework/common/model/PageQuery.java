package io.novafs.framework.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分页查询参数
 */
@Data
public class PageQuery {

    @Schema(description = "当前页码", example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "20")
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为1")
    @Max(value = 1000, message = "每页条数最大值为1000")
    private Integer pageSize = 20;

    public long getOffset() {
        return (long) (page - 1) * pageSize;
    }
}
