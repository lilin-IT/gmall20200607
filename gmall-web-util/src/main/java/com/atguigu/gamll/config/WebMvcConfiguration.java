package com.atguigu.gamll.config;


import com.atguigu.gamll.interceptors.AuthInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {
    @Autowired
    AuthInterceptor authInterceptor;
    public void addInterceptors(InterceptorRegistry registry){
        System.out.println("进入拦截器！");
        registry.addInterceptor(authInterceptor).addPathPatterns("/**").excludePathPatterns("/error");
        System.out.println("拦截器中！");
        super.addInterceptors(registry);
        System.out.println("走出拦截器！");
    }
}
