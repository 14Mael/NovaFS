package io.novafs.file.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.service.FileFolderService;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件夹管理服务实现
 */
@Service
@RequiredArgsConstructor
public class FileFolderServiceImpl implements FileFolderService {

    private final FileInfoMapper fileInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO createFolder(Long userId, Long workspaceId, Long parentId, String name) {
        String cleanName = validateName(name);
        requireTargetFolder(workspaceId, parentId);
        ensureNameUnique(workspaceId, parentId, cleanName, null);

        FileInfo folder = new FileInfo();
        folder.setWorkspaceId(workspaceId);
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setOriginalName(cleanName);
        folder.setIsDir(true);
        folder.setSize(0L);
        folder.setIsDeleted(false);
        fileInfoMapper.insert(folder);
        return toVO(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO rename(Long userId, Long fileId, String name) {
        String cleanName = validateName(name);
        FileInfo file = requireOwnedFile(fileId, userId);
        ensureNameUnique(file.getWorkspaceId(), file.getParentId(), cleanName, fileId);

        file.setOriginalName(cleanName);
        fileInfoMapper.update(file);
        return toVO(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO move(Long userId, Long fileId, Long targetParentId) {
        FileInfo file = requireOwnedFile(fileId, userId);
        requireTargetFolder(file.getWorkspaceId(), targetParentId);
        if (file.isDirectory()) {
            ensureNotSelfOrDescendant(file.getId(), targetParentId);
        }
        ensureNameUnique(file.getWorkspaceId(), targetParentId, file.getOriginalName(), fileId);

        file.setParentId(targetParentId);
        fileInfoMapper.update(file);
        return toVO(file);
    }

    /** 名称校验：去空格、非空、禁止路径分隔符 */
    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "名称不能为空");
        }
        String clean = name.trim();
        if (clean.length() > 255) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "名称长度不能超过 255");
        }
        if (clean.contains("/") || clean.contains("\\")) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "名称不能包含 / 或 \\");
        }
        return clean;
    }

    /** 目标父目录校验：非空时必须存在、是目录且属于同一工作空间 */
    private void requireTargetFolder(Long workspaceId, Long parentId) {
        if (parentId == null) {
            return;
        }
        FileInfo parent = fileInfoMapper.selectOneById(parentId);
        if (parent == null || !parent.isDirectory()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "目标目录不存在");
        }
        if (!parent.getWorkspaceId().equals(workspaceId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }
    }

    /** 同目录重名检查（excludeId 用于重命名/移动时排除自身） */
    private void ensureNameUnique(Long workspaceId, Long parentId, String name, Long excludeId) {
        QueryWrapper qw = QueryWrapper.create()
                .where(FileInfo::getWorkspaceId).eq(workspaceId)
                .and(FileInfo::getParentId).eq(parentId)
                .and(FileInfo::getIsDeleted).eq(false)
                .and(FileInfo::getOriginalName).eq(name);
        if (excludeId != null) {
            qw.and(FileInfo::getId).ne(excludeId);
        }
        if (fileInfoMapper.selectCountByQuery(qw) > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "同名文件或文件夹已存在");
        }
    }

    /** 防环：目标父目录不能是自身或自身的子孙目录 */
    private void ensureNotSelfOrDescendant(Long dirId, Long targetParentId) {
        Long cursor = targetParentId;
        while (cursor != null) {
            if (cursor.equals(dirId)) {
                throw new BaseException(ErrorCode.BAD_REQUEST, "不能移动到自身或其子目录中");
            }
            FileInfo parent = fileInfoMapper.selectOneById(cursor);
            if (parent == null || parent.getParentId() == null) {
                return;
            }
            cursor = parent.getParentId();
        }
    }

    private FileInfo requireOwnedFile(Long fileId, Long userId) {
        FileInfo file = fileInfoMapper.selectOneById(fileId);
        if (file == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!file.canBeDeletedBy(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }
        return file;
    }

    private FileInfoVO toVO(FileInfo file) {
        FileInfoVO vo = new FileInfoVO();
        vo.setId(file.getId());
        vo.setWorkspaceId(file.getWorkspaceId());
        vo.setUserId(file.getUserId());
        vo.setParentId(file.getParentId());
        vo.setOriginalName(file.getOriginalName());
        vo.setDisplayName(file.getDisplayName());
        vo.setSuffix(file.getSuffix());
        vo.setSize(file.getSize());
        vo.setMimeType(file.getMimeType());
        vo.setIsDir(file.getIsDir());
        vo.setContentMd5(file.getContentMd5());
        vo.setUploadTime(file.getCreatedAt());
        vo.setIsDeleted(file.isDeleted());
        return vo;
    }
}
