package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 查询所有1,2,3级分类列表
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据1级分类ID查询标签列表（包含标签值列表）
     * @param category1Id 1级分类ID
     * @return 标签列表
     */
    List<BaseAttribute> findAttribute(Long category1Id);

    /**
     * 根据3级分类ID查询分类视图对象
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);
}
