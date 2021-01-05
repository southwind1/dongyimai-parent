package com.offcn.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.offcn.pojo.TbItem;
import com.offcn.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.List;
/**
 * 监听：用于添加索引库中记录
 * @author Administrator
 *
 */
@Component
public class itemSearchListener implements MessageListener {
    @Autowired
    private ItemSearchService itemSearchService;
    @Override
    public void onMessage(Message message) {
        System.out.println("监听接收到消息...");
        TextMessage textMessage=(TextMessage)message;
        String text = null;
        try {
            text = textMessage.getText();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        List<TbItem> tbItemList = JSON.parseArray(text, TbItem.class);
        itemSearchService.importList(tbItemList);
        System.out.println("成功导入到索引库");
    }
}
