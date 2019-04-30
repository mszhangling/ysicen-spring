package com.ysicen.mvcframework.servlet.v2;

/**
 * Created by zhangling on 2019/4/26.
 */
import com.ysicen.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author zhangling
 * @create 2019/04/26
 * 采用了常用的设计模式（工厂模式、单例模式、委派模式、策略模式）
 **/
public class SICENDispatcherServlet extends HttpServlet{

    //保存application.properties配件文件中的内容
    private Properties contextConfig = new Properties();

    //保存扫描的所有的类名
    private List<String> classNames = new ArrayList<String>();

    //为了简化程序，暂时不考虑ConcurrentHashMap
    //主要是关注设计思想和原理
    // IOC 容器就是注册时单例的具体案例：
    private Map<String,Object> ioc = new HashMap<String, Object>();

    //保存url和Method的关系
    private Map<String, Method> handlerMapping = new HashMap<String,Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            //委派模式，委派模式的具体逻辑在 doDispatch()方法中：
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
        }
    }

    //委派
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Method method = this.handlerMapping.get(url);

        //第一个参数：方法所在的实例
        //第二个参数：调用时所需要的实参
        Map<String, String[]> params = req.getParameterMap();

        //投机取巧方式
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        method.invoke(ioc.get(beanName), new Object[]{req, resp, params.get("name")[0]});

    }

    //优化doDispatch
    private void doOptimizedDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        Method method = this.handlerMapping.get(url);
        //第一个参数：方法所在的实例
        //第二个参数：调用时所需要的实参
        Map<String,String[]> params = req.getParameterMap();
        //获取方法的形参列表
        Class<?> [] parameterTypes = method.getParameterTypes();
        //保存请求的 url 参数列表
        Map<String,String[]> parameterMap = req.getParameterMap();
        //保存赋值参数的位置
        Object [] paramValues = new Object[parameterTypes.length];
        //按根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i ++){
            Class parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
                continue;
            }else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
                continue;
            }else if(parameterType == String.class){
                //提取方法中加了注解的参数
                Annotation[] [] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length ; j ++) {
                    for(Annotation a : pa[i]){
                        if(a instanceof SICENRequestParam){
                            String paramName = ((SICENRequestParam) a).value();
                            if(!"".equals(paramName.trim())){
                                String value = Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s",",");
                                paramValues[i] = value;
                            }
                        }
                    }
                }
            }
        }
        //投机取巧的方式
        //通过反射拿到 method 所在 class，拿到 class 之后还是拿到 class 的名称
        //再调用 toLowerFirstCase 获得 beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});
    }



    /**
     * 初始化
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3.初始化扫描到的类，并且将它们放入到IOC容器中
        doInstance();

        //4.完成依赖注入
        doAutowired();

        //5.初始化HandlerMapping
        initHandleMapping();

        System.out.println("SICEN MVC Framework is init");

    }

    //加载配置文件
    private void doLoadConfig(String contextConfigLocation){
        //直接从类路径下找到Spirng配置文件所在的路径
        //并且将其读取出来放到Properties对象中
        //相对于scanPackage=com.ysicen 从文件中保存到了内存中
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(fis);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (null != fis){
                try {
                    fis.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //扫描相应的类
    private void doScanner(String scanPackage) {
        //scanPackage = com.ysicen，存储的是包路径
        //转换为文件路径，实际上就是把.替换为/，就OK了
        //classpath
        URL url = this.getClass().getClassLoader().getResource("/" +
                scanPackage.replaceAll("\\.","/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            if(file.isDirectory()){ doScanner(scanPackage + "." + file.getName());}else {
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String clazzName = (scanPackage + "." + file.getName().replace(".class",""));
                classNames.add(clazzName);
            }
        }
    }

    //doInstance()方法就是工厂模式的具体实现
    private void doInstance(){
        //初始化，为DI做准备
        if (classNames.isEmpty()){
            return;
        }

        try {

            for (String className : classNames){
                Class<?> clazz = Class.forName(className);

                //什么样的类才需要初始化呐？ -》 加了注解的类，才初始化，怎么判断呐？
                //为了简化代码逻辑，主要体会设计思想，只距离 @Controller 和 @Service
                if (clazz.isAnnotationPresent(SICENController.class)){
                    Object instance = clazz.newInstance();
                    //Spring默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                }else if (clazz.isAnnotationPresent(SICENService.class)){
                    //1.自定义的beanName
                    SICENService service = clazz.getAnnotation(SICENService.class);
                    String beanName = service.value();
                    //2.默认类名首字母小写
                    if ("".equals(beanName.trim())){
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();

                    ioc.put(beanName, instance);

                    //3.根据类型自动赋值，投机取巧的方式
                    for (Class<?> i : clazz.getInterfaces()){
                        if (ioc.containsKey(i.getName())){
                            throw new Exception("The [" + i.getName() + "] is exists");
                        }
                        //把接口的类型直接当成key
                        ioc.put(i.getName(), instance);
                    }
                }else {
                    continue;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //如果类名本身是小写字母，确实会出问题
    //但是我要说明的是：这个方法是我自己用，private 的
    //传值也是自己传，类也都遵循了驼峰命名法
    //默认传入的值，存在首字母小写的情况，也不可能出现非字母的情况
    //为了简化程序逻辑，就不做其他判断了，大家了解就 OK
    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        //之所以加，是因为大小写字母的 ASCII 码相差 32，
        // 而且大写字母的 ASCII 码要小于小写字母的 ASCII 码
        //在 Java 中，对 char 做算学运算，实际上就是对 ASCII 码做算学运算
        chars[0] += 32;
        return String.valueOf(chars);
    }

    //自动依赖注入
    private void doAutowired(){

        if (ioc.isEmpty()){
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()){
            //Declared 所有的，特定的字段，包括private/protected/default
            //正常来说，普通的OOP编程只能拿到public的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields){
                if (!field.isAnnotationPresent(SICENAutowired.class)){
                    continue;
                }
                SICENAutowired autowired = field.getAnnotation(SICENAutowired.class);
                //如果用户没有自定义beanName，默认就根据类型注入

                //这个地方省去了对类名首字母小写的判断

                String beanName = autowired.value().trim();
                if ("".equals(beanName)){
                    //获得接口的类型，作为key，待会拿这个 key 到 ioc 容器中去取值
                    beanName = field.getType().getName();
                }
                //如果是public以外的修饰符，只要加了@Autowired 注解，都要强制赋值
                //反射中叫做暴力访问
                field.setAccessible(true);

                try {
                    //用反射机制，动态给字段赋值
                    field.set(entry.getValue(), ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    //初始化url和Method的一对一对应关系
    // initHandlerMapping()方法，handlerMapping 就是策略模式的应用案例：
    private void  initHandleMapping(){
        if (ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(SICENController.class)){
                continue;
            }

            //保存写在类上面的@SICENRequestMapping("/demo")
            String baseUrl = "";
            if (clazz.isAnnotationPresent(SICENRequestMapping.class)){
                SICENRequestMapping requestMapping = clazz.getAnnotation(SICENRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            for (Method method : clazz.getMethods()){
                if (!method.isAnnotationPresent(SICENRequestMapping.class)){
                    continue;
                }
                SICENRequestMapping requestMapping = method.getAnnotation(SICENRequestMapping.class);
                //优化
                // //demo///query
                String url = ("/" + baseUrl + "/" + requestMapping.value())
                        .replaceAll("/+","/");
                handlerMapping.put(url,method);

                System.out.println("Mapped :" + url + "," + method);
            }
        }
    }



}
