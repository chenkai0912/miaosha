package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.OrderService;

import com.miaoshaproject.service.model.UserModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Controller("order")
@RequestMapping("/order")
@CrossOrigin(origins = {"*"},allowCredentials = "true")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;


    //封装下单请求，接收商品id和数量
    @RequestMapping(value = "/createorder",method = {RequestMethod.POST})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId",required = false) Integer promoId) throws BusinessException {

        //从session中拿出登录标记
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");

        if(isLogin==null||!isLogin.booleanValue()){

            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);

        }

        //获取用户登录信息
        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");


        orderService.createOrder(userModel.getId(),itemId,promoId,amount);

        return CommonReturnType.create(null);

    }


}
