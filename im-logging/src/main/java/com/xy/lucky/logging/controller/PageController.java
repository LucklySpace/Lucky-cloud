package com.xy.lucky.logging.controller;

import com.xy.lucky.logging.exception.ResponseNotIntercept;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ResponseNotIntercept
@Schema(description = "页面控制器")
public class PageController {

    @Schema(description = "首页")
    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @Schema(description = "首页")
    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @Schema(description = "日志首页")
    @GetMapping("/logs/ui")
    public String logs() {
        return "redirect:/home";
    }

    @Schema(description = "错误页面")
    @GetMapping("/error")
    public String error() {
        return "error";
    }
}
