package io.novafs.file.listener;

import io.novafs.file.entity.FileInfo;
import io.novafs.file.event.FileUploadCompleteEvent;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.framework.notify.EmailNotifyService;
import io.novafs.framework.sse.SseConnectionManager;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 上传完成监听器：SSE 推送 + 邮件通知
 */
@Component
@RequiredArgsConstructor
public class UploadCompleteListener {

    private final SseConnectionManager sseManager;
    private final EmailNotifyService emailNotifyService;
    private final SysUserService sysUserService;
    private final FileInfoMapper fileInfoMapper;

    @EventListener
    public void onUploadComplete(FileUploadCompleteEvent event) {
        sseManager.send(event.getUserId(), "upload-complete", Map.of(
                "fileId", event.getFileId(),
                "fileSize", event.getFileSize(),
                "workspaceId", event.getWorkspaceId()));

        FileInfo file = fileInfoMapper.selectOneById(event.getFileId());
        SysUser user = sysUserService.findById(event.getUserId());
        if (file != null && user != null && user.getEmail() != null) {
            String subject = "文件上传完成";
            String content = "您的文件「" + file.getOriginalName() + "」已上传完成。";
            emailNotifyService.sendMail(user.getEmail(), subject, content);
        }
    }
}
