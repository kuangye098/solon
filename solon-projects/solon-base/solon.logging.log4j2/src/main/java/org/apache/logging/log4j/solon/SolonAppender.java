package org.apache.logging.log4j.solon;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.spi.StandardLevel;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.noear.solon.Utils;
import org.noear.solon.logging.AppenderManager;
import org.noear.solon.logging.event.Level;

import java.io.Serializable;


/**
 * @author noear
 * @since 1.10
 */
@Plugin(name="Solon", category="Core", elementType="appender", printObject=true)
public final  class SolonAppender extends AbstractAppender {

    protected SolonAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent e) {
        //solon console 1个，所以最少2个起
        if (AppenderManager.count() < 2) {
            return;
        }

        Level level;

        int eLevel = e.getLevel().intLevel();
        if (StandardLevel.DEBUG.intLevel() == (eLevel)) {
            level = Level.DEBUG;
        } else if (StandardLevel.WARN.intLevel() == (eLevel)) {
            level = Level.WARN;
        } else if (StandardLevel.INFO.intLevel() == (eLevel)) {
            level = Level.INFO;
        } else if (StandardLevel.TRACE.intLevel() == (eLevel)) {
            level = Level.TRACE;
        } else {
            level = Level.ERROR;
        }

        String message = e.getMessage().getFormattedMessage();
        Throwable throwable = e.getMessage().getThrowable();
        if (throwable != null) {
            String errorStr = Utils.throwableToString(throwable);

            if (message.contains("{}")) {
                message = message.replace("{}", errorStr);
            } else {
                message = message + "\n" + errorStr;
            }
        }

        ReadOnlyStringMap eData = e.getContextData();

        org.noear.solon.logging.event.LogEvent event = new org.noear.solon.logging.event.LogEvent(
                e.getLoggerName(),
                level,
                (eData == null ? null : eData.toMap()),
                message,
                e.getTimeMillis(),
                e.getThreadName(),
                throwable);

        AppenderManager.appendNotPrinted(event);
    }

    @PluginFactory
    public static SolonAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {

        if (name == null) {
            LOGGER.error("No name provided for SolonAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        return new SolonAppender(name, filter, layout, true);
    }
}
