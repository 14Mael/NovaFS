package io.novafs.storage.settings.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.storage.settings.entity.StorageSettings;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StorageSettingsMapper extends BaseMapper<StorageSettings> {}
