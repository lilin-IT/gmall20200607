package com.atguigu.gmall.util;


import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    private JedisPool jedisPool;
    //初始化连接池
    public void initPool(String host,int port,int database,String password){
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(200);
        jedisPoolConfig.setMaxIdle(30);
        jedisPoolConfig.setBlockWhenExhausted(true);
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool=new JedisPool(jedisPoolConfig,host,port,20*1000,password);
    }
    public Jedis getJedis(){
        Jedis jedis=jedisPool.getResource();
        return jedis;
    }
}
