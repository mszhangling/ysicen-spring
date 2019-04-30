package com.ysicen.controller;/**
 * Created by zhangling on 2019/4/26.
 */

import com.ysicen.annotation.SICENAutowired;
import com.ysicen.annotation.SICENController;
import com.ysicen.annotation.SICENRequestMapping;
import com.ysicen.annotation.SICENRequestParam;
import com.ysicen.service.interfaces.SICENDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author zhangling
 * @create 2019/04/26
 **/
@SICENController
@SICENRequestMapping("/demo")
public class SICENDemoController {

    @SICENAutowired
    SICENDemoService demoService;

    @SICENRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
                     @SICENRequestParam("name") String name){

        String result = demoService.name(name);
        try {
            response.getWriter().write(result);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @SICENRequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response,
                     @SICENRequestParam("name") String name,
                     @SICENRequestParam("age") Integer age){

        try {
            response.getWriter().write("姓名：" + name + "，年龄：" + age);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @SICENRequestMapping("/remove")
    public void remove(HttpServletRequest request, HttpServletResponse response,
                     @SICENRequestParam("id") Integer id){

        try {

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
