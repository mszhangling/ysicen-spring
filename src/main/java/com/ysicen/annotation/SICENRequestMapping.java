package com.ysicen.annotation;

import java.lang.annotation.*;

/**
 * Created by zhangling on 2019/4/26.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SICENRequestMapping {

    String value() default "";

}
