package io.novafs.system.user.mapper;

import com.mybatisflex.core.BaseMapper;
import io.novafs.system.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
