package com.miaoshaproject.response;

public class CommonReturnType {

    //表明对应请求的返回处理结果“success”或者“fail”，成功或失败
    private String status;

    //若status=success，则data内返回前端需要的json数据
    //若status=fail,则data内使用通用的错误码格式
    private Object data;

    //定义一个通用的创建方法
    //Controller处理完数据后，调这个静态方法，传入返回数据和对应的状态码
    public static CommonReturnType create(Object result,String status){
        CommonReturnType type = new CommonReturnType();
        type.setStatus(status);
        type.setData(result);
        return type;
    }

   //定义一个默认的成功的处理，只传数据的方法，默认认为是请求正确的
    public static CommonReturnType create(Object result){
        return CommonReturnType.create(result,"success");
    }


    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}
