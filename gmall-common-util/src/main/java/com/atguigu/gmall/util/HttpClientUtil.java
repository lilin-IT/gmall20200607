package com.atguigu.gmall.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClientUtil {
    public static String doGet(String url){
        System.out.println("到doGet方法中来了");
        //创建httpClient对象
        CloseableHttpClient httpClient= HttpClients.createDefault();
        //创建http get请求
        HttpGet httpGet=new HttpGet(url);
        CloseableHttpResponse response=null;
        System.out.println("response");
        try {
            System.out.println("执行请求！");
            //执行请求
            response=httpClient.execute(httpGet);
            System.out.println("response:"+response);
            System.out.println("状态："+response.getStatusLine().getStatusCode());
            //判断返回状态是否为200
            if (response.getStatusLine().getStatusCode()== HttpStatus.SC_OK){
                HttpEntity httpEntity=response.getEntity();
                System.out.println("httpEntity:"+httpEntity);
                String result= EntityUtils.toString(httpEntity,"UTF-8");
                System.out.println("result:"+result);
                EntityUtils.consume(httpEntity);
                httpClient.close();
                System.out.println("result:"+result);
                return result;
            }
            httpClient.close();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return null;
    }
    public static String doPost(String url, Map<String,String> paramMap){
        //创建httpClient对象
        CloseableHttpClient httpClient=HttpClients.createDefault();
        //创建http post请求
        HttpPost httpPost=new HttpPost(url);
        CloseableHttpResponse response=null;
        try {
            List<BasicNameValuePair> list=new ArrayList<>();
            for (Map.Entry<String,String> entry:paramMap.entrySet()){
                list.add(new BasicNameValuePair(entry.getKey(),entry.getValue()));
            }
            HttpEntity httpEntity=new UrlEncodedFormEntity(list,"utf-8");
            httpPost.setEntity(httpEntity);

            //执行请求
            response=httpClient.execute(httpPost);
            //判断状态是否为200
            if (response.getStatusLine().getStatusCode()==HttpStatus.SC_OK){
                HttpEntity httpEntity1=response.getEntity();
                String result=EntityUtils.toString(httpEntity1,"UTF-8");
                EntityUtils.consume(httpEntity1);
                httpClient.close();
                System.out.println("doPost方法返回result:"+result);
                return result;
            }
            httpClient.close();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

        return null;
    }

}
