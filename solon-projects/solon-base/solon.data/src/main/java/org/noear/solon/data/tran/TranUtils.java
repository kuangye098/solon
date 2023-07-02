package org.noear.solon.data.tran;

import org.noear.solon.Solon;
import org.noear.solon.data.annotation.Tran;
import org.noear.solon.core.util.RunnableEx;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 事务工具
 *
 * @author noear
 * @since 1.0
 * */
public class TranUtils {
    private static TranExecutor executor = ()->false;
    static {
        Solon.context().getBeanAsync(TranExecutor.class, bean -> executor = bean);
    }

    /**
     * 执行事务
     * */
    public static void execute(Tran tran, RunnableEx runnable) throws Throwable {
        executor.execute(tran, runnable);
    }

    /**
     * 是否在事务中
     */
    public static boolean inTrans() {
        return executor.inTrans();
    }

    /**
     * 是否在事务中且只读
     */
    public static boolean inTransAndReadOnly() {
        return executor.inTransAndReadOnly();
    }

    /**
     * 获取链接
     */
    public static Connection getConnection(DataSource ds) throws SQLException {
        return executor.getConnection(ds);
    }
}
