package io.novafs.file.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.file.entity.FileShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 文件分享 Mapper
 */
@Mapper
public interface FileShareMapper extends BaseMapper<FileShare> {

    /**
     * 原子自增查看次数（携带上限校验，防并发绕过 max_view_count）
     */
    @Update("UPDATE file_shares SET view_count = view_count + 1 "
            + "WHERE id = #{id} AND (max_view_count IS NULL OR view_count < max_view_count)")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 原子自增下载次数（携带上限校验，防并发绕过 max_download_count）
     */
    @Update("UPDATE file_shares SET download_count = download_count + 1 "
            + "WHERE id = #{id} AND (max_download_count IS NULL OR download_count < max_download_count)")
    int incrementDownloadCount(@Param("id") Long id);
}
