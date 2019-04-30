package com.ysicen.service.impl;/**
 * Created by zhangling on 2019/4/26.
 */

import com.ysicen.annotation.SICENService;
import com.ysicen.service.interfaces.SICENDemoService;

/**
 * DemoImpl
 *
 * @author zhangling
 * @create 2019/04/26
 **/
@SICENService
public class SICENDemoServiceImpl implements SICENDemoService {

    @Override
    public String name(String name){
        return "My Name Is " + name;
    }

}
