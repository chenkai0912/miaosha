package com.miaoshaproject.error;

public enum EmBusinessError implements CommonError {

    //通用错误类型00001
    PARAMETER_VALIDATION_ERROR(10001,"参数不合法"),

    //20000开头为用户信息相关错误定义
    USER_NOT_EXIST(20001,"用户不存在"),
    //用户还未登陆，不能下单
    USER_NOT_LOGIN(20002,"用户还未登陆，不能下单！"),

    //自定义未知错误
    KNOWN_ERROR(10002,"未知错误"),

    //自定义未知错误
    USER_LOGIN_FAIL(20002,"手机号或密码错误！"),

    //30000开头，交易相关的错误
    STOCK_NOT_ENOUGH(30001,"库存不足"),

    ;


    private int errCode;
    private String errMsg;

    private EmBusinessError(int errCode,String errMsg){
        this.errCode = errCode;
        this.errMsg = errMsg;
    }
    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
