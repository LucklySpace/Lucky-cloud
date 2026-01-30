package com.xy.lucky.server.service;

import com.xy.lucky.server.domain.vo.FileVo;
import org.springframework.http.codec.multipart.FilePart;

import java.io.File;

public interface FileService {

    FileVo uploadFile(FilePart file);

    FileVo uploadFile(File file);
}
