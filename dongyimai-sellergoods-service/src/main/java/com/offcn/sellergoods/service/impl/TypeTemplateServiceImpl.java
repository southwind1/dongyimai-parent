package com.offcn.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.entity.PageResult;
import com.offcn.mapper.TbSpecificationOptionMapper;
import com.offcn.mapper.TbTypeTemplateMapper;
import com.offcn.pojo.*;
import com.offcn.pojo.TbTypeTemplateExample.Criteria;
import com.offcn.sellergoods.service.TypeTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class TypeTemplateServiceImpl implements TypeTemplateService {
	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private TbTypeTemplateMapper typeTemplateMapper;
	@Autowired
	private TbSpecificationOptionMapper tbSpecificationOptionMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbTypeTemplate> findAll() {
		return typeTemplateMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbTypeTemplate> page=   (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbTypeTemplate typeTemplate) {
		typeTemplateMapper.insert(typeTemplate);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbTypeTemplate typeTemplate){
		typeTemplateMapper.updateByPrimaryKey(typeTemplate);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbTypeTemplate findOne(Long id){
		return typeTemplateMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			typeTemplateMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbTypeTemplate typeTemplate, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbTypeTemplateExample example=new TbTypeTemplateExample();
		Criteria criteria = example.createCriteria();
		
		if(typeTemplate!=null){			
						if(typeTemplate.getName()!=null && typeTemplate.getName().length()>0){
				criteria.andNameLike("%"+typeTemplate.getName()+"%");
			}			if(typeTemplate.getSpecIds()!=null && typeTemplate.getSpecIds().length()>0){
				criteria.andSpecIdsLike("%"+typeTemplate.getSpecIds()+"%");
			}			if(typeTemplate.getBrandIds()!=null && typeTemplate.getBrandIds().length()>0){
				criteria.andBrandIdsLike("%"+typeTemplate.getBrandIds()+"%");
			}			if(typeTemplate.getCustomAttributeItems()!=null && typeTemplate.getCustomAttributeItems().length()>0){
				criteria.andCustomAttributeItemsLike("%"+typeTemplate.getCustomAttributeItems()+"%");
			}	
		}
		
		Page<TbTypeTemplate> page= (Page<TbTypeTemplate>)typeTemplateMapper.selectByExample(example);
			//存入数据到缓存
			saveToRedis();
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public List<Map> selectOptionList() {
		List<Map> maps = typeTemplateMapper.selectOptionList();
		return maps;
	}
	/**
	 * 返回指定模板id的规格列表
	 * @return
	 */
	@Override
	public List<Map> findSpecList(Long id) {
//		根据id首先获取模板对象
		TbTypeTemplate typeTemplate  = typeTemplateMapper.selectByPrimaryKey(id);
//		因为从数据库中获取的是json字符串，所以我们要将其转换为对象，利用JSON,需要引入相应的jar包
		List<Map> list = JSON.parseArray(typeTemplate.getSpecIds(), Map.class);
//		便利规格集合
		if (list != null){
			for (Map map : list){
				Long specid = new Long((Integer) map.get("id"));
//				利用map中的规格id获取规格选项
				TbSpecificationOptionExample tbSpecificationOptionExample = new TbSpecificationOptionExample();
				TbSpecificationOptionExample.Criteria criteria = tbSpecificationOptionExample.createCriteria();
				criteria.andSpecIdEqualTo(specid);
				List<TbSpecificationOption> tbSpecificationOptions = tbSpecificationOptionMapper.selectByExample(tbSpecificationOptionExample);
//				添加到map
				map.put("options",tbSpecificationOptions);
			}
		}
		return list;
	}


	/**
	 * 将数据存入缓存
	 */
	private void saveToRedis(){
//		获取模板信息
		List<TbTypeTemplate> typeTemplateList = findAll();
		//循环模板
		for (TbTypeTemplate tbTypeTemplate : typeTemplateList) {
//			存储品牌列表
			List<Map> brandList  = JSON.parseArray(tbTypeTemplate.getBrandIds(), Map.class);
//			根据模板ID查询品牌列表
			redisTemplate.boundHashOps("brandList").put(tbTypeTemplate.getId(),brandList);
//			存储规格列表
			List<Map> specList = findSpecList(tbTypeTemplate.getId());
			redisTemplate.boundHashOps("specList").put(tbTypeTemplate.getId(), specList);

		}
	}


}
