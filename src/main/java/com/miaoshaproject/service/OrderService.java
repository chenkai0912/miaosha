package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.OrderModel;

public interface OrderService {

    //创建订单
    //需要用户的id，商品的id，商品的数量
    OrderModel createOrder(Integer userId,Integer itemId,Integer amount) throws BusinessException;
}
