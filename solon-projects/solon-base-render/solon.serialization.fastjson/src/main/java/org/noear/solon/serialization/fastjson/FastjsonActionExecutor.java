package org.noear.solon.serialization.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import org.noear.solon.Utils;
import org.noear.solon.core.handle.ActionExecuteHandlerDefault;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.wrap.ParamWrap;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

/**
 * Json 动作执行器
 *
 * @author noear
 * @author 夜の孤城
 * @since 1.5
 */
public class FastjsonActionExecutor extends ActionExecuteHandlerDefault {
    private static final String label = "/json";

    private final ParserConfig config = new ParserConfig();

    /**
     * 反序列化配置
     * */
    public ParserConfig config(){
        return config;
    }

    @Override
    public boolean matched(Context ctx, String ct) {
        if (ct != null && ct.contains(label)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected Object changeBody(Context ctx) throws Exception {
        String json = ctx.bodyNew();

        if (Utils.isNotEmpty(json)) {
            return JSON.parse(json, config);
        } else {
            return null;
        }
    }

    @Override
    protected Object changeValue(Context ctx, ParamWrap p, int pi, Class<?> pt, Object bodyObj) throws Exception {
        if (p.requireBody() == false && ctx.paramMap().containsKey(p.getName())) {
            //有可能是path、queryString变量
            return super.changeValue(ctx, p, pi, pt, bodyObj);
        }

        if (bodyObj == null) {
            return super.changeValue(ctx, p, pi, pt, bodyObj);
        }

        if (bodyObj instanceof JSONObject) {
            JSONObject tmp = (JSONObject) bodyObj;

            if (p.requireBody() == false) {
                //
                //如果没有 body 要求；尝试找按属性找
                //
                if (tmp.containsKey(p.getName())) {
                    //支持泛型的转换
                    ParameterizedType gp = p.getGenericType();
                    if (gp != null) {
                        return tmp.getObject(p.getName(), gp);
                    }
                    return tmp.getObject(p.getName(), pt);
                }
            }

            //尝试 body 转换
            if (pt.isPrimitive() || pt.getTypeName().startsWith("java.lang.")) {
                return super.changeValue(ctx, p, pi, pt, bodyObj);
            } else {
                if (List.class.isAssignableFrom(p.getType())) {
                    return null;
                }

                if (p.getType().isArray()) {
                    return null;
                }

                //支持泛型的转换 如：Map<T>
                ParameterizedType gp = p.getGenericType();
                if (gp != null) {
                    return tmp.toJavaObject(gp);
                }
                return tmp.toJavaObject(pt);
                // return tmp.toJavaObject(pt);
            }
        }

        if (bodyObj instanceof JSONArray) {
            JSONArray tmp = (JSONArray) bodyObj;
            //如果参数是非集合类型
            if(!Collection.class.isAssignableFrom(pt)){
                return null;
            }
            //集合类型转换
            ParameterizedType gp = p.getGenericType();
            if (gp != null) {
                //转换带泛型的集合
                return  tmp.toJavaObject(gp);
            }
            //不仅可以转换为List 还可以转换成Set
            return tmp.toJavaObject(p.getType());
        }

        return bodyObj;
    }
}
