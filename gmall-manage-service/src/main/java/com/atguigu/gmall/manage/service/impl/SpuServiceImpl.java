package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsProductSaleAttrValue;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class SpuServiceImpl implements SpuService {
    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;

    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;

    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;

    @Autowired
    PmsProductImageMapper pmsProductImageMapper;
    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {
        PmsProductInfo pmsProductInfo=new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> pmsProductInfos= pmsProductInfoMapper.select(pmsProductInfo);
        return pmsProductInfos;
    }

    @Override
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {
        System.out.println("A+pmsProdectInfo;"+pmsProductInfo);
        pmsProductInfoMapper.insert(pmsProductInfo);
        String id=pmsProductInfo.getId();
        System.out.println("B+id:"+id);
        List<PmsProductSaleAttr> pmsProductSaleAttrs= pmsProductInfo.getSpuSaleAttrList();
        System.out.println("C+pmsProductSaleAttrs:"+pmsProductSaleAttrs);
        for (PmsProductSaleAttr pmsProductSaleAttr:pmsProductSaleAttrs){
            //添加spu单元的销售属性
            pmsProductSaleAttr.setProductId(id);
            pmsProductSaleAttrMapper.insert(pmsProductSaleAttr);
            //添加sp单元的销售属性值
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValueList= pmsProductSaleAttr.getSpuSaleAttrValueList();
            for (PmsProductSaleAttrValue pmsProductSaleAttrValue:pmsProductSaleAttrValueList){
                pmsProductSaleAttrValue.setProductId(id);
                pmsProductSaleAttrValueMapper.insert(pmsProductSaleAttrValue);
            }
        }
        //添加spu单元的图片集合
        List<PmsProductImage> pmsProductImageList=pmsProductInfo.getSpuImageList();
        for (PmsProductImage pmsProductImage:pmsProductImageList){
            pmsProductImage.setProductId(pmsProductInfo.getId());
            pmsProductImageMapper.insert(pmsProductImage);
        }
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        PmsProductSaleAttr pmsProductSaleAttr=new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        List<PmsProductSaleAttr> pmsProductSaleAttrs= pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        for (PmsProductSaleAttr productSaleAttr:pmsProductSaleAttrs){
            PmsProductSaleAttrValue pmsProductSaleAttrValue=new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setProductId(spuId);
            pmsProductSaleAttrValue.setSaleAttrId(productSaleAttr.getSaleAttrId());//销售属性id用的是系统的字典表中的id，不是销售属性表的主键
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValues= pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            productSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValues);



        }
        return pmsProductSaleAttrs;
    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage pmsProductImage=new PmsProductImage();
        pmsProductImage.setProductId(spuId);
        List<PmsProductImage> pmsProductImages= pmsProductImageMapper.select(pmsProductImage);
        return pmsProductImages;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId,String skuId) {
      /*  PmsProductSaleAttr pmsProductSaleAttr=new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(productId);
        List<PmsProductSaleAttr> pmsProductSaleAttrs= pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        for (PmsProductSaleAttr productSaleAttr:pmsProductSaleAttrs){
            String saleAttrId= pmsProductSaleAttr.getSaleAttrId();
            PmsProductSaleAttrValue pmsProductSaleAttrValue=new PmsProductSaleAttrValue();
            pmsProductSaleAttrValue.setSaleAttrId(saleAttrId);
            pmsProductSaleAttrValue.setProductId(productId);
            List<PmsProductSaleAttrValue> pmsProductSaleA   ttrValueList= pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            pmsProductSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValueList);
        }*/
        List<PmsProductSaleAttr> pmsProductSaleAttrs= pmsProductSaleAttrMapper.selectSpuSaleAttrListCheckBySku(productId,skuId);
        return pmsProductSaleAttrs;
    }
}
