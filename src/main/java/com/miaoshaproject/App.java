package com.miaoshaproject;

import com.miaoshaproject.dao.UserDoMapper;
import com.miaoshaproject.dataobject.UserDo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 启动web项目
 *
 */
//开启springboot的自动化配置
//指定要扫描的包，会自动扫描此包及子包加了注解的类，创建组件放入容器
@SpringBootApplication(scanBasePackages = {"com.miaoshaproject"})
@RestController
//指定要扫描的Mapper接口包，会创建接口的实现类
@MapperScan("com.miaoshaproject.dao")
public class App 
{

    @Autowired
    private UserDoMapper userDoMapper;

    /**
     * 指定一个请求
     * @return
     */
    @RequestMapping("/")
    public String home(){

        UserDo userDo = userDoMapper.selectByPrimaryKey(1);
        if(userDo==null){
            return "用户对象不存在！";
        }else{
            return userDo.getName();
        }


    }

    public static void main( String[] args )
    {
        SpringApplication.run(App.class,args);
    }





}
