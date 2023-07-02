package org.noear.nami.coder.fastjson;

import org.noear.nami.NamiManager;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Plugin;

/**
 * @author noear
 * @since 1.2
 */
public class XPluginImp implements Plugin {
    @Override
    public void start(AopContext context) {
        NamiManager.reg(FastjsonDecoder.instance);
        NamiManager.reg(FastjsonEncoder.instance);
        NamiManager.reg(FastjsonTypeEncoder.instance);
    }
}
