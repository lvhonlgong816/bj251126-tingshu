package com.atguigu.tingshu.search.service;

public interface SearchService {

    /**
     * 手动上架指定专辑到索引库
     * @param albumId
     * @return
     */
    void upperAlbum(Long albumId);


    /**
     * 手动从索引库下架指定专辑
     * @param albumId
     * @return
     */
    void lowerAlbum(Long albumId);
}
