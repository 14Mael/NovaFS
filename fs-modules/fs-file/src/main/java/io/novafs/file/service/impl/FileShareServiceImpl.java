package io.novafs.file.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.FileShareRequest;
import io.novafs.file.dto.FileShareVO;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.entity.FileShare;
import io.novafs.file.enums.FileShareScope;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.mapper.FileShareMapper;
import io.novafs.file.service.FileShareService;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;

/**
 * 文件分享服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileShareServiceImpl implements FileShareService {

    private static final String DEFAULT_SCOPE = FileShareScope.PREVIEW + "," + FileShareScope.DOWNLOAD;
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final FileShareMapper shareMapper;
    private final FileInfoMapper fileInfoMapper;
    private final StorageConfigResolver configResolver;
    private final StorageServiceFacade storageFacade;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileShareVO create(Long userId, FileShareRequest request) {
        FileInfo file = fileInfoMapper.selectOneById(request.getFileId());
        if (file == null || !file.canBeDeletedBy(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "无权分享该文件");
        }

        FileShare share = new FileShare();
        share.setUserId(userId);
        share.setWorkspaceId(file.getWorkspaceId());
        share.setFileId(request.getFileId());
        share.setShareCode(generateUniqueCode());
        share.setSharePwd(encodeIfPresent(request.getSharePwd()));
        share.setExpireTime(request.getExpireTime());
        share.setViewCount(0);
        share.setMaxViewCount(request.getMaxViewCount());
        share.setDownloadCount(0);
        share.setMaxDownloadCount(request.getMaxDownloadCount());
        share.setScope(request.getScope() == null ? DEFAULT_SCOPE : request.getScope());
        shareMapper.insert(share);
        log.info("Share created: code={}, fileId={}", share.getShareCode(), request.getFileId());
        return toVO(share, file);
    }

    @Override
    public FileShareVO access(String shareCode, String password) {
        FileShare share = requireShare(shareCode);
        if (share.isExpired()) {
            throw new BaseException(ErrorCode.SHARE_EXPIRED);
        }
        if (share.viewLimitReached()) {
            throw new BaseException(ErrorCode.SHARE_VIEW_LIMIT_EXCEEDED);
        }
        verifyPassword(share, password);

        int rows = shareMapper.incrementViewCount(share.getId());
        if (rows == 0) {
            throw new BaseException(ErrorCode.SHARE_VIEW_LIMIT_EXCEEDED);
        }
        share.setViewCount(share.getViewCount() + 1);

        FileInfo file = fileInfoMapper.selectOneById(share.getFileId());
        if (file == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND);
        }
        return toVO(share, file);
    }

    @Override
    public StreamDownload download(String shareCode, String password) {
        FileShare share = requireShare(shareCode);
        if (share.isExpired()) {
            throw new BaseException(ErrorCode.SHARE_EXPIRED);
        }
        verifyPassword(share, password);
        if (!hasScope(share.getScope(), FileShareScope.DOWNLOAD)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "该分享未开放下载权限");
        }
        if (share.getMaxDownloadCount() != null
                && share.getDownloadCount() >= share.getMaxDownloadCount()) {
            throw new BaseException(ErrorCode.SHARE_DOWNLOAD_LIMIT_EXCEEDED);
        }
        FileInfo file = fileInfoMapper.selectOneById(share.getFileId());
        if (file == null || file.getObjectKey() == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND);
        }
        StorageConfigResolver.StorageTarget target = configResolver.resolve(file.getStoragePlatformSettingId());
        InputStream in = storageFacade.downloadFile(target.platformType(), target.config(), file.getObjectKey());

        // 先取流再原子扣减：存储不可用时不计下载次数；并发下由 SQL 保证不超过上限
        int rows = shareMapper.incrementDownloadCount(share.getId());
        if (rows == 0) {
            throw new BaseException(ErrorCode.SHARE_DOWNLOAD_LIMIT_EXCEEDED);
        }
        share.setDownloadCount(share.getDownloadCount() + 1);
        return new StreamDownload(toVO(share, file), in);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long shareId, Long userId) {
        FileShare share = shareMapper.selectOneById(shareId);
        if (share == null) {
            throw new BaseException(ErrorCode.SHARE_NOT_FOUND);
        }
        if (!Objects.equals(share.getUserId(), userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }
        shareMapper.deleteById(shareId);
        log.info("Share cancelled: id={}", shareId);
    }

    // ===== 私有方法 =====

    private FileShare requireShare(String shareCode) {
        FileShare share = shareMapper.selectOneByQuery(
                QueryWrapper.create().where(FileShare::getShareCode).eq(shareCode));
        if (share == null) {
            throw new BaseException(ErrorCode.SHARE_NOT_FOUND);
        }
        return share;
    }

    private static boolean hasScope(String scope, String required) {
        if (scope == null) {
            return false;
        }
        return Arrays.stream(scope.split(","))
                .map(String::trim)
                .anyMatch(required::equalsIgnoreCase);
    }

    private void verifyPassword(FileShare share, String password) {
        if (share.getSharePwd() == null) {
            return;
        }
        if (password == null || !passwordEncoder.matches(password, share.getSharePwd())) {
            throw new BaseException(ErrorCode.SHARE_PASSWORD_ERROR);
        }
    }

    private String encodeIfPresent(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return null;
        }
        return passwordEncoder.encode(rawPassword);
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String code = randomCode();
            long count = shareMapper.selectCountByQuery(
                    QueryWrapper.create().where(FileShare::getShareCode).eq(code));
            if (count == 0) {
                return code;
            }
        }
        throw new BaseException(ErrorCode.INTERNAL_ERROR, "分享码生成失败");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private static FileShareVO toVO(FileShare share, FileInfo file) {
        FileShareVO vo = new FileShareVO();
        vo.setId(share.getId());
        vo.setFileId(share.getFileId());
        vo.setFileName(file.getOriginalName());
        vo.setSuffix(file.getSuffix());
        vo.setFileSize(file.getSize());
        vo.setShareCode(share.getShareCode());
        vo.setHasPassword(share.getSharePwd() != null);
        vo.setExpireTime(share.getExpireTime());
        vo.setViewCount(share.getViewCount());
        vo.setDownloadCount(share.getDownloadCount());
        vo.setScope(share.getScope());
        vo.setCreatedAt(share.getCreatedAt());
        return vo;
    }
}
