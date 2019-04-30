package com.ysicen.mvcframework.servlet.v1;

/**
 * Created by zhangling on 2019/4/26.
 */
import com.ysicen.annotation.SICENAutowired;
import com.ysicen.annotation.SICENController;
import com.ysicen.annotation.SICENRequestMapping;
import com.ysicen.annotation.SICENService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author zhangling
 * @create 2019/04/26
 **/
public class SICENDispatcherServlet extends HttpServlet{

    private Map<String,Object> mapping = new HashMap<String, Object>();

    private Map<String,Object> valueMapping = new HashMap<String, Object>();

    private Map<String,Object> matchingMapping = new HashMap<String, Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        //请求方法路径
        String requestURI = req.getRequestURI();
        //请求项目路径
        String contextPath = req.getContextPath();

        String integratedUrl = requestURI.replace(contextPath, "").replaceAll("/+", "/");

        if (!matchingMapping.containsKey(integratedUrl)){
            resp.getWriter().write("404 Not Found");
            return;
        }

        Method method = (Method)matchingMapping.get(integratedUrl);

        Map<String, String[]> params = req.getParameterMap();

        method.invoke(this.matchingMapping.get(method.getDeclaringClass().getName()),new
                Object[]{req,resp, params.get("name")[0]});

    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        InputStream is = null;

        try {

            Properties configContext = new Properties();

            is = this.getClass().getClassLoader().getResourceAsStream(
                    config.getInitParameter("contextConfigLocation"));

            configContext.load(is);

            String scanPackage = configContext.getProperty("scanPackage");

            doScanner(scanPackage);

            valueMapping.putAll(mapping);

            for (String className : mapping.keySet()){

                if (!className.contains(".")){
                    continue;
                }

                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(SICENController.class)){
                    valueMapping.put(className, clazz.newInstance());

                    String baseUrl = "";

                    if (clazz.isAnnotationPresent(SICENRequestMapping.class)){
                        SICENRequestMapping requestMapping = clazz.getAnnotation(SICENRequestMapping.class);
                        baseUrl = requestMapping.value();
                    }

                    Method[] methods = clazz.getMethods();

                    for (Method method : methods){
                        if (!method.isAnnotationPresent(SICENRequestMapping.class)){
                            continue;
                        }
                        SICENRequestMapping requestMapping = method.getAnnotation(SICENRequestMapping.class);
                        String url = (baseUrl + requestMapping.value()).replace("/+", "/");
                        valueMapping.put(url, method);
                        System.out.println("Mapped " + url + "," + method);
                    }
                }else if (clazz.isAnnotationPresent(SICENService.class)){
                    SICENService service = clazz.getAnnotation(SICENService.class);
                    String beanName = service.value();
                    if ("".equals(beanName)){
                        beanName = clazz.getName();
                    }
                    Object instance = clazz.newInstance();
                    valueMapping.put(beanName, instance);

                    for (Class<?> i : clazz.getInterfaces()){
                        valueMapping.put(i.getName(), instance);
                    }
                }else {
                    continue;
                }
            }

            for (Object object : valueMapping.values()){
                if (object == null){
                    continue;
                }

                Class<?> clazz = object.getClass();

                if (clazz.isAnnotationPresent(SICENController.class)){

                    Field[] fields = clazz.getDeclaredFields();

                    for (Field field : fields){

                        if (!field.isAnnotationPresent(SICENAutowired.class)){
                            continue;
                        }

                        SICENAutowired autowired = field.getAnnotation(SICENAutowired.class);

                        String beanName = autowired.value();

                        if ("".equals(beanName)){
                            beanName = field.getType().getName();
                        }
                        field.setAccessible(true);
                           try {
                               field.set(valueMapping.get(clazz.getName()), valueMapping.get(beanName));
                           }catch (Exception e){
                                e.printStackTrace();
                           }
                    }
                }

                matchingMapping.putAll(valueMapping);
            }

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (is != null){
                try {
                    is.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

        System.out.println("SICEN MVC Framework is init");
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" +
                scanPackage.replaceAll("\\.","/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            if(file.isDirectory()){ doScanner(scanPackage + "." + file.getName());}else {
                if(!file.getName().endsWith(".class")){continue;}
                String clazzName = (scanPackage + "." + file.getName().replace(".class",""));
                mapping.put(clazzName,null);
            }
        }
    }

}
