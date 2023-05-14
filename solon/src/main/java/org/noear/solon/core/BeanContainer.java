package org.noear.solon.core;


import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.ProxyComponent;
import org.noear.solon.core.aspect.Interceptor;
import org.noear.solon.core.aspect.InterceptorEntity;
import org.noear.solon.core.exception.InjectionException;
import org.noear.solon.core.handle.HandlerLoader;
import org.noear.solon.core.runtime.AotCollector;
import org.noear.solon.core.util.ConvertUtil;
import org.noear.solon.core.util.ResourceUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Bean 容器，提供注册及关系映射管理（不直接使用；作为AopContext的基类）
 *
 * @author noear
 * @since 1.0
 * */
public abstract class BeanContainer {
    private final Props props;
    private final ClassLoader classLoader;
    private Map<Class<?>, Object> attrs = new HashMap<>();
    private final AotCollector aot = new AotCollector();


    public BeanContainer(ClassLoader classLoader, Props props) {
        this.classLoader = classLoader;
        this.props = props;
    }

    /**
     * 获取属性（写法上，更接近 Solon.cfg()）
     */
    public Props cfg() {
        if (props == null) {
            return Solon.cfg();
        } else {
            return props;
        }
    }

    public AotCollector aot(){
        return aot;
    }

    /**
     * 获取特性
     */
    public Map<Class<?>, Object> getAttrs() {
        return attrs;
    }

    /**
     * 获取类加载器
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            return JarClassLoader.global();
        } else {
            return classLoader;
        }
    }

    //////////////////////////
    //
    // 容器存储
    //
    /////////////////////////
    /**
     * bean包装库
     */
    private final Map<Class<?>, BeanWrap> beanWrapsOfType = new HashMap<>();
    private final Map<String, BeanWrap> beanWrapsOfName = new HashMap<>();
    private final Set<BeanWrap> beanWrapSet = new HashSet<>();


    /**
     * for Aot
     */


    /**
     * clz mapping
     */
    private final Map<Class<?>, Class<?>> clzMapping = new HashMap<>();


    //启动时写入
    /**
     * bean 构建器
     */
    protected final Map<Class<?>, BeanBuilder<?>> beanBuilders = new HashMap<>();
    /**
     * bean 注入器
     */
    protected final Map<Class<?>, BeanInjector<?>> beanInjectors = new HashMap<>();
    /**
     * bean 提取器
     */
    protected final Map<Class<?>, BeanExtractor<?>> beanExtractors = new HashMap<>();
    /**
     * bean 拦截器
     */
    protected final Map<Class<?>, InterceptorEntity> beanInterceptors = new HashMap<>();


    /**
     * bean 订阅者
     */
    protected final Map<Object, Set<Consumer<BeanWrap>>> beanSubscribers = new HashMap<>();
    /**
     * wrap 外部消费者
     */
    protected final Set<Consumer<BeanWrap>> wrapExternalConsumers = new LinkedHashSet<>();


    public void clear() {
        beanWrapsOfType.clear();
        beanWrapsOfName.clear();
        beanWrapSet.clear();

        clzMapping.clear();
        attrs.clear();
        aot.clear();

        wrapExternalConsumers.clear();

        //内部关系，不能清除
//        beanBuilders.clear();
//        beanInjectors.clear();
//        beanExtractors.clear();
//        beanInterceptors.clear();
//
//        beanSubscribers.clear();
    }

    /**
     * 容器能力制复到另一个容器
     */
    public void copyTo(BeanContainer container) {
        //ProxyComponent 需要二次赋值
        container.beanBuilders.remove(ProxyComponent.class);

        //构建器
        beanBuilders.forEach((k, v) -> {
            container.beanBuilders.putIfAbsent(k, v);
        });

        //注入器
        beanInjectors.forEach((k, v) -> {
            container.beanInjectors.putIfAbsent(k, v);
        });

        //拦截器
        beanInterceptors.forEach((k, v) -> {
            container.beanInterceptors.putIfAbsent(k, v);
        });

        //提取器
        beanExtractors.forEach((k, v) -> {
            container.beanExtractors.putIfAbsent(k, v);
        });
    }



    /**
     * 添加 bean builder, injector, extractor
     */
    public <T extends Annotation> void beanBuilderAdd(Class<T> annoClz, BeanBuilder<T> builder) {
        beanBuilders.put(annoClz, builder);
    }

    /**
     * 添加注入处理
     */
    public <T extends Annotation> void beanInjectorAdd(Class<T> annoClz, BeanInjector<T> injector) {
        beanInjectors.put(annoClz, injector);
    }

