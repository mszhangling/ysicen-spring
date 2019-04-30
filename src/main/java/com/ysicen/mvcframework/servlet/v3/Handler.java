package com.ysicen.mvcframework.servlet.v3;/**
 * Created by zhangling on 2019/4/29.
 */

import com.ysicen.annotation.SICENRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author zhangling
 * @create 2019/04/29
 *
 * Handler 记录 Controller 中的 RequestMapping 和 Method 的对应关系，
 * 真实的 Spring 源码中，HandlerMapping 其实是一个 List 而非 Map
 * */
public class Handler {

    //保存方法对应的实例
    protected Object controller;

    //保存映射关系
    protected Method method;

    protected Pattern pattern;

    //参数顺序
    protected Map<String, Integer> paramIndexMapping;

    /**
     * 构造一个Handler基本参数
     * @param controller
     * @param method
     * @param pattern
     */
    public Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
        paramIndexMapping = new HashMap<>();
        putParamIndexMapping(method);
    }

    private void putParamIndexMapping(Method method){
        //提取方法中加了注解的参数
        Annotation[][] pa = method.getParameterAnnotations();

        for (int i = 0; i < pa.length; i++){
            for (Annotation a :
                    pa[i]) {
                if (a instanceof SICENRequestParam){
                    String paramName = ((SICENRequestParam) a).value();
                    if (!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName, i);
                    }
                }
            }
        }

        //提取方法中的request和Respon 参数
        Class<?>[] paramsTypes = method.getParameterTypes();
        for (int i = 0; i < paramsTypes.length; i++ ){
            Class<?> type = paramsTypes[i];
            if (type == HttpServletRequest.class
                    || type == HttpServletResponse.class){
                paramIndexMapping.put(type.getName(), i);
            }
        }
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }
}
