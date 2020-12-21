package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.redisson.Redisson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.security.interfaces.RSAPrivateCrtKey;
import java.util.List;

@Controller
public class SecKillController {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedissonClient redissonClient;
    /*
    * 随机凭运气秒杀
    * */
    @RequestMapping("kill")
    @ResponseBody
    public String kill(){
        String memberId="1";
        Jedis jedis=redisUtil.getJedis();
        jedis.watch("106");
        Integer stock=Integer.parseInt(jedis.get("106"));
        if (stock>0){
            Transaction multi=jedis.multi();
            multi.incrBy("106",-1);
            List<Object> exec=multi.exec();
            if (exec!=null&&exec.size()>0){
                System.out.println("当前库存剩余数量"+stock+",用户"+memberId+"抢购成功");
                //用消息队列发出订单消息
                System.out.println("发出订单的消息队列！由订单系统对当前抢购生成订单。");
            }else {
                System.out.println("当前库存剩余数量"+stock+",某用户抢购失败！");
            }
        }
        return "1";
    }

    /**
     * 先到先得，秒杀
     * @return
     */
    @RequestMapping("secKill")
    @ResponseBody
    public String secKill(){
        RSemaphore semaphore= redissonClient.getSemaphore("106");
        boolean b=semaphore.tryAcquire();
        if (b){
            System.out.println("当前库存剩余数量,用户抢购成功");
            //用消息队列发出订单消息
        }else {
            System.out.println("当前库存剩余数量,某用户抢购失败！");
        }
        return null;
    }
}
