package features.masterslave;

import features.UserInfo;
import org.beetl.sql.core.SQLManager;
import org.beetl.sql.solon.annotation.Db;
import org.noear.solon.data.annotation.Tran;
import org.noear.solon.annotation.ProxyComponent;

/**
 * Solon 的事务，只支持 XController, XService, XDao ，且只支持注在函数上（算是较为克制）
 * */
@ProxyComponent
public class MasterSlaveService {
    @Db
    SQLManager sqlManager;

    @Db
   MasterSlaveUserInfoMapper userInfoMapper;

    @Tran
    public void test(){
        userInfoMapper.deleteById(19999);
        sqlManager.single(UserInfo.class,1);
        userInfoMapper.single(1);
    }
}
