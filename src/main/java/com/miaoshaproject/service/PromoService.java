package com.miaoshaproject.service;

import com.miaoshaproject.service.model.PromoModel;

//秒杀服务
public interface PromoService {

    //根据商品id获取该商品秒杀信息
    PromoModel getPromoByItemId(Integer itemId);


    //判断当前时间是否秒杀活动即将开始或正在进行


}
