# 存储插件体系设计

## 一、设计目标

- **可插拔**：新增存储平台无需修改核心代码，只需实现 SPI 接口
- **统一抽象**：所有存储平台对外暴露相同的操作接口
- **动态切换**：运行时切换文件上传到不同存储平台

## 二、接口设计

### 2.1 核心接口

```java
package io.novafs.storage.plugin.core;

public interface IStorageOperationService {

    // ===== 文件操作 =====

    /** 上传文件 */
    String uploadFile(StorageConfig config, FileUploadRequest request);

    /** 分片上传初始化（返回 uploadId） */
    String initChunkUpload(StorageConfig config, ChunkInitRequest request);

    /** 上传分片 */
    void uploadChunk(StorageConfig config, ChunkUploadRequest request);

    /** 合并分片 */
    String mergeChunks(StorageConfig config, ChunkMergeRequest request);

    /** 下载文件（返回 InputStream） */
    InputStream downloadFile(StorageConfig config, String objectKey);

    /** 删除文件 */
    void deleteFile(StorageConfig config, String objectKey);

    /** 重命名文件 */
    void renameFile(StorageConfig config, String oldKey, String newKey);

    /** 复制文件 */
    void copyFile(StorageConfig config, String sourceKey, String destKey);

    /** 获取文件元信息 */
    FileMetadata getFileMetadata(StorageConfig config, String objectKey);

    /** 列出目录下的文件 */
    List<FileItem> listFiles(StorageConfig config, String prefix);

    /** 生成预签名 URL（带过期时间） */
    String generatePresignedUrl(StorageConfig config, String objectKey, long expirySeconds);

    /** 获取平台类型标识 */
    String getPlatformType();
}
```

### 2.2 配置模型

```java
// 存储配置——由 DB 中的 JSON 字段反序列化而来
@Data
public class StorageConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region;
    private String bucketName;
    private String basePath;       // 基础路径前缀
    private String domain;          // 自定义域名
    private Boolean isPrivate;      // 是否为私有存储（需要签名）
}

// 上传请求
@Data
public class FileUploadRequest {
    private String objectKey;       // 存储上的 key
    private InputStream inputStream;
    private Long fileSize;
    private String contentType;
}

// 分片相关
@Data
public class ChunkInitRequest {
    private String objectKey;
    private Long fileSize;
    private Integer totalChunks;
}

@Data
public class ChunkUploadRequest {
    private String uploadId;
    private Integer chunkNumber;
    private InputStream inputStream;
    private Long chunkSize;
    private String md5;            // 分片 MD5 校验
}

@Data
public class ChunkMergeRequest {
    private String uploadId;
    private String objectKey;
    private String fileName;
}

// 文件元信息
@Data
public class FileMetadata {
    private String objectKey;
    private Long fileSize;
    private String contentType;
    private String etag;
    private Date lastModified;
}

// 文件列表项
@Data
public class FileItem {
    private String objectKey;
    private Long fileSize;
    private Boolean isDir;
    private Date lastModified;
}
```

### 2.3 抽象基类

```java
public abstract class AbstractStorageOperationService
        implements IStorageOperationService {

    @Override
    public String uploadFile(StorageConfig config, FileUploadRequest request) {
        // 1. 校验参数
        validateConfig(config);
        // 2. 构建存储路径
        String finalKey = buildObjectKey(config, request.getObjectKey());
        // 3. 调用子类实现
        return doUpload(config, finalKey, request);
    }

    // 子类只需实现这个方法
    protected abstract String doUpload(StorageConfig config, String objectKey,
                                       FileUploadRequest request);

    protected void validateConfig(StorageConfig config) {
        // 公共参数校验
    }

    protected String buildObjectKey(StorageConfig config, String originalKey) {
        return config.getBasePath() != null
            ? config.getBasePath() + "/" + originalKey
            : originalKey;
    }
}
```

### 2.4 S3 兼容基类

```java
public abstract class AbstractS3CompatibleStorageService
        extends AbstractStorageOperationService {

    // 子类只需提供 S3 客户端构建方法
    protected abstract S3Client createS3Client(StorageConfig config);

    @Override
    protected String doUpload(StorageConfig config, String objectKey,
                              FileUploadRequest request) {
        S3Client client = createS3Client(config);
        PutObjectRequest putReq = PutObjectRequest.builder()
            .bucket(config.getBucketName())
            .key(objectKey)
            .contentType(request.getContentType())
            .build();
        client.putObject(putReq, RequestBody.fromInputStream(
            request.getInputStream(), request.getFileSize()));
        return objectKey;
    }
}
```

## 三、SPI 加载机制

### 3.1 SPI 约定

每个存储插件必须在 `META-INF/services/` 下注册：

```
META-INF/services/io.novafs.storage.plugin.core.IStorageOperationService
```

文件内容（插件实现类全限定名）：

```
io.novafs.storage.plugin.local.LocalStorageOperationService
io.novafs.storage.plugin.minio.MinioStorageOperationService
io.novafs.storage.plugin.aliyunoss.AliyunOssStorageOperationService
```

### 3.2 插件管理（Spring Boot 集成）

```java
@Component
public class StoragePluginRegistry implements ApplicationContextAware {

    // key = platform identifier (如 "local", "minio")
    private final Map<String, IStorageOperationService> pluginMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 方式1：SPI 加载
        ServiceLoader<IStorageOperationService> loader =
            ServiceLoader.load(IStorageOperationService.class);
        for (IStorageOperationService plugin : loader) {
            pluginMap.put(plugin.getPlatformType(), plugin);
            log.info("已注册存储插件: {}", plugin.getPlatformType());
        }

        // 方式2：Spring Bean 注册（备选）
        // 直接从 ApplicationContext 获取所有 IStorageOperationService Bean
    }

    public IStorageOperationService getPlugin(String platformType) {
        IStorageOperationService plugin = pluginMap.get(platformType);
        if (plugin == null) {
            throw new UnsupportedStoragePlatformException(platformType);
        }
        return plugin;
    }
}
```

