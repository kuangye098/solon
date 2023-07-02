package org.noear.solon.extend.sqltoy;

import org.noear.solon.Solon;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.data.cache.CacheService;
import org.noear.solon.extend.sqltoy.annotation.Db;
import org.noear.solon.extend.sqltoy.configure.SqlToyContextProperties;
import org.noear.solon.extend.sqltoy.translate.SolonTranslateCacheManager;
import org.sagacity.sqltoy.SqlToyContext;

/**
 * 去除spring依赖，适配到Solon的Tran、Aop。TranslateCache默认设置为Solon CacheService
 * 实现Mapper接口功能
 *
 * @author 夜の孤城
 * @since 1.5
 * @since 1.8
 */
public class XPluginImp implements Plugin {

    AopContext context;

    @Override
    public void start(AopContext context) {
        this.context = context;

        //尝试初始化 rdb
        SqlToyContextProperties properties = context.cfg().getBean("sqltoy", SqlToyContextProperties.class);
        if (properties == null) {
            properties = new SqlToyContextProperties();
        }

        if (Solon.cfg().isDebugMode()) {
            properties.setDebug(true);
        }

        try {
            final SqlToyContext sqlToyContext = new SqlToyContextBuilder(properties, context).build();

            if ("solon".equals(properties.getCacheType()) || properties.getCacheType() == null) {
                context.getWrapAsync(CacheService.class, bw -> {
                    sqlToyContext.setTranslateCacheManager(new SolonTranslateCacheManager(bw.get()));
                    try {
                        DbManager.setContext(sqlToyContext);
                        initSqlToy(sqlToyContext);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                DbManager.setContext(sqlToyContext);
                initSqlToy(sqlToyContext);
            }

            context.beanInjectorAdd(Db.class, new DbInjector());
        } catch (Exception e) {
            //e.printStackTrace();
            EventBus.pushTry(e); //转到事件总线
        }
    }

    private void initSqlToy(SqlToyContext sqlToyContext) throws Exception {
        sqlToyContext.initialize();
        context.wrapAndPut(SqlToyContext.class, sqlToyContext);
    }

    @Override
    public void stop() throws Throwable {
        SqlToyContext sqlToyContext = context.getBean(SqlToyContext.class);
        sqlToyContext.destroy();
    }
}
