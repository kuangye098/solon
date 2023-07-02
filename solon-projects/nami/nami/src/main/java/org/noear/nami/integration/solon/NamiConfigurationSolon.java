package org.noear.nami.integration.solon;

import org.noear.nami.*;
import org.noear.nami.annotation.NamiClient;
import org.noear.solon.Utils;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Bridge;
import org.noear.solon.core.LoadBalance;

/**
 * NamiConfiguration 的 solon 实现，实现与 solon LoadBalance 对接及注入处理
 *
 * @author noear
 * @since 1.2
 * */
public final class NamiConfigurationSolon implements NamiConfiguration {

    private NamiConfiguration custom;
    private AopContext context;

    public NamiConfigurationSolon(AopContext context) {
        this.context = context;

        //
        //如果有定制的NamiConfiguration, 则用之
        //
        context.getBeanAsync(NamiConfiguration.class, (bean) -> {
            custom = bean;
        });

        //订阅拦截器
        context.subBeansOfType(Filter.class, it -> {
            NamiManager.reg(it);
        });
    }

    @Override
    public void config(NamiClient client, NamiBuilder builder) {
        if (Utils.isEmpty(client.name())) {
            return;
        }

        //尝试自定义
        if (custom != null) {
            custom.config(client, builder);
        }

        //尝试从负载工厂获取
        LoadBalance upstream = getUpstream(client);
        if (upstream != null) {
            builder.upstream(upstream::getServer);
        } else {
            //尝试从Ioc容器获取
            context.getWrapAsync(client.name(), (bw) -> {
                LoadBalance tmp = bw.raw();
                builder.upstream(tmp::getServer);
            });
        }
    }

    private LoadBalance getUpstream(NamiClient anno) {
        if (Bridge.upstreamFactory() == null) {
            return null;
        }

        return Bridge.upstreamFactory().create(anno.group(), anno.name());
    }
}
