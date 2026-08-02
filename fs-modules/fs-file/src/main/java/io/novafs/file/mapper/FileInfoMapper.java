package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件 Mapper
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {

    /**
     * 物理删除记录（绕过逻辑删除拦截，供回收站彻底删除使用）
     */
    @Delete("DELETE FROM file_info WHERE id = #{id}")
    int deletePhysical(@Param("id") Long id);
}
