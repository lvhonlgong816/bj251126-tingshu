package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodUploadClient vodUploadClient;

    /**
     * 将音视频文件上传到腾讯云点播平台
     *
     * @param file 文件
     * @return {"mediaFileId":"5145403723983411008", "mediaUrl":"http://1255727855.vod-qcloud.com/9cbe3378vodsh1255727855/a41826195145403723983411008/SfS3kjP0PEQA.mp3"}
     */
    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        try {
            //1.将文件保存指定临时目录下
            String localFilePath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            //2.文件上传
            //2.1 构造上传请求对象
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(localFilePath);

            //2.2 调用上传
            VodUploadResponse response = vodUploadClient.upload(vodConstantProperties.getRegion(), request);

            //3.得到文件唯一标识、文件在线地址
            if (response != null) {
                String fileId = response.getFileId();
                String mediaUrl = response.getMediaUrl();
                return Map.of("mediaFileId", fileId, "mediaUrl", mediaUrl);
            }
        } catch (Exception e) {
            log.error("文件上传点播平台异常", e);
            throw new GuiguException(500, "文件上传点播平台异常：" + e.getMessage());
        }
        return null;
    }

    @Autowired
    private VodClient vodClient;

    /**
     * 从云点播平台获取音频文件详情
     *
     * @param mediaFileId 唯一标识
     * @return
     */
    @Override
    public TrackMediaInfoVo getMediaInfo(String mediaFileId) {
        try {
            //1.实例化一个请求对象,每个接口都会对应一个request对象
            DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
            req.setFileIds(new String[]{mediaFileId});
            //2.调用获取音频文件详情接口
            DescribeMediaInfosResponse resp = vodClient.DescribeMediaInfos(req);
            //3.解析结果
            if (resp != null) {
                //3.1 媒体文件信息列表
                MediaInfo[] mediaInfoSet = resp.getMediaInfoSet();
                //3.2 获取媒体文件信息
                MediaInfo mediaInfo = mediaInfoSet[0];
                //3.2.1 获取基础信息 包括视频名称、分类、播放地址、封面图片等。
                MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                //3.2.2 获取元信息：包括大小、时长、视频流信息、音频流信息等。
                MediaMetaData metaData = mediaInfo.getMetaData();
                //3.3 封装vo结果
                TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
                trackMediaInfoVo.setSize(metaData.getSize());
                trackMediaInfoVo.setDuration(metaData.getDuration());
                trackMediaInfoVo.setType(basicInfo.getType());
                return trackMediaInfoVo;
            }
            return null;
        } catch (TencentCloudSDKException e) {
            log.error("从腾讯点播平台获取音频文件异常：", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 从点播平台删除音频文件
     *
     * @param mediaFileId
     */
    @Override
    public void deleteMedia(String mediaFileId) {
        try {
            DeleteMediaRequest req = new DeleteMediaRequest();
            req.setFileId(mediaFileId);
            // 返回的resp是一个DeleteMediaResponse的实例，与请求对象对应
            vodClient.DeleteMedia(req);
        } catch (TencentCloudSDKException e) {
            log.error("从点播平台删除音频文件异常", e);
        }
    }
}
