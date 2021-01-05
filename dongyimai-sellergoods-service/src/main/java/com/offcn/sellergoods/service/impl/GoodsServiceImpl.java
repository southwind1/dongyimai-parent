package com.offcn.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.group.Goods;
import com.offcn.mapper.*;
import com.offcn.pojo.*;
import com.offcn.pojo.TbGoodsExample.Criteria;
import com.offcn.sellergoods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 服务实现层
 * @author Administrator
 *
 */
//可以定义在方法上也可以定义在类上，定义在类上就相当于定义在所有的方法上
@Transactional
@Service
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;

	@Autowired
	private TbItemMapper itemMapper;

	@Autowired
	private TbBrandMapper brandMapper;

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Autowired
	private TbSellerMapper sellerMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbGoods> page=   (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		goods.getGoods().setAuditStatus("0");//设置待审核状态
		goodsMapper.insert(goods.getGoods());
		goods.getGoodsDesc().setGoodsId(goods.getGoods().getId());
		goodsDescMapper.insert(goods.getGoodsDesc());
//		int i= 10/0;
//		设置保存sku表
		saveItemList(goods);
	}

//	设置没有启用规格是的item
	public void setItemValues(Goods goods,TbItem item){
		item.setGoodsId(goods.getGoods().getId());//商品SPU编号
		item.setSellerId(goods.getGoods().getSellerId());//商家编号
		item.setCategoryid(goods.getGoods().getCategory3Id());//商品分类编号（3级）
		item.setCreateTime(new Date());//创建日期
		item.setUpdateTime(new Date());//修改日期
//			品牌名称
		TbBrand tbBrand = brandMapper.selectByPrimaryKey(goods.getGoods().getBrandId());
		item.setBrand(tbBrand.getName());
//			分类名称、
		TbItemCat tbItemCat = itemCatMapper.selectByPrimaryKey(goods.getGoods().getCategory3Id());
		item.setCategory(tbItemCat.getName());
//			商家名称
		TbSeller tbSeller = sellerMapper.selectByPrimaryKey(goods.getGoods().getSellerId());
		item.setSeller(tbSeller.getNickName());
		//图片地址（取spu的第一个图片）
		List<Map> imgList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
//			判断是否有图片
		if (imgList.size() > 0){
			item.setImage((String) imgList.get(0).get("url"));
		}
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
		//设置待审核状态,修改过的商品需要重新审核
		goods.getGoods().setAuditStatus("0");
		goodsMapper.updateByPrimaryKey(goods.getGoods());
		goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());
//		删除之前的sku
		TbItemExample tbItemExample = new TbItemExample();
		TbItemExample.Criteria criteria = tbItemExample.createCriteria();
		criteria.andGoodsIdEqualTo(goods.getGoods().getId());
		itemMapper.deleteByExample(tbItemExample);
