package io.novafs.system.sse;

import cn.dev33.satoken.stp.StpUtil;
import io.novafs.framework.sse.SseConnectionManager;
import io.novafs.system.user.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 连接控制器（需登录）
 */
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseConnectionManager connectionManager;
    private final SysUserService sysUserService;

    /**
     * 建立 SSE 长连接，返回事件流
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        Long userId = sysUserService.findByUsername(StpUtil.getLoginIdAsString()).getId();
        SseEmitter emitter = connectionManager.connect(userId);
        connectionManager.sendLocal(userId, "connected", "{\"userId\":" + userId + "}");
        return emitter;
    }
}
