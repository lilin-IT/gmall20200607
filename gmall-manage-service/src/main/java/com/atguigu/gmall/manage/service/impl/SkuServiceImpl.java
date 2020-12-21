package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        System.out.println("到这里了！：saveSkuInfo");
        //插入skuInfo
        int i=pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId=pmsSkuInfo.getId();
        System.out.println("skuId:"+skuId);

        //插入平台属性关联
        List<PmsSkuAttrValue> pmsSkuAttrValueList= pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue:pmsSkuAttrValueList){
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }
        //插入销售属性关联
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValueList= pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue:pmsSkuSaleAttrValueList){
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        //插入图片信息
        List<PmsSkuImage> pmsSkuImageList= pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage:pmsSkuImageList){
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId, String ip) {
        return null;
    }
    @Override
    public PmsSkuInfo getSkuById(String skuId, String ip) {
        System.out.println("IP为："+ip+"的同学："+Thread.currentThread().getName()+"进入了商品详情的请求。");
        PmsSkuInfo pmsSkuInfo=new PmsSkuInfo();
       //连接缓存
        Jedis jedis=redisUtil.getJedis();
        //查询缓存
        String skuKey="sku:"+skuId+":info";
        String skuJson=jedis.get(skuKey);
        if (StringUtils.isNoneBlank(skuJson)){
            System.out.println("IP为："+ip+"的同学："+Thread.currentThread().getName()+"从缓存中获取到商品详情信息。");
            pmsSkuInfo= JSON.parseObject(skuJson,PmsSkuInfo.class);
        }else {
            //如果缓存中没有，查询mysql
            //设置分布式锁
            System.out.println("IP为："+ip+"的同学："+Thread.currentThread().getName()+"发现缓存中没有，申请缓存的分布式锁:"+"sku:"+skuId+":lock");
            String token= UUID.randomUUID().toString();
            String OK=jedis.set("sku:"+skuId+":lock",token,"nx","px",10*1000);//拿到锁的线程有10秒的过期时间
            if (StringUtils.isNoneBlank(OK)&&OK.equals("OK")){
                //设置成功，有权在10秒的过期时间内访问数据库
                System.out.println("IP为："+ip+"的同学："+Thread.currentThread().getName()+"成功拿到锁，有权在10秒的时间内访问数据库:"+"sku:"+skuId+":lock");
                pmsSkuInfo=getSkuByIdFromDb(skuId);
                try {
                    Thread.sleep(1000*5);
                }catch (InterruptedException e){
                    e.printStackTrace();

                }
                if (pmsSkuInfo!=null){
                    //mysql查询结果存入redis
                    jedis.set("sku:"+skuId+":info",JSON.toJSONString(pmsSkuInfo));
                }else {
                    //数据库中不存在该sku
                    //为了防止缓存穿透，null或者空字符串值设置给redis,并设置过期时间为3分钟
                    jedis.setex("sku:"+skuId+":info",60*3,JSON.toJSONString(""));
                }

                //在访问完mysql后，将mysql的分布式锁释放
                //用token确认删除的是自己的sku的锁
                String lockToken=jedis.get("sku:"+skuId+":info");
                if (StringUtils.isNoneBlank(lockToken)&&lockToken.equals(token)){
                    // jedis.eval("lua");可以用lua脚本，在查询到key的同时删除key,防止高并发下的意外的发生
                    jedis.del("sku:"+skuId+":lock");
                }

                System.out.println("IP为："+ip+"的同学："+Thread.currentThread().getName()+"使用完毕，已将锁归还:"+"sku:"+skuId+":lock");
            }else {
                System.out.println("IP为："+ip+"的同学："+Thread.currentThread().getName()+"没有拿到锁，开始自旋:"+"sku:"+skuId+":lock");
                //设置失败，自旋（该线程在沉睡几秒后，重新尝试访问本方法）
                try {
                    Thread.sleep(3000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                return getSkuById(skuId,ip);
            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos= pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        System.out.println("没到这里来："+catalog3Id);
        List<PmsSkuInfo> pmsSkuInfos= pmsSkuInfoMapper.selectAll();
        System.out.println("pmsSkuInfos:"+pmsSkuInfos);
        for (PmsSkuInfo pmsSkuInfo:pmsSkuInfos){
            String skuId=pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue=new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> pmsSkuAttrValueList= pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValueList);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal productPrice) {
        System.out.println("传过来的productSkuId:"+productSkuId+";productPrice:"+productPrice);
        boolean b=false;
        PmsSkuInfo pmsSkuInfo=new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        System.out.println("pmsSkuInfo:"+pmsSkuInfo);
        PmsSkuInfo psi= pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        System.out.println("检查价格："+psi);
        BigDecimal price=psi.getPrice();
        if (price.compareTo(productPrice)==0){
            b=true;
        }
        return b;
    }

    public PmsSkuInfo getSkuByIdFromDb(String skuId) {
        //sku的商品对象
        System.out.println("详情页商品ID："+skuId);
        PmsSkuInfo pmsSkuInfo=new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo= pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //sku的图片集合
        PmsSkuImage pmsSkuImage=new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImageList= pmsSkuImageMapper.select(pmsSkuImage);
        skuInfo.setSkuImageList(pmsSkuImageList);


        return skuInfo;
    }
}
