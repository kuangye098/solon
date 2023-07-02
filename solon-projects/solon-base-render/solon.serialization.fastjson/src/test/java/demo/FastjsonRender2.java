package demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Render;
import org.noear.solon.serialization.SerializationConfig;

//不要要入参，方便后面多视图混用
//
public class FastjsonRender2 implements Render {
    private boolean _typedJson;

    public FastjsonRender2(boolean typedJson) {
        _typedJson = typedJson;
    }

    @Override
    public void render(Object obj, Context ctx) throws Throwable {
        String txt = null;

        if (_typedJson) {
            //序列化处理
            //
            txt = JSON.toJSONString(obj,
                    SerializerFeature.BrowserCompatible,
                    SerializerFeature.WriteClassName,
                    SerializerFeature.DisableCircularReferenceDetect);
        } else {
            //非序列化处理
            //
            if (obj == null) {
                return;
            }

            if (obj instanceof Throwable) {
                throw (Throwable) obj;
            }

            if (obj instanceof String) {
                txt = (String) obj;
            } else {
                txt = JSON.toJSONString(obj,
                        SerializerFeature.BrowserCompatible,
                        SerializerFeature.DisableCircularReferenceDetect);
            }
        }

        if (SerializationConfig.isOutputMeta()) {
            ctx.headerAdd("solon.serialization", "FastjsonRender");
        }

        ctx.attrSet("output", txt);

        if (obj instanceof String && ctx.accept().contains("/json") == false) {
            ctx.output(txt);
        } else {
            ctx.outputAsJson(txt);
        }
    }
}
