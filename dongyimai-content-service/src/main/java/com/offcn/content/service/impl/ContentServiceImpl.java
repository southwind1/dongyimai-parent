package com.offcn.content.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.content.service.ContentService;
import com.offcn.entity.PageResult;
import com.offcn.mapper.TbContentMapper;
import com.offcn.pojo.TbContent;
import com.offcn.pojo.TbContentExample;
import com.offcn.pojo.TbContentExample.Criteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class ContentServiceImpl implements ContentService {

	@Autowired
	private TbContentMapper contentMapper;
//	缓存
	@Autowired
	private RedisTemplate redisTemplate;
	/**
	 * 查询全部
	 */
	@Override
	public List<TbContent> findAll() {
		return contentMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbContent> page=   (Page<TbContent>) contentMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbContent content) {
		contentMapper.insert(content);
//		当广告数据发生变更时，需要将缓存数据清除，这样再次查询才能获取最新的数据
//		清除缓存
		redisTemplate.boundHashOps("content").delete(content.getCategoryId());
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbContent content){
		//		查询修改前的分类
		Long categoryId = contentMapper.selectByPrimaryKey(content.getId()).getCategoryId();
//		因为修改删除缓存
		redisTemplate.boundHashOps("content").delete(categoryId);
		contentMapper.updateByPrimaryKey(content);
//		考虑到用户可能会修改广告的分类，这样需要把原分类的缓存和新分类的缓存都清除掉。
		//如果分类ID发生了修改,清除修改后的分类ID的缓存
		if (categoryId != content.getCategoryId()){
			redisTemplate.boundHashOps("content").delete(content.getCategoryId());
		}
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbContent findOne(Long id){
		return contentMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			contentMapper.deleteByPrimaryKey(id);
			Long categoryId = contentMapper.selectByPrimaryKey(id).getCategoryId();
//			清除缓存
			redisTemplate.boundHashOps("content").delete(categoryId);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbContent content, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbContentExample example=new TbContentExample();
		Criteria criteria = example.createCriteria();
		
		if(content!=null){			
						if(content.getTitle()!=null && content.getTitle().length()>0){
				criteria.andTitleLike("%"+content.getTitle()+"%");
			}			if(content.getUrl()!=null && content.getUrl().length()>0){
				criteria.andUrlLike("%"+content.getUrl()+"%");
			}			if(content.getPic()!=null && content.getPic().length()>0){
				criteria.andPicLike("%"+content.getPic()+"%");
			}			if(content.getStatus()!=null && content.getStatus().length()>0){
				criteria.andStatusLike("%"+content.getStatus()+"%");
			}	
		}
		
		Page<TbContent> page= (Page<TbContent>)contentMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}
	/**
	 * 根据广告类型ID查询列表
	 * @param categoryId
	 * @return
	 */

	@Override
	public List<TbContent> findByCategoryId(Long categoryId) {
//		先查看缓存中有没有广告列表
		List<TbContent> tbContents = (List<TbContent>)redisTemplate.boundHashOps("content").get(categoryId);

		if (tbContents == null){
			//根据广告分类ID查询广告列表
			TbContentExample contentExample = new TbContentExample();
			TbContentExample.Criteria criteria = contentExample.createCriteria();
			criteria.andCategoryIdEqualTo(categoryId);
//		查找的都是状态开启的
			criteria.andStatusEqualTo("1");
//		进行排序
			contentExample.setOrderByClause("sort_order");
			tbContents = contentMapper.selectByExample(contentExample);
//			查到后存入缓存
			redisTemplate.boundHashOps("content").put(categoryId,tbContents);
		}else {
			System.out.println("从缓存中读取数据");
		}

		return tbContents;
	}

}
