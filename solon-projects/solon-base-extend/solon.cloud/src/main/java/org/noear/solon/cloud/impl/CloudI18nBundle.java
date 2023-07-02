package org.noear.solon.cloud.impl;

import org.noear.solon.cloud.model.Pack;
import org.noear.solon.core.Props;
import org.noear.solon.i18n.I18nBundle;

import java.util.Locale;

/**
 * @author noear
 * @since 1.6
 */
public class CloudI18nBundle implements I18nBundle {
    Pack pack;
    Locale locale;

    public CloudI18nBundle(Pack pack, Locale locale) {
        this.pack = pack;
        this.locale = locale;
    }

    @Override
    public Props toProps() {
        return pack.getData();
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public String get(String key) {
        return pack.getData().get(key);
    }
}