    /**
     * 添加提取处理
     */
    public <T extends Annotation> void beanExtractorAdd(Class<T> annoClz, BeanExtractor<T> extractor) {
        beanExtractors.put(annoClz, extractor);
    }

    /**
     * 是否有提取处理
     * */
    public boolean beanExtractorHas(Class<? extends Annotation> annoClz){
        return beanExtractors.containsKey(annoClz);
    }

    /**
     * 添加环绕处理
     *
     * @param index 执行顺序
     */
    public <T extends Annotation> void beanAroundAdd(Class<T> annoClz, Interceptor interceptor, int index) {
        beanInterceptors.put(annoClz, new InterceptorEntity(index, interceptor));
    }

    /**
     * 添加环绕处理
     */
    public <T extends Annotation> void beanAroundAdd(Class<T> annoClz, Interceptor interceptor) {
        beanAroundAdd(annoClz, interceptor, 0);
    }

    /**
     * 获取环绕处理
     */
    public <T extends Annotation> InterceptorEntity beanAroundGet(Class<T> annoClz) {
        return beanInterceptors.get(annoClz);
    }


    //////////////////////////
    //
    // bean 对内通知体系
    //
    /////////////////////////

    /**
     * bean 订阅
     */
    protected void beanSubscribe(Object nameOrType, Consumer<BeanWrap> callback) {
        if (nameOrType != null) {
            synchronized (beanSubscribers) {
                Set<Consumer<BeanWrap>> tmp = beanSubscribers.get(nameOrType);
                if (tmp == null) {
                    tmp = new LinkedHashSet<>();
                    beanSubscribers.put(nameOrType, tmp);
                }
                tmp.add(callback);
            }
        }
    }

    /**
     * wrap 外部订阅
     */
    protected void wrapExternalSubscribe(Consumer<BeanWrap> callback) {
        wrapExternalConsumers.add(callback);
    }

    /**
     * bean 通知，偏向对内
     */
    protected void beanNotice(Object nameOrType, BeanWrap wrap) {
        if (wrap.raw() == null) {
            return;
        }

        synchronized (beanSubscribers) {
            //避免在forEach时，对它进行add
            Set<Consumer<BeanWrap>> tmp = beanSubscribers.get(nameOrType);
            if (tmp != null) {
                tmp.forEach(s1 -> {
                    s1.accept(wrap);
                });
            }
        }
    }

    /**
     * wrap 发布，偏向对外 （只支持 @Bean 和 @Component 的 wrap）
     */
    protected void wrapPublish(BeanWrap wrap) {
        //避免在forEach时，对它进行add
        new ArrayList<>(wrapExternalConsumers).forEach(s1 -> {
            s1.accept(wrap);
        });
    }


    //public abstract BeanWrap wrap(Class<?> clz, Object raw);


    /**
     * 存入bean库（存入成功会进行通知）
     *
     * @param wrap 如果raw为null，拒绝注册
     */
    public synchronized void putWrap(String name, BeanWrap wrap) {
        if (Utils.isNotEmpty(name) && wrap.raw() != null) {
            if (beanWrapsOfName.containsKey(name) == false) {
                beanWrapsOfName.put(name, wrap);
                beanWrapSet.add(wrap);

                beanNotice(name, wrap);
            }
        }
    }

    /**
     * 存入到bean库（存入成功会进行通知）
     *
     * @param wrap 如果raw为null，拒绝注册
     */
    public synchronized void putWrap(Class<?> type, BeanWrap wrap) {
        if (type != null && wrap.raw() != null) {
            //
            //wrap.raw()==null, 说明它是接口；等它完成代理再注册；以@Db为例，可以看一下
            //
            if (beanWrapsOfType.containsKey(type) == false) {
                beanWrapsOfType.put(type, wrap);
                beanWrapSet.add(wrap);

                beanNotice(type, wrap);
            }
        }
    }

    public boolean hasWrap(Object nameOrType) {
        return getWrap(nameOrType) != null;
    }

    /**
     * 获取一个bean包装
     *
     * @param nameOrType bean name or type
     */
    public BeanWrap getWrap(Object nameOrType) {
        if (nameOrType instanceof String) {
            return beanWrapsOfName.get(nameOrType);
        } else {
            return beanWrapsOfType.get(nameOrType);
        }
    }

    public List<BeanWrap> getWrapsOfType(Class<?> baseType) {
        List<BeanWrap> wraps = new ArrayList<>();

        beanForeach(bw -> {
            if (baseType.isAssignableFrom(bw.clz())) {
                wraps.add(bw.raw());
            }
        });

        return wraps;
    }

