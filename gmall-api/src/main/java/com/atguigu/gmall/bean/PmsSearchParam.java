package com.atguigu.gmall.bean;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class PmsSearchParam  implements Serializable {
    private String catalog3Id;
    private String[] valueId;
    private String keyword;

    public String getCatalog3Id() {
        return catalog3Id;
    }

    public void setCatalog3Id(String catalog3Id) {
        this.catalog3Id = catalog3Id;
    }


    public String[] getValueId() {
        return valueId;
    }

    public void setValueId(String[] valueId) {
        this.valueId = valueId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return "PmsSearchParam{" +
                "catalog3Id='" + catalog3Id + '\'' +
                ", valueId=" + Arrays.toString(valueId) +
                ", keyword='" + keyword + '\'' +
                '}';
    }
}
