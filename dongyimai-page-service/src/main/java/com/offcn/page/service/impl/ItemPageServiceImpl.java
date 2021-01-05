package com.offcn.page.service.impl;


import com.offcn.mapper.TbGoodsDescMapper;
import com.offcn.mapper.TbGoodsMapper;
import com.offcn.mapper.TbItemCatMapper;
import com.offcn.mapper.TbItemMapper;
import com.offcn.page.service.ItemPageService;
import com.offcn.pojo.TbGoods;
import com.offcn.pojo.TbGoodsDesc;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbItemExample;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.io.File;
import java.io.FileWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ItemPageServiceImpl implements ItemPageService {

//生成文件存放目录
    @Value("${pagedir}")
    private String pagedir;
//    注入freemarker配置对象
    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;
    @Autowired
    private TbGoodsDescMapper goodsDescMapper;
    @Autowired
    private TbGoodsMapper goodsMapper;
    @Autowired
    private TbItemCatMapper itemCatMapper;

    @Autowired
    private TbItemMapper itemMapper;
    @Override
    public boolean genItemHtml(Long goodsId) {
        try {
            Configuration configuration = freeMarkerConfigurer.getConfiguration();
            //        根据goodsId查询商品数据，goods 和 goodsDesc
            TbGoods goods = goodsMapper.selectByPrimaryKey(goodsId);
            TbGoodsDesc goodsDesc = goodsDescMapper.selectByPrimaryKey(goodsId);
/**
 * 生成面包屑导航条
 */
//            按照分类ID查找分类名称
            String itemCat1  = itemCatMapper.selectByPrimaryKey(goods.getCategory1Id()).getName();
            String itemCat2  = itemCatMapper.selectByPrimaryKey(goods.getCategory2Id()).getName();
            String itemCat3  = itemCatMapper.selectByPrimaryKey(goods.getCategory3Id()).getName();
            /**
             * 查找商品的sku信息，状态审核通过
             */
            TbItemExample example = new TbItemExample();
            TbItemExample.Criteria criteria = example.createCriteria();
            criteria.andStatusEqualTo("1");
//            指定id
            criteria.andGoodsIdEqualTo(goodsId);
//            按照状态降序，保证第一个为默认
            example.setOrderByClause("is_default desc");
            List<TbItem> itemList = itemMapper.selectByExample(example);
            //        将数据存到Map中
            Map map = new HashMap();
            map.put("goods",goods);
            map.put("goodsDesc",goodsDesc);
            map.put("itemCat1",itemCat1);
            map.put("itemCat2",itemCat2);
            map.put("itemCat3",itemCat3);
            map.put("itemList",itemList);
//        获得一个freeMarker模板
            Template template = configuration.getTemplate("item.ftl");
//        执行process（out，map）map
            File file = new File(pagedir);
            if (!file.exists()){//如果文件夹不存在
                file.mkdir();//创建文件夹
            }
            FileWriter out = new FileWriter(new File(pagedir + goodsId +".html"));
            template.process(map,out);
//            关闭流
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

         return false;
    }

    /**
     * 删除商品详情页
     * @param goodsIds
     * @return
     */
    @Override
    public boolean deleteItemHtml(Long[] goodsIds) {
        try {
            for (Long goodsId : goodsIds) {
                new File(pagedir + goodsId + ".html").delete();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }
}
