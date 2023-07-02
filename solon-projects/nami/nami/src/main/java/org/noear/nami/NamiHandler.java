package org.noear.nami;

import org.noear.nami.annotation.NamiMapping;
import org.noear.nami.annotation.NamiClient;
import org.noear.nami.common.*;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nami - 调用处理程序
 *
 * @author noear
 * @since 1.0
 * */
public class NamiHandler implements InvocationHandler {
    private final static Pattern pathKeyExpr = Pattern.compile("\\{([^\\\\}]+)\\}");

    private final Config config;
    private final NamiClient client;

    private final Map<String, String> headers0 = new LinkedHashMap<>();
    private final Class<?> clz0;
    private final Map<String, Map> pathKeysCached = new LinkedHashMap<>();

    /**
     * @param config 配置
     * @param client 客户端注解
     */
    public NamiHandler(Class<?> clz, Config config, NamiClient client) {
        this.config = config;
        this.client = client;

        this.clz0 = clz;

        try {
            init();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void init() throws Exception {
        //1.运行配置器
        if (client != null) {
            //尝试添加全局拦截器
            for (Filter mi : NamiManager.getFilters()) {
                config.filterAdd(mi);
            }

            //尝试配置器配置
            NamiConfiguration tmp = NamiManager.getConfigurator(client.configuration());
            if (tmp != null) {
                tmp.config(client, new NamiBuilder(config));
            }

            //尝试设置超时
            if (client.timeout() > 0) {
                config.setTimeout(client.timeout());
            }

            //尝试设置心跳
            if (client.heartbeat() > 0) {
                config.setHeartbeat(client.heartbeat());
            }

            //>>添加接口url
            if (TextUtils.isNotEmpty(client.url())) {
                config.setUrl(client.url());
            }

            //>>添加接口group
            if (TextUtils.isNotEmpty(client.group())) {
                config.setGroup(client.group());
            }

            //>>添加接口name
            if (TextUtils.isNotEmpty(client.name())) {
                config.setName(client.name());
            }

            //>>添加接口path
            if (TextUtils.isNotEmpty(client.path())) {
                config.setPath(client.path());
            }

            //>>添加接口header
            if (client.headers().length > 0) {
                for (String h : client.headers()) {
                    String[] ss = null;
                    if (h.contains(":")) {
                        ss = h.split(":");
                    } else {
                        ss = h.split("=");
                    }
                    if (ss.length == 2) {
                        headers0.put(ss[0].trim(), ss[1].trim());
                    }
                }
            }

            //>>添加upstream
            if (client.upstream().length > 0) {
                config.setUpstream(new UpstreamFixed(Arrays.asList(client.upstream())));
            }
        }

        //2.配置初始化
        config.init();
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] vals) throws Throwable {
        //检查upstream
        if (TextUtils.isEmpty(config.getUrl()) && config.getUpstream() == null) {
            StringBuilder buf = new StringBuilder();
            buf.append("NamiClient: Not found upstream: ").append(clz0.getName());

            if (TextUtils.isEmpty(config.getName())) {
                buf.append(": '").append(config.getName()).append("'");
            }

            throw new NamiException(buf.toString());
        }

        //调用 Default 函数
        if (method.isDefault()) {
            return MethodHandlerUtils.invokeDefault(proxy, method, vals);
        }

        //调用 Object 函数
        if(method.getDeclaringClass() == Object.class){
            return MethodHandlerUtils.invokeObject(clz0,proxy, method, vals);
        }

        MethodWrap methodWrap = MethodWrap.get(method);

        //构建 headers
        Map<String, String> headers = new HashMap<>(headers0);

        //构建 args
        Map<String, Object> args = new LinkedHashMap<>();
        Object body = null;
        Parameter[] names = methodWrap.getParameters();
        for (int i = 0, len = names.length; i < len; i++) {
            if (vals[i] != null) {
                args.put(names[i].getName(), vals[i]);
            }
        }

        //确定body及默认编码
        if (methodWrap.getBodyName() != null) {
            body = args.get(methodWrap.getBodyName());

            if (config.getEncoder() == null) {
                headers.putIfAbsent(Constants.HEADER_CONTENT_TYPE, methodWrap.getBodyAnno().contentType());
            }
        }

        //构建 fun
        String fun = method.getName();
        String act = null;

        //处理mapping
        NamiMapping mapping = methodWrap.getMappingAnno();
        if (mapping != null) {
            if (methodWrap.getAct() != null) {
                act = methodWrap.getAct();
            }

            if (methodWrap.getFun() != null) {
                fun = methodWrap.getFun();
            }

            if (methodWrap.getMappingHeaders() != null) {
                headers.putAll(methodWrap.getMappingHeaders());
            }
        }

        //处理附加信息
        Map<String, String> contextMap = NamiAttachment.getData();
        if (contextMap.size() > 0) {
            headers.putAll(contextMap);
        }


        //构建 url
        String url = null;
        if (TextUtils.isEmpty(config.getUrl())) {
            url = config.getUpstream().get();

            if (url == null) {
                StringBuilder buf = new StringBuilder();
                buf.append("NamiClient: Upstream not found server instance: ").append(clz0.getName());

                if (TextUtils.isEmpty(config.getName())) {
                    buf.append(": '").append(config.getName()).append("'");
                }

                throw new NamiException(buf.toString());
            }

            if (url.indexOf("://") < 0) {
                url = "http://" + url;
            }

            if (TextUtils.isNotEmpty(config.getPath())) {
                int idx = url.indexOf("/", 9);//https://a
                if (idx > 0) {
                    url = url.substring(0, idx);
                }

                if (config.getPath().endsWith("/")) {
                    fun = config.getPath() + fun;
                } else {
                    fun = config.getPath() + "/" + fun;
                }
            }

        } else {
            url = config.getUrl();
        }

        if (fun != null && fun.indexOf("{") > 0) {
            //
            //处理Path参数
            //
            Map<String, String> pathKeys = buildPathKeys(fun);

            for (Map.Entry<String, String> kv : pathKeys.entrySet()) {
                //
                //处理path参数不为String类型时报错的问题
                //String val = (String) args.get(kv.getValue());
                //
                Object arg = args.get(kv.getValue());

                if (arg != null) {
                    String val = arg.toString();
                    fun = fun.replace(kv.getKey(), val);
                    args.remove(kv.getValue());
                }
            }
        }

        //确定返回类型
        Type type = method.getGenericReturnType();
        if (type == null) {
            type = method.getReturnType();
        }


        //执行调用
        Object rst = new Nami(config)
                .method(proxy, method)
                .action(act)
                .url(url, fun)
                .call(headers, args, body)
                .getObject(type);

        return rst;//调试时，方便看
    }

    private Map<String, String> buildPathKeys(String path) {
        Map<String, String> pathKeys = pathKeysCached.get(path);
        if (pathKeys == null) {
            synchronized (path.intern()) {
                pathKeys = pathKeysCached.get(path);
                if (pathKeys == null) {
                    pathKeys = new LinkedHashMap<>();

                    Matcher pm = pathKeyExpr.matcher(path);

                    while (pm.find()) {
                        pathKeys.put(pm.group(), pm.group(1));
                    }
                }
            }
        }

        return pathKeys;
    }
}
