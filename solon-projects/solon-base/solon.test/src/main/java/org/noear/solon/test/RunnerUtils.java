package org.noear.solon.test;

import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.SolonTestApp;
import org.noear.solon.Utils;
import org.noear.solon.proxy.BeanProxy;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.core.NvMap;
import org.noear.solon.core.event.AppInitEndEvent;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.test.annotation.TestPropertySource;
import org.noear.solon.test.annotation.TestRollback;
import org.noear.solon.test.data.TestRollbackInterceptor;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.*;

/**
 * @author noear
 * @since 1.11
 */
public class RunnerUtils {
    private static Map<Class<?>, AopContext> appCached = new HashMap<>();

    private static Class<?> getMainClz(SolonTest anno, Class<?> klass) {
        if (anno == null) {
            return klass;
        }

        Class<?> mainClz = anno.value();
        if (mainClz == Void.class) {
            mainClz = anno.classes();
        }

        if (mainClz == Void.class) {
            return klass;
        } else {
            return mainClz;
        }
    }

    private static Method getMainMethod(Class<?> mainClz) {
        try {
            return mainClz.getMethod("main", String[].class);
        } catch (Exception ex) {
            return null;
        }
    }

    private static void addPropertySource(AopContext context, TestPropertySource propertySource) {
        if (propertySource == null) {
            return;
        }

        for (String uri : propertySource.value()) {
            if (uri.startsWith(Utils.TAG_classpath)) {
                context.cfg().loadAdd(uri.substring(Utils.TAG_classpath.length()));
            } else {
                try {
                    context.cfg().loadAdd(new File(uri).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 初始化测试目标类
     * */
    public static Object initTestTarget(AopContext aopContext, Object tmp) {
        //注入
        aopContext.beanInject(tmp);
        //构建临时包装（用于支持提取操作）
        BeanWrap beanWrap = new BeanWrap(aopContext, tmp.getClass(), tmp);
        //尝试提取操作
        aopContext.beanExtract(beanWrap);
        //设置代理(把 final 排除掉)
        if (Modifier.isFinal(tmp.getClass().getModifiers()) == false) {
            beanWrap.proxySet(BeanProxy.getGlobal());
            if(beanWrap.raw() != null){
                aopContext.beanInject(beanWrap.raw());
            }
        }
        //重新获取bean
        tmp = beanWrap.get();


        return tmp;
    }


    /**
     * 初始化测试运行器
     * */
    public static AopContext initRunner(Class<?> klass) throws Throwable {
        //添加测试类包名检测（包名为必须要求）
        if (klass.getPackage() == null || Utils.isEmpty(klass.getPackage().getName())) {
            throw new IllegalStateException("The test class is missing package: " + klass.getName());
        }

        SolonTest anno = klass.getAnnotation(SolonTest.class);

        if (anno != null) {
            if (anno.properties().length > 0) {
                for (String tmp : anno.properties()) {
                    String[] kv = tmp.split("=");
                    if (kv.length == 2) {
                        System.setProperty(kv[0], kv[1]);
                    }
                }
            }

            List<String> argsAry = new ArrayList<>();
            if (anno.args().length > 0) {
                argsAry.addAll(Arrays.asList(anno.args()));
            }

            //添加调试模式
            if (anno.debug()) {
                argsAry.add("-debug=1");
            }

            //添加环境变量
            if (Utils.isNotEmpty(anno.env())) {
                argsAry.add("-env=" + anno.env());
            }

            Class<?> mainClz = RunnerUtils.getMainClz(anno, klass);

            if (appCached.containsKey(mainClz)) {
                return appCached.get(mainClz);
            }

            AopContext aopContext = startDo(mainClz, argsAry, klass);

            appCached.put(mainClz, aopContext);
            //延迟秒数
            if (anno.delay() > 0) {
                try {
                    Thread.sleep(anno.delay() * 1000);
                } catch (Exception ex) {

                }
            }

            return aopContext;
        } else {
            List<String> argsAry = new ArrayList<>();
            argsAry.add("-debug=1");
            return startDo(klass, argsAry, klass);
        }
    }

    private static AopContext startDo(Class<?> mainClz, List<String> argsAry, Class<?> klass) throws Throwable {


        if (mainClz == klass) {
            String[] args = argsAry.toArray(new String[argsAry.size()]);

            SolonTestApp testApp = new SolonTestApp(mainClz, NvMap.from(args));
            testApp.start(x -> {
                initDo(x, klass);
            });

            return testApp.context();

        } else {
            Method main = RunnerUtils.getMainMethod(mainClz);

            if (main != null && Modifier.isStatic(main.getModifiers())) {
                String[] args = argsAry.toArray(new String[argsAry.size()]);

                initDo(null, klass);
                main.invoke(null, new Object[]{args});

                return Solon.context();
            } else {
                String[] args = argsAry.toArray(new String[argsAry.size()]);

                SolonTestApp testApp = new SolonTestApp(mainClz, NvMap.from(args));
                testApp.start(x -> {
                    initDo(x, klass);
                });

                return testApp.context();
            }
        }
    }

    private static void initDo(SolonApp app, Class<?> klass) {
        TestPropertySource propAnno = klass.getAnnotation(TestPropertySource.class);

        if (app == null) {
            EventBus.subscribe(AppInitEndEvent.class, event -> {
                //加载测试配置
                RunnerUtils.addPropertySource(event.context(), propAnno);
                //event.context().wrapAndPut(klass);
                event.context().beanAroundAdd(TestRollback.class, new TestRollbackInterceptor(), 120);
            });
        } else {
            //加载测试配置
            RunnerUtils.addPropertySource(app.context(), propAnno);
            //app.context().wrapAndPut(klass);
            app.context().beanAroundAdd(TestRollback.class, new TestRollbackInterceptor(), 120);
        }
    }
}
