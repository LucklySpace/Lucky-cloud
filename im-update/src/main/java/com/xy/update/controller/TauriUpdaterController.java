package com.xy.update.controller;

import com.xy.update.config.TauriUpdaterProperties;
import com.xy.update.domain.tauri.TauriUpdaterResponse;
import jakarta.annotation.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 检查版本 下载
 */
@RestController
@RequestMapping("/api/{version}/tauri/update")
public class TauriUpdaterController {

    @Resource
    private TauriUpdaterProperties updater;

    private static final String FILE_DIRECTORY = "D:\\Project\\im-client\\src-tauri\\target\\debug\\bundle\\msi\\";

    @GetMapping("/latest")
    public TauriUpdaterResponse latest(@RequestHeader(value = "platform", required = false, defaultValue = "windows-x86_64") String platform) {
        return new TauriUpdaterResponse(
                updater.version(),
                updater.notes(),
                updater.pubDate(),
                updater.getPlatformInfo(platform)
        );
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile( @PathVariable("fileName") String fileName) {
        Path filePath = Paths.get(FILE_DIRECTORY).resolve(fileName).normalize();
        try {
            org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }


}
