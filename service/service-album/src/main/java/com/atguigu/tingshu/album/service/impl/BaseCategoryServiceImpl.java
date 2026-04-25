package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

    /**
     * 根据1级分类ID查询标签列表（包含标签值列表）
     *
     * @param category1Id 1级分类ID
     * @return 标签列表
     */
    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        //方式一：根据1级分类ID查询标签列表、遍历标签列表，根据标签ID查询标签值列表 问题：SQL执行次数过多
        //方式二：调用持久层执行动态SQL，进行两张表关联查询
        return baseAttributeMapper.findAttribute(category1Id);
    }

    /**
     * 根据3级分类ID查询分类视图对象
     *
     * @param category3Id
     * @return
     */
    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    /**
     * 根据1级分类ID查询置顶前七个三级分类列表
     *
     * @param category1Id 1级分类ID
     * @return 三级分类列表
     */
    @Override
    public List<BaseCategory3> findTop7BaseCategory3(Long category1Id) {
        //1.根据1级分类ID查询二级分类ID列表
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(
                new LambdaQueryWrapper<BaseCategory2>()
                        .eq(BaseCategory2::getCategory1Id, category1Id)
                        .select(BaseCategory2::getId)
        );
        //2.根据二级分类ID列表+置顶标识+排序+数量限制 查询三级分类列表
        if (CollUtil.isNotEmpty(baseCategory2List)) {
            List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
            LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(BaseCategory3::getCategory2Id, category2IdList);
            queryWrapper.eq(BaseCategory3::getIsTop, 1);
            queryWrapper.orderByAsc(BaseCategory3::getOrderNum);
            queryWrapper.last("limit 7");
            List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(queryWrapper);
            return baseCategory3List;
        }
        return null;
    }

    /**
     * 查询1级分类下包含所有2,3级分类列表
     *
     * @param category1Id
     * @return 1级分类对象 包含2,3级分类列表
     */
    @Override
    public JSONObject getBaseCategoryListByCategory1Id(Long category1Id) {
        //1.处理1级分类JSON对象
        //1.1 创建1级分类JSON对象
        JSONObject jsonObject1 = new JSONObject();
        //1.2 根据1级分类ID查询分类视图得到"1级"分类列表
        List<BaseCategoryView> category1ViewList = baseCategoryViewMapper.selectList(
                new LambdaQueryWrapper<BaseCategoryView>()
                        .eq(BaseCategoryView::getCategory1Id, category1Id)
        );

        //1.3. 封装1级JSON对象 1级分类ID跟名称
        jsonObject1.put("categoryId", category1ViewList.get(0).getCategory1Id());
        jsonObject1.put("categoryName", category1ViewList.get(0).getCategory1Name());

        //2.处理2级分类
        //2.1 创建2级分类JSON集合
        ArrayList<JSONObject> jsonObject2List = new ArrayList<>();
        //2.2 对"1级"分类列表按照2级分类ID进行分组 得到 二级分类Map key="二级分类ID" value="'2级'分类列表"
        Map<Long, List<BaseCategoryView>> map2 = category1ViewList
                .stream()
                .collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
        //2.3 遍历Map 封装二级分类对象
        for (Map.Entry<Long, List<BaseCategoryView>> entry2 : map2.entrySet()) {
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("categoryId", entry2.getKey());
            jsonObject2.put("categoryName", entry2.getValue().get(0).getCategory2Name());
            jsonObject2List.add(jsonObject2);
            //3.处理3级分类
            //3.1 创建3级分类JSON集合
            ArrayList<JSONObject> jsonObject3List = new ArrayList<>();
            //3.2 遍历"2级分类列表"
            for (BaseCategoryView baseCategoryView : entry2.getValue()) {
                //3.3 封装三级分类JSON对象
                JSONObject jsonObject3 = new JSONObject();
                jsonObject3.put("categoryId", baseCategoryView.getCategory3Id());
                jsonObject3.put("categoryName", baseCategoryView.getCategory3Name());
                //3.4 将3级分类JSON对象加入3级分类集合中
                jsonObject3List.add(jsonObject3);
            }
            //3.5 将3级分类列表加入到2级分类对象"categoryChild"中
            jsonObject2.put("categoryChild", jsonObject3List);
        }
        //2.4 将二级列表加入一级分类对象"categoryChild"中
        jsonObject1.put("categoryChild", jsonObject2List);

        //4.响应1级分类JSON对象
        return jsonObject1;
    }
}
