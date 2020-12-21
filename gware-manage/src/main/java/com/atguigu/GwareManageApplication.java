package com.atguigu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@ComponentScan(basePackages = "com.atguigu.gmall.gware")
@MapperScan(basePackages = "com.atguigu.gmall.gware.mapper")
@SpringBootApplication
public class GwareManageApplication {

	public static void main(String[] args) {
		SpringApplication.run(GwareManageApplication.class, args);
	}
}
