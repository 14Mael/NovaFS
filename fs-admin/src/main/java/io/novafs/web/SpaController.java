package io.novafs.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 路由回退控制器
 * <p>Vue Router 使用 history 模式,前端路由路径(如 /chat、/library)由前端处理,
 * 后端将这些路径全部转发到 index.html,避免刷新时 404 / 500。</p>
 * <p>注意: /api/** 由后端接口处理;静态资源 /assets/** 由 Spring ResourceHandler 直接提供。</p>
 */
@Controller
public class SpaController {

    /** NovaFS 前端路由列表 */
    @GetMapping(value = {"/login", "/chat", "/library", "/files", "/recycle", "/share/*"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}