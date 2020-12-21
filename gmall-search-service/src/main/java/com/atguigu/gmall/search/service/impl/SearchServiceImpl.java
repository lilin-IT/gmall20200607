package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.hibernate.validator.resourceloading.AggregateResourceBundleLocator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    JestClient jestClient;
    @Override
    public List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam) {
        String dslStr= getSearchDel(pmsSearchParam);
        System.out.println("dslStr:"+dslStr);
        //用api复杂查询
        List<PmsSearchSkuInfo> pmsSearchSkuInfos=new ArrayList<PmsSearchSkuInfo>();
        Search search= new Search.Builder(dslStr).addIndex("gmall20200607").addType("PmsSkuInfo").build();
        SearchResult result= null;
        try {
            result = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<SearchResult.Hit<PmsSearchSkuInfo,Void>> hits=result.getHits(PmsSearchSkuInfo.class);
        for (SearchResult.Hit<PmsSearchSkuInfo,Void> hit:hits){
            PmsSearchSkuInfo pmsSearchSkuInfo= hit.source;
            Map<String,List<String>> highlight= hit.highlight;
            if(highlight!=null){
                String skuName= highlight.get("skuName").get(0);
                pmsSearchSkuInfo.setSkuName(skuName);


            }

            pmsSearchSkuInfos.add(pmsSearchSkuInfo);
        }
        System.out.println("复杂查询出的数据："+pmsSearchSkuInfos.size());
        return pmsSearchSkuInfos;
    }

    private String getSearchDel(PmsSearchParam pmsSearchParam) {
        String[] skuAttrValueList=pmsSearchParam.getValueId();
        String keyword=pmsSearchParam.getKeyword();
        String catalog3Id=pmsSearchParam.getCatalog3Id();
        //jest的dsl工具
        SearchSourceBuilder searchSourceBuilder=new SearchSourceBuilder();
        //bool
        BoolQueryBuilder boolQueryBuilder=new BoolQueryBuilder();
        //filter
        if (StringUtils.isNotBlank(catalog3Id)){
            TermQueryBuilder termQueryBuilder=new TermQueryBuilder("catalog3Id",catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        if (skuAttrValueList!=null){
            for (String pmsSkuAttrValue:skuAttrValueList){
                TermQueryBuilder termQueryBuilder=new TermQueryBuilder("skuAttrValueList.valueId",pmsSkuAttrValue);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //must
        if (StringUtils.isNotBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder=new MatchQueryBuilder("skuName",keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }
        //query
        searchSourceBuilder.query(boolQueryBuilder);
        //highlight
        HighlightBuilder highlightBuilder=new HighlightBuilder();
        //高亮显示
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);
        //sort
        searchSourceBuilder.sort("id", SortOrder.DESC);

        //from
        searchSourceBuilder.from(0);
        //size
        searchSourceBuilder.size(20);
        //aggs聚合函数
        TermsBuilder groupby_attr= AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        return searchSourceBuilder.toString();
    }




}
