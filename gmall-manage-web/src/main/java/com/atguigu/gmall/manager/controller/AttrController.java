package com.atguigu.gmall.manager.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.service.AttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import java.util.List;
@Controller
@CrossOrigin
public class AttrController {
    @Reference
    AttrService attrService;
    // 根据三级分类id获取平台属性列表
    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id){
        List<PmsBaseAttrInfo> pmsBaseAttrInfos= attrService.attrInfoList(catalog3Id);
        return pmsBaseAttrInfos;
    }

    //新增平台属性保存
    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(@RequestBody PmsBaseAttrInfo pmsBaseAttrInfo){
        System.out.println("新增平台属性："+pmsBaseAttrInfo);
        String success= attrService.saveAttrInfo(pmsBaseAttrInfo);
        return "success";
    }
    // 根据属性id获取属性值列表,为修改页面查询一个属性值集合
    @RequestMapping("getAttrValueList")
    @ResponseBody
    public  List<PmsBaseAttrValue> getAttrValueList(String attrId){
        List<PmsBaseAttrValue> pmsBaseAttrValues= attrService.getAttrValueList(attrId);
        return pmsBaseAttrValues;
    }

    //查询销售属性字典表
    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<PmsBaseSaleAttr> baseSaleAttrList(){
       List<PmsBaseSaleAttr> pmsBaseSaleAttrs= attrService.baseSaleAttrList();
       return  pmsBaseSaleAttrs;
    }
}
