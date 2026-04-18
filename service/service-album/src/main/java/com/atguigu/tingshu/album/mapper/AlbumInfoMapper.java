package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    /**
     * MP中自定义方法第一个参数分页对象，底层自动拼接limit部分
     * 分页条件查询指定用户专辑列表
     * @param pageInfo 分页对象
     * @param albumInfoQuery 查询条件
     * @return 分页对象
     */
    IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, @Param("vo") AlbumInfoQuery albumInfoQuery);
}
