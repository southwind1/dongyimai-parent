package com.offcn.mail.util;


import com.alibaba.dubbo.config.annotation.Service;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;

@Service
public class MailUtil {

    @Value("sentMail")
    private String sentMail;
    @Value("mailTomAddres")
    private String mailTomAddres;
    @Autowired
    private JavaMailSenderImpl mailSender;

    public boolean sentMail(String tomail){
        try {
            MimeMessage mimemsg = mailSender.createMimeMessage();

            MimeMessageHelper help = new MimeMessageHelper(mimemsg, true, "GBK");

            help.setFrom(sentMail);
            help.setTo(tomail);
            help.setSubject("html格式的邮件11");
            //加载邮件模板
            String html= IOUtils.toString(new FileInputStream(mailTomAddres),"utf-8");
            //设定要发送的邮件内容,支持html
            help.setText(html, true);
            //添加html嵌入图片
            help.addInline("www", new File("F:\\upLoad\\50ab38cd1d14838100fdd5d030a58ed3.jpg"));

            mailSender.send(mimemsg);
            System.out.println("send ok");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
