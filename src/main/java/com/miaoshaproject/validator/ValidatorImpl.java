package com.miaoshaproject.validator;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

//实现初始化bean接口
@Component
public class ValidatorImpl implements InitializingBean {

    private Validator validator;

    //实现校验方法并返回校验结果
    public ValidationResult validate(Object bean){
        //创建自定义的验证结果对象，用来封装是否错误和错误信息
        ValidationResult result = new ValidationResult();

        //调验证器的验证方法，返回一个set
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);
        if(constraintViolationSet.size()>0){
            //bean中有错误
            result.setHasErrors(true);
            //遍历set
            constraintViolationSet.forEach(constraintViolation->{
                //获取bean的属性上注解定义的错误信息
                String errMsg = constraintViolation.getMessage();
                //获取是哪个属性有错误
                String propertyName = constraintViolation.getPropertyPath().toString();
                //将错误信息和对应的属性放入错误map里
                result.getErrorMsgMap().put(errMsg,propertyName);
            });
        }
        //将这个map返回
        return result;
    }

    //在bean初始化属性之后，调用这个方法
    @Override
    public void afterPropertiesSet() throws Exception {

        //将hibernate validator通过工厂的初始化方法使其实例化
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }
}