//		//		设置保存sku表
		saveItemList(goods);
	}

	/**
	 * 设置保存sku表
	 * @param goods
	 */
	public void saveItemList(Goods goods){
		//		设置保存sku表
//		判断商家是否启用规格
		if (goods.getGoods().getIsEnableSpec().equals("1")){
			//		设置标题title
			for (TbItem item : goods.getItemList()) {
				String title = goods.getGoods().getGoodsName();
//			循环sku
				Map<String,Object> map = JSON.parseObject(item.getSpec(), Map.class);
				for (String key : map.keySet()) {
//				通过建获取值
					title += " " + map.get(key);
				}
				item.setTitle(title);
				setItemValues(goods,item);
				itemMapper.insert(item);
			}
		}else {
//			当没有启用规格，item没有从页面传入，自定义一个item
			TbItem item = new TbItem();
//			标题
			item.setTitle(goods.getGoods().getGoodsName());
//			状态
			item.setStatus("1");
//			是否默认
			item.setIsDefault("1");
//			库存数量
			item.setNum(9999);
//			规格
			item.setSpec("{}");
//			价格
			item.setPrice(goods.getGoods().getPrice());
			setItemValues(goods,item);
			itemMapper.insert(item);
		}
	}
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id){
		//		创建一个Goods
		Goods goods = new Goods();
//		查找TbGoods
		TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
//		查找TbGoodsDesc
		TbGoodsDesc tbGoodsDesc = goodsDescMapper.selectByPrimaryKey(id);
//		查找itemList
		TbItemExample tbItemExample = new TbItemExample();
		TbItemExample.Criteria criteria = tbItemExample.createCriteria();
		criteria.andGoodsIdEqualTo(id);
		List<TbItem> tbItemList = itemMapper.selectByExample(tbItemExample);
		goods.setGoods(tbGoods);
		goods.setGoodsDesc(tbGoodsDesc);
		goods.setItemList(tbItemList);
		return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
//			我们没有权限删除商品，但是可以修改商品的状态
//			获取商品
			TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
//			设置状态
			tbGoods.setIsDelete("1");
			goodsMapper.updateByPrimaryKey(tbGoods);
		}
		//修改商品sdu状态为禁用
		List<TbItem> listitem = findItemListByGoodsIdandStatus(ids,"1");
		for (TbItem tbItem : listitem) {
			tbItem.setStatus("0");
			itemMapper.updateByPrimaryKey(tbItem);
		}
	}
	
	
		@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbGoodsExample example=new TbGoodsExample();
		Criteria criteria = example.createCriteria();

		if(goods!=null){			
						if(goods.getSellerId()!=null && goods.getSellerId().length()>0){
//				criteria.andSellerIdLike("%"+goods.getSellerId()+"%");
							criteria.andSellerIdEqualTo(goods.getSellerId());
			}			if(goods.getGoodsName()!=null && goods.getGoodsName().length()>0){
				criteria.andGoodsNameLike("%"+goods.getGoodsName()+"%");
			}			if(goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0){
				criteria.andAuditStatusLike("%"+goods.getAuditStatus()+"%");
			}			if(goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0){
				criteria.andIsMarketableLike("%"+goods.getIsMarketable()+"%");
			}			if(goods.getCaption()!=null && goods.getCaption().length()>0){
				criteria.andCaptionLike("%"+goods.getCaption()+"%");
			}			if(goods.getSmallPic()!=null && goods.getSmallPic().length()>0){
				criteria.andSmallPicLike("%"+goods.getSmallPic()+"%");
			}			if(goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0){
				criteria.andIsEnableSpecLike("%"+goods.getIsEnableSpec()+"%");
			}

			//		查的时候排除已近删除的商品
			if (goods.getSellerId() == null){
				criteria.andIsDeleteIsNull();
			}else if(goods.getIsDelete()!=null && goods.getIsDelete().length()>0){
				criteria.andIsDeleteLike("%"+goods.getIsDelete()+"%");
			}
		}
		
		Page<TbGoods> page= (Page<TbGoods>)goodsMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void updateStatus(Long[] ids, String status) {
//		循环ids
		for (Long id : ids) {
//		根据id获取商品信息
			TbGoods tbGoods = goodsMapper.selectByPrimaryKey(id);
//			设置商品状态
			tbGoods.setAuditStatus(status);
//			更新商品信息
			goodsMapper.updateByPrimaryKey(tbGoods);
//			修改sku的状态
//			获取sku
			TbItemExample itemExample = new TbItemExample();
			TbItemExample.Criteria criteria = itemExample.createCriteria();
			criteria.andGoodsIdEqualTo(id);
			List<TbItem> tbItemList = itemMapper.selectByExample(itemExample);
//			遍历sku集合
			for (TbItem item : tbItemList) {
//				设置sku状态
				item.setStatus("1");
//				更新
				itemMapper.updateByPrimaryKey(item);
			}
		}
	}
	/**
	 * 根据商品ID和状态查询Item表信息
	 * @param goodsIds
	 * @param status
	 * @return
	 */
	@Override
	public List<TbItem> findItemListByGoodsIdandStatus(Long[] goodsIds, String status) {
		TbItemExample example=new TbItemExample();
		com.offcn.pojo.TbItemExample.Criteria criteria = example.createCriteria();
		criteria.andGoodsIdIn(Arrays.asList(goodsIds));
		criteria.andStatusEqualTo(status);
		return itemMapper.selectByExample(example);
	}

}
