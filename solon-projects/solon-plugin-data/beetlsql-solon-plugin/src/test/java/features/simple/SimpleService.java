package features.simple;

import features.UserInfo;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.solon.annotation.Db;
import org.noear.solon.data.annotation.Tran;
import org.noear.solon.annotation.ProxyComponent;

/**
 * Solon 的事务，只支持 Controller, Service, Dao ，且只支持注在函数上（算是较为克制）
 * */
@ProxyComponent
public class SimpleService {
    @Db
    SQLManager sqlManager;

    @Db
    SimpleUserInfoMapper userInfoMapper;

    @Tran
    public void test(){
        sqlManager.single(UserInfo.class,1);
        userInfoMapper.single(1);
    }
}
