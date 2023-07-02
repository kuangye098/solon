package org.noear.solon.serialization.snack3;

import org.noear.snack.core.Options;
import org.noear.snack.core.NodeEncoder;
import org.noear.solon.core.handle.Render;
import org.noear.solon.serialization.StringSerializerRender;

/**
 * Json 类型化渲染器工厂
 *
 * @author noear
 * @since 1.5
 */
public class SnackRenderTypedFactory extends SnackRenderFactoryBase {


    private final Options config;
    public SnackRenderTypedFactory(){
        config = Options.serialize();
    }


    /**
     * 添加编码器
     * */
    public <T> void addEncoder(Class<T> clz, NodeEncoder<T> encoder) {
        config.addEncoder(clz, encoder);
    }

    @Override
    public Render create() {
        return new StringSerializerRender(true, new SnackSerializer(config));
    }

    @Override
    public Options config() {
        return config;
    }
}
