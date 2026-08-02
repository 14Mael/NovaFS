package io.novafs.storage.platform.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.storage.platform.entity.StoragePlatform;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StoragePlatformMapper extends BaseMapper<StoragePlatform> {}
