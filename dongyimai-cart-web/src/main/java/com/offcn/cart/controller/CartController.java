package com.offcn.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.offcn.cart.service.CartService;
import com.offcn.entity.Cart;
import com.offcn.entity.Result;
import com.offcn.util.CookieUtil;
import org.opensaml.xml.schema.XSString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Reference
    private CartService cartService;

    /**
     * 添加购物车，首先要将购物车列表取出
     *
     */
    @RequestMapping("/findCartList")
    public List<Cart> findCartList(HttpServletRequest request,HttpServletResponse response){
//得到登陆人账号,判断当前是否有人登陆，当用户未登陆时，username的值为anonymousUser
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Cart> cartList_cookie = new ArrayList<>();
        //        首先从cookie中取出购物车列表
        String cartListString = CookieUtil.getCookieValue(request, "cartList", "UTF-8");

        if(cartListString==null || cartListString.equals("")){
            cartListString="[]";
        }
        //        转换json成list
        cartList_cookie = JSONArray.parseArray(cartListString, Cart.class);
        System.out.println(cartList_cookie + "阿萨达");
        if(username.equals("anonymousUser")) {//如果未登录
            return cartList_cookie;
        }else {
//            登录后从redis中获取购物车列表
            List<Cart>  cartList_redis = cartService.findCartListFromRedis(username);
            if(cartList_cookie.size()>0){//如果本地存在购物车
                //合并购物车
                System.out.println("合并购物车");
                cartList_redis=cartService.mergeCartList(cartList_redis, cartList_cookie);
                //清除本地cookie的数据
                CookieUtil.deleteCookie(request, response, "cartList");
                //将合并后的数据存入redis
                cartService.saveCartListToRedis(username, cartList_redis);
            }
            return  cartList_redis;
        }

    }
    /**
     * 添加商品到购物车
     * @param request
     * @param response
     * @param itemId
     * @param num
     * @return
     */
    /**
     * springMVC的版本在4.2或以上版本，可以使用注解实现跨域, 我们只需要在需要跨域的方法上添加注解@CrossOrigin即可 allowCredentials="true"  可以缺省
     */
    @CrossOrigin(origins="http://localhost:9108",allowCredentials="true")
    @RequestMapping("/addGoodsToCartList")
    public Result addGoodsToCartList(HttpServletRequest request, HttpServletResponse response, Long itemId, Integer num){

////     购物车工程能够接收跨域请求
////        允许跨域
//        response.setHeader("Access-Control-Allow-Origin", "http://localhost:9108");
////        允许携带参数
//        response.setHeader("Access-Control-Allow-Credentials", "true");


        //得到登陆人账号,判断当前是否有人登陆，当用户未登陆时，username的值为anonymousUser
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录用户："+username);
        try {
            //        获取购物车列表
            List<Cart> cartList = findCartList(request,response);
            cartList = cartService.addGoodsToCartList(cartList, itemId, num);
//            如果未登录保存到cookie
            if (username.equals("anonymousUser")){
                CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList),3600*24,"UTF-8");
                System.out.println("向cookie存入数据");
            }else {
                cartService.saveCartListToRedis(username,cartList);
                System.out.println("向redis存入数据");
            }

            return new Result(true, "添加成功");
        }catch (Exception e){
            e.printStackTrace();
            return new Result(false, "添加失败");
        }
    }


}
