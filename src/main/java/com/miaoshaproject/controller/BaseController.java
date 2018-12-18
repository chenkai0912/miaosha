package com.miaoshaproject.controller;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class BaseController {

    //定义内容类型常量
    public static final String CONTENT_TYPE_FORMED = "application/x-www-form-urlencoded";


    //定义exceptionhandler解决未被Controller层吸收的exception
    //指定为异常的父类，会调用我们自定义的BusinessException（继承了Exception）
    //上面当用户不存在时，抛出了自定义的异常BusinessException，加这个注解，会被注解标记的方法捕获。
    @ExceptionHandler(Exception.class)
    //一些异常是因为请求逻辑导致，而非服务器本身内部处理异常，这时服务器端是接受了请求，而在返回时发生异常，这时服务器接受请求
    //的状态是成功的，此时再处理请求逻辑异常,将会进入这个方法处理。需要ResponseStatus注解
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Object handleException(HttpServletRequest request, Exception ex){

        //创建一个Map进行封装
        Map<String,Object> responseData = new HashMap<>();

        //如果捕获到的异常是我们抛出的异常对象
        if(ex instanceof BusinessException){
            //强转为自定义类型
            BusinessException businessException = (BusinessException) ex;

            responseData.put("errCode",businessException.getErrCode());
            responseData.put("errMsg",businessException.getErrMsg());

            //捕获到异常后，进入这个方法中处理，需要做的是在这个方法内封装异常返回信息给页面
            //调用自定义的统一数据json格式返回

        }else{
            //从枚举类中取出自定义的错误码和错误信息
            responseData.put("errCode", EmBusinessError.KNOWN_ERROR.getErrCode());
            responseData.put("errMsg",ex.getMessage());

        }

        //将map封装后的错误信息传入，统一返回
        return CommonReturnType.create(responseData,"fail");

    }
}
