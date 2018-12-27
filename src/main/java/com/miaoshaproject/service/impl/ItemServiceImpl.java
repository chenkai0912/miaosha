package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.ItemDoMapper;
import com.miaoshaproject.dao.ItemStockDoMapper;
import com.miaoshaproject.dataobject.ItemDo;
import com.miaoshaproject.dataobject.ItemStockDo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;


import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {

    //引入自定义的校验器
    @Autowired
    private ValidatorImpl validator;


    //注入ItemMapper组件
    @Autowired
    private ItemDoMapper itemDoMapper;

    //注入ItemStockMapper组件
    @Autowired
    private ItemStockDoMapper itemStockDoMapper;

    //注入秒杀活动组件
    @Autowired
    private PromoService promoService;

    //创建商品，需要事务，在方法上添加
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {

        //首先进行入参校验
        ValidationResult validationResult = validator.validate(itemModel);

        if(validationResult.isHasErrors()){

            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,validationResult.getErrMsg());
        }

        //将ItemModel转为ItemDo（dataObject）
        ItemDo itemDo = convertItemDoFromItemModel(itemModel);

        //将ItemDo写入数据库
        //写入后返回了itemDo的id
        itemDoMapper.insertSelective(itemDo);
        //将id给itemModel
        itemModel.setId(itemDo.getId());
        //itemModel转itemStockDo
        ItemStockDo itemStockDo = convertItemStockDoFromItemModel(itemModel);

        //将ItemStockdo写入数据库
        itemStockDoMapper.insertSelective(itemStockDo);


        //返回创建完成的对象,通过调getItemById完成
        return this.getItemById(itemModel.getId());
    }

    //将ItemModel转为ItemDo的转换方法
    private ItemDo convertItemDoFromItemModel(ItemModel itemModel){

        if(itemModel==null){
            return null;
        }

        ItemDo itemDo = new ItemDo();

        //UserModel中的price是BigDecimal类型而不用Double，Double在java内部传到前端，会有精度问题，不精确
        //有可能1.9，显示时是1.999999，为此在Service层，将price定为比较精确的BigDecimal类型
        //但是在拷贝到Dao层时，存入的是Double类型，拷贝方法对应类型不匹配的属性，不会进行拷贝。
        //在拷贝完，将BigDecimal转为Double，再set进去
        BeanUtils.copyProperties(itemModel,itemDo);
        //转为double
        itemDo.setPrice(itemModel.getPrice().doubleValue());
        return itemDo;
    }

    //从itemModel中取stock和id转为ItemStockDo方法
    private ItemStockDo convertItemStockDoFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemStockDo itemStockDo = new ItemStockDo();
        itemStockDo.setItemId(itemModel.getId());
        itemStockDo.setStock(itemModel.getStock());
        return itemStockDo;
    }

    @Override
    public List<ItemModel> listItem() {

        List<ItemDo> itemDoList = itemDoMapper.listItem();
        //遍历List，每一个itemDo转为ItemModel
        List<ItemModel> itemModelList = itemDoList.stream().map(itemDo -> {
            //加入itemstockDo，获取库存
            ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());
            ItemModel itemModel = this.convertModelFromDataObject(itemDo,itemStockDo);

            return itemModel;
            //转为List集合
        }).collect(Collectors.toList());

        return itemModelList;
    }

    /**
     * 根据商品id查询商品
     * @param id
     * @return
     * 先查出itemDo
     * 再查出对应的stock，封装成itemModel
     */
    @Override
    public ItemModel getItemById(Integer id) {

        ItemDo itemDo = itemDoMapper.selectByPrimaryKey(id);
        if(itemDo==null){
            return null;
        }
        //根据item_id查出stock
        ItemStockDo itemStockDo = itemStockDoMapper.selectByItemId(itemDo.getId());
        ItemModel itemModel = convertModelFromDataObject(itemDo, itemStockDo);

        //获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());

        //如果存在该商品秒杀对象并且秒杀状态不等于3,说明秒杀有效
        if(promoModel!=null && promoModel.getStatus().intValue()!=3){
            //将秒杀对象聚合进ItemModel，将该商品和秒杀对象关联起来
            itemModel.setPromoModel(promoModel);
        }


        return itemModel;

    }


    //扣减库存
    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        /*
            item商品表大部分用户查询，查询对应的商品信息
            库存表，在某些高压力的情况下做降级
            比如在微服务下，库存服务可以拆为item的展示服务（item表）和item的库存服务（item_stock表）
            这个item的库存服务独立出来，专门进行库存减操作。
            目前只操作item_stock表，为保证冻结操作的原子性，对item_stock表加锁，针对某一条记录进行加行锁，减掉对应的库存
            看减完后是否还大于表中库存。

            修改itemStockDoMapper映射文件，修改sql语句

         */
        //返回影响的条目数
        //sql成功执行返回的影响条目数不一定为1，如果购买数量大于库存，超卖，sql语句也会执行，但返回的就是0
        int affectRow = itemStockDoMapper.decreaseStock(itemId, amount);
        if(affectRow>0){
            //更新库存成功
            return true;
        }else{
            return false;
        }

    }

    /**
     * 商品销量增加
     * @param id
     * @param amount
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void increaseSales(Integer id, Integer amount) throws BusinessException {

        itemDoMapper.increaseSales(id,amount);

    }

    //将dataobject转换成Model领域模型
    private ItemModel convertModelFromDataObject(ItemDo itemDo,ItemStockDo itemStockDo){

        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDo,itemModel);
        itemModel.setPrice(new BigDecimal(itemDo.getPrice()));
        itemModel.setStock(itemStockDo.getStock());

        return itemModel;

    }
}
