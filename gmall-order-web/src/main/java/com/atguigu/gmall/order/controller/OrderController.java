package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gamll.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class OrderController {
    @Reference
    CartService cartService;
    @Reference
    UserService userService;
    @Reference
    OrderService orderService;
    @Reference
    SkuService skuService;
    //去结算
    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap){
        String memberId= (String) request.getAttribute("memberId");
        String nickname= (String) request.getAttribute("nickname");
        System.out.println("memberId:"+memberId+";nickename:"+nickname);
        //将购物车集合转化为页面计算清单集合
        List<OmsCartItem> omsCartItems= cartService.cartList(memberId);
        System.out.println("结算页面获取购物车集合："+omsCartItems);
        //收件人地址列表
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses= userService.getReceiveAddressByMebmerId(memberId);
        System.out.println("收件人地址页列表集合："+umsMemberReceiveAddresses);
        List<OmsOrderItem> omsOrderItems=new ArrayList<>();
        for (OmsCartItem omsCartItem:omsCartItems){
            //每循环一个购物车对象，就封装一个商品的详情到OmsOrderItem
            if (omsCartItem.getIsChecked().equals("1")){
                OmsOrderItem omsOrderItem=new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItems.add(omsOrderItem);
            }
            System.out.println("结算页中的商品："+omsCartItems);
        }
        modelMap.put("omsOrderItems",omsOrderItems);
        modelMap.put("userAddressLsit",umsMemberReceiveAddresses);
        modelMap.put("totalAmount",getTotalAmount(omsCartItems));
        //生成交易码，为了在提交订单时做交易码的校验
        String tradeCode= orderService.genTradeCode(memberId);
        modelMap.put("tradeCode",tradeCode);
        System.out.println("结算页信息："+modelMap);
        return "trade";
    }
    //计算总金额
    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        System.out.println("查询购物车总数："+omsCartItems);
        BigDecimal totalAmount=new BigDecimal("0");
        for (OmsCartItem omsCartItem:omsCartItems){
            System.out.println("购物车中的商品："+omsCartItem);
            BigDecimal totalPrice= omsCartItem.getTotalPrice();
            System.out.println("totalPrice:"+totalPrice);
            if (omsCartItem.getIsChecked().equals("1")){
                totalAmount=totalAmount.add(totalPrice);
                System.out.println("这里的totalAmount:"+totalAmount);
            }
        }
        System.out.println("返回");
        return totalAmount;
    }

    /**
     * 提交订单
     * @param receiveAddressId
     * @param totalAmount
     * @param tradeCode
     * @param request
     * @param response
     * @param session
     * @param modelMap
     * @return
     */
    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public ModelAndView submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap){
        String memberId= (String) request.getAttribute("memberId");
        String nickname= (String) request.getAttribute("nickname");
        //检查交易码
        String success=orderService.checkTradeCode(memberId,tradeCode);
        System.out.println("success:"+success);

        if (success.equals("success")){
            List<OmsOrderItem> omsOrderItems=new ArrayList<>();
            //订单对象
            OmsOrder omsOrder=new OmsOrder();
            omsOrder.setAutoConfirmDay(7);
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);//折扣价格
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("订单备注");
            System.out.println("omsOrder:"+omsOrder);
            String outTradeNo="gmall";
            outTradeNo=outTradeNo+System.currentTimeMillis();//将毫秒时间戳拼接到外部订单
            SimpleDateFormat sdf=new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo=outTradeNo+sdf.format(new Date());//将时间字符串拼接到外部订单号
            System.out.println("outTradeNo:"+outTradeNo);
            omsOrder.setOrderSn(outTradeNo);//外部订单号
            omsOrder.setPayAmount(totalAmount);//总金额
            omsOrder.setOrderType(1);
            UmsMemberReceiveAddress umsMemberReceiveAddress=userService.getReceiveAddressById(receiveAddressId);
            System.out.println("umsMemberReceiveAddress:"+umsMemberReceiveAddress);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            //当前日期加一天
            Calendar c=Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time=c.getTime();
            omsOrder.setReceiveTime(time);
            omsOrder.setSourceType(0);
            omsOrder.setStatus("0");
            omsOrder.setTotalAmount(totalAmount);
            //根据用户id获取要购买的商品列表（购物车），总价
            List<OmsCartItem> omsCartItems= cartService.cartList(memberId);
            System.out.println("根据用户id获取要购买的商品列表（购物车）:"+omsCartItems);
            for (OmsCartItem omsCartItem:omsCartItems){
                if (omsCartItem.getIsChecked().equals("1")){
                    //获取订单详情列表
                    OmsOrderItem omsOrderItem=new OmsOrderItem();
                    //检价
                    boolean b= skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if (b==false){
                        ModelAndView modelAndView=new ModelAndView("tradeFail");
                        return modelAndView;
                    }
                    //验库存，远程调用库存系统
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setOrderSn(outTradeNo);//外部订单号,用来和其他系统进行交互，防止重复
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductSkuCode("11111111111");
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库对应的商品编码");//在仓库中的skuId
                    System.out.println("结算单个订单信息："+omsCartItem);
                    omsOrderItems.add(omsOrderItem);

                }
                System.out.println("结算信息："+omsCartItems);
            }

                omsOrder.setOmsOrderItems(omsOrderItems);
            System.out.println("omsOrder:"+omsOrder);

            //将订单和订单详情写入数据库
            //删除购物车的对应商品
            orderService.saveOrder(omsOrder);
            System.out.println("删除购物车的对应商品!");
            //重定向到支付系统
            ModelAndView modelAndView=new ModelAndView("redirect:http://payment.gmall.com:8087/index");
            modelAndView.addObject("outTradeNo",outTradeNo);
            modelAndView.addObject("totalAmount",totalAmount);
            Result result=new Result();
            result.setOutTradeNo(outTradeNo);
            result.setTotalAmount(totalAmount);
            System.out.println("result:"+result);
            request.setAttribute("result",result);
            return modelAndView;
        }else {
            ModelAndView modelAndView=new ModelAndView("tradeFail");
            return modelAndView;
        }

    }

}
