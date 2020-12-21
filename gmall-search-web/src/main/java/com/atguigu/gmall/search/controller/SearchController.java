package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gamll.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
public class SearchController {
    @Reference
    SearchService searchService;
    @Reference
    AttrService attrService;

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index(HttpServletRequest request) {
        String token=request.getParameter("token");
        System.out.println("search中index:"+token);
        return "index";
    }

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {//三级分类Id关键字
        //调用搜索服务返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfos);
        System.out.println("获取到的信息：" + pmsSearchSkuInfos);
        System.out.println("pmsSearchParm:" + pmsSearchParam);
        //抽取检索结果所包含的平台属性集合
        Set<String> valueIdSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            System.out.println("pmsSkuAttrValueList:" + pmsSkuAttrValueList);
            for (PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                //System.out.println("valueId:" + valueId);
                valueIdSet.add(valueId.toString());
            }
        }
        //根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueListByValueId(valueIdSet);
        System.out.println("根据valueId查询出的数据："+pmsBaseAttrInfos);
        for (int i = 0; i < pmsBaseAttrInfos.size(); i++) {
            List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrInfos.get(i).getAttrValueList();
            System.out.println("获取到的pmsBaseAttrValue值：" + pmsBaseAttrValues);
        }
        System.out.println("查找出的属性值：" + pmsBaseAttrInfos);
        modelMap.put("attrList", pmsBaseAttrInfos);
        //对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds != null) {
            //面包屑
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            for (String delValueId : delValueIds) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                //生成面包屑的参数
                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, delValueId));
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : pmsBaseAttrValues) {
                        String valueId = pmsBaseAttrValue.getId();
                        if (valueId.equals(delValueId)) {
                            //查找面包屑的属性值名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            //删除当前valueId所在的属性组
                            iterator.remove();

                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbs);
        }

        /*for (PmsBaseAttrInfo pmsBaseAttrInfo:pmsBaseAttrInfos){
            List<PmsBaseAttrValue> pmsBaseAttrValues=pmsBaseAttrInfo.getAttrValueList();
            System.out.println("pmsBaseAttrValues集合："+pmsBaseAttrValues);
            for (PmsBaseAttrValue pmsBaseAttrValue:pmsBaseAttrValues){
                String valueId=pmsBaseAttrValue.getId();
                System.out.println("pmsBaseAttrValue的id:"+valueId);
                for (String delValueId:delValueIds){
                    if (valueId.equals(delValueId)){
                        //删除当前valueId所在的属性组
                    }
                }

            }
        }*/
        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam", urlParam);
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }
        return "list";
    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] pmsSkuAttrValues = pmsSearchParam.getValueId();
        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (pmsSkuAttrValues != null) {
            for (String pmsSkuAttrValue : pmsSkuAttrValues) {
                if (!pmsSkuAttrValue.equals(delValueId)) {
                    urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
                }

            }
        }
        return urlParam;
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam, String... delValueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] pmsSkuAttrValues = pmsSearchParam.getValueId();
        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (pmsSkuAttrValues != null) {
            for (String pmsSkuAttrValue : pmsSkuAttrValues) {

                urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
            }
        }
        return urlParam;
    }

}
