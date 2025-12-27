package com.xy.lucky.server.service;

import com.xy.lucky.domain.vo.FileVo;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.io.File;

public interface FileService {

    Mono<FileVo> uploadFile(FilePart file);

    Mono<FileVo> uploadFile(File file);
}
