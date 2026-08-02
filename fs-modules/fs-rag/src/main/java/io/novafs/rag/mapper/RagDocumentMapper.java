package io.novafs.rag.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.rag.entity.RagDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAG 文档 Mapper
 */
@Mapper
public interface RagDocumentMapper extends BaseMapper<RagDocument> {
}