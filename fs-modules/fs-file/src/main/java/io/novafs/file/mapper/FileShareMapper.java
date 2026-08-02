package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件分享 Mapper
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {
}
