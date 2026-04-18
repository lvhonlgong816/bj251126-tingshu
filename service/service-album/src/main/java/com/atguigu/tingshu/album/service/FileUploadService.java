package com.atguigu.tingshu.album.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    /**
     * 将上传文件保存到MINIO服务器
     * @param file 文件
     * @return 地址
     */
    String upload(MultipartFile file);
}
