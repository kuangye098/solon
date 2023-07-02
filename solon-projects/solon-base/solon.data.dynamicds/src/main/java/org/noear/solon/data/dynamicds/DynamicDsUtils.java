package org.noear.solon.data.dynamicds;

import org.noear.solon.Utils;
import org.noear.solon.core.Props;
import org.noear.solon.core.PropsConverter;
import org.noear.solon.core.util.ClassUtil;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 动态数据源切换
 *
 * @author noear
 * @since 1.11
 */
public class DynamicDsUtils {
    static ThreadLocal<String> targetThreadLocal = new ThreadLocal<>();

    /**
     * 清除
     */
    public static void clear() {
        targetThreadLocal.remove();
    }

    /**
     * 获取
     */
    public static String getCurrent() {
        return targetThreadLocal.get();
    }

    /**
     * 设置
     */
    public static void setCurrent(String dsBeanName) {
        if (dsBeanName == null) {
            targetThreadLocal.remove();
        } else {
            targetThreadLocal.set(dsBeanName);
        }
    }

    /////////////////////////////

    /**
     * 构建数据源字典
     */
    public static Map<String, DataSource> buildDsMap(Properties props) {
        //::类型
        String typeStr = props.getProperty("type");
        if (Utils.isEmpty(typeStr)) {
            //缺少类型配置
            throw new IllegalStateException("Missing type configuration");
        }
        props.remove("type");

        Class<?> typeClz = ClassUtil.loadClass(typeStr);
        if (typeClz == null || DataSource.class.isAssignableFrom(typeClz) == false) {
            throw new IllegalStateException("Type configuration not is data source");
        }


        return buildDsMap(props, typeClz);
    }

    public static Map<String, DataSource> buildDsMap(Properties props, Class<?> typeClz) {
        //::数据源构建
        Props rootProps;
        if (props instanceof Props) {
            rootProps = ((Props) props);
        } else {
            rootProps = new Props();
            rootProps.putAll(props);
        }

        Map<String, Props> groupProps = rootProps.getGroupedProp("");

        if (groupProps.size() == 0) {
            //缺少数据源配置
            throw new IllegalStateException("Missing dynamic data source configuration");
        }


        Map<String, DataSource> dataSourceMap = new HashMap<>();
        groupProps.forEach((key, prop) -> {
            if (prop.size() > 1) {
                //超过1个以上的，才可能是数据源属性
                DataSource source = (DataSource) PropsConverter.global().convert(prop, typeClz);
                dataSourceMap.put(key, source);
            }
        });

        return dataSourceMap;
    }
}
