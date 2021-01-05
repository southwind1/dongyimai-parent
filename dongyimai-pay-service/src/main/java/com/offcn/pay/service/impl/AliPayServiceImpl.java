package com.offcn.pay.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.offcn.pay.service.AliPayService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AliPayServiceImpl implements AliPayService {

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 生成支付宝支付二维码，获取支付订单号，和流水号
     * @param out_trade_no 订单号
     * @param total_fee 金额(分)
     * @return
     */

    @Override
    public Map createNative(String out_trade_no, String total_fee) {
//        预下单的请求对象
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest (); //创建API对应的request类
     // 创建一个map存储响应数据
        Map<String,String> map=new HashMap<String, String>();
        //转换下单金额按照元
        long total = Long.parseLong(total_fee);//将支付金额转化为long类型的数据
        BigDecimal bigTotal = BigDecimal.valueOf(total);//因为要显示的是到分所以为保证精确用BigDecimal
        BigDecimal cs = BigDecimal.valueOf(100d);//传建一个100
        BigDecimal bigYuan = bigTotal.divide(cs);//分---》元
        System.out.println("预下单金额:"+bigYuan.doubleValue());

//        设置请求参数
        request . setBizContent ( "{"   +
                "\"out_trade_no\":\""+ out_trade_no +"\"," + //商户订单号
                "\"total_amount\":\""+ bigYuan +"\","   +
                "\"subject\":\"Iphone6 16G\","   +
                "\"store_id\":\"NJ_001\","   +
                "\"timeout_express\":\"90m\"}" ); //订单允许的最晚付款时间
//        预下单的响应对象
        AlipayTradePrecreateResponse response = null;
        try {
            response = alipayClient.execute (request);

            //从相应对象读取相应结果
            String code = response.getCode();
            System.out.println("响应码:"+code);
            //全部的响应结果
            String body = response.getBody();
            System.out.println("返回结果:"+body);

            //如果状态码为10000就是成功
            if (code.equals("10000")){
                //取出响应数据
                //qr_code：订单二维码（有效时间 2 小时）以字符串的格式返回，
                map.put("qrcode",response.getQrCode());
                //out_trade_no：支付时传入的商户订单号，与 trade_no 必填一个。
                map.put("out_trade_no", response.getOutTradeNo());

                map.put("total_fee",total_fee);
                System.out.println("qrcode:"+response.getQrCode());
                System.out.println("out_trade_no:"+response.getOutTradeNo());
                System.out.println("total_fee:"+total_fee);
            }else {
                System.out.println("预下单接口调用失败:"+body);
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
//        打印响应的信息
        System.out.print( response.getBody ());
        //根据response中的结果继续业务逻辑处理
        return map;
    }


    /**
     * 交易查询接口alipay.trade.query：
     * 获取指定订单编号的，交易状态
     * @throws AlipayApiException
     */
    @Override
    public  Map<String,String> queryPayStatus(String out_trade_no){
        Map<String,String> map=new HashMap<String, String>();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "    \"out_trade_no\":\""+out_trade_no+"\"," +
                "    \"trade_no\":\"\"}"); //设置业务参数
        //发出请求
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            String code=response.getCode();
            System.out.println("返回值1:"+response.getBody());
            if(code.equals("10000")){
                //System.out.println("返回值2:"+response.getBody());
                map.put("out_trade_no", out_trade_no);
                map.put("tradestatus", response.getTradeStatus());
                map.put("trade_no",response.getTradeNo());
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return map;
    }
    /**
     * 关闭订单接口
     */
    @Override
    public Map closePay(String out_trade_no) {
        Map<String,String> map=new HashMap<String, String>();
        //撤销交易请求对象
        AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
        request.setBizContent("{" +
                "    \"out_trade_no\":\""+out_trade_no+"\"," +
                "    \"trade_no\":\"\"}"); //设置业务参数

        try {
            AlipayTradeCancelResponse response = alipayClient.execute(request);
            String code=response.getCode();

            if(code.equals("10000")){

                System.out.println("返回值:"+response.getBody());
                map.put("code", code);
                map.put("out_trade_no", out_trade_no);
                return map;
            }
        } catch (AlipayApiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return null;



    }
}
