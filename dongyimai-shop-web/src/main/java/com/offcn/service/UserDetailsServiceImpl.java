package com.offcn.service;


import com.offcn.pojo.TbSeller;
import com.offcn.sellergoods.service.SellerService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class UserDetailsServiceImpl implements UserDetailsService {
    private SellerService sellerService;
//	<!-- 定义自定义认证类 -->
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        创建一个存放角色权限的列表
        List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
//        向列表中添加角色
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_SELLER"));
//        得到商家对象
        TbSeller seller = sellerService.findOne(username);
//        判断商家是否为空
        if (seller != null){
//            只有商家通过了审核状态为1才可以登录
            if (seller.getStatus().equals("1")){
                //        返回这个用户
                return new User(seller.getSellerId(),seller.getPassword(),grantedAuthorities);
            }else{
                return  null;
            }
        }else {
            return null;
        }


    }

    public void setSellerService(SellerService  sellerService) {
        this.sellerService = sellerService;
    }
}
