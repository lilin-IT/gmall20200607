package com.atguigu.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@CrossOrigin
public class SkuController {
    @Reference
    SkuService skuService;
    //保存spu信息
    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody PmsSkuInfo pmsSkuInfo){
        System.out.println("pmsSkuInfo:"+pmsSkuInfo);
        //将spuId封装给productId
        pmsSkuInfo.setProductId(pmsSkuInfo.getSpuId());
        //处理默认图片
         String skuDefaultImg =pmsSkuInfo.getSkuDefaultImg();
         System.out.println("skuDefaultImg:"+skuDefaultImg);
         if (StringUtils.isBlank(skuDefaultImg)){
             pmsSkuInfo.setSkuDefaultImg(pmsSkuInfo.getSkuImageList().get(0).getImgUrl());
         }
         System.out.println("是这里错了！");
        skuService.saveSkuInfo(pmsSkuInfo);
        System.out.println("回来了：saveSkuInfo！");
        return  "success";
    }
}
