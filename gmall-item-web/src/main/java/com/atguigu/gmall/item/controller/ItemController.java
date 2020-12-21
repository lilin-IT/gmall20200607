package com.atguigu.gmall.item.controller;



import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    SkuService skuService;

    @Reference
    SpuService spuService;
    //测试thymeleaf模板技术
    @RequestMapping("index")
    public String index(ModelMap modelMap){
        List<String> list=new ArrayList<>();
        for (int i=0;i<5;i++){
            list.add("循环数据："+i);
        }
        modelMap.put("list",list);
        modelMap.put("hello","hello thymeleaf!");

        return "index";
    }

    //查询商品详情页信息
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request){
        System.out.println("请求商品ID为："+skuId+"的详情信息。");
        //获取请求IP地址
        String remoteAddr= request.getRemoteAddr();
        PmsSkuInfo pmsSkuInfo= skuService.getSkuById(skuId,remoteAddr);
        System.out.println("ID为："+skuId+"的商品信息："+pmsSkuInfo);
        //sku对象
        modelMap.put("skuInfo",pmsSkuInfo);
        String psiId=pmsSkuInfo.getId();
        String psipId=pmsSkuInfo.getProductId();
        System.out.println("psiId:"+psiId+";psipId:"+psiId);

        //销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs= spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        System.out.println("pmsSkuInfo的销售属性:"+pmsProductSaleAttrs);
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);
        //查询当前sku的spu的其他sku的集合的hash表
        Map<String,String> skuSaleAttrHash=new HashMap<>();
        List<PmsSkuInfo> pmsSkuInfos= skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());
        for (PmsSkuInfo skuInfo:pmsSkuInfos){
            String k="";
            String v=skuInfo.getId();
            List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValues= skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue:pmsSkuSaleAttrValues){
                k+=pmsSkuSaleAttrValue.getSaleAttrValueId()+"|";    // "239|245"
            }
            skuSaleAttrHash.put(k,v);
        }
        //将sku的销售属性hash表放到页面
        String skuSaleAttrHashJsonStr= JSON.toJSONString(skuSaleAttrHash);
        modelMap.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonStr);


        return "item";


    }
}
