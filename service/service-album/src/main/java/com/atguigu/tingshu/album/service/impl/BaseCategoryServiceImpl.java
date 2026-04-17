package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.BaseCategory1Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory2Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory3Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    /**
     * 查询所有1,2,3级分类列表
     *
     * @return
     */
    @Override
    public List<JSONObject> getBaseCategoryList() {
        //1.查询分类视图所有记录 共计401条记录
        List<BaseCategoryView> allCategoryViewList = baseCategoryViewMapper.selectList(null);
        //2.处理1级分类
        //2.1 创建1级分类集合
        ArrayList<JSONObject> jsonList1 = new ArrayList<>();

        //2.2 对所有分类按1级分类ID进行分组 采用stream流进行分组 的Map中Key=1级分类ID Value=当前1级分类下的所有子分类
        Map<Long, List<BaseCategoryView>> map1 = allCategoryViewList
                .stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));

        //2.3 遍历1级分类后Map
		for (Map.Entry<Long, List<BaseCategoryView>> entry1 : map1.entrySet()) {
			//2.3.1 构建1级分类JSON对象 封装 id跟名称、子分类
			JSONObject jsonObject1 = new JSONObject();
			jsonObject1.put("categoryId", entry1.getKey());
			jsonObject1.put("categoryName", entry1.getValue().get(0).getCategory1Name());

			//3.处理2级分类
			//3.1 创建存放2级分类集合
			ArrayList<JSONObject> jsonObject2List = new ArrayList<>();
			//3.2 遍历"2级分类"列表 ，按照2级分类ID进行分组
			Map<Long, List<BaseCategoryView>> map2 = entry1.getValue()
					.stream()
					.collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
				//3.3 创建2级分类JSON对象
				JSONObject jsonObject2 = new JSONObject();
				jsonObject2.put("categoryId", entry2.getKey());
				jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
				//3.4 将2级分类对象加入到2级分类集合中
				jsonObject2List.add(jsonObject2);
				//4. 处理3级分类
				//4.1 创建存放3级分类集合
				ArrayList<JSONObject> jsonObject3List = new ArrayList<>();
				//4.2 遍历“2级分类列表”
				for (BaseCategoryView baseCategoryView : entry2.getValue()) {
					//4.3 封装3级分类对象
					JSONObject jsonObject3 = new JSONObject();
					jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
					jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
					//4.4 将3级分类对象加入到3级分类集合中
					jsonObject3List.add(jsonObject3);
				}
				//4.5 将3级分类列表加入到2级分类对象中"categoryChild"中
				jsonObject2.put("categoryChild", jsonObject3List);
			}
			//3.5 将2级分类集合加入到1级分类对象中"categoryChild"中
			jsonObject1.put("categoryChild", jsonObject2List);
			//将1级分类对象加入到1级分类集合中
			jsonList1.add(jsonObject1);
		}
        //5.响应所有1级分类列表
        return jsonList1;
    }
}
