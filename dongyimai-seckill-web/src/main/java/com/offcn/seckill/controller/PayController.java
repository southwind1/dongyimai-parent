package com.offcn.seckill.controller;


import com.alibaba.dubbo.config.annotation.Reference;

import com.offcn.entity.Result;
import com.offcn.pay.service.AliPayService;

import com.offcn.pojo.TbSeckillOrder;
import com.offcn.seckill.service.SeckillOrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制层
 * @author Administrator
 *
 *//**
 * 支付控制层
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference
    private  AliPayService aliPayService;

    @Reference
    private SeckillOrderService seckillOrderService;

    /**
     * 生成二维码
     * @return
     */
    @RequestMapping("/createNative")
    public Map createNative(){
        //获取当前用户
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        //到redis查询秒杀订单
        TbSeckillOrder seckillOrder = seckillOrderService.searchOrderFromRedisByUserId(userId);
        //判断秒杀订单存在
        if(seckillOrder!=null){
            long fen=  (long)(seckillOrder.getMoney().doubleValue()*100);//金额（分）
            return aliPayService.createNative(seckillOrder.getId()+"",+fen+"");
        }else{
            return new HashMap();
        }
    }

    /**
     * 查询支付状态
     * @param out_trade_no
     * @return
     */
    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no){
        //获取当前用户
        String userId=SecurityContextHolder.getContext().getAuthentication().getName();
        Result result=null;
        int x=0;
        while(true){
            //调用查询接口
            Map<String,String> map = aliPayService.queryPayStatus(out_trade_no);
            if(map==null){//出错
                result=new  Result(false, "支付出错");
                break;
            }
            if(map.get("tradestatus")!=null&&map.get("tradestatus").equals("TRADE_SUCCESS")){//如果成功
                result=new  Result(true, "支付成功");
                //保存秒杀结果到数据库
                seckillOrderService.saveOrderFromRedisToDb(userId, Long.valueOf(out_trade_no), map.get("transaction_id"));
                break;
            }
            if(map.get("tradestatus")!=null&&map.get("tradestatus").equals("TRADE_CLOSED")){//如果成功
                result=new  Result(true, "未付款交易超时关闭，或支付完成后全额退款");
                break;
            }
            if(map.get("tradestatus")!=null&&map.get("tradestatus").equals("TRADE_FINISHED")){//如果成功
                result=new  Result(true, "交易结束，不可退款");
                break;
            }
            try {
                Thread.sleep(3000);//间隔三秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //不让循环无休止地运行定义变量，如果超过了这个值则退出循环，设置时间为1分钟
            x++;//设置超时时间为3分钟
            if(x>100){
                result=new  Result(false, "超过时间未支付,订单取消");
                //1.调用支付宝的关闭订单接口（学员实现）
                Map<String,String> payresult = aliPayService.closePay(out_trade_no);
                if("10000".equals(payresult.get("code")) ){//如果返回结果是正常关闭
                    System.out.println("超时，取消订单");
                    //2.调用删除
                    seckillOrderService.deleteOrderFromRedis(userId, Long.valueOf(out_trade_no));

                }

                break;
            }
        }
        return result;
    }
}