    public void getWrapAsync(Object nameOrType, Consumer<BeanWrap> callback) {
        BeanWrap bw = getWrap(nameOrType);

        if (bw == null || bw.raw() == null) {
            beanSubscribe(nameOrType, callback);
        } else {
            callback.accept(bw);
        }
    }

    /**
     * 订阅某类型的 bean 包装
     *
     * @param baseType 基类
     */
    public void subWrapsOfType(Class<?> baseType, Consumer<BeanWrap> callback) {
        //获取现有的
        beanForeach(bw -> {
            if (baseType.isAssignableFrom(bw.clz())) {
                callback.accept(bw);
            }
        });

        //获取未来的
        wrapExternalSubscribe((bw) -> {
            if (baseType.isAssignableFrom(bw.clz())) {
                callback.accept(bw);
            }
        });
    }

    /**
     * 获取 Bean
     *
     * @param name 名字
     */
    public <T> T getBean(String name) {
        BeanWrap bw = getWrap(name);
        return bw == null ? null : bw.get();
    }

    /**
     * 获取 Bean
     *
     * @param type 类型
     */
    public <T> T getBean(Class<T> type) {
        BeanWrap bw = getWrap(type);
        return bw == null ? null : bw.get();
    }


    /**
     * 获取某类型的 bean
     *
     * @param baseType 基类
     */
    public <T> List<T> getBeansOfType(Class<T> baseType) {
        List<T> beans = new ArrayList<>();

        beanForeach(bw -> {
            if (baseType.isAssignableFrom(bw.clz())) {
                beans.add(bw.raw());
            }
        });

        return beans;
    }

    /**
     * 获取 或创建 bean
     *
     * @param type 类型
     */
    public <T> T getBeanOrNew(Class<T> type) {
        return wrapAndPut(type).get();
    }

    /**
     * 异步获取 Bean
     *
     * @param name 名字
     */
    public <T> void getBeanAsync(String name, Consumer<T> callback) {
        getWrapAsync(name, (bw) -> {
            callback.accept(bw.get());
        });
    }

    /**
     * 异步获取 Bean
     *
     * @param type 类型
     */
    public <T> void getBeanAsync(Class<T> type, Consumer<T> callback) {
        getWrapAsync(type, (bw) -> {
            callback.accept(bw.get());
        });
    }

    /**
     * 订阅某类型的 Bean
     *
     * @param baseType 基类
     */
    public <T> void subBeansOfType(Class<T> baseType, Consumer<T> callback) {
        subWrapsOfType(baseType, (bw) -> {
            callback.accept(bw.get());
        });
//        EventBus.subscribe(baseType,callback::accept);
    }

    /**
     * 包装
     */
    public BeanWrap wrap(Class<?> type) {
        return wrap(type, null);
    }
    public BeanWrap wrap(Class<?> type, Object bean) {
        BeanWrap wrap = getWrap(type);
        if (wrap == null) {
            wrap = wrapCreate(type, bean, null);
        }

        return wrap;
    }

    /**
     * 包装
     */
    public BeanWrap wrap(String name, Object bean) {
        BeanWrap wrap = getWrap(name);
        if (wrap == null) {
            wrap = wrapCreate(bean.getClass(), bean, name);
        }

        return wrap;
    }

    /**
     * 包装并推入
     */
    public BeanWrap wrapAndPut(Class<?> type) {
        return wrapAndPut(type, null);
    }

    /**
     * 包装并推入
     */
    public BeanWrap wrapAndPut(Class<?> type, Object bean) {
        BeanWrap wrap = getWrap(type);
        if (wrap == null) {
            wrap = wrapCreate(type, bean, null);
            putWrap(type, wrap);
        }

        return wrap;
    }

    protected abstract BeanWrap wrapCreate(Class<?> type, Object bean, String name);

    //////////////////////////
    //
    // bean 注册与注入
    //
    /////////////////////////

    /**
     * 尝试BEAN注册（按名字和类型存入容器；并进行类型映射）
     */
    public void beanRegister(BeanWrap bw, String name, boolean typed) {
        //按名字注册
        if (Utils.isNotEmpty(name)) {
            //有name的，只用name注入
            //
            putWrap(name, bw);
            if (typed == false) {
                //如果非typed，则直接返回
                return;
            }
        }

        //按类型注册
        putWrap(bw.clz(), bw);
        putWrap(bw.clz().getName(), bw);

        //尝试Bean的基类注册
        beanRegisterSup0(bw);

        //尝试Remoting处理。如果是，则加载到 Solon 路由器
        if (bw.remoting()) {
            HandlerLoader bww = new HandlerLoader(bw);
            if (bww.mapping() != null) {
                //
                //如果没有xmapping，则不进行web注册
                //
                bww.load(Solon.app());
            }
        }
    }

