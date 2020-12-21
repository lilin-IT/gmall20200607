package com.atguigu.gmall.cart.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    OmsCartItemMapper omsCartItemMapper;
    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {
        OmsCartItem omsCartItem=new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        return omsCartItemMapper.selectOne(omsCartItem);
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        if (StringUtils.isNotBlank(omsCartItem.getMemberId())){
            omsCartItemMapper.insert(omsCartItem);
        }

    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        Example e=new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb,e);

    }

    @Override
    public void flushCartCache(String memberId) {
        OmsCartItem omsCartItem=new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems= omsCartItemMapper.select(omsCartItem);
        //同步到redis缓存中
        System.out.println("同步到缓存中！");
        Jedis jedis=new Jedis();
        jedis=redisUtil.getJedis();
        System.out.println("获取到jedi:"+jedis);
        Map<String,String> map=new HashMap<>();
        for (OmsCartItem cartItem:omsCartItems){
            map.put(cartItem.getProductId(), JSON.toJSONString(cartItem));
        }
        jedis.hmset("user:"+memberId+":cart",map);
        jedis.close();

    }

    //查询购物车的集合
    @Override
    public List<OmsCartItem> cartList(String userId) {
        Jedis jedis=null;
        List<OmsCartItem> omsCartItems=new ArrayList<>();
        try {
            redisUtil=new RedisUtil();
            jedis=redisUtil.getJedis();
            List<String> hvals=jedis.hvals("user:"+userId+":cart");
            for (String hval:hvals){
                OmsCartItem omsCartItem= JSON.parseObject(hval,OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }
        }catch (Exception e){
            //处理异常，记录系统日志
            e.printStackTrace();
            //String message=e.getMessage();
            return null;
        }finally {
            jedis.close();
        }
        return omsCartItems;
    }
}
