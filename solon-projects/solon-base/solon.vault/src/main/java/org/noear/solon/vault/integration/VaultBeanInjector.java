package org.noear.solon.vault.integration;

import org.noear.solon.Utils;
import org.noear.solon.core.*;
import org.noear.solon.core.util.ConvertUtil;
import org.noear.solon.vault.VaultUtils;
import org.noear.solon.vault.annotation.VaultInject;

import java.util.Properties;

/**
 * 脱敏注入器
 *
 * @author noear
 * @since 1.9
 */
public class VaultBeanInjector implements BeanInjector<VaultInject> {
    @Override
    public void doInject(VarHolder varH, VaultInject anno) {
        beanInject(varH, anno.value(), anno.required(), anno.autoRefreshed());
    }

    protected void beanInject(VarHolder varH, String name, boolean required, boolean autoRefreshed) {
        varH.required(required);

        if (name.startsWith("${")) {
            //
            // @Inject("${xxx}") //注入配置 ${xxx} or ${xxx:def},只适合单值
            //
            String name2 = name.substring(2, name.length() - 1).trim();

            beanInjectConfig(varH, name2);

            if (autoRefreshed && varH.isField()) {
                varH.context().cfg().onChange((key, val) -> {
                    if (key.startsWith(name2)) {
                        beanInjectConfig(varH, name2);
                    }
                });
            }
        }
    }

    private void beanInjectConfig(VarHolder varH, String name) {
        if (Properties.class == varH.getType()) {
            //如果是 Properties
            Properties val = varH.context().cfg().getProp(name);

            //脱敏处理
            val = VaultUtils.guard(val);

            varH.setValue(val);
        } else {
            //2.然后尝试获取配置
            String def = null;
            int defIdx = name.indexOf(":");
            if (defIdx > 0) {
                if (name.length() > defIdx + 1) {
                    def = name.substring(defIdx + 1).trim();
                } else {
                    def = "";
                }
                name = name.substring(0, defIdx).trim();
            }

            String val = varH.context().cfg().get(name);

            if (def != null) {
                if (Utils.isEmpty(val)) {
                    val = def;
                }
            }

            //脱敏处理
            val = VaultUtils.guard(val);

            if (val == null) {
                Class<?> pt = varH.getType();

                if (pt.getName().startsWith("java.lang.") || pt.isPrimitive()) {
                    //如果是java基础类型，则不注入配置值
                } else {
                    //尝试转为实体
                    Properties val0 = varH.context().cfg().getProp(name);
                    if (val0.size() > 0) {
                        //脱敏处理
                        val0 = VaultUtils.guard(val0);

                        //如果找到配置了
                        Object val2 = PropsConverter.global().convert(val0, null, pt, varH.getGenericType());
                        varH.setValue(val2);
                    }
                }
            } else {
                Object val2 = ConvertUtil.to(varH.getType(), varH.getGenericType(), val);

                varH.setValue(val2);
            }
        }
    }
}