### 3.3 实例工厂（带缓存）

```java
@Component
public class StorageInstanceFactory {

    private final StoragePluginRegistry pluginRegistry;
    private final Cache<String, IStorageOperationService> instanceCache;

    // 根据 storage_platform_setting_id 获取可用的存储服务实例
    public IStorageOperationService getService(String settingId) {
        // 1. 查 DB 获取配置
        StorageSetting setting = storageSettingMapper.selectById(settingId);
        // 2. 查平台信息
        StoragePlatform platform = storagePlatformMapper
            .selectByIdentifier(setting.getPlatformIdentifier());
        // 3. 从注册中心获取插件
        IStorageOperationService plugin = pluginRegistry.getPlugin(platform.getIdentifier());
        // 4. 反序列化配置
        StorageConfig config = JsonUtils.parse(setting.getConfigData(), StorageConfig.class);
        // 5. 包装成带配置的实例（可缓存）
        return new ConfiguredStorageService(plugin, config);
    }
}
```

## 四、存储门面（统一入口）

```java
@Service
@RequiredArgsConstructor
public class StorageServiceFacade {

    private final StorageInstanceFactory instanceFactory;

    public String uploadFile(String settingId, FileUploadRequest request) {
        IStorageOperationService service = instanceFactory.getService(settingId);
        return service.uploadFile(request);
    }

    public InputStream downloadFile(String settingId, String objectKey) {
        IStorageOperationService service = instanceFactory.getService(settingId);
        return service.downloadFile(objectKey);
    }

    public void deleteFile(String settingId, String objectKey) {
        IStorageOperationService service = instanceFactory.getService(settingId);
        service.deleteFile(objectKey);
    }

    // ... 其他操作委派
}
```

## 五、插件实现示例

### 5.1 本地存储

```java
@StoragePlugin(platformType = "local")
public class LocalStorageOperationService extends AbstractStorageOperationService {

    @Override
    public String getPlatformType() {
        return "local";
    }

    @Override
    protected String doUpload(StorageConfig config, String objectKey,
                              FileUploadRequest request) {
        Path targetPath = Path.of(config.getBasePath(), objectKey);
        Files.createDirectories(targetPath.getParent());
        Files.copy(request.getInputStream(), targetPath,
            StandardCopyOption.REPLACE_EXISTING);
        return objectKey;
    }

    // ... doDownload, doDelete 等
}
```

### 5.2 MinIO 存储

```java
@StoragePlugin(platformType = "minio")
public class MinioStorageOperationService
        extends AbstractS3CompatibleStorageService {

    @Override
    public String getPlatformType() {
        return "minio";
    }

    @Override
    protected S3Client createS3Client(StorageConfig config) {
        return S3Client.builder()
            .endpointOverride(URI.create(config.getEndpoint()))
            .region(Region.of(config.getRegion() != null
                ? config.getRegion() : "us-east-1"))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    config.getAccessKey(), config.getSecretKey())))
            .forcePathStyle(true)  // MinIO 需要
            .build();
    }
}
```

## 六、模块结构

```
storage-plugin-core/
├── src/main/java/.../storage/plugin/core/
│   ├── IStorageOperationService.java      ← SPI 接口
│   ├── AbstractStorageOperationService.java ← 抽象基类
│   ├── AbstractS3CompatibleStorageService.java ← S3 兼容基类
│   ├── model/
│   │   ├── StorageConfig.java
│   │   ├── FileUploadRequest.java
│   │   ├── ChunkInitRequest.java
│   │   └── ...
│   └── exception/
│       └── StoragePluginException.java

storage-plugin-boot/
├── src/main/java/.../storage/plugin/boot/
│   ├── StoragePluginRegistry.java          ← SPI 加载 + Spring 集成
│   ├── StorageInstanceFactory.java         ← 实例工厂 + 缓存
│   ├── StorageServiceFacade.java           ← 统一门面
│   └── annotation/
│       └── StoragePlugin.java              ← 插件注解

storage-plugin-local/
├── src/main/java/.../storage/plugin/local/
│   └── LocalStorageOperationService.java
├── src/main/resources/META-INF/services/
│   └── io.novafs.storage.plugin.core.IStorageOperationService

storage-plugin-minio/
├── src/main/java/.../storage/plugin/minio/
│   └── MinioStorageOperationService.java
├── src/main/resources/META-INF/services/
│   └── ... (同上)
```

## 七、新增存储平台的步骤

```
1. 创建新模块：storage-plugin-xxx
2. 添加依赖：storage-plugin-core
3. 实现 IStorageOperationService 接口
4. 注册 SPI：在 META-INF/services/ 添加实现类全限定名
5. 在 storage_platform 表中插入一条平台定义记录（含 config_scheme）
6. 在 storage_platform_icon 中配置平台图标
7. 在后台界面选择该平台并配置参数
```

## 八、关键设计决策

| 决策 | 选项 | 理由 |
|---|---|---|
| SPI vs Spring @Service | 双通道（SPI + Bean） | SPI 保障纯粹的可插拔性，Bean 保障 Spring 生态兼容 |
| 配置存储格式 | JSON（MySQL JSON 类型） | 不同平台的配置字段不同，JSON 最灵活 |
| 缓存策略 | Caffeine（插件实例缓存） | 避免每次请求都查 DB 和重建客户端 |
| 分片上传 | 委托给存储平台 SDK | S3、OSS 等原生支持分片，本地存储自己实现文件合并 |
