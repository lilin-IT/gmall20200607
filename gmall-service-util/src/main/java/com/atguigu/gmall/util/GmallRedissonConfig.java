package com.atguigu.gmall.util;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;

public class GmallRedissonConfig {
    @Value("${spring.redis.host:0}")
    private  String host;
    @Value("${spring.redis.port:6379}")
    private  String port;
  /*  @Value("${spring.redis.password}")
    private String password;*/
    public RedissonClient redissonClient(){
        Config config=new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port);
        RedissonClient redisson= Redisson.create(config);
        return redisson;
    }

}
