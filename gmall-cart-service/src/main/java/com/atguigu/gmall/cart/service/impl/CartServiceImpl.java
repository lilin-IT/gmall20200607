package com.atguigu.gmall.cart.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.config.RedisConfig;
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
        System.out.println("准备添加到购物车："+omsCartItem);
        /*if (StringUtils.isNotBlank(omsCartItem.getMemberId())){
            System.out.println("服务层添加到购物车："+omsCartItem);
             int result=omsCartItemMapper.insert(omsCartItem);
             System.out.println("添加到购物车结果："+result);
        }*/
        int result=omsCartItemMapper.insert(omsCartItem);
        System.out.println("添加到购物车结果："+result);

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
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
            map.put(cartItem.getProductId(), JSON.toJSONString(cartItem));
        }
        jedis.del("user:"+memberId+":cart");
        jedis.hmset("user:"+memberId+":cart",map);
        jedis.close();

    }

    //查询购物车的集合
    @Override
    public List<OmsCartItem> cartList(String userId) {
        System.out.println("查询（"+userId+"）的购物车集合");
        Jedis jedis=null;
        List<OmsCartItem> omsCartItems=new ArrayList<>();
        try {
            jedis=redisUtil.getJedis();
            List<String> hvals=jedis.hvals("user:"+userId+":cart");
            System.out.println("hvals:"+hvals);
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

    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        Example e=new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,e);
        //缓存同步
        flushCartCache(omsCartItem.getMemberId());
    }


}
