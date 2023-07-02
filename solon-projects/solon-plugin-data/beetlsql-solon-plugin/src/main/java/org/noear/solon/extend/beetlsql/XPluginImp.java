package org.noear.solon.extend.beetlsql;

import org.beetl.sql.core.SQLManager;
import org.noear.solon.Utils;
import org.noear.solon.core.*;

import javax.sql.DataSource;

/**
 * Solon 插件接口实现，完成对接与注入支持
 *
 * @author noear
 * @since 2020-09-01
 * */
public class XPluginImp implements Plugin {
    @Override
    public void start(AopContext context) {
        //监听事件
        context.subWrapsOfType(DataSource.class, bw->{
            DbManager.reg(bw);
        });


        //for new
        context.beanBuilderAdd(org.beetl.sql.solon.annotation.Db.class, (clz, wrap, anno) -> {
            builderAddDo(clz, wrap, anno.value());
        });

        context.beanInjectorAdd(org.beetl.sql.solon.annotation.Db.class, (varH, anno) -> {
            injectorAddDo(varH, anno.value());
        });


        //初始化管理器（主要为了生成动态管理器）
        //
        context.lifecycle(-99, () -> {
            BeanWrap defBw = context.getWrap(DataSource.class);

            if (defBw != null) {
                DbManager.dynamicBuild(defBw);

                if (DbManager.dynamicGet() != null) {
                    context.wrapAndPut(SQLManager.class, DbManager.dynamicGet());
                }
            }
        });
    }

    private void builderAddDo(Class<?> clz, BeanWrap wrap, String annoValue) {
        if (clz.isInterface() == false) {
            return;
        }

        if (Utils.isEmpty(annoValue)) {
            wrap.context().getWrapAsync(DataSource.class, (dsBw) -> {
                create0(clz, dsBw);
            });
        } else {
            wrap.context().getWrapAsync(annoValue, (dsBw) -> {
                if (dsBw.raw() instanceof DataSource) {
                    create0(clz, dsBw);
                }
            });
        }
    }

    private void injectorAddDo(VarHolder varH, String annoValue) {
        if (Utils.isEmpty(annoValue)) {
            varH.context().getWrapAsync(DataSource.class, (dsBw) -> {
                inject0(varH, dsBw, annoValue);
            });
        } else {
            varH.context().getWrapAsync(annoValue, (dsBw) -> {
                if (dsBw.raw() instanceof DataSource) {
                    inject0(varH, dsBw, annoValue);
                }
            });
        }
    }

    private void create0(Class<?> clz, BeanWrap dsBw) {
        Object raw = DbManager.get(dsBw).getMapper(clz);

        if (raw != null) {
            dsBw.context().wrapAndPut(clz, raw);
        }
    }

    /**
     * 字段注入
     */
    private void inject0(VarHolder varH, BeanWrap dsBw, String annoValue) {
        SQLManager tmp = DbManager.get(dsBw);

        if (varH.getType().isInterface()) {
            Object mapper = tmp.getMapper(varH.getType());

            varH.setValue(mapper);
            return;
        }

        if (SQLManager.class.isAssignableFrom(varH.getType())) {
            if (Utils.isNotEmpty(annoValue)) {
                varH.setValue(tmp);
            } else {
                dsBw.context().getBeanAsync(SQLManager.class, (bean) -> {
                    varH.setValue(bean);
                });
            }
            return;
        }
    }
}
