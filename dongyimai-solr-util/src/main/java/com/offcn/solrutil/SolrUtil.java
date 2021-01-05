package com.offcn.solrutil;

import com.alibaba.fastjson.JSON;
import com.github.promeg.pinyinhelper.Pinyin;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbItemExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//让配置文件扫描到
@Component
public class SolrUtil {

    @Autowired
    private TbItemMapper itemMapper;
//  导入solr模板
    @Autowired
    private SolrTemplate solrTemplate;

    /**
     * 导入商品数据
     */
    public void importItemData() {
//        查询所有的，状态为1审核通过的商品
//        创建查询条件
        TbItemExample itemExample = new TbItemExample();
        TbItemExample.Criteria criteria = itemExample.createCriteria();
        criteria.andStatusEqualTo("1");
        List<TbItem> tbItemList = itemMapper.selectByExample(itemExample);
        System.out.println("=============商品列表====================");
        for (TbItem item : tbItemList) {
            System.out.println(item.getTitle());
//            读取规格数据，字符串，转换成json对象
            Map<String,String> map = JSON.parseObject(item.getSpec(), Map.class);
            //创建一个新map集合存储拼音
            Map<String,String> mapPinyin=new HashMap<>();
            //遍历map，替换key从汉字变为拼音
            for (String key : map.keySet()) {
                mapPinyin.put(Pinyin.toPinyin(key,"").toLowerCase(),map.get(key));
            }
            item.setSpecMap(mapPinyin);
        }
//        批量保存到索引库
        solrTemplate.saveBeans(tbItemList);
        solrTemplate.commit();
        System.out.println("=============商品列表结束====================");
    }
    //删除全部数据

    public void testDeleteAll(){
        Query query=new SimpleQuery("*:*");
        solrTemplate.delete(query);
        solrTemplate.commit();
    }
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
//        取出组件
        SolrUtil solrUtil = (SolrUtil) context.getBean("solrUtil");
////        执行
        solrUtil.importItemData();
//        删除
//        solrUtil.testDeleteAll();
    }
}
