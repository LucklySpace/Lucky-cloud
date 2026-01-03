package com.xy.lucky.logging.controller;

import com.xy.lucky.general.response.ResponseNotIntercept;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ResponseNotIntercept
@Schema(description = "页面控制器")
public class PageController {

    @Schema(description = "日志首页")
    @GetMapping("/logs/ui")
    public String index() {
        return "index";
    }

    @Schema(description = "错误页面")
    @GetMapping("/error")
    public String error() {
        return "error";
    }

//    @GetMapping("/{filename}")
//    public ResponseEntity<Resource> getResource(@PathVariable String filename){
//        Resource resource = new ClassPathResource("static/" + filename);
//        return ResponseEntity.ok(resource);
//    }
}
