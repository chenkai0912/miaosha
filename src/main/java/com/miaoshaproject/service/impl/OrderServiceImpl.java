package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderDoMapper;
import com.miaoshaproject.dao.SequenceDoMapper;
import com.miaoshaproject.dataobject.OrderDo;
import com.miaoshaproject.dataobject.SequenceDo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDoMapper orderDoMapper;

    @Autowired
    private SequenceDoMapper sequenceDoMapper;


    @Override
    @Transactional
    //需要用户id，商品id，购买数量
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount) throws BusinessException {

        //1.校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确。
        //判断商品是否存在
        ItemModel itemModel = itemService.getItemById(itemId);
        if(itemModel==null){
            throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }
        //判断用户是否合法
        UserModel userModel = userService.getUserById(userId);
        if(userModel==null){
            throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
        }

        //判断购买数量是否合法
        if(amount<=0||amount>=99){
            throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不合法");
        }

        //2.落单减库存，支付减库存。
        //采用落单减库存，itemService中提供一个减库存的方法
        boolean result = itemService.decreaseStock(itemId, amount);
        //返回false，库存不够扣的，下单数量大于库存量
        if(!result){
            throw  new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //否则，扣减库存正常开始
        //3.订单入库
        //创建OrderModel对象，封装数据。
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setItemPrice(itemModel.getPrice());
        //订单总价 商品单价X数量
        orderModel.setOrderPrice(itemModel.getPrice().multiply(new BigDecimal(amount)));

        //生成交易流水号（订单号）
        orderModel.setId(generateOrderNo());

        //将OrderModel转为OrderDo
        OrderDo orderDo = convertFromOrderModel(orderModel);
        //保存订单
        orderDoMapper.insertSelective(orderDo);

        //增加该商品销量
        itemService.increaseSales(itemId,amount);

        //4.返回前端
        return orderModel;
    }

    /*
        为了保证订单号的全剧唯一，在生成方法中加注解，并设置传播机制是开新_NEW
        这样即使外层创建订单操作失败要回滚，里面的生成订单方法都会生成一个新的事务进行提交。
        这样下一个事务操作订单生成，会拿到最新的，而不是回滚时生成过的
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generateOrderNo(){

        StringBuffer sb = new StringBuffer();
        //订单号16位
        //前8位为时间信息，年月日，归档记录切分点
        LocalDateTime now = LocalDateTime.now();
        //格式化后的格式是2018-12-12带横线的，将-去掉
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        sb.append(nowDate);

        //中间6位为自增序列，某一天的某一个时间点，订单号不重复
        //数据库中创建一张自增序列表sequence_info
        //从sequence_info表中获取当前序列值
        int sequence = 0;
        SequenceDo sequenceDo = sequenceDoMapper.getSequenceByName("order_info");

        //获取库中当前的序列值
        sequence = sequenceDo.getCurrentValue();
        //获取当前之后，生成新的，步长+1
        sequenceDo.setCurrentValue(sequenceDo.getCurrentValue()+sequenceDo.getStep());
        //之后马上更新表中的sequence
        sequenceDoMapper.updateByPrimaryKeySelective(sequenceDo);

        //凑足6位拼接序列
        String sequenceStr = String.valueOf(sequence);
        //序列值前面的几位补0
        for(int i=0;i<6-sequenceStr.length();i++){
            sb.append(0);
        }
        //将序列拼接上去
        //000001
        sb.append(sequenceStr);

        //最后2位为分库分表位，00-99，分库分表，订单水平拆分
        //订单信息落到拆分后的100个库的100张表中，分散数据库从查询和落单压力
        //订单号不变，这条订单记录一定会落到某一个库的某一张表上
//        Integer userId = 1000122;
//        userId % 100
        //暂时写死
        sb.append("00");

        return sb.toString();
    }

    //将Model转为dataObject
    private OrderDo convertFromOrderModel(OrderModel orderModel){
        if(orderModel==null){
            return null;
        }
        OrderDo orderDo = new OrderDo();
        BeanUtils.copyProperties(orderModel,orderDo);
        //单独处理商品价格和订单金额
        orderDo.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDo.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDo;
    }
}
