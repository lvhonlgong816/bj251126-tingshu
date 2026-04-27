package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author: atguigu
 * @create: 2026-04-27 09:28
 */
public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex, String> {


}
