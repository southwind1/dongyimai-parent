package com.offcn.user.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {
    /**
     * 获取当前登录成功的用户名
     */
    @RequestMapping("/showName")
    public Map showName(){
        //取出登录用户名
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        Map map=new HashMap<String,String>();

        map.put("loginName",name);

        return map;

    }
}
