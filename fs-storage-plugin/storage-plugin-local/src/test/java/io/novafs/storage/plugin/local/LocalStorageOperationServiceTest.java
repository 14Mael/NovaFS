package io.novafs.storage.plugin.local;

import io.novafs.storage.plugin.core.exception.StoragePluginException;
import io.novafs.storage.plugin.core.model.ChunkInitRequest;
import io.novafs.storage.plugin.core.model.ChunkMergeRequest;
import io.novafs.storage.plugin.core.model.ChunkUploadRequest;
import io.novafs.storage.plugin.core.model.FileItem;
import io.novafs.storage.plugin.core.model.FileMetadata;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import io.novafs.storage.plugin.core.model.StorageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 本地存储插件单元测试
 */
class LocalStorageOperationServiceTest {

    @TempDir
    Path tempDir;

    LocalStorageOperationService service;
    StorageConfig config;

    @BeforeEach
    void setUp() {
        service = new LocalStorageOperationService();
        config = new StorageConfig();
        config.setBasePath(tempDir.toString());
    }

    @Test
    void shouldUploadAndDownload() throws Exception {
        String key = "dir/hello.txt";
        service.uploadFile(config, request(key, "hello nova"));

        assertThat(Files.exists(tempDir.resolve("dir/hello.txt"))).isTrue();
        try (InputStream in = service.downloadFile(config, key)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hello nova");
        }
    }

    @Test
    void shouldDeleteFile() throws Exception {
        String key = "to-delete.txt";
        service.uploadFile(config, request(key, "x"));

        service.deleteFile(config, key);

        assertThat(Files.exists(tempDir.resolve(key))).isFalse();
    }

    @Test
    void shouldRenameAndCopyFile() throws Exception {
        service.uploadFile(config, request("a.txt", "data"));

        service.renameFile(config, "a.txt", "sub/b.txt");
        assertThat(Files.exists(tempDir.resolve("a.txt"))).isFalse();
        assertThat(Files.exists(tempDir.resolve("sub/b.txt"))).isTrue();

        service.copyFile(config, "sub/b.txt", "c.txt");
        assertThat(Files.exists(tempDir.resolve("c.txt"))).isTrue();
        try (InputStream in = service.downloadFile(config, "c.txt")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("data");
        }
    }

    @Test
    void shouldGetMetadataAndListFiles() throws Exception {
        service.uploadFile(config, request("dir/a.txt", "aaa"));
        service.uploadFile(config, request("dir/b.txt", "bbb"));

        FileMetadata metadata = service.getFileMetadata(config, "dir/a.txt");
        assertThat(metadata.getFileSize()).isEqualTo(3L);
        assertThat(metadata.getObjectKey()).isEqualTo("dir/a.txt");

        List<FileItem> items = service.listFiles(config, "dir");
        assertThat(items).extracting(FileItem::getObjectKey)
                .containsExactly("a.txt", "b.txt");
    }

    @Test
    void shouldRejectPathTraversal() {
        assertThatThrownBy(() -> service.uploadFile(config, request("../evil.txt", "x")))
                .isInstanceOf(StoragePluginException.class)
                .hasMessageContaining("非法的对象路径");
    }

    @Test
    void shouldRejectMissingBasePath() {
        StorageConfig empty = new StorageConfig();
        assertThatThrownBy(() -> service.uploadFile(empty, request("a.txt", "x")))
                .isInstanceOf(StoragePluginException.class)
                .hasMessageContaining("basePath");
    }

    @Test
    void shouldChunkUploadAndMerge() throws Exception {
        ChunkInitRequest init = new ChunkInitRequest();
        init.setObjectKey("big.bin");
        init.setTotalChunks(3);
        String uploadId = service.initChunkUpload(config, init);
        assertThat(uploadId).isNotBlank();

        for (int i = 1; i <= 3; i++) {
            ChunkUploadRequest chunk = new ChunkUploadRequest();
            chunk.setUploadId(uploadId);
            chunk.setChunkNumber(i);
            chunk.setInputStream(new ByteArrayInputStream(("part" + i).getBytes(StandardCharsets.UTF_8)));
            service.uploadChunk(config, chunk);
        }

        ChunkMergeRequest merge = new ChunkMergeRequest();
        merge.setUploadId(uploadId);
        merge.setObjectKey("big.bin");
        assertThat(service.mergeChunks(config, merge)).isEqualTo("big.bin");

        try (InputStream in = service.downloadFile(config, "big.bin")) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("part1part2part3");
        }
        // 合并后临时分片目录应被清理
        assertThat(Files.exists(tempDir.resolve(".novafs-chunks"))).isFalse();
    }

    @Test
    void shouldRejectChunkMd5Mismatch() {
        String uploadId = service.initChunkUpload(config, new ChunkInitRequest());
        ChunkUploadRequest chunk = new ChunkUploadRequest();
        chunk.setUploadId(uploadId);
        chunk.setChunkNumber(1);
        chunk.setInputStream(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));
        chunk.setMd5("00000000000000000000000000000000");

        assertThatThrownBy(() -> service.uploadChunk(config, chunk))
                .isInstanceOf(StoragePluginException.class)
                .hasMessageContaining("MD5");
    }

    @Test
    void shouldMergeFailWhenNoChunks() {
        String uploadId = service.initChunkUpload(config, new ChunkInitRequest());
        ChunkMergeRequest merge = new ChunkMergeRequest();
        merge.setUploadId(uploadId);
        merge.setObjectKey("empty.bin");

        assertThatThrownBy(() -> service.mergeChunks(config, merge))
                .isInstanceOf(StoragePluginException.class)
                .hasMessageContaining("没有可合并的分片");
    }

    private FileUploadRequest request(String key, String content) {
        FileUploadRequest r = new FileUploadRequest();
        r.setObjectKey(key);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        r.setInputStream(new ByteArrayInputStream(bytes));
        r.setFileSize((long) bytes.length);
        r.setContentType("text/plain");
        return r;
    }
}
