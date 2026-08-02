package io.novafs.storage.plugin.local;

import io.novafs.storage.plugin.boot.annotation.StoragePlugin;
import io.novafs.storage.plugin.core.AbstractStorageOperationService;
import io.novafs.storage.plugin.core.exception.StoragePluginException;
import io.novafs.storage.plugin.core.model.ChunkInitRequest;
import io.novafs.storage.plugin.core.model.ChunkMergeRequest;
import io.novafs.storage.plugin.core.model.ChunkUploadRequest;
import io.novafs.storage.plugin.core.model.FileItem;
import io.novafs.storage.plugin.core.model.FileMetadata;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import io.novafs.storage.plugin.core.model.StorageConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 本地文件系统存储插件（platformType = local）。
 * basePath 为本地存储根目录，objectKey 为根目录下的相对路径。
 */
@StoragePlugin("local")
public class LocalStorageOperationService extends AbstractStorageOperationService {

    /** 分片临时目录名（位于 basePath 下） */
    private static final String CHUNK_TMP_DIR = ".novafs-chunks";

    private static final String CHUNK_FILE_PREFIX = "chunk-";

    @Override
    public String getPlatformType() {
        return "local";
    }

    /** 本地存储由 resolve() 统一拼接 basePath，因此对象键保持原样 */
    @Override
    protected String buildObjectKey(StorageConfig config, String originalKey) {
        if (originalKey == null || originalKey.isBlank()) {
            throw new StoragePluginException("objectKey 不能为空");
        }
        return originalKey;
    }

    @Override
    protected String doUpload(StorageConfig config, String objectKey, FileUploadRequest request) {
        Path target = resolve(config, objectKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = request.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return objectKey;
        } catch (IOException e) {
            throw new StoragePluginException("本地文件上传失败: " + objectKey, e);
        }
    }

    @Override
    public InputStream downloadFile(StorageConfig config, String objectKey) {
        try {
            return Files.newInputStream(resolve(config, objectKey));
        } catch (IOException e) {
            throw new StoragePluginException("本地文件下载失败: " + objectKey, e);
        }
    }

    @Override
    public void deleteFile(StorageConfig config, String objectKey) {
        try {
            Files.deleteIfExists(resolve(config, objectKey));
        } catch (IOException e) {
            throw new StoragePluginException("本地文件删除失败: " + objectKey, e);
        }
    }

    @Override
    public void renameFile(StorageConfig config, String oldKey, String newKey) {
        try {
            Path source = resolve(config, oldKey);
            Path target = resolve(config, newKey);
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StoragePluginException("本地文件重命名失败: " + oldKey + " -> " + newKey, e);
        }
    }

    @Override
    public void copyFile(StorageConfig config, String sourceKey, String destKey) {
        try {
            Path source = resolve(config, sourceKey);
            Path target = resolve(config, destKey);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StoragePluginException("本地文件复制失败: " + sourceKey + " -> " + destKey, e);
        }
    }

    @Override
    public FileMetadata getFileMetadata(StorageConfig config, String objectKey) {
        Path path = resolve(config, objectKey);
        try {
            if (!Files.exists(path)) {
                throw new StoragePluginException("文件不存在: " + objectKey);
            }
            FileMetadata metadata = new FileMetadata();
            metadata.setObjectKey(objectKey);
            metadata.setFileSize(Files.size(path));
            metadata.setContentType(Files.probeContentType(path));
            metadata.setLastModified(Date.from(Files.getLastModifiedTime(path).toInstant()));
            return metadata;
        } catch (IOException e) {
            throw new StoragePluginException("获取文件元信息失败: " + objectKey, e);
        }
    }

