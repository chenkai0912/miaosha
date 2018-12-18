package com.miaoshaproject.controller;

import com.alibaba.druid.util.StringUtils;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;


//指定创建的Controller的Bean名称为user
@Controller("user")
@RequestMapping("/user")
//
@CrossOrigin(allowCredentials = "true",allowedHeaders = "*")
public class UserController extends BaseController{

    @Autowired
    private UserService userService;

    /*
    这里注入request，也是经过spring包装后的代理对象，可以使用threadlocal的map，
    每个用户只处理自己线程中的数据
     */
    @Autowired
    private HttpServletRequest httpServletRequest;



    //用户登录模块
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name="telephone") String telephone,
                                  @RequestParam(name="password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //入参校验
        if(org.apache.commons.lang3.StringUtils.isEmpty(telephone)||
                org.apache.commons.lang3.StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登录服务，校验用户登录是否合法
        UserModel userModel = userService.validateLogin(telephone,this.EncodeByMD5(password));

        //将登录凭证加入到用户登录成功的session内
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);

        return CommonReturnType.create(null);

    }

    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    //用户注册接口,传入用户手机号码和根据此号码生成的验证码以及用户的信息
    //这些信息会封装到userVO中
    public CommonReturnType register (
            @RequestParam(name="telephone") String telephone,
            @RequestParam(name="otpCode") String otpCode,
            @RequestParam(name="name") String name,
            @RequestParam(name="password") String password,
            @RequestParam(name="gender") Byte gender,
            @RequestParam(name="age") Integer age) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //验证手机号和对应的otpcode是否相符
        //在生成otpcode时，将验证码放入了session中，根据注册时传入的手机号，取对应的otpCode
        String inSessionOtpCode = (String)this.httpServletRequest.getSession().getAttribute(telephone);
        //将取出的otp和注册时的otp进行比对
        //使用alibaba的druid中的StringUtils工具类，equals方法本身有一个判空处理
        if(!StringUtils.equals(otpCode,inSessionOtpCode)){
            //抛自定义的异常，这里使用了自定义错误信息覆盖原错误信息。
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"短息验证码不匹配");
        }

        //用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setAge(age);
        userModel.setTelephone(telephone);
        userModel.setRegistMode("byPhone");
        //密码进行MD5加密
        userModel.setEncrptPassword(EncodeByMD5(password));

        userService.register(userModel);

        return CommonReturnType.create(null);

    }

    //对密码进行MD5加密修改
    public String EncodeByMD5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密字符串
        String newStr = base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }



    //用户获取otp短信接口
    //consumes 对应后端消费content-type名字
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam("telephone") String telephone){
        //需要按照一定的规则生成otp验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999); //取[0,99999)之间的随机数
        randomInt+=10000; //随机数范围在[10000,109999)之间
        String otpCode = String.valueOf(randomInt);


        //将otp验证码同对应用户的手机号关联
        // 一般采取key value对的方式，key是手机号，value是生成码
        // 一般都放到redis中，天生是kv结构，而且相同key覆盖value，还能指定有效期。
        //这里用session绑定手机号和生成码
        httpServletRequest.getSession().setAttribute(telephone,otpCode);

        //将otp验证码通过短信通道发送给用户,省略
        System.out.println("telephone: "+telephone+"&otpCode: "+otpCode);

        return CommonReturnType.create(null);
    }

    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam("id") Integer id) throws BusinessException {

        //调用Service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //如果获取的用户不存在
        if(userModel==null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
//        //转为UserVO后返回给前端
//        return convertFromModel(userModel);

        UserVO userVO =  convertFromModel(userModel);

        //返回通用数据格式
        return CommonReturnType.create(userVO);

    }
    //将Model转为VO
    private UserVO convertFromModel(UserModel userModel){
        if(userModel==null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }

}
