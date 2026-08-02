package io.novafs.rag.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.rag.entity.RagChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 切片 Mapper
 */
@Mapper
public interface RagChunkMapper extends BaseMapper<RagChunk> {
}