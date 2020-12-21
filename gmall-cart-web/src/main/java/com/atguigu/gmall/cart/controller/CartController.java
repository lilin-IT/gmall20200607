package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gamll.annotations.LoginRequired;
import com.atguigu.gamll.util.CookieUtil;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {
    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, HttpSession session, ModelMap modelMap){
        System.out.println("去购物车结算");
        List<OmsCartItem> omsCartItems=new ArrayList<>();
        String memberId= (String) request.getAttribute("memberId");
        String nickname= (String) request.getAttribute("nickname");

        if (StringUtils.isNotBlank(memberId)){
            //已经登录查询db
           omsCartItems= cartService.cartList(memberId);
           System.out.println("登录后查询到的omsCartItems:"+omsCartItems);
        }else {
            //没有登录查询cookie
            String cartListCookie= CookieUtil.getCookieValue(request,"cartListCookie",true);
            System.out.println("没有登录查询到的cartListCookie:"+cartListCookie);
            if (StringUtils.isNotBlank(cartListCookie)){
                omsCartItems= JSON.parseArray(cartListCookie,OmsCartItem.class);
            }
        }
        //计算总价
        for (OmsCartItem omsCartItem:omsCartItems){
            omsCartItem.setIsChecked("1");
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
            System.out.println("购物车总价："+omsCartItem.getTotalPrice());
        }
        System.out.println("omsCartItem.setIsChecked(\"1\")后购物车清单："+omsCartItems);
        modelMap.put("cartList",omsCartItems);
        //被勾选商品的总额
        BigDecimal totalAmount=getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);

        return "cartList";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        System.out.println("查询购物车总数："+omsCartItems);
        BigDecimal totalAmount=new BigDecimal("0");
        for (OmsCartItem omsCartItem:omsCartItems){
            System.out.println("购物车中的商品："+omsCartItem);
            BigDecimal totalPrice= omsCartItem.getTotalPrice();
            System.out.println("totalPrice:"+totalPrice);
            if (omsCartItem.getIsChecked().equals("1")){
                totalAmount=totalAmount.add(totalPrice);
            }

        }
        System.out.println("返回");
        return totalAmount;
    }

    //添加商品到购物车
    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){
        List<OmsCartItem> omsCartItems=new ArrayList<>();
        //调用商品服务查询商品信息
        PmsSkuInfo pmsSkuInfo= skuService.getSkuById(skuId,"");
        System.out.println("加入购物车中的商品信息："+pmsSkuInfo);
        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem=new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(pmsSkuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("11111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));
        omsCartItem.setIsChecked("1");


        System.out.println("封装后的购物车信息："+omsCartItem);
        //判断用户是否登录
        String memberId= (String) request.getAttribute("memberId");
        String nickname= (String) request.getAttribute("nickname");
        System.out.println("memberId:"+memberId+";nickname:"+nickname);
        if (StringUtils.isBlank(memberId)){
            //用户没有登录

            //cookie里原有的购物车数据
            String cartListCookie= CookieUtil.getCookieValue(request,"cartListCookie",true);
            System.out.println("cookie里原有的购物车数据："+cartListCookie);
            if(StringUtils.isBlank(cartListCookie)){
                //cookie为空
                omsCartItems.add(omsCartItem);
                System.out.println("未登录且cookie为空，添加商品："+omsCartItem+"集合中。");
            }else {
                //cookie不为空
                omsCartItems= JSON.parseArray(cartListCookie,OmsCartItem.class);
                //判断添加的购物车数据在cookie中是否存在
                boolean exist= if_cart_exist(omsCartItems,omsCartItem);
                System.out.println("未登录，cookie不为空，判断该商品信息是否在cookie中："+exist);
                if (exist){
                    //之前添加过，更新购物车数量
                    System.out.println("添加过该商品，更新购物车数量");
                    for (OmsCartItem cartItem:omsCartItems){
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                            cartItem.setPrice(cartItem.getPrice().add(omsCartItem.getPrice()));

                        }
                    }
                }else {
                    //之前没有添加，新增当前购物车
                    System.out.println("没有添加过该商品，添加到购物车！");
                    omsCartItems.add(omsCartItem);
                    System.out.println("添加购物车成功！");
                    //cartService.addCart(omsCartItem);
                }
            }
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);
        }else {
            //用户已经登录
            System.out.println("用户已经登录，从购物车中查询商品信息！");
            //从db中查出购物车数据
            OmsCartItem omsCartItemFromDb= new OmsCartItem();
            omsCartItemFromDb=cartService.ifCartExistByUser(memberId,skuId);
            System.out.println("从db中查出的购物车数据："+omsCartItemFromDb);
            if (omsCartItemFromDb==null){
                //该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname("test林");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                System.out.println("设置必要数据并加入到购物车："+omsCartItem);
                cartService.addCart(omsCartItem);
                System.out.println("成功加入到购物车！");


            }else {
                //该用户添加过当前商品
                System.out.println("该用户添加过当前商品！");
                omsCartItemFromDb.setQuantity(omsCartItem.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);
                System.out.println("已经更新数量！");
            }
            //同步缓存
            cartService.flushCartCache(memberId);
        }

        return "redirect:/success.html";
    }

    //判断购物车是否存在
    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean b=false;
        for (OmsCartItem cartItem:omsCartItems){
            String productSkuId=cartItem.getProductSkuId();
            if (productSkuId.equals(omsCartItem.getProductId())){
                b=true;
            }
        }
        return b;
    }
    //返回一个inner内嵌页面给ajax,我们返回的新的页面刷新替换掉原来老的页面
    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isChecked,String skuId,HttpServletRequest request,HttpServletResponse response,HttpSession session,ModelMap modelMap){
        String memberId= (String) request.getAttribute("memberId");
        String nickname= (String) request.getAttribute("nickname");
        System.out.println("inner内嵌页面："+memberId+","+nickname);
        //调用服务修改状态
        OmsCartItem omsCartItem=new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        System.out.println("修改服务状态："+omsCartItem);
        cartService.checkCart(omsCartItem);
        //将最新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> omsCartItems= cartService.cartList(memberId);
        System.out.println("最新的数据："+omsCartItems);
        modelMap.put("cartList",omsCartItems);
        //被勾选商品的总额
        BigDecimal totalAmount=getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartListInner";
    }

    /**
     * 去结算
     * @param request
     * @param response
     * @param session
     * @param modelMap
     * @return
     */
    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response,HttpSession session,ModelMap modelMap){
        String memberId= (String) request.getAttribute("memberId");
        String nickname= (String) request.getAttribute("nickname");
        System.out.println("memberId:"+memberId+";nickename:"+nickname);

        return "toTrade";
    }
}