    /**
     * 尝试Bean的基类注册
     */
    protected void beanRegisterSup0(BeanWrap bw) {
        //如果有父级接口，则建立关系映射
        Class<?>[] list = bw.clz().getInterfaces();
        for (Class<?> c : list) {
            if (c.getName().contains("java.") == false) {
                //建立关系映射
                clzMapping.putIfAbsent(c, bw.clz());
                putWrap(c, bw);
            }
        }

        Type[] list2 = bw.clz().getGenericInterfaces(); //有可能跟 getInterfaces() 一样
        for (Type t : list2) {
            if (t instanceof ParameterizedType) { //有可能不是 ParameterizedType
                putWrap(t.getTypeName(), bw);
            }
        }
    }


    /**
     * 尝试变量注入 字段或参数
     *
     * @param varH 变量包装器
     * @param name 名字（bean name || config ${name}）
     */
    public void beanInject(VarHolder varH, String name) {
        beanInject(varH, name, false, false);
    }

    protected void beanInject(VarHolder varH, String name, boolean required, boolean autoRefreshed) {
        try {
            varH.required(required);
            beanInjectDo(varH, name, required, autoRefreshed);
        } catch (InjectionException e) {
            throw e;
        } catch (Throwable e) {
            throw new InjectionException("Injection failed: " + varH.getFullName(), e);
        }
    }

    private void beanInjectDo(VarHolder varH, String name, boolean required, boolean autoRefreshed) {
        if (Utils.isEmpty(name)) {
            //
            // @Inject //使用 type, 注入BEAN
            //
            if(varH.getType() == null) { //检查类型问题
                if (required) {
                    throw new InjectionException("Unrecognized type，injection failed: " + varH.getFullName());
                } else {
                    return;
                }
            }

            if(AopContext.class.isAssignableFrom(varH.getType())){
                varH.setValue(this);
                return;
            }

            if(varH.getGenericType() != null){
                //如果是泛型
                getWrapAsync(varH.getGenericType().getTypeName(), (bw) -> {
                    varH.setValue(bw.get());
                });
            }else{
                getWrapAsync(varH.getType(), (bw) -> {
                    varH.setValue(bw.get());
                });
            }
        } else if (name.startsWith("${classpath:")) {
            //
            // @Inject("${classpath:user.yml}") //注入配置文件
            //
            String url = name.substring(12, name.length() - 1);
            Properties val = Utils.loadProperties(ResourceUtil.getResource(getClassLoader(),url));

            if (val == null) {
                if (required) {
                    throw new IllegalStateException(name + "  failed to load!");
                }
            }else {
                if (Properties.class == varH.getType()) {
                    varH.setValue(val);
                } else if (Map.class == varH.getType()) {
                    Map<String, String> val2 = new HashMap<>();
                    val.forEach((k, v) -> {
                        if (k instanceof String && v instanceof String) {
                            val2.put((String) k, (String) v);
                        }
                    });
                    varH.setValue(val2);
                } else {
                    Object val2 = PropsConverter.global().convert(val, null, varH.getType(), varH.getGenericType());
                    varH.setValue(val2);
                    aot().registerEntityType(varH.getType(), varH.getGenericType());
                }
            }
        } else if (name.startsWith("${")) {
            //
            // @Inject("${xxx}") //注入配置 ${xxx} or ${xxx:def},只适合单值
            //
            String name2 = name.substring(2, name.length() - 1).trim();

            beanInjectConfig(varH, name2, required);

            if (autoRefreshed && varH.isField()) {
                int defIdx = name2.indexOf(":");
                if (defIdx > 0) {
                    name2 = name2.substring(0, defIdx).trim();
                }
                String name3 = name2;

                cfg().onChange((key, val) -> {
                    if(key.startsWith(name3)){
                        beanInjectConfig(varH, name3,required);
                    }
                });
            }
        } else {
            //
            // @Inject("xxx") //使用 name, 注入BEAN
            //
            getWrapAsync(name, (bw) -> {
                if(BeanWrap.class.isAssignableFrom(varH.getType())){
                    varH.setValue(bw);
                }else{
                    varH.setValue(bw.get());
                }
            });
        }
    }

