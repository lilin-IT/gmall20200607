package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseSaleAttrMapper;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AttrServiceImpl implements AttrService {
    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;
    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;
    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;


    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo=new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos= pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        System.out.println("AttrServiceImpl中根据三级ID"+catalog3Id+";查询出的pmsBaseAttrInfos值："+pmsBaseAttrInfos);
        for (PmsBaseAttrInfo baseAttrInfo:pmsBaseAttrInfos){
            List<PmsBaseAttrValue> pmsBaseAttrValueList=new ArrayList<>();
            PmsBaseAttrValue pmsBaseAttrValue=new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());
            System.out.println("AttrServiceImpl里attrInfoList中待查询的的pmsBaseAttrInfo"+pmsBaseAttrValue);
            pmsBaseAttrValueList=pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            System.out.println("AttrServiceImpl中根据pmsBaseAttrInfo的id;查询出的pmsBaseAttrValue值："+pmsBaseAttrValueList);
            baseAttrInfo.setAttrValueList(pmsBaseAttrValueList);
        }
        return pmsBaseAttrInfos;
    }

    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        String id=pmsBaseAttrInfo.getId();
        if (StringUtils.isBlank(id)){
            //id为空，保存
            //保存属性
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo); //insert  insertSelective 是否将null数据库
            //保存属性值
            List<PmsBaseAttrValue> attrValueList=pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue:attrValueList){
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }
        }else {
            //id不为空，修改属性
            Example example=new Example(PmsBaseAttrInfo.class);
            example.createCriteria().andEqualTo("id",pmsBaseAttrInfo.getId());
            pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo,example);
            //属性值
            //按照属性id删除所有属性
            PmsBaseAttrValue pmsBaseAttrValueDel=new PmsBaseAttrValue();
            pmsBaseAttrValueDel.setAttrId(pmsBaseAttrInfo.getId());
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);
            //删除后，将新的属性值插入
            List<PmsBaseAttrValue> pmsBaseAttrValueList= pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue: pmsBaseAttrValueList){
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }


        }

        return "success";
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue=new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValues= pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValues;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueId(Set<String> valueIdSet) {
        String valueIdStr= StringUtils.join(valueIdSet,",");
        List<PmsBaseAttrInfo> pmsBaseAttrInfos= pmsBaseAttrInfoMapper.selectAttrValueListByValueId(valueIdStr);

        return pmsBaseAttrInfos;
    }
}
