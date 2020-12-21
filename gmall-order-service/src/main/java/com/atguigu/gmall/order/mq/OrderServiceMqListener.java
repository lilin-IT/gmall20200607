package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {
    @Autowired
    OrderService orderService;
    @JmsListener(destination = "",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no=mapMessage.getString("out_trade_no");
        System.out.println("订单业务监听out_trade_no:"+out_trade_no);
        //更新订单状态业务
        OmsOrder omsOrder=new OmsOrder();
        omsOrder.setOrderSn(out_trade_no);
        orderService.updateOrder(omsOrder);
        System.out.println("打一个断点！");

    }

}
