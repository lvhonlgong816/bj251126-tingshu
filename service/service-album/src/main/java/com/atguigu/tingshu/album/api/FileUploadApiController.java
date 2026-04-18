package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.FileUploadService;
import com.atguigu.tingshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * <form type="multipart/form-data">
     * <input type="file" name="file">
     * <form></form>
     * 文件上传 要求必须是Post请求 请求参数file，表单格式：multipart/form-data
     *
     * @return
     */
    @Operation(summary = "文件上传（专辑声音封面图片、用户头像等）")
    @PostMapping("/fileUpload")
    public Result<String> fileUpload(@RequestParam("file") MultipartFile file) {
        String fileUrlPath = fileUploadService.upload(file);
        return Result.ok(fileUrlPath);
    }
}
