package com.xy.lucky.chat.service;

import com.xy.lucky.chat.domain.vo.FileVo;
import org.springframework.http.codec.multipart.FilePart;

import java.io.File;

public interface FileService {

    FileVo uploadFile(FilePart file);

    FileVo uploadFile(File file);
}
