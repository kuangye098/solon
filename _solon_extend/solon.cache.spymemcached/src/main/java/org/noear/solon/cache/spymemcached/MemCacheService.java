package org.noear.solon.cache.spymemcached;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.data.cache.CacheService;

import java.io.IOException;
import java.util.Properties;

/**
 * MemCache 封装的缓存服务
 *
 * @author noear
 * @since 1.3
 */
public class MemCacheService implements CacheService {
    protected String _cacheKeyHead;
    protected int _defaultSeconds;

    protected final MemcachedClient client;

    public MemCacheService(MemcachedClient client, String keyHeader, int defSeconds){
        this.client = client;

        _cacheKeyHead = keyHeader;
        _defaultSeconds = defSeconds;

        if (_defaultSeconds < 1) {
            _defaultSeconds = 30;
        }
    }

    public MemCacheService(Properties prop) {
        this(prop, prop.getProperty("keyHeader"), 0);
    }

    public MemCacheService(Properties prop, String keyHeader, int defSeconds) {
        String defSeconds_str = prop.getProperty("defSeconds");
        String server = prop.getProperty("server");
        String user = prop.getProperty("user");
        String password = prop.getProperty("password");

        if (defSeconds == 0) {
            if (Utils.isNotEmpty(defSeconds_str)) {
                defSeconds = Integer.parseInt(defSeconds_str);
            }
        }

        if (Utils.isEmpty(keyHeader)) {
            keyHeader = Solon.cfg().appName();
        }


        _cacheKeyHead = keyHeader;
        _defaultSeconds = defSeconds;

        if (_defaultSeconds < 1) {
            _defaultSeconds = 30;
        }

        if (Utils.isEmpty(_cacheKeyHead)) {
            _cacheKeyHead = Solon.cfg().appName();
        }

        ConnectionFactoryBuilder builder = new ConnectionFactoryBuilder();
        builder.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY);

        try {
            if (Utils.isNotEmpty(user) && Utils.isNotEmpty(password)) {
                AuthDescriptor ad = new AuthDescriptor(new String[]{"PLAIN"},
                        new PlainCallbackHandler(user, password));

                builder.setAuthDescriptor(ad);
            }

            client = new MemcachedClient(builder.build(), AddrUtil.getAddresses(server));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public MemcachedClient client() {
        return client;
    }

    @Override
    public void store(String key, Object obj, int seconds) {
        if (obj == null) {
            return;
        }

        if (seconds < 1) {
            seconds = _defaultSeconds;
        }

        String newKey = newKey(key);

        try {
            client.set(newKey, seconds, obj);
        } catch (Exception e) {
            EventBus.pushTry(e);
        }
    }

    @Override
    public Object get(String key) {
        String newKey = newKey(key);

        try {
            return client.get(newKey);
        } catch (Exception e) {
            EventBus.pushTry(e);
            return null;
        }
    }

    @Override
    public void remove(String key) {
        String newKey = newKey(key);

        client.delete(newKey);
    }


    protected String newKey(String key) {
        return _cacheKeyHead + "$" + Utils.md5(key);
    }
}
