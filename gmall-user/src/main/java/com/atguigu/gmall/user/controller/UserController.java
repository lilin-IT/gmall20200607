package com.atguigu.gmall.user.controller;


import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {
    @Autowired
    UserService userService;
    @RequestMapping("index")
    @ResponseBody
    public String index(){
        return "hello user";
    }
    //查询全部用户信息
    @RequestMapping("getAllUser")
    @ResponseBody
    public Object getAllUser(){
        List<UmsMember> umsMembersList=userService.getAllUser();
        return umsMembersList;

    }
    //更加用户id查询收货地址
    @RequestMapping("getReceiveAddressByMemberId")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressByMebmerId(String  memberId){  //接收json类型的参数
         List<UmsMemberReceiveAddress> umsMemberReceiveAddressList= userService.getReceiveAddressByMebmerId(memberId);
         return umsMemberReceiveAddressList;
    }
}
