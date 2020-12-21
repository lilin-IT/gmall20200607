package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.atguigu.gamll.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.Result;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {
    @Reference
    UserService userService;
    @Autowired
    AlipayClient alipayClient;
    @Reference
    PaymentService paymentService;
    @Reference
    OrderService orderService;
    @RequestMapping("index")
    @LoginRequired(loginSuccess = true)
    public String index(String outTradeNo, BigDecimal totalAmount, Result result, HttpServletRequest request, ModelMap modelMap){
        String memberId= (String) request.getAttribute("memberId");
        UmsMember umsMember= userService.getUmsMemberById(memberId);
        String nickname= umsMember.getNickname();
        //System.out.println("outTradeNo:"+outTradeNo+",totalAmount:"+totalAmount+",memberId:"+memberId+"，modelMap:"+modelMap);
        System.out.println(nickname+"的订单提交成功，待付款："+totalAmount+"，请尽快支付！");
        modelMap.put("memberId",memberId);
        modelMap.put("nickname",nickname);
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("result",result);
        System.out.println("modelMap:"+modelMap);
        return "index";
    }
    @RequestMapping("mx/submit")
    @LoginRequired(loginSuccess = true)
    public String mx(String outTradeNo, BigDecimal totalAmount, Result result, HttpServletRequest request, ModelMap modelMap){
     return null;
    }

    /*
    * 支付宝支付
    * */
    @RequestMapping("alipay/submit")
    @LoginRequired(loginSuccess = true)
    @ResponseBody
    public String alipay(String outTradeNo, BigDecimal totalAmount, Result result, HttpServletRequest request, ModelMap modelMap){
        System.out.println("准备支付宝支付！");
        //获得一个支付宝请求的客户端（它并不是一个链接，而是一个封装好的http的表单请求）
        String form=null;
        AlipayTradeAppPayRequest alipayRequest=new AlipayTradeAppPayRequest();//创建API对应的request
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        Map<String,Object> map=new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",0.01);
        map.put("subject","尚硅谷感光徕卡");
        System.out.println("支付宝支付中map："+map);
        String param= JSON.toJSONString(map);
        alipayRequest.setBizContent(param);
        System.out.println("alipayRequest:"+alipayRequest);
        try {
            form=alipayClient.pageExecute(alipayRequest).getBody();//调用SDK生成表单
            System.out.println("生成的表单："+form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //生成并且保存用户的支付信息
        OmsOrder omsOrder= orderService.getOrderByOutTradeNo(outTradeNo);
        PaymentInfo paymentInfo=new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject("谷粒商城商品一件");
        paymentInfo.setTotalAmount(totalAmount);
        paymentService.savePaymentInfo(paymentInfo);
        System.out.println("支付业务中paymentInfo："+paymentInfo);
        //向消息中间件发送一个检查支付状态（支付服务消息）的延迟消息队列
        paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);
        //提交请求到支付宝

        return form;
    }
    @RequestMapping("alipay/callback/return")
    @LoginRequired(loginSuccess = true)
    public String aliPayCallBackReturn(HttpServletRequest request, ModelMap modelMap){
        //回调请求中获取支付宝参数
        String sign=request.getParameter("sign");
        String trade_no=request.getParameter("trade_no");
        String out_trade_no=request.getParameter("out_trade_no");
        String trade_status=request.getParameter("trade_status");
        String total_amount=request.getParameter("total_amount");
        String subject=request.getParameter("subject");
        String call_back_content=request.getQueryString();

        //通过支付宝的paramsMap进行签名验证
        if (StringUtils.isNotBlank(sign)){
            //验签成功
            //更新用户的支付状态

            PaymentInfo paymentInfo=new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);//支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            //更新用户的支付状态
            paymentService.updatePayment(paymentInfo);

        }

        //更新用户的支付状态
        return "finish";
    }
}
