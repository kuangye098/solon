package org.noear.solon.serialization.gson;

import com.google.gson.GsonBuilder;
import org.noear.solon.core.handle.Render;
import org.noear.solon.serialization.StringSerializerRender;

/**
 * Json 类型化渲染器工厂
 *
 * @author noear
 * @since 1.5
 */
public class GsonRenderTypedFactory extends GsonRenderFactoryBase {

    private final GsonBuilder config;
    public GsonRenderTypedFactory() {
        config = new GsonBuilder();
        config.registerTypeAdapter(java.util.Date.class, new GsonDateSerialize());
    }


    @Override
    public Render create() {
        return new StringSerializerRender(true, new GsonSerializer(config.create()));
    }

    @Override
    public GsonBuilder config() {
        return config;
    }
}
