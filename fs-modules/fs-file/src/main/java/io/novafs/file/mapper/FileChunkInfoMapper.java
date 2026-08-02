package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileChunkInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分片明细 Mapper
 */
@Mapper
public interface FileChunkInfoMapper extends BaseMapper<FileChunkInfo> {
}