    @Override
    public List<FileItem> listFiles(StorageConfig config, String prefix) {
        Path dir = resolve(config, prefix == null ? "" : prefix);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.map(LocalStorageOperationService::toFileItem)
                    .sorted(Comparator.comparing(FileItem::getObjectKey))
                    .toList();
        } catch (IOException e) {
            throw new StoragePluginException("列出文件失败: " + dir, e);
        }
    }

    @Override
    public String generatePresignedUrl(StorageConfig config, String objectKey, long expirySeconds) {
        String domain = config.getDomain();
        if (domain == null || domain.isBlank()) {
            return "file://" + resolve(config, objectKey).toAbsolutePath();
        }
        return domain + "/" + objectKey;
    }

    // ===== 分片上传 =====

    @Override
    public String initChunkUpload(StorageConfig config, ChunkInitRequest request) {
        String uploadId = UUID.randomUUID().toString();
        try {
            Files.createDirectories(chunkDir(config, uploadId));
            return uploadId;
        } catch (IOException e) {
            throw new StoragePluginException("初始化分片上传失败", e);
        }
    }

    @Override
    public void uploadChunk(StorageConfig config, ChunkUploadRequest request) {
        Path chunkFile = chunkDir(config, request.getUploadId())
                .resolve(String.format("%s%06d", CHUNK_FILE_PREFIX, request.getChunkNumber()));
        try {
            Files.createDirectories(chunkFile.getParent());
            writeChunkWithMd5Check(request, chunkFile);
        } catch (IOException e) {
            throw new StoragePluginException("分片上传失败, chunk=" + request.getChunkNumber(), e);
        }
    }

    @Override
    public String mergeChunks(StorageConfig config, ChunkMergeRequest request) {
        Path chunkDir = chunkDir(config, request.getUploadId());
        Path target = resolve(config, request.getObjectKey());
        List<Path> chunks;
        try (Stream<Path> stream = Files.list(chunkDir)) {
            chunks = stream.sorted(Comparator.comparing(LocalStorageOperationService::chunkNumber)).toList();
        } catch (IOException e) {
            throw new StoragePluginException("读取分片列表失败: " + request.getUploadId(), e);
        }
        if (chunks.isEmpty()) {
            throw new StoragePluginException("没有可合并的分片: " + request.getUploadId());
        }
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(target)) {
                for (Path chunk : chunks) {
                    Files.copy(chunk, out);
                }
            }
            deleteRecursively(chunkDir);
            deleteIfEmpty(chunkDir.getParent());
            return request.getObjectKey();
        } catch (IOException e) {
            throw new StoragePluginException("合并分片失败: " + request.getObjectKey(), e);
        }
    }

    // ===== 私有方法 =====

    /** 解析并校验对象路径（防路径穿越：normalize + 前缀检查） */
    private Path resolve(StorageConfig config, String objectKey) {
        String basePath = config.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new StoragePluginException("本地存储必须配置 basePath");
        }
        Path root = Path.of(basePath).toAbsolutePath().normalize();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new StoragePluginException("非法的对象路径: " + objectKey);
        }
        return target;
    }

    private Path chunkDir(StorageConfig config, String uploadId) {
        return resolve(config, CHUNK_TMP_DIR + "/" + uploadId);
    }

    /** 边写边算 MD5，与请求中的 md5 比对（可选校验） */
    private void writeChunkWithMd5Check(ChunkUploadRequest request, Path chunkFile) throws IOException {
        MessageDigest digest = newDigest();
        try (InputStream in = request.getInputStream();
             DigestInputStream dis = new DigestInputStream(in, digest);
             OutputStream out = Files.newOutputStream(chunkFile)) {
            dis.transferTo(out);
        }
        if (request.getMd5() != null && !request.getMd5().isBlank()) {
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(request.getMd5())) {
                Files.deleteIfExists(chunkFile);
                throw new StoragePluginException("分片 MD5 校验失败, chunk=" + request.getChunkNumber());
            }
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    private static int chunkNumber(Path chunkFile) {
        String name = chunkFile.getFileName().toString();
        return Integer.parseInt(name.substring(CHUNK_FILE_PREFIX.length()));
    }

    private static FileItem toFileItem(Path path) {
        FileItem item = new FileItem();
        item.setObjectKey(path.getFileName().toString());
        item.setIsDir(Files.isDirectory(path));
        try {
            if (!Files.isDirectory(path)) {
                item.setFileSize(Files.size(path));
            }
            item.setLastModified(Date.from(Files.getLastModifiedTime(path).toInstant()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return item;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    /** 尝试删除空目录（非空或删除失败时静默忽略） */
    private static void deleteIfEmpty(Path dir) {
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // 目录非空（其他分片任务进行中）或删除失败，保留
        }
    }
}
