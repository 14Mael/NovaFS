package io.novafs.file.preview;

import io.novafs.file.dto.FilePreviewVO;
import io.novafs.file.entity.FileInfo;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 音视频预览策略
 */
@Component
public class VideoAudioPreviewStrategy implements PreviewStrategy {

    private static final Set<String> VIDEO_SUFFIX = Set.of("mp4", "webm", "mov", "avi", "mkv");
    private static final Set<String> AUDIO_SUFFIX = Set.of("mp3", "wav", "flac", "ogg", "m4a");

    @Override
    public boolean support(FileInfo file) {
        return file.getSuffix() != null
                && (VIDEO_SUFFIX.contains(file.getSuffix()) || AUDIO_SUFFIX.contains(file.getSuffix()));
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public FilePreviewVO preview(FileInfo file, Long userId) {
        FilePreviewVO vo = new FilePreviewVO();
        vo.setPreviewType(VIDEO_SUFFIX.contains(file.getSuffix()) ? "VIDEO" : "AUDIO");
        vo.setUrl(ImagePreviewStrategy.previewUrl(file));
        vo.setFileName(file.getOriginalName());
        return vo;
    }
}
