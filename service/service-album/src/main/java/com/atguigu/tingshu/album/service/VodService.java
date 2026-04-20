package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    /**
     * 将音视频文件上传到腾讯云点播平台
     * @param file 文件
     * @return {"mediaFileId":"5145403723983411008", "mediaUrl":"http://1255727855.vod-qcloud.com/9cbe3378vodsh1255727855/a41826195145403723983411008/SfS3kjP0PEQA.mp3"}
     */
    Map<String, String> uploadTrack(MultipartFile file);

    /**
     * 从云点播平台获取音频文件详情
     * @param mediaFileId 唯一标识
     * @return
     */
    TrackMediaInfoVo getMediaInfo(String mediaFileId);

    /**
     * 从点播平台删除音频文件
     * @param oldMediaFileId
     */
    void deleteMedia(String oldMediaFileId);
}
