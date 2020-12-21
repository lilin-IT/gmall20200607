package com.atguigu.gamll.passport.controller;

import com.atguigu.gmall.util.HttpClientUtil;

public class TestOauth2 {
    public static void main(String[] args) {
        //String s1= HttpClientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=69158814&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");
        // System.out.println("s1:"+s1);
        String s2="http://passport.gmall.com:8085/vlogin?code=5edf4d5ddfc1c21a43e469a6ad7a2332";
        String s3="https://api.weibo.com/oauth2/access_token?client_id=69158814&client_secret=b603ee1d77dc890896e0ef48c2e66849&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=CODE";
        String access_token= HttpClientUtil.doPost(s3,null);
        System.out.println("access_token:"+access_token);
    }
}
