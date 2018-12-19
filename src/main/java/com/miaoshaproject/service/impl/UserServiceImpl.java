package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.UserDoMapper;
import com.miaoshaproject.dao.UserPasswordDoMapper;
import com.miaoshaproject.dataobject.UserDo;
import com.miaoshaproject.dataobject.UserPasswordDo;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.validator.ValidationResult;
import com.miaoshaproject.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class UserServiceImpl implements UserService {

    //注入用户Mapper组件
    @Autowired
    private UserDoMapper userDoMapper;

    //注入用户密码Mapper组件
    @Autowired
    private UserPasswordDoMapper userPasswordDoMapper;


    @Autowired
    private ValidatorImpl validator;

    @Override
    public UserModel getUserById(Integer id) {

        //调用userdomapper获取到对应的dataobject对象
        UserDo userDo = userDoMapper.selectByPrimaryKey(id);
        if(userDo==null){
            return null;
        }
        //根据用户id查询对应的用户加密密码信息
        UserPasswordDo userPasswordDo = userPasswordDoMapper.selectByUserId(userDo.getId());
        return convertFormDataObject(userDo,userPasswordDo);

    }


    @Override
    public UserModel validateLogin(String telephone, String encrptPassword) throws BusinessException {
        //通过手机号获取用户信息
        UserDo userDo = userDoMapper.selectByTelephone(telephone);
        if(userDo==null){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        UserPasswordDo userPasswordDo = userPasswordDoMapper.selectByUserId(userDo.getId());

        UserModel userModel = convertFormDataObject(userDo, userPasswordDo);

        //比对用户信息内加密的密码是否和传输进来的密码相匹配
        //传过来的密码是加密过的，和从userModel中的进行比对
        if(!StringUtils.equals(encrptPassword,userModel.getEncrptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        return userModel;
    }



    //用户注册
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {

        if(userModel==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }


        //判断userModel的各个字段
        //String类型的使用StringUtils判断
//        if(StringUtils.isEmpty(userModel.getName())
//            || userModel.getGender()==null
//            || userModel.getAge()==null
//            || StringUtils.isEmpty(userModel.getTelephone())){
//
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
//        }

        //使用validator校验userModel的各个属性是否符合校验规则
        ValidationResult validationResult = validator.validate(userModel);
        //验证userModel，如果有错误，会将这个boolean置为true
        if(validationResult.isHasErrors()){
            //抛异常，封装错误信息
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,validationResult.getErrMsg());
        }


        //都不为null，创建UserDO封装
        //实现model->dataObject方法
        UserDo userDO = convertUserModel2UserDO(userModel);

        //插入数据库记录是这一步完成的，也就是在插入重复记录时，这句代码会报异常。
        try{
            //调Mapper保存
            userDoMapper.insertSelective(userDO);

        }catch(DuplicateKeyException ex){
            throw  new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"该手机号已存在！");
        }


        //给userModel设置id
        userModel.setId(userDO.getId());


        //保存UserPasswordDo
        UserPasswordDo userPasswordDo = convertPasswordFromModel(userModel);
        userPasswordDoMapper.insertSelective(userPasswordDo);

    }




    //将userModel中的密码拿出来，封装为UserpasswordDo
    private UserPasswordDo convertPasswordFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserPasswordDo userPasswordDo = new UserPasswordDo();
        userPasswordDo.setEncrptPassword(userModel.getEncrptPassword());
        //设置该密码对应的用户id
        userPasswordDo.setUserId(userModel.getId());
        return userPasswordDo;
    }

    //将userModel转为userDo
    private UserDo convertUserModel2UserDO(UserModel userModel) {
        if(userModel==null){
            return null;
        }
        UserDo userDo = new UserDo();
        BeanUtils.copyProperties(userModel,userDo);
        return userDo;
    }


    private UserModel convertFormDataObject(UserDo userDo, UserPasswordDo userPasswordDo){

        if(userDo==null){
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDo,userModel);
        //拷贝完UserDO后，还要设置密码
        if(userPasswordDo!=null){
        userModel.setEncrptPassword(userPasswordDo.getEncrptPassword());
        }
        //将这个UserModel返回
        return userModel;
    }



}
