package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件资源 Mapper
 */
@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}
