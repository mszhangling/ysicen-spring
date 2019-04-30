package com.ysicen.annotation;

import java.lang.annotation.*;

/**
 * Created by zhangling on 2019/4/26.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SICENRequestParam {

    String value() default "";

}
