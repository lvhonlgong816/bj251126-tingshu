package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {


    /**
     * 专辑详情页数据汇总
     * @param albumId
     * @return {"announcer":用户对象,"albumInfo":专辑对象,"albumStatVo":专辑统计对象,"baseCategoryView":分类对象}
     */
    Map<String, Object> getItem(Long albumId);
}
