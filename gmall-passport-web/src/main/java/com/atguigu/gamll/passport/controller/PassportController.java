package com.atguigu.gamll.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gamll.annotations.LoginRequired;
import com.atguigu.gamll.util.JwtUtil;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.HttpClientUtil;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {
    @Reference
    UserService userService;
    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap){
        System.out.println("收到的ReturnUrl:"+ReturnUrl);
        modelMap.put("ReturnUrl",ReturnUrl);
        return "index";
    }
    //用户登录
    @RequestMapping("login")
    @ResponseBody
    public  String login(UmsMember umsMember, HttpServletRequest request){
        String token="";
        System.out.println("带过来的umsMember:"+umsMember);
        UmsMember umsMemberLogin= userService.login(umsMember);
        System.out.println("umsMemberLogin:"+umsMemberLogin);
        if (umsMemberLogin!=null){
            //登录成功
            //用jwt制作token
            String memberId=umsMemberLogin.getId();
            String nikename=umsMemberLogin.getNickname();
            System.out.println("memberId:"+memberId+",nikeNme:"+nikename);
            Map<String,Object> userMap=new HashMap<>();
            userMap.put("memberId",memberId);
            userMap.put("nikename",nikename);
            String ip=request.getHeader("x-forwarded-for");
            System.out.println("获取到的ip:"+ip);
            if (StringUtils.isBlank(ip)){
                ip=request.getRemoteAddr();//从request中获取
                if (StringUtils.isBlank(ip)){
                    ip="127.0.0.1";
                }
            }
            //按照设计的算法对参数进行加密后，生成token
             token=JwtUtil .encode("gmall20200607",userMap,ip);
            System.out.println("登录后制作token:"+token);
            //将token存入redis一份
            userService.addUserToken(token,memberId);
            System.out.println("用户:"+umsMember.getUsername()+"登录成功！");
        }else {
            //登录失败
            token="fail";
            System.out.println("用户:"+umsMember.getUsername()+"登录失败！");
        }
        //调用用户服务验证用户名和密码
        System.out.println("返回token!："+token);
        return token;
    }
    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String currentIp){
        System.out.println("verify()中token:"+token+";currentIp:"+currentIp);
        //通过jwt校验token
        Map<String,String> map=new HashMap<>();
        Map<String,Object> decode=null;
        if(StringUtils.isNotBlank(token)&&StringUtils.isNotBlank(currentIp)){
            decode=JwtUtil.decode(token,"gmall20200607",currentIp);
            System.out.println("verify()中decode:"+decode);

        }
        if (decode!=null){
            map.put("status","success");
            map.put("memberId",(String)decode.get("memberId"));
            map.put("nikename",(String)decode.get("nikename"));
            System.out.println("verify()中：map:"+map);
        }else {
            map.put("status","fail");
        }
        return JSON.toJSONString(map);
    }

    @RequestMapping(value = "vlogin")
    //新浪微博认证登录
    public String vlogin(String code,HttpServletRequest request){
        //1、授权码换取access_token
        String s3="https://api.weibo.com/oauth2/access_token?";
        Map<String,String> paramMap=new HashMap<>();
        paramMap.put("client_id","69158814");
        paramMap.put("client_secret","b603ee1d77dc890896e0ef48c2e66849");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code",code); //授权有效期内可以使用，每新生一次授权码，说明用户对第三方数据重新授权
        System.out.println("paramMap:"+paramMap);
        String access_token_json= HttpClientUtil.doPost(s3,paramMap);
        System.out.println("access_token_json:"+access_token_json);
        Map<String,Object> access_map=JSON.parseObject(access_token_json,Map.class);
        System.out.println("access_map:"+access_map);
        //2.access_token换取用户信息
        String uid=(String)access_map.get("uid");
        System.out.println("uid:"+uid);
        String access_token=(String)access_map.get("access_token");
        String show_user_url="http://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        System.out.println("show_user_url:"+show_user_url);
        String user_json=HttpClientUtil.doGet(show_user_url);
        System.out.println("user_json:"+user_json);
        Map<String,Object> user_map=JSON.parseObject(user_json,Map.class);
        System.out.println("user_map:"+user_map);
        //将用户信息保存数据库，用户类型设置为微博用户
        UmsMember umsMember=new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((String) user_map.get("idstr"));
        umsMember.setCity((String)user_map.get("location"));
        umsMember.setNickname((String)user_map.get("screen_name"));
        System.out.println("用户信息：umsMember:"+umsMember);
        String gender=(String)user_map.get("gender");
        System.out.println("gender:"+gender);
        String g="0";
        if (gender.equals("m")){
            g="1";
        }
        umsMember.setGender(g);
        UmsMember umsCheck=new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember userMemberCheck= userService.checkOauthUser(umsCheck);//检查该用户（社交登陆）前是否登陆过系统
        if (userMemberCheck==null){
            umsMember= userService.addOauthUser(umsMember);
        }else {
            umsMember=userMemberCheck;
        }
        //生成jwt的token,并且重定向到首页，携带该token
        String token=null;
        String memberId=umsMember.getId();//rpc的主键返回策略失效
        String nickname=umsMember.getNickname();
        Map<String,Object> userMap=new HashMap<>();
        userMap.put("memberId",memberId);   //是保存数据库后主键返回策略生成的id
        userMap.put("nikename",nickname);
        String ip=request.getHeader("x-forwarded-for"); //通过nginx转发的客户端
        System.out.println("获取到的ip:"+ip);
        if (StringUtils.isBlank(ip)){
            ip=request.getRemoteAddr();//从request中获取ip
            if (StringUtils.isBlank(ip)){
                ip="127.0.0.1";
            }
        }
        //按照设计的算法对参数进行加密后，生成token
        token=JwtUtil .encode("gmall20200607",userMap,ip);
        //将token存入redis一份
        userService.addUserToken(token,memberId);

        //生成jwt的token,并且从定向到首页携带该token
        return "redirect:http://search.gmall.com:8083/index?token="+token;

    }
}
