package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileTransferTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 传输任务 Mapper
 */
@Mapper
public interface FileTransferTaskMapper extends BaseMapper<FileTransferTask> {
}
