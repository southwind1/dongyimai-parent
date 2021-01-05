package com.offcn.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.cart.service.CartService;
import com.offcn.entity.Cart;
import com.offcn.mapper.TbItemMapper;
import com.offcn.pojo.TbItem;
import com.offcn.pojo.TbOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private TbItemMapper itemMapper;

    /**
     * 添加商品到购物车
     * @param cartList 购物车列表
     * @param itemId 商品条目id
     * @param num 商品的数量
     * @return
     */
    @Override
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {

        //1.根据商品SKU ID查询SKU商品信息
        TbItem item = itemMapper.selectByPrimaryKey(itemId);
//        判断该商品是否存在
        if (item == null){
            throw new RuntimeException("该商品不存在");
        }
//        判断该商品是否审核通过
        if(!item.getStatus().equals("1")){
            throw new RuntimeException("商品状态无效");
        }
        //2.获取商家ID
        String sellerId = item.getSellerId();
        //3.根据商家ID判断购物车列表中是否存在该商家的购物车
        Cart cart = searchCartBySellerId(cartList, sellerId);
        //4.如果购物车列表中不存在该商家的购物车
        if (cart == null){
            //4.1 新建购物车对象
            cart = new Cart();
            cart.setSellerId(sellerId);
            cart.setSellerName(item.getSeller());
//            创建一个新的购物车明细列表
            List<TbOrderItem> orderItemList = new ArrayList<TbOrderItem>();
//            根据商品的sku创建购物明细
            TbOrderItem orderItem = createOrderItem(item, num);
            orderItemList.add(orderItem);
            cart.setOrderItemList(orderItemList);
            //4.2 将新建的购物车对象添加到购物车列表
            cartList.add(cart);
        }else {
//            存在这个购物车，取出具体的购物明细
            // 判断购物车明细列表中是否存在该商品
            TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(), itemId);
            if (orderItem == null){
//                创建一个新的
                orderItem = createOrderItem(item, num);
                cart.getOrderItemList().add(orderItem);
            }else {
//                存在该商品明细
//                修改该商品明细
                orderItem.setNum(orderItem.getNum() + num);
//                修改小计
                orderItem.setTotalFee(new BigDecimal(orderItem.getPrice().doubleValue()*orderItem.getNum()));
//                判断更新后是否<= 0
                if (orderItem.getNum() <= 0){
//                    移除该商品
                    cart.getOrderItemList().remove(orderItem);
                }
//                判断该商家购物列表还有没有购物明细，如果没有删除该购物车
                if (cart.getOrderItemList().size() == 0){
                    cartList.remove(cart);
                }
            }
        }

        //5.如果购物车列表中存在该商家的购物车
        // 查询购物车明细列表中是否存在该商品
        //5.1. 如果没有，新增购物车明细
        //5.2. 如果有，在原购物车明细上添加数量，更改金额
        System.out.println(cartList);
        return cartList;
    }
    /**
     * 从redis中查询购物车
     * @param username
     * @return
     */
    @Override
    public List<Cart> findCartListFromRedis(String username) {
        System.out.println("从redis中提取购物车数据....."+username);
        List<Cart> cartList = (List<Cart>)redisTemplate.boundHashOps("cartList").get(username);
        if (cartList == null){
            cartList = new ArrayList<Cart>();
        }
        return cartList;
    }

    @Override
    public void saveCartListToRedis(String username, List<Cart> cartList) {
        System.out.println("向redis存入购物车数据....."+username);
        redisTemplate.boundHashOps("cartList").put(username,cartList);
    }

    @Override
    public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
        System.out.println("合并购物车");
        for(Cart cart: cartList2){
            for(TbOrderItem orderItem:cart.getOrderItemList()){
                cartList1= addGoodsToCartList(cartList1,orderItem.getItemId(),orderItem.getNum());
            }
        }
        return cartList1;
    }


    /**
     * 根据商家ID判断购物车列表中是否存在该商家的购物车
     */
    public Cart searchCartBySellerId(List<Cart> cartList,String sellerId){
        for (Cart cart : cartList) {
            if (cart.getSellerId().equals(sellerId)){
                return cart;
            }
        }
        return null;
    }

    /**
     *  根据商品的sku和购买的数量创建购物明细
     * @param item 商品的详情
     * @param num 购买数量
     * @return
     */
    public TbOrderItem createOrderItem(TbItem item,Integer num){
//        判断数量是否有效
        if (num <= 0){
            throw new RuntimeException("数量无效，小于或等于0");
        }
        TbOrderItem orderItem = new TbOrderItem();
        /**
         * `item_id` bigint(20) NOT NULL COMMENT '商品id',
         *   `goods_id` bigint(20) DEFAULT NULL COMMENT 'SPU_ID',
         *   `order_id` bigint(20) NOT NULL COMMENT '订单id',
         *   `title` varchar(200) COLLATE utf8_bin DEFAULT NULL COMMENT '商品标题',
         *   `price` decimal(20,2) DEFAULT NULL COMMENT '商品单价',
         *   `num` int(10) DEFAULT NULL COMMENT '商品购买数量',
         *   `total_fee` decimal(20,2) DEFAULT NULL COMMENT '商品总金额',
         *   `pic_path` varchar(200) COLLATE utf8_bin DEFAULT NULL COMMENT '商品图片地址',
         *   `seller_id` varchar(100) COLLATE utf8_bin DEFAULT NULL,
         */
        orderItem.setGoodsId(item.getGoodsId());
        orderItem.setItemId(item.getId());
        orderItem.setNum(num);
        orderItem.setTitle(item.getTitle());
        orderItem.setPrice(item.getPrice());
        orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue()*num));
        orderItem.setPicPath(item.getImage());
        orderItem.setSellerId(item.getSellerId());
        return orderItem;
    }

    /**
     * 判断购物车明细列表中是否存在该商品
     * @param orderItemList
     * @param itemId
     * @return
     */

    public TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
        for (TbOrderItem orderItem : orderItemList) {
            if (orderItem.getItemId().longValue()==itemId.longValue()){
                return orderItem;
            }
        }
        return null;
    }
}