    protected void beanInjectProperties(Class<?> clz, Object obj) {
        Inject typeInj = clz.getAnnotation(Inject.class);

        if (typeInj != null && Utils.isNotEmpty(typeInj.value())) {
            String name = typeInj.value();

            if (name.startsWith("${classpath:")) {
                //
                // @Inject("${classpath:user.yml}") //注入配置文件
                //
                String url = name.substring(12, name.length() - 1);
                Properties val = Utils.loadProperties(ResourceUtil.getResource(getClassLoader(), url));

                if (val == null) {
                    if (typeInj.required()) {
                        throw new IllegalStateException(name + "  failed to load!");
                    }
                } else {
                    Utils.injectProperties(obj, val);
                }
            } else if (typeInj.value().startsWith("${")) {
                //
                // @Inject("${xxx}") //注入配置 ${xxx} or ${xxx:def},只适合单值
                //
                String name2 = name.substring(2, name.length() - 1).trim();

                beanInjectPropertiesDo(name, obj, cfg().getProp(name2), typeInj.required());

                //支持自动刷新
                if (typeInj.autoRefreshed()) {
                    cfg().onChange((key, val) -> {
                        if (key.startsWith(name2)) {
                            beanInjectPropertiesDo(name, obj, cfg().getProp(name2), typeInj.required());
                        }
                    });
                }
            }
        }
    }

    private void beanInjectPropertiesDo(String name, Object obj, Properties val, boolean required) {
        if (required && val.size() == 0) {
            throw new InjectionException("Missing required property: '" + name + "', config injection failed: " + obj.getClass().getName());
        }

        Utils.injectProperties(obj, val);
    }


    private void beanInjectConfig(VarHolder varH, String name, boolean required){
        if (Properties.class == varH.getType()) {
            //如果是 Properties
            Props val = cfg().getProp(name);

            if(required && val.size() == 0){
                throw new InjectionException("Missing required property: '" +name+"', config injection failed: " + varH.getFullName());
            }

            varH.setValue(val);
        } else {
            //2.然后尝试获取配置（支持默认值获取）
            String val = cfg().getByExpr(name);

            if (val == null) {
                Class<?> pt = varH.getType();

                if (pt.getName().startsWith("java.lang.") || pt.isPrimitive()) {
                    //如果是java基础类型，则不注入配置值
                    if(required){
                        throw new InjectionException("Missing required property: '" +name+"', config injection failed: " + varH.getFullName());
                    }

                    varH.setValue(null); //用于触发事件
                } else {
                    //尝试转为实体
                    Properties val0 = cfg().getProp(name);
                    if (val0.size() > 0) {
                        //如果找到配置了
                        Object val2 = PropsConverter.global().convert(val0, null, pt, varH.getGenericType());
                        varH.setValue(val2);
                        aot().registerEntityType(varH.getType(), varH.getGenericType());
                    }else{
                        if(required){
                            throw new InjectionException("Missing required property: '" +name+"', config injection failed: " + varH.getFullName());
                        }

                        varH.setValue(null); //用于触发事件
                    }
                }
            } else {
                Object val2 = ConvertUtil.to(varH.getType(), val);
                varH.setValue(val2);
                aot().registerEntityType(varH.getType(), varH.getGenericType());
            }
        }
    }

    //////////////////////////
    //
    // bean 遍历与查找
    //
    /////////////////////////

    /**
     * 遍历bean库 (拿到的是bean包装)
     */
    public void beanForeach(BiConsumer<String, BeanWrap> action) {
        beanWrapsOfName.forEach(action);
    }


    /**
     * 遍历bean包装库
     */
    public void beanForeach(Consumer<BeanWrap> action) {
        //相关于 beanWraps ，不会出现重复的 // 复制一下，避免 ConcurrentModificationException
        new ArrayList<>(beanWrapSet).forEach(bw -> {
            action.accept(bw);
        });
    }

    /**
     * 查找bean包装
     */
    public List<BeanWrap> beanFind(BiPredicate<String, BeanWrap> condition) {
        List<BeanWrap> list = new ArrayList<>();
        beanForeach((k, v) -> {
            if (condition.test(k, v)) {
                list.add(v);
            }
        });

        return list;
    }

    /**
     * 查找bean包装
     */
    public List<BeanWrap> beanFind(Predicate<BeanWrap> condition) {
        List<BeanWrap> list = new ArrayList<>();
        beanForeach((v) -> {
            if (condition.test(v)) {
                list.add(v);
            }
        });

        return list;
    }
}
