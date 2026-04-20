package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

/**
 * @author: atguigu
 * @create: 2026-04-18 10:09
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Autowired
    private AuditService auditService;

    /**
     * 将上传文件保存到MINIO服务器
     *
     * @param file 文件
     * @return 地址
     */
    @Override
    public String upload(MultipartFile file) {
        //1. 验证图片内容格式是否合法 保证是图片：png jpg jpeg常规格式 以及大小是否合法
        //ImageIO是Java的标准图像输入输出工具类，用于读取、写入和处理各种格式的图像文件
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            if (null == bufferedImage) {
                throw new GuiguException(500, "请上传常规图片格式,要求：png jpg jpeg");
            }
            int height = bufferedImage.getHeight();
            int width = bufferedImage.getWidth();
            if (height > 900 || width > 900) {
                throw new GuiguException(500, "超过图片大小限制：900*900");
            }
            //2. 验证图片内容是否违规
            String suggest = auditService.auditImage(Base64.encode(file.getInputStream()));
            if ("block".equals(suggest) || "review".equals(suggest)) {
                throw new GuiguException(500, "图片非法");
            }
        } catch (IOException e) {
            throw new GuiguException(500, "请上传常规图片格式,要求：png jpg jpeg");
        }

        //3. 将图片上传到MINIO服务器，返回地址
        //3.1 确定文件唯一名称 日期/文件唯一标识.文件后缀
        String folder = DateUtil.today();
        String fileName = IdUtil.fastSimpleUUID();
        String objectName = folder + "/" + fileName + "." + FileNameUtil.extName(file.getOriginalFilename());
        //3.2 调用minioclient上传文件
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConstantProperties.getBucketName())
                            .object(objectName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException(e);
        }
        //3.3 返回图片的访问路径 http://192.168.1.1:9000/bucketName/objectName
        return minioConstantProperties.getEndpointUrl() + "/" + minioConstantProperties.getBucketName() + "/" + objectName;
    }
}
