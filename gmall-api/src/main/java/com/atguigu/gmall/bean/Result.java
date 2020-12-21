package com.atguigu.gmall.bean;

import java.math.BigDecimal;

public class Result {
    private String outTradeNo;
    private BigDecimal totalAmount;

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "Result{" +
                "outTradeNo='" + outTradeNo + '\'' +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
