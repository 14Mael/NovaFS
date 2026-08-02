package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileTransferTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 传输任务 Mapper
 */
@Mapper
public interface FileTransferTaskMapper extends BaseMapper<FileTransferTask> {

    /**
     * 原子累加已上传分片数与字节数（防并发分片上传丢失更新）
     */
    @Update("UPDATE file_transfer_task SET uploaded_chunks = uploaded_chunks + 1, "
            + "uploaded_size = uploaded_size + #{size} WHERE id = #{id}")
    int incrementProgress(@Param("id") Long id, @Param("size") long size);
}
