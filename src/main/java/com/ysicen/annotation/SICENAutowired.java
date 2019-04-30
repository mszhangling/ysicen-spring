package com.ysicen.annotation;

import java.lang.annotation.*;

/**
 * Created by zhangling on 2019/4/26.
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SICENAutowired {

    String value() default "";

}
