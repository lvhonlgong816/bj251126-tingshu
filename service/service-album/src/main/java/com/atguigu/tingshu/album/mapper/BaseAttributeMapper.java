package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.BaseAttribute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttributeMapper extends BaseMapper<BaseAttribute> {


    /**
     * 根据1级分类ID查询标签列表（包含标签值列表）
     * @param category1Id 1级分类ID
     * @return 标签列表
     */
    List<BaseAttribute> findAttribute(@Param("category1Id") Long category1Id);
}
