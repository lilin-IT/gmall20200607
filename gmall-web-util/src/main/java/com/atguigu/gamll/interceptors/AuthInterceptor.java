package com.atguigu.gamll.interceptors;

import com.alibaba.fastjson.JSON;
import com.atguigu.gamll.annotations.LoginRequired;
import com.atguigu.gamll.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截代码
        // 判断被拦截请求的访问方法的注解（是否需要拦截）
        System.out.println("判断代码是否需要拦截！");
        HandlerMethod hm= (HandlerMethod)handler;
        System.out.println("hm:"+hm);
        LoginRequired loginRequired= hm.getMethodAnnotation(LoginRequired.class);
        System.out.println("拦截器中获取到的loginRequired:"+loginRequired);
        //是否拦截
        if (loginRequired==null){
            return true;
        }
        /**
         * 如果oldToken为null newToken为null 说明从未登lu过
         * newToken为null oldToken不为null之前登录过
         * newToken不为null oldToken为null刚刚登录过
         * newToken不为空   oldToken不为空  oldToken过期
         */
        System.out.println("进入拦截器的拦截方法");
        String token="";
        String oldToken=CookieUtil.getCookieValue(request,"oldToken",true);
        System.out.println("拦截器中oldToken:"+oldToken);
        if (StringUtils.isNotBlank(oldToken)){
            token=oldToken;
        }
        String newToken=request.getParameter("token");
        System.out.println("拦截器中newToken:"+newToken);
        if (StringUtils.isNotBlank(newToken)){
            token=newToken;
        }
        //是否必须登录
        boolean loginSuccess= loginRequired.loginSuccess();//获得该请求是否必登录成功
        System.out.println("loginSuccess:"+loginSuccess);

        //调用认证中心进行验证
        String success="fail";
        System.out.println("调用认证中心进行验证！");
        Map<String,String> successMap=new HashMap<>();
        String ip=request.getHeader("x-forwarded-for");//通过nginx转发的获得的客户端ip
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();//从request中获取
            System.out.println("获取到的ip:" + ip);
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";
            }
        }
        System.out.println("token:"+token);
        String successJson= HttpClientUtil.doGet("http://passport.gmall.com:8085/verify?token="+token+"&currentIp="+ip);
        System.out.println("successJson:"+successJson);
        successMap= JSON.parseObject(successJson, Map.class);
        System.out.println("successMap:"+successMap);
        if(successMap.get("status")!=null){
            success=successMap.get("status").toString();
        }
        System.out.println("必须登录成功才能使用！");
        if (loginSuccess==true){
            //必须登录成功才能使用
            if (!success.equals("success")){
                //重定向回passport登录
                StringBuffer requestUrl=request.getRequestURL();
                System.out.println("requestUrl:"+requestUrl);
                response.sendRedirect("http://passport.gmall.com:8085/index?ReturnUrl="+requestUrl);
                return false;

            }else {
                System.out.println("A将用户ID和名称放入请求域中！");
                //需要将token携带的用户信息写入
                request.setAttribute("memberId",successMap.get("memberId"));
                request.setAttribute("nickname",successMap.get("nickname"));
                //验证通过，覆盖cookie中的token
               /* String memberId= (String) request.getAttribute("memberId");
                String nickname= (String) request.getAttribute("nickname");
                System.out.println("id:"+memberId+",名字为："+nickname+"的订单提交成功!");*/
                if (StringUtils.isNotBlank(token)){
                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }
                return true;
            }

        }else {
            //没有登录也能用，但必须验证
            if (success.equals("success")){
                //需要将token携带的用户信息写入
                System.out.println("B将用户ID和名称放入请求域中！");
                request.setAttribute("memberId",successMap.get("memberId"));
                request.setAttribute("nickname",successMap.get("nickname"));
                //验证通过，覆盖cookie中的token
                if (StringUtils.isNotBlank(token)){
                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }

            }
        }
        System.out.println("进入拦截器的拦截方法！");
        System.out.println("返回一个true");
        return true;
    }
}
