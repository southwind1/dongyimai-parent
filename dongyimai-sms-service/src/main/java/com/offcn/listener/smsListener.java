package com.offcn.listener;

import com.offcn.util.SmsUtil;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
@Component
public class smsListener implements MessageListener {
    @Autowired
    private SmsUtil smsUtil;
    @Override
    public void onMessage(Message message) {
//        判断类型是不是MapMessage数据，是的话强转
        if (message instanceof MapMessage){
            MapMessage mapMessage = (MapMessage)message;
            try {
                System.out.println("收到短信发送请求------>>");
                //1.接收手机号码
                String mobile = mapMessage.getString("mobile");
                String param = mapMessage.getString("param");
                System.out.println("手机号："+ mobile);
                System.out.println("验证码："+ param);
                //发送验证信息
                HttpResponse response = smsUtil.sendSms(mobile, param);

                System.out.println("data:"+response.getStatusLine());
                System.out.println("发送短信成功");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
