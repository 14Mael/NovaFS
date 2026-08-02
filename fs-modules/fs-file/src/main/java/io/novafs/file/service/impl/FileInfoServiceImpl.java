package io.novafs.file.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.service.FileInfoService;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.framework.common.model.PageQuery;
import io.novafs.framework.common.model.PageResult;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 文件信息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileInfoServiceImpl implements FileInfoService {

    private final FileInfoMapper fileInfoMapper;
    private final StorageConfigResolver configResolver;
    private final StorageServiceFacade storageFacade;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO upload(Long userId, Long workspaceId, Long parentId, Long storagePlatformSettingId,
                             String fileName, InputStream in) {
        StorageConfigResolver.StorageTarget target = configResolver.resolve(storagePlatformSettingId);
        String objectKey = UUID.randomUUID() + "/" + fileName;

        MessageDigest digest = newDigest();
        DigestInputStream digestStream = new DigestInputStream(in, digest);
        FileUploadRequest pluginReq = new FileUploadRequest();
        pluginReq.setObjectKey(objectKey);
        pluginReq.setInputStream(digestStream);
        pluginReq.setContentType(contentTypeOf(fileName));
        storageFacade.uploadFile(target.platformType(), target.config(), pluginReq);
        String md5 = HexFormat.of().formatHex(digest.digest());

        FileInfo file = buildFile(userId, workspaceId, parentId, fileName, objectKey, md5, storagePlatformSettingId);
        fileInfoMapper.insert(file);
        log.info("File uploaded: id={}, name={}", file.getId(), fileName);
        return toVO(file);
    }

    @Override
    public PageResult<FileInfoVO> list(Long workspaceId, Long parentId, PageQuery pageQuery) {
        QueryWrapper qw = QueryWrapper.create()
                .where(FileInfo::getWorkspaceId).eq(workspaceId)
                .and(FileInfo::getParentId).eq(parentId)
                .and(FileInfo::getIsDeleted).eq(false)
                .orderBy(FileInfo::getIsDir, false)
                .orderBy(FileInfo::getCreatedAt, false);
        Page<FileInfo> page = fileInfoMapper.paginate(pageQuery.getPage(), pageQuery.getPageSize(), qw);
        return PageResult.of(pageQuery.getPage(), pageQuery.getPageSize(), page.getTotalRow(),
                page.getRecords().stream().map(FileInfoServiceImpl::toVO).toList());
    }

    @Override
    public InputStream download(Long fileId, Long userId) {
        FileInfo file = requireOwnedFile(fileId, userId);
        if (file.getObjectKey() == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND, "文件尚未上传完成");
        }
        StorageConfigResolver.StorageTarget target = configResolver.resolve(file.getStoragePlatformSettingId());
        return storageFacade.downloadFile(target.platformType(), target.config(), file.getObjectKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long fileId, Long userId) {
        FileInfo file = requireOwnedFile(fileId, userId);
        if (file.isDeleted()) {
            return;
        }
        file.setIsDeleted(true);
        file.setDeletedTime(LocalDateTime.now());
        fileInfoMapper.update(file);
        log.info("File moved to recycle: id={}", fileId);
    }

    @Override
    public PageResult<FileInfoVO> recycleList(Long workspaceId, PageQuery pageQuery) {
        QueryWrapper qw = QueryWrapper.create()
                .where(FileInfo::getWorkspaceId).eq(workspaceId)
                .and(FileInfo::getIsDeleted).eq(true)
                .orderBy(FileInfo::getDeletedTime, false);
        Page<FileInfo> page = fileInfoMapper.paginate(pageQuery.getPage(), pageQuery.getPageSize(), qw);
        return PageResult.of(pageQuery.getPage(), pageQuery.getPageSize(), page.getTotalRow(),
                page.getRecords().stream().map(FileInfoServiceImpl::toVO).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long fileId, Long userId) {
        FileInfo file = requireOwnedFile(fileId, userId);
        file.setIsDeleted(false);
        file.setDeletedTime(null);
        fileInfoMapper.update(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purge(Long fileId, Long userId) {
        FileInfo file = requireOwnedFile(fileId, userId);
        if (!file.isDeleted()) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "请先移入回收站再彻底删除");
        }
        if (file.getObjectKey() != null) {
            StorageConfigResolver.StorageTarget target = configResolver.resolve(file.getStoragePlatformSettingId());
            storageFacade.deleteFile(target.platformType(), target.config(), file.getObjectKey());
        }
        fileInfoMapper.deleteById(fileId);
        log.info("File purged: id={}", fileId);
    }

    // ===== 私有方法 =====

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

    private static FileInfo buildFile(Long userId, Long workspaceId, Long parentId, String fileName,
                                      String objectKey, String md5, Long settingId) {
        FileInfo file = new FileInfo();
        file.setWorkspaceId(workspaceId);
        file.setUserId(userId);
        file.setParentId(parentId);
        file.setOriginalName(fileName);
        file.setSuffix(suffixOf(fileName));
        file.setSize(0L);
        file.setMimeType(contentTypeOf(fileName));
        file.setIsDir(false);
        file.setObjectKey(objectKey);
        file.setContentMd5(md5);
        file.setStoragePlatformSettingId(settingId);
        file.setUploadTime(LocalDateTime.now());
        file.setIsDeleted(false);
        return file;
    }

    private static String suffixOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : null;
    }

    private static String contentTypeOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return "application/octet-stream";
        }
        return switch (fileName.substring(dot + 1).toLowerCase()) {
            case "txt", "md", "log" -> "text/plain";
            case "json" -> "application/json";
            case "pdf" -> "application/pdf";
            case "png", "jpg", "jpeg", "gif", "webp", "svg", "bmp" -> "image/" + fileName.substring(dot + 1).toLowerCase();
            case "mp4", "webm", "mov", "avi" -> "video/" + fileName.substring(dot + 1).toLowerCase();
            case "mp3", "wav", "flac", "ogg" -> "audio/" + fileName.substring(dot + 1).toLowerCase();
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "xml" -> "application/xml";
            case "zip", "tar", "gz", "rar", "7z" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    private static FileInfoVO toVO(FileInfo file) {
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
        vo.setUploadTime(file.getUploadTime());
        vo.setIsDeleted(file.getIsDeleted());
        return vo;
    }
}
