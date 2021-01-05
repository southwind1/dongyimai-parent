package com.offcn.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ItemSearchServiceImpl implements ItemSearchService {
//  定义solrmoban
    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> search(Map searchMap) {
//        创建一个集合保存查询出的数据
        Map<String,Object> map = new HashMap<String, Object>();
//        //        创建一个分页查询对象,初始化查询条件
//        SimpleQuery simpleQuery = new SimpleQuery();
////        创建一个条件对象,并初始化
//        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
////        将条件添加到查询对象中
//        simpleQuery.addCriteria(criteria);
//        ScoredPage<TbItem> page = solrTemplate.queryForPage(simpleQuery, TbItem.class);
////        得到数据
//        map.put("rows", page.getContent());
        //1.按关键字查询（高亮显示）
        map.putAll(searchList(searchMap));
        //2.根据关键字查询商品分类
        List categoryList = searchCategoryList(searchMap);
        map.put("categoryList",categoryList);
//        3.查询品牌和规格列表
        //动态读取分类名称，只有在前端选着分类categoryName才有值
        String categoryName=(String) searchMap.get("category");
        if (!"".equals(categoryName)){
            //按照分类名称重新读取对应品牌、规格
            map.putAll(searchBrandAndSpecList(categoryName));
        } else if (categoryList.size()>0){
//            默认查询第一个
            map.putAll(searchBrandAndSpecList((String)categoryList.get(0)));
        }
        return map;
    }

    @Override
    public void importList(List<TbItem> list) {
        for(TbItem item:list){
            System.out.println(item.getTitle());
            Map<String,Object> specMap = JSON.parseObject(item.getSpec(),Map.class);//从数据库中提取规格json字符串转换为map
            Map map = new HashMap();


            for(String key : specMap.keySet()) {
                map.put("item_spec_"+Pinyin.toPinyin(key, "").toLowerCase(), specMap.get(key));
            }

            item.setSpecMap(map);	//给带动态域注解的字段赋值

        }
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }
        /**
     * 从solr删除被后台删除的商品
     * @param goodsIdList
     */
    @Override
    public void deleteByGoodsIds(List goodsIdList) {
        System.out.println("删除商品ID"+goodsIdList);
        Query query=new SimpleQuery();
        Criteria criteria=new Criteria("item_goodsid").in(goodsIdList);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }



    /**
     * 对查询的关键词进行高亮显示
     * @param searchMap
     * @return
     */

    private Map<String, Object> searchList(Map searchMap) {
//        创建一个集合保存查询出的数据
        Map<String,Object> map = new HashMap<String, Object>();
//        创建一个高亮显示的查询对象，根据关键字高亮查询
        SimpleHighlightQuery simpleHighlightQuery = new SimpleHighlightQuery();
//        设置需要高亮显示的字段
        HighlightOptions highlightOptions = new HighlightOptions();
        highlightOptions.addField("item_title");
//        设置高亮显示前缀
        highlightOptions.setSimplePrefix("<em style='color:red'>");
//        设置高亮显示后缀
        highlightOptions.setSimplePostfix("</em>");
//        添加高亮选项到查询对象中国
        simpleHighlightQuery.setHighlightOptions(highlightOptions);

        /**
         * 关键字空格处理
         */
        if (!"".equals(searchMap.get("keywords"))){
            if (searchMap.get("keywords").toString().indexOf(" ") >= 0){
                String keywords = searchMap.get("keywords").toString().replaceAll(" ","");
                searchMap.put("keywords",keywords);
            }
        }



//        设置查询条件
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        simpleHighlightQuery.addCriteria(criteria);
//        更具商品分类来查询商品信息
        if (!"".equals(searchMap.get("category"))){
            //        设置查询条件
            Criteria categoryCriteria = new Criteria("item_category").is(searchMap.get("category"));
//            创建一个过滤查询
            SimpleFilterQuery simpleFilterQuery = new SimpleFilterQuery(categoryCriteria);
            simpleHighlightQuery.addFilterQuery(simpleFilterQuery);
        }

//      根剧品牌查询商品信息
        if (!"".equals(searchMap.get("brand"))){
            //        设置查询条件
            Criteria brandCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
//            创建一个过滤查询
            SimpleFilterQuery simpleFilterQuery = new SimpleFilterQuery(brandCriteria);
            simpleHighlightQuery.addFilterQuery(simpleFilterQuery);
        }
//        根据规格查询商品信息
        if (searchMap.get("spec") != null){
            Map<String,String> spec = (Map) searchMap.get("spec");
            for (String key : spec.keySet()) {

                //        设置查询条件,将前端创来的key转换为拼音和item_spec拼接，彩盒solr中的相同
                Criteria brandCriteria = new Criteria("item_spec" + Pinyin.toPinyin(key,"").toLowerCase()).is(spec.get("key"));
//            创建一个过滤查询
                SimpleFilterQuery simpleFilterQuery = new SimpleFilterQuery(brandCriteria);
                simpleHighlightQuery.addFilterQuery(simpleFilterQuery);
            }
            
        }

//       按价格进行筛选

//        判断价格是否为空
        if (!"".equals(searchMap.get("price"))){
//       500-1000以-拆分字符串
            String[] prices = searchMap.get("price").toString().split("-");
            //如果区间起点不等于0
            if (!prices[0].equals("0")){
//                创建过滤条件
                Criteria greaterThanEqual = new Criteria("item_price").greaterThanEqual(prices[0]);
                SimpleQuery simpleQuery = new SimpleQuery(greaterThanEqual);
                simpleHighlightQuery.addFilterQuery(simpleQuery);
            }
            //如果区间终点不等于*
            if (!prices[1].equals("*")){
//                创建过滤条件
                Criteria lessThanEqual = new Criteria("item_price").lessThanEqual(prices[1]);
                SimpleQuery simpleQuery = new SimpleQuery(lessThanEqual);
                simpleHighlightQuery.addFilterQuery(simpleQuery);
            }
        }
        /*
            进行分页查询
         */
//        提取页码
        Integer pageNo = (Integer)searchMap.get("pageNo");
        if (pageNo == null){
//            默认
            pageNo = 0;
        }
//        提取每页条数
        Integer pageSize = (Integer) searchMap.get("pageSize");
        if (pageSize == null){
//            默认20
            pageSize = 20;
        }
//        定义从第几条开始查
        simpleHighlightQuery.setOffset((pageNo - 1) * pageSize);
        simpleHighlightQuery.setRows(pageSize);
        /**
         * 排序
         */
//        取出排序的规则
        String sortValue = searchMap.get("sort").toString();
//        取出按照那个域排序
        String sortField = searchMap.get("sortField").toString();
        if (sortValue!=null && !sortValue.equals("")){
            if(sortValue.equals("ASC")){
                Sort sort=new Sort(Sort.Direction.ASC, "item_"+sortField);
                simpleHighlightQuery.addSort(sort);
            }
            if(sortValue.equals("DESC")){
                Sort sort=new Sort(Sort.Direction.DESC, "item_"+sortField);
                simpleHighlightQuery.addSort(sort);
            }
        }

//        发出带高亮请求的查询请求
        HighlightPage<TbItem> tbItems = solrTemplate.queryForHighlightPage(simpleHighlightQuery, TbItem.class);
//        获取高亮集合
        List<HighlightEntry<TbItem>> highlighted = tbItems.getHighlighted();
//         遍历高亮集合
        for (HighlightEntry<TbItem> tbItemHighlightEntry : highlighted) {
//            获取基本数据集合
            TbItem item = tbItemHighlightEntry.getEntity();
//            判断高亮部分是否为空
            if (tbItemHighlightEntry.getHighlights().size() > 0&& tbItemHighlightEntry.getHighlights().get(0).getSnipplets().size() > 0){
//                取出组合后的item_title
                List<HighlightEntry.Highlight> highlights = tbItemHighlightEntry.getHighlights();
                List<String> snipplets = highlights.get(0).getSnipplets();
                //获取第一个高亮字段对应的高亮结果，设置到商品标题
                item.setTitle(snipplets.get(0));
            }
        }
        //把带高亮数据集合存放map
        map.put("rows",tbItems.getContent());
//        分页查询
//        返回总页数
        map.put("totalPages",tbItems.getTotalPages());
//        返回总条数
        map.put("total",tbItems.getTotalElements());
        return map;
    }

    /**
     * 查询分类列表
     * @param searchMap
     * @return
     */
    private  List searchCategoryList(Map searchMap){
//        创建一个容纳分类列表的容器
        List<String> list = new ArrayList<String>();
        Query query = new SimpleQuery();
//        创建一个条件对象,并初始化
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
//        将条件添加到查询对象中
        query.addCriteria(criteria);
//        设置分组选项
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOptions);
//        得到分组页
        GroupPage<TbItem> groupPage = solrTemplate.queryForGroupPage(query, TbItem.class);
//        根据分组域得到分组结果集
        GroupResult<TbItem> groupResult = groupPage.getGroupResult("item_category");
//        得到分组结果入口
        Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
//        得到分组入口集合
        List<GroupEntry<TbItem>> content = groupEntries.getContent();
//        遍历基础数据TbItem集合
        for (GroupEntry<TbItem> tbItemGroupEntry : content) {
//            将分组组名存入list
            list.add(tbItemGroupEntry.getGroupValue());
        }
        return list;

    }

    /**
     * 查询品牌和规格列表
     * @param category 分类名称
     * @return
     */
    private Map searchBrandAndSpecList(String category){
        Map map = new HashMap();
//        获取模板id
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        if(typeId!=null) {
            //根据模板ID查询品牌列表
            List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
            map.put("brandList", brandList);//返回值添加品牌列表
            //根据模板ID查询规格列表
            List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
            map.put("specList", specList);

        }
        return map;
    }

}
