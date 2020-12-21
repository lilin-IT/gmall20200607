package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;


@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;
    @Autowired
    AlipayClient alipayClient;
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insert(paymentInfo);

    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        //幂等性检查
        PaymentInfo paymentInfoParam=new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult= paymentInfoMapper.selectOne(paymentInfoParam);
        if (StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus())&& paymentInfoResult.getPaymentStatus().equals("已支付") ){
            return;
        }else {
            String orderSn=paymentInfo.getOrderSn();
            Example example=new Example(PaymentInfo.class);
            example.createCriteria().andEqualTo("orderSn",orderSn);
            Connection connection=null;
            Session session=null;
            try {
                connection= activeMQUtil.getConnectionFactory().createConnection();
                session=connection.createSession(true, Session.SESSION_TRANSACTED);//开启事务
                //第一个值表示是否使用事务，如果是true，第二个相当于选择0
            } catch (JMSException e) {
                e.printStackTrace();
            }
            try{
                paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
                //调用MQ发送支付成功的消息
                Queue queue= session.createQueue("PAYHMBNT_SUCCESS_QUEUE");
                MessageProducer payhmbnt_success_queue = session.createProducer(queue);
                MapMessage mapMessage=new ActiveMQMapMessage();
                mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());
                payhmbnt_success_queue.send(mapMessage);
                session.commit();
            }catch (Exception e){
                //消息回滚
                try {
                    session.rollback();
                } catch (JMSException ex) {
                    ex.printStackTrace();
                }
            }finally {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo,int count) {
        Connection connection=null;
        Session session=null;
        try {
            connection= activeMQUtil.getConnectionFactory().createConnection();
            session=connection.createSession(true, Session.SESSION_TRANSACTED);//开启事务
            //第一个值表示是否使用事务，如果是true，第二个相当于选择0
        } catch (JMSException e) {
            e.printStackTrace();
        }
        try{
            //调用MQ发送支付成功的消息
            Queue queue= session.createQueue("PAYHMBNT_SUCCESS_QUEUE");
            MessageProducer payhmbnt_success_queue = session.createProducer(queue);
            MapMessage mapMessage=new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",outTradeNo);
            mapMessage.setInt("count",count);
            //为消息加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*30);
            payhmbnt_success_queue.send(mapMessage);
            session.commit();
        }catch (Exception e){
            //消息回滚
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }

        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {
        Map<String,String> resultMap=new HashMap<>();
        AlipayTradeQueryRequest request= new AlipayTradeQueryRequest();
        Map<String,Object> requestMap=new HashMap<>();
        requestMap.put("out_trade_no",out_trade_no);
        request.setBizContent(JSON.toJSONString(requestMap));
        AlipayTradeQueryResponse response=null;
        try {
            response= alipayClient.execute(request);
        }catch (Exception e){
            e.printStackTrace();
        }
        if (response.isSuccess()){
            System.out.println("交易已经创建，调用成功");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            requestMap.put("call_back_content",response.getMsg());


        }else {
            System.out.println("调用失败");
        }

        return requestMap;
    }
}
