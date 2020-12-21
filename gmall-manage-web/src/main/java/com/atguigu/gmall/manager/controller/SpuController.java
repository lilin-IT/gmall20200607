package com.atguigu.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.manager.util.PmsUploadUtil;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@CrossOrigin
public class SpuController {
    @Reference
    SpuService spuService;
    //获取spu属性列表
    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id){
        List<PmsProductInfo> pmsProductInfos= spuService.spuList(catalog3Id);
        return  pmsProductInfos;
    }

    //用户点击保存spu信息
    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo){

        spuService.saveSpuInfo(pmsProductInfo);
        return "success";
    }

    //添加spu图标上传保存
    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file")MultipartFile multipartFile){
        //将图片或音视频上传到分布式的文件存储系统
        String imgUrl= PmsUploadUtil.uploadImage(multipartFile   );
        //将图片的存储路径返回给页面
        System.out.println("上传图片路径："+imgUrl);
        return imgUrl;
    }

    //获取销售属性列表
    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){
        List<PmsProductSaleAttr> pmsProductSaleAttrList= spuService.spuSaleAttrList(spuId);

        return pmsProductSaleAttrList;
    }

    //获取图片列表
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(String spuId){
         List<PmsProductImage> pmsProductImages= spuService.spuImageList(spuId);

        return pmsProductImages;
    }
}
