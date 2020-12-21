package com.atguigu.gmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.mapper.UserMapper;
import com.atguigu.gmall.service.UserService;

import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;


import java.util.List;

@Service
public class UserServiceImpl implements UserService {
   @Autowired
   UserMapper userMapper;
   @Autowired
   RedisUtil redisUtil;

   @Autowired
   UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Override
    public List<UmsMember> getAllUser(  ) {
        List<UmsMember> umsMemberList= userMapper.selectAllUser();
        return  umsMemberList;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMebmerId(String memberId) {
       //封装参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList= umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddressList;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        UmsMember umsMemberFromDb=null;
        System.out.println("umsMember:"+umsMember);
        Jedis jedis=new Jedis();
        try{
            jedis=redisUtil.getJedis();
            System.out.println("jedis:"+jedis);
            if (jedis!=null){
                String umsMemberStr= jedis.get("user:"+umsMember.getUsername()+umsMember.getPassword()+":info");
                System.out.println("umsMemberStr:"+umsMemberStr);
                if (StringUtils.isNotBlank(umsMemberStr)){
                    //密码正确
                    UmsMember umsMemberFromCache= JSON.parseObject(umsMemberStr,UmsMember.class);
                    System.out.println("密码正确！");
                    return umsMemberFromCache;
                }else {
                    //密码错误
                    //缓存中没有开启数据库
                    umsMemberFromDb=loginFromDb(umsMember);
                    if (umsMemberFromDb!=null){
                        jedis.setex("user:" + umsMemberFromDb.getUsername()+umsMemberFromDb.getPassword()+ ":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
                    }
                    return umsMemberFromDb;
                }
            }
            //连接redis失败，开启数据库
            System.out.println("准备开启数据库！");
            umsMemberFromDb= loginFromDb(umsMember);
            System.out.println("umsMemberFromDb:"+umsMemberFromDb);
            if (umsMemberFromDb!=null){
                jedis.setex("user:"+umsMember.getUsername()+umsMember.getPassword()+":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
            }

            return umsMemberFromDb;
        }finally {
            System.out.println("关闭jedis!");
            jedis.close();
        }

    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis=redisUtil.getJedis();
        jedis.setex("user:"+memberId+":token",60*60*2,token);
        jedis.close();
    }
    private UmsMember loginFromDb(UmsMember umsMember) {
        System.out.println("执行这个方法开启数据库："+umsMember);
        List<UmsMember> umsMembers= userMapper.select(umsMember);
        System.out.println("获取到的umsMembers："+umsMembers);
        if (umsMembers!=null){
            return umsMembers.get(0);
        }
        return null;
    }

     public  UmsMember addOauthUser(UmsMember umsMember){
        userMapper.insertSelective(umsMember);
        return umsMember;

    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsCheck) {
        UmsMember umsMember= userMapper.selectOne(umsCheck);
        return umsMember;
    }

    @Override
    public UmsMember getOauthUser(UmsMember userMemberCheck) {

        UmsMember umsMember= userMapper.selectOne(userMemberCheck);
        return umsMember;
    }

    @Override
    public UmsMember getUmsMemberById(String memberId) {
        UmsMember umsMember=new UmsMember();
        umsMember.setId(memberId);
        return userMapper.selectOne(umsMember);
    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        System.out.println("地址ID："+receiveAddressId);
        UmsMemberReceiveAddress umsMemberReceiveAddress=new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress umra= umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
        return umra;
    }
}
