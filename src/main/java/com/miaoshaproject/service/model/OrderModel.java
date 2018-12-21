package com.miaoshaproject.service.model;

import java.math.BigDecimal;

//用户下单的交易模型
//一个订单是交易模型的一部分
public class OrderModel {

    //String类型的交易号（订单号）
    //201812023349823723 一般可以以时间开头，后面每一位都有特殊含义
    private String id;


    //用户id，哪个用户下的单
    private Integer userId;

    //商品id，买的哪个商品
    private Integer itemId;

    //购买的数量
    private Integer amount;

    //购买的金额（商品下单时的单价X数量）
    //商品的价格是浮动的，后期价格会改变，但创建订单时的价格是不随后期实际价格改变的
    //因此这里需要一个额外字段，存下单时的价格，即使后期实际价格变化，查询这个订单时，价格是当时的价格
    private BigDecimal orderPrice;

    //创建订单时商品的单价
    private BigDecimal itemPrice;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }
}